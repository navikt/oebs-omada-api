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
                    kallLoggHelper.loggUt(KallLogg.METHOD_POST, "/scim/v2/Users", 400, kalltid,
                            toJson(resource), errorJson(400, "id eller externalId må være satt"), null);
                    throw new ResourceException(400, "id eller externalId må være satt");
                }
            }
            String userJson = toJson(resource);
            PlsqlProcedureResult result = plsqlRepository.executeInOutProcedure(plsqlProcedureName, Operasjon.NY, userJson);
            long kalltid = System.currentTimeMillis() - startTid;
            int httpStatus = result.getMessageNumber() < 0 ? 500 : 201;
            kallLoggHelper.loggUt(KallLogg.METHOD_POST, "/scim/v2/Users", httpStatus, kalltid, userJson, result.getData(), result.getMessage());
            checkResult(result, "CREATE", resource.getId());
            ensureMeta(resource).setVersion("W/\"" + resource.getId().hashCode() + "\"");
            log.info("CREATE User OK: id={}", resource.getId());
            return resource;
        } catch (ResourceException e) {
            throw e;
        } catch (Exception e) {
            long kalltid = System.currentTimeMillis() - startTid;
            log.error("CREATE User FEIL: id={}", resource.getId(), e);
            kallLoggHelper.loggUt(KallLogg.METHOD_POST, "/scim/v2/Users", 500, kalltid,
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
                kallLoggHelper.loggUt(KallLogg.METHOD_PUT, "/scim/v2/Users/" + id, 400, 0,
                        null, errorJson(400, "Request-body mangler"), null);
                throw new ResourceException(400, "Request-body mangler eller kunne ikke deserialiseres");
            }
            resource.setId(id);
            String userJson = toJson(resource);
            PlsqlProcedureResult result = plsqlRepository.executeInOutProcedure(plsqlProcedureName, Operasjon.ENDRE, userJson);
            long kalltid = System.currentTimeMillis() - startTid;
            int httpStatus = result.getMessageNumber() < 0 ? 500 : 200;
            kallLoggHelper.loggUt(KallLogg.METHOD_PUT, "/scim/v2/Users/" + id, httpStatus, kalltid, userJson, result.getData(), result.getMessage());
            checkResult(result, "UPDATE", id);
            ensureMeta(resource).setVersion("W/\"" + (id + System.currentTimeMillis()).hashCode() + "\"");
            log.info("UPDATE User OK: id={}", id);
            return resource;
        } catch (ResourceException e) {
            throw e;
        } catch (Exception e) {
            long kalltid = System.currentTimeMillis() - startTid;
            log.error("UPDATE User FEIL: id={}", id, e);
            kallLoggHelper.loggUt(KallLogg.METHOD_PUT, "/scim/v2/Users/" + id, 500, kalltid,
                    null, errorJson(500, e.getMessage()), null);
            throw new ResourceException(500, "Intern feil: " + e.getMessage());
        }
    }

    @Override
    public ScimUser patch(String id, String version, List<PatchOperation> patchOperations,
                          Set<AttributeReference> includedAttributes,
                          Set<AttributeReference> excludedAttributes) throws ResourceException {
        long startTid = System.currentTimeMillis();
        long kalltid = System.currentTimeMillis() - startTid;
        kallLoggHelper.loggUt(KallLogg.METHOD_PUT, "/scim/v2/Users/" + id, 501, kalltid,
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
            kallLoggHelper.loggUt(KallLogg.METHOD_DELETE, "/scim/v2/Users/" + id, httpStatus, kalltid, deleteJson, result.getData(), result.getMessage());
            checkResult(result, "DELETE", id);
            log.info("DELETE User OK: id={}", id);
        } catch (ResourceException e) {
            throw e;
        } catch (Exception e) {
            long kalltid = System.currentTimeMillis() - startTid;
            log.error("DELETE User FEIL: id={}", id, e);
            kallLoggHelper.loggUt(KallLogg.METHOD_DELETE, "/scim/v2/Users/" + id, 500, kalltid,
                    null, errorJson(500, e.getMessage()), null);
            throw new ResourceException(500, "Intern feil: " + e.getMessage());
        }
    }

    /**
     * Sjekker prosedyre-resultatet og kaster ResourceException ved feil.
     * retcode: 0 = OK, 1 = advarsel (behandles som OK), 2+ = feil
     */
    private void checkResult(PlsqlProcedureResult result, String operasjon, String id) throws ResourceException {
        if (result.getMessageNumber() < 0) {
            log.error("{} User FEIL: id={}, errbuf={}", operasjon, id, result.getMessage());
            throw new ResourceException(500, "Prosedyrefeil: " + result.getMessage());
        }
        if (result.getMessageNumber() > 0) {
            log.warn("{} User advarsel: id={}, errbuf={}", operasjon, id, result.getMessage());
        }
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

    private String errorJson(int status, String detail) {
        return String.format(
            "{\"schemas\":[\"urn:ietf:params:scim:api:messages:2.0:Error\"],\"status\":\"%d\",\"detail\":\"%s\"}",
            status, detail != null ? detail.replace("\"", "'") : "Ukjent feil");
    }
}
