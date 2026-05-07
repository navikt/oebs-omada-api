package no.nav.oebs.api.scim.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.oebs.api.db.entity.KallLogg;
import no.nav.oebs.api.db.repository.PlsqlProcedureRepository;
import no.nav.oebs.api.db.repository.PlsqlProcedureRepository.Operasjon;
import no.nav.oebs.api.db.repository.PlsqlProcedureResult;
import no.nav.oebs.api.scim.extension.NavOebsExtension;
import no.nav.oebs.api.scim.KallLoggHelper;
import org.apache.directory.scim.spec.extension.EnterpriseExtension;
import no.nav.oebs.api.scim.service.ScimUserService;
import org.apache.directory.scim.core.repository.Repository;
import org.apache.directory.scim.spec.exception.ResourceException;
import org.apache.directory.scim.spec.filter.Filter;
import org.apache.directory.scim.spec.filter.FilterResponse;
import org.apache.directory.scim.spec.filter.PageRequest;
import org.apache.directory.scim.spec.filter.SortRequest;
import org.apache.directory.scim.spec.filter.attribute.AttributeReference;
import org.apache.directory.scim.spec.patch.PatchOperation;
import org.apache.directory.scim.spec.resources.ScimExtension;
import org.apache.directory.scim.spec.resources.ScimUser;
import org.apache.directory.scim.spec.schema.Meta;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * SCIMple Repository<ScimUser> — implementerer alle SCIM User-operasjoner.
 * Registreres automatisk i SCIMples RepositoryRegistry via Spring autoconfiguration.
 * Tilgangskontroll håndteres av ScimTokenValidationFilter i Jersey-laget.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScimUserResourceProvider implements Repository<ScimUser> {

    @Value("${oebs.plsql.insert-procedure:XXRTV_INT_OMADA_INSERT_MESSAGE.InsertOmadaMessage}")
    private String plsqlProcedureName;

    @Value("${oebs.plsql.sync-procedure:XXRTV_OMADA_JSON_IDENT_PKG.start_import_ident_melding}")
    private String plsqlSyncProcedureName;

    @Value("${oebs.scim.sync-enabled:true}")
    private boolean syncEnabled;

    private final ScimUserService userService;
    private final PlsqlProcedureRepository plsqlRepository;
    private final KallLoggHelper kallLoggHelper;

    private static final ObjectMapper objectMapper = JsonMapper.builder()
            .findAndAddModules()
            .enable(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS.mappedFeature())
            .build();

    @Override
    public Class<ScimUser> getResourceClass() {
        return ScimUser.class;
    }

    @Override
    public List<Class<? extends ScimExtension>> getExtensionList() {
        return List.of(NavOebsExtension.class, EnterpriseExtension.class);
    }

    @Override
    public ScimUser get(String id) throws ResourceException {
        log.debug("GET User: id={}", id);
        long startTid = System.currentTimeMillis();
        try {
            Optional<ScimUser> user = userService.getUser(id);
            long kalltid = System.currentTimeMillis() - startTid;
            if (user.isEmpty()) {
                kallLoggHelper.loggInn(KallLogg.METHOD_GET, "/scim/v2/Users/" + id, 404, kalltid, null, "User not found");
                return null;
            }
            String responseJson = toJson(user.get());
            kallLoggHelper.loggInn(KallLogg.METHOD_GET, "/scim/v2/Users/" + id, 200, kalltid, responseJson, null);
            return user.get();
        } catch (Exception e) {
            long kalltid = System.currentTimeMillis() - startTid;
            log.error("GET User FEIL: id={}", id, e);
            kallLoggHelper.loggInn(KallLogg.METHOD_GET, "/scim/v2/Users/" + id, 500, kalltid,
                    errorJson(500, e.getMessage()), null);
            throw new ResourceException(500, "Intern feil: " + e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public FilterResponse<ScimUser> find(Filter filter, PageRequest pageRequest, SortRequest sortRequest) {
        int startIndex = pageRequest != null && pageRequest.getStartIndex() != null ? pageRequest.getStartIndex() : 1;
        int count      = pageRequest != null && pageRequest.getCount()      != null ? pageRequest.getCount()      : 100;
        log.debug("LIST Users: startIndex={}, count={}", startIndex, count);
        long startTid = System.currentTimeMillis();
        try {
            Page<ScimUser> userPage = userService.getUsers(startIndex, count);
            long kalltid = System.currentTimeMillis() - startTid;
            kallLoggHelper.loggInn(KallLogg.METHOD_GET, "/scim/v2/Users", 200, kalltid, null,
                    "totalResults=" + userPage.getTotalElements() + " returnert=" + userPage.getNumberOfElements());
            return new FilterResponse<>(userPage.getContent(), pageRequest, (int) userPage.getTotalElements());
        } catch (Exception e) {
            long kalltid = System.currentTimeMillis() - startTid;
            log.error("LIST Users FEIL", e);
            kallLoggHelper.loggInn(KallLogg.METHOD_GET, "/scim/v2/Users", 500, kalltid,
                    errorJson(500, e.getMessage()), null);
            throw new RuntimeException("Intern feil: " + e.getMessage(), e);
        }
    }

    @Override
    public ScimUser create(ScimUser resource) throws ResourceException {
        log.info("CREATE User: userName={}, id={}", resource.getUserName(), resource.getId());
        long startTid = System.currentTimeMillis();
        try {
            if (resource.getId() == null || resource.getId().isBlank()) {
                if (resource.getExternalId() != null && !resource.getExternalId().isBlank()) {
                    log.warn("CREATE User: id mangler, bruker externalId={} som id", resource.getExternalId());
                    resource.setId(resource.getExternalId());
                } else {
                    log.error("CREATE User: verken id eller externalId er satt");
                    long kalltid = System.currentTimeMillis() - startTid;
                    kallLoggHelper.loggInn(KallLogg.METHOD_POST, "/scim/v2/Users", 400, kalltid,
                            toJson(resource), errorJson(400, "id eller externalId må være satt"), null);
                    throw new ResourceException(400, "id eller externalId må være satt");
                }
            }
            String userJson = toJson(resource);
            PlsqlProcedureResult result = plsqlRepository.executeInOutProcedure(plsqlProcedureName, Operasjon.NY, userJson);
            long kalltid = System.currentTimeMillis() - startTid;
            int httpStatus = result.getMessageNumber() < 0 ? 500 : 201;
            kallLoggHelper.loggInn(KallLogg.METHOD_POST, "/scim/v2/Users", httpStatus, kalltid, userJson, null, null);
            kallLoggHelper.loggUt(KallLogg.METHOD_POST, plsqlProcedureName, httpStatus, kalltid, userJson, plsqlResponse(result), result.getMessage());
            checkResult(result, "CREATE", resource.getId());
            executeSyncIfPresent("CREATE", resource.getId(), result);
            log.info("CREATE User OK: id={}", resource.getId());
            return fetchFromViewOrFallback("CREATE", resource.getId(), resource);
        } catch (ResourceException e) {
            throw e;
        } catch (Exception e) {
            long kalltid = System.currentTimeMillis() - startTid;
            log.error("CREATE User FEIL: id={}", resource.getId(), e);
            kallLoggHelper.loggInn(KallLogg.METHOD_POST, "/scim/v2/Users", 500, kalltid,
                    toJson(resource), errorJson(500, e.getMessage()), null);
            kallLoggHelper.loggUt(KallLogg.METHOD_POST, plsqlProcedureName, 500, kalltid,
                    toJson(resource), errorJson(500, e.getMessage()), null);
            throw new ResourceException(500, "Intern feil: " + e.getMessage());
        }
    }

    @Override
    public ScimUser update(String id, String version, ScimUser resource,
                           Set<AttributeReference> includedAttributes,
                           Set<AttributeReference> excludedAttributes) throws ResourceException {
        log.info("UPDATE User: id={}", id);
        long startTid = System.currentTimeMillis();
        try {
            if (resource == null) {
                log.error("UPDATE User: resource er null for id={}", id);
                kallLoggHelper.loggInn(KallLogg.METHOD_PUT, "/scim/v2/Users/" + id, 400, 0,
                        null, errorJson(400, "Request-body mangler"), null);
                throw new ResourceException(400, "Request-body mangler eller kunne ikke deserialiseres");
            }
            resource.setId(id);
            String userJson = toJson(resource);
            PlsqlProcedureResult result = plsqlRepository.executeInOutProcedure(plsqlProcedureName, Operasjon.ENDRE, userJson);
            long kalltid = System.currentTimeMillis() - startTid;
            int httpStatus = result.getMessageNumber() < 0 ? 500 : 200;
            kallLoggHelper.loggInn(KallLogg.METHOD_PUT, "/scim/v2/Users/" + id, httpStatus, kalltid, userJson, null, null);
            kallLoggHelper.loggUt(KallLogg.METHOD_PUT, plsqlProcedureName, httpStatus, kalltid, userJson, plsqlResponse(result), result.getMessage());
            checkResult(result, "UPDATE", id);
            executeSyncIfPresent("UPDATE", id, result);
            log.info("UPDATE User OK: id={}", id);
            return fetchFromViewOrFallback("UPDATE", id, resource);
        } catch (ResourceException e) {
            throw e;
        } catch (Exception e) {
            long kalltid = System.currentTimeMillis() - startTid;
            log.error("UPDATE User FEIL: id={}", id, e);
            kallLoggHelper.loggInn(KallLogg.METHOD_PUT, "/scim/v2/Users/" + id, 500, kalltid,
                    toJson(resource), errorJson(500, e.getMessage()), null);
            kallLoggHelper.loggUt(KallLogg.METHOD_PUT, plsqlProcedureName, 500, kalltid,
                    toJson(resource), errorJson(500, e.getMessage()), null);
            throw new ResourceException(500, "Intern feil: " + e.getMessage());
        }
    }

    @Override
    public ScimUser patch(String id, String version, List<PatchOperation> patchOperations,
                          Set<AttributeReference> includedAttributes,
                          Set<AttributeReference> excludedAttributes) throws ResourceException {
        long startTid = System.currentTimeMillis();
        long kalltid = System.currentTimeMillis() - startTid;
        kallLoggHelper.loggInn(KallLogg.METHOD_PATCH, "/scim/v2/Users/" + id, 501, kalltid,
                null, errorJson(501, "PATCH ikke støttet"), null);
        throw new ResourceException(501, "PATCH er ikke støttet for Users");
    }

    @Override
    public void delete(String id) throws ResourceException {
        log.info("DELETE User: id={}", id);
        long startTid = System.currentTimeMillis();
        try {
            String deleteJson = String.format("{\"id\":\"%s\"}", id);
            PlsqlProcedureResult result = plsqlRepository.executeInOutProcedure(plsqlProcedureName, Operasjon.SLETTE, deleteJson);
            long kalltid = System.currentTimeMillis() - startTid;
            int httpStatus = result.getMessageNumber() < 0 ? 500 : 204;
            kallLoggHelper.loggInn(KallLogg.METHOD_DELETE, "/scim/v2/Users/" + id, httpStatus, kalltid, deleteJson, null, null);
            kallLoggHelper.loggUt(KallLogg.METHOD_DELETE, plsqlProcedureName, httpStatus, kalltid, deleteJson, plsqlResponse(result), result.getMessage());
            checkResult(result, "DELETE", id);
            executeSyncIfPresent("DELETE", id, result);
            log.info("DELETE User OK: id={}", id);
        } catch (ResourceException e) {
            throw e;
        } catch (Exception e) {
            long kalltid = System.currentTimeMillis() - startTid;
            log.error("DELETE User FEIL: id={}", id, e);
            String deleteJson = String.format("{\"id\":\"%s\"}", id);
            kallLoggHelper.loggInn(KallLogg.METHOD_DELETE, "/scim/v2/Users/" + id, 500, kalltid,
                    deleteJson, errorJson(500, e.getMessage()), null);
            kallLoggHelper.loggUt(KallLogg.METHOD_DELETE, plsqlProcedureName, 500, kalltid,
                    deleteJson, errorJson(500, e.getMessage()), null);
            throw new ResourceException(500, "Intern feil: " + e.getMessage());
        }
    }

    private void executeSyncIfPresent(String operasjon, String id,
                                      PlsqlProcedureResult result) throws ResourceException {
        if (result.getInterfaceMsgId() == null) return;

        if (!syncEnabled) {
            log.info("{} User: sync deaktivert (SCIM_SYNC_ENABLED=false) – melding id={} er lagret i tabell, synkronisering hoppes over",
                    operasjon, result.getInterfaceMsgId());
            return;
        }

        log.info("{} User: interfaceMsgId={} – kaller synkron prosedyre", operasjon, result.getInterfaceMsgId());
        long syncStart = System.currentTimeMillis();

        PlsqlProcedureResult syncResult;
        try {
            syncResult = plsqlRepository.executeSyncProcedure(
                    plsqlSyncProcedureName, result.getInterfaceMsgId());
        } catch (Exception e) {
            long syncKalltid = System.currentTimeMillis() - syncStart;
            log.error("{} User SYNC FEIL: id={}, interfaceMsgId={}", operasjon, id, result.getInterfaceMsgId(), e);
            // Log with interfaceMsgId in request so it is always traceable in KallLogg even on failure
            kallLoggHelper.loggUt(KallLogg.METHOD_POST, plsqlSyncProcedureName, 500, syncKalltid,
                    String.valueOf(result.getInterfaceMsgId()),
                    errorJson(500, e.getMessage()),
                    "interfaceMsgId=" + result.getInterfaceMsgId());
            throw new ResourceException(500, "Intern feil: " + e.getMessage());
        }

        long syncKalltid = System.currentTimeMillis() - syncStart;

        boolean pending = isSyncPending(syncResult);
        int syncStatus = syncResult.getMessageNumber() < 0
                ? mapSyncRetcodeToHttpStatus(syncResult.getRetcode())
                : pending ? 202 : syncSuccessHttpStatus(operasjon);

        kallLoggHelper.loggUt(KallLogg.METHOD_POST, plsqlSyncProcedureName, syncStatus,
                syncKalltid, String.valueOf(result.getInterfaceMsgId()),
                plsqlSyncResponse(syncResult), syncResult.getMessage());

        if (pending) {
            log.info("{} User SYNC akseptert (202): id={}, interfaceMsgId={}, dev_phase={}, dev_status={}",
                    operasjon, id, result.getInterfaceMsgId(), syncResult.getDevPhase(), syncResult.getDevStatus());
            throw new ResourceException(202,
                    "Synkronisering er akseptert og pågår (dev_phase=" + syncResult.getDevPhase()
                    + ", dev_status=" + syncResult.getDevStatus() + ")");
        }

        checkSyncResult(syncResult, operasjon + "_SYNC", id);

        log.info("{} User synkron OK: id={}, interfaceMsgId={}, tid={}ms",
                operasjon, id, result.getInterfaceMsgId(), syncKalltid);
    }

    private ScimUser fetchFromViewOrFallback(String operasjon, String id, ScimUser fallback) {
        try {
            return userService.getUser(id)
                    .map(u -> {
                        log.debug("{} User: returnerer bruker fra view: id={}", operasjon, id);
                        return u;
                    })
                    .orElseGet(() -> {
                        log.warn("{} User: bruker ikke funnet i view etter sync — returnerer innsendt objekt: id={}", operasjon, id);
                        ensureMeta(fallback).setVersion("W/\"" + id.hashCode() + "\"");
                        return fallback;
                    });
        } catch (Exception e) {
            log.warn("{} User: feil ved henting fra view — returnerer innsendt objekt: id={}", operasjon, id, e);
            ensureMeta(fallback).setVersion("W/\"" + id.hashCode() + "\"");
            return fallback;
        }
    }

    private void checkResult(PlsqlProcedureResult result, String operasjon, String id) throws ResourceException {
        if (result.getMessageNumber() < 0) {
            int httpStatus = toHttpStatus(result.getRetcode());
            log.error("{} User FEIL: id={}, retcode={}, errbuf={}", operasjon, id, result.getRetcode(), result.getMessage());
            throw new ResourceException(httpStatus, result.getMessage());
        }
        if (result.getMessageNumber() > 0) {
            log.warn("{} User advarsel: id={}, retcode={}, errbuf={}", operasjon, id, result.getRetcode(), result.getMessage());
        }
    }

    /**
     * Sjekker resultat fra sync-prosedyren og kaster ResourceException med korrekt
     * HTTP-status og errbuf-melding fra databasen inkludert i API-svaret.
     *
     * Konvensjon for sync (Oracle concurrent program):
     *   retcode=0 → suksess
     *   retcode=1 → advarsel  → HTTP 422 Unprocessable Content
     *   retcode=2+ → feil     → HTTP 500 Internal Server Error
     */
    private void checkSyncResult(PlsqlProcedureResult result, String operasjon, String id) throws ResourceException {
        if (result.getMessageNumber() < 0) {
            int httpStatus = mapSyncRetcodeToHttpStatus(result.getRetcode());
            String errbuf = result.getMessage() != null ? result.getMessage() : "Synkronisering feilet uten feilmelding";
            if (httpStatus == 422) {
                log.warn("{} User SYNC advarsel: id={}, retcode={}, errbuf={}", operasjon, id, result.getRetcode(), errbuf);
            } else {
                log.error("{} User SYNC FEIL: id={}, retcode={}, errbuf={}", operasjon, id, result.getRetcode(), errbuf);
            }
            throw new ResourceException(httpStatus,
                    "Synkronisering mot OEBS feilet (retcode=" + result.getRetcode() + "): " + errbuf);
        }
        if (result.getMessageNumber() > 0) {
            log.warn("{} User SYNC advarsel: id={}, retcode={}, errbuf={}", operasjon, id, result.getRetcode(), result.getMessage());
        }
    }

    /**
     * Mapper Oracle concurrent program retcode til HTTP-statuskode:
     *   1  → 422 Unprocessable Content (advarsel — melding er lagret men synkronisering hadde avvik)
     *   2+ → 500 Internal Server Error  (feil — synkronisering feilet)
     * Gyldige HTTP-koder (200–599) i retcode brukes direkte (fremtidssikring).
     */
    static int mapSyncRetcodeToHttpStatus(String retcode) {
        try {
            int code = Integer.parseInt(retcode);
            if (code >= 200 && code <= 599) return code;
            return code == 1 ? 422 : 500;
        } catch (NumberFormatException | NullPointerException ignored) {
            return 500;
        }
    }

    static int syncSuccessHttpStatus(String operasjon) {
        return "CREATE".equalsIgnoreCase(operasjon) ? 201 : 200;
    }

    static boolean isSyncPending(PlsqlProcedureResult result) {
        try {
            int retcodeInt = Integer.parseInt(result.getRetcode() != null ? result.getRetcode() : "0");
            if (retcodeInt >= 2) return false;
        } catch (NumberFormatException ignored) {
            return false;
        }
        String phase  = result.getDevPhase()  != null ? result.getDevPhase().toUpperCase()  : "";
        String status = result.getDevStatus() != null ? result.getDevStatus().toUpperCase() : "";
        boolean pendingPhase  = phase.equals("PENDING")  || phase.equals("RUNNING");
        boolean pendingStatus = status.equals("STANDBY") || status.equals("NORMAL");
        return pendingPhase && pendingStatus;
    }

    private int toHttpStatus(String retcode) {
        try {
            int code = Integer.parseInt(retcode);
            if (code >= 200 && code <= 599) return code;
        } catch (NumberFormatException | NullPointerException ignored) {}
        return 500;
    }

    private Meta ensureMeta(ScimUser resource) {
        if (resource.getMeta() == null) {
            resource.setMeta(new Meta());
        }
        return resource.getMeta();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Kunne ikke serialisere til JSON", e);
            return null;
        }
    }

    /**
     * Bygger et JSON-svar med alle ut-parametre fra InsertOmadaMessage for lagring i RESPONSE-kolonnen.
     * retcode      = Oracle-retcode fra prosedyren
     * interfaceMsgId = tildelt meldingsid
     * errbuf       = feilmelding / logginfo fra Oracle
     * data         = eventuelt returdata
     */
    private String plsqlResponse(PlsqlProcedureResult result) {
        String errbuf = result.getMessage() != null ? "\"" + result.getMessage().replace("\"", "'") + "\"" : "null";
        String data   = result.getData()    != null ? "\"" + result.getData().replace("\"", "'")    + "\"" : "null";
        return String.format("{\"retcode\":\"%s\",\"interfaceMsgId\":%s,\"errbuf\":%s,\"data\":%s}",
                result.getRetcode() != null ? result.getRetcode() : "",
                result.getInterfaceMsgId() != null ? result.getInterfaceMsgId() : "null",
                errbuf, data);
    }

    /**
     * Bygger et JSON-svar med alle ut-parametre fra start_import_ident_melding for RESPONSE-kolonnen.
     * retcode   = Oracle concurrent program-retcode (0=OK, 1=advarsel, 2+=feil)
     * devPhase  / devStatus = concurrent request-fase
     * errbuf    = feilmelding / logginfo fra Oracle
     */
    private String plsqlSyncResponse(PlsqlProcedureResult result) {
        String errbuf = result.getMessage() != null ? "\"" + result.getMessage().replace("\"", "'") + "\"" : "null";
        return String.format("{\"retcode\":\"%s\",\"devPhase\":\"%s\",\"devStatus\":\"%s\",\"errbuf\":%s}",
                result.getRetcode()   != null ? result.getRetcode()   : "",
                result.getDevPhase()  != null ? result.getDevPhase()  : "",
                result.getDevStatus() != null ? result.getDevStatus() : "",
                errbuf);
    }

    private String errorJson(int status, String detail) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "schemas", List.of("urn:ietf:params:scim:api:messages:2.0:Error"),
                    "status", String.valueOf(status),
                    "detail", detail != null ? detail : "Ukjent feil"
            ));
        } catch (JsonProcessingException e) {
            log.warn("Kunne ikke serialisere SCIM-feil til JSON", e);
            return "{\"schemas\":[\"urn:ietf:params:scim:api:messages:2.0:Error\"],\"status\":\"500\",\"detail\":\"Ukjent feil\"}";
        }
    }
}
