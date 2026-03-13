package no.nav.oebs.api.scim.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.oebs.api.db.entity.KallLogg;
import no.nav.oebs.api.db.repository.PlsqlProcedureRepository;
import no.nav.oebs.api.db.repository.PlsqlProcedureResult;
import no.nav.oebs.api.scim.extension.NavOebsExtension;
import no.nav.oebs.api.scim.KallLoggHelper;
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
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * SCIMple Repository<ScimUser> — implementerer alle SCIM User-operasjoner.
 * Registreres automatisk i SCIMples RepositoryRegistry via Spring autoconfiguration.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScimUserResourceProvider implements Repository<ScimUser> {

    private static final String PLSQL_PROCEDURE_NAME = "XXRTV_INT_OMADA_INSERT_MESSAGE.InsertOmadaMessage";

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
        return List.of(NavOebsExtension.class);
    }

    @Override
    public ScimUser get(String id) throws ResourceException {
        log.debug("GET User: id={}", id);
        long startTid = System.currentTimeMillis();

        Optional<ScimUser> user = userService.getUser(id);

        long kalltid = System.currentTimeMillis() - startTid;
        if (user.isEmpty()) {
            kallLoggHelper.loggInn(KallLogg.METHOD_GET, "/scim/v2/Users/" + id, 404, kalltid, null, "User not found");
            return null;
        }

        String responseJson = toJson(user.get());
        kallLoggHelper.loggInn(KallLogg.METHOD_GET, "/scim/v2/Users/" + id, 200, kalltid, responseJson, null);
        return user.get();
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public FilterResponse<ScimUser> find(Filter filter, PageRequest pageRequest, SortRequest sortRequest) {
        int startIndex = pageRequest != null && pageRequest.getStartIndex() != null
                ? pageRequest.getStartIndex() : 1;
        int count = pageRequest != null && pageRequest.getCount() != null
                ? pageRequest.getCount() : 100;

        log.debug("LIST Users: startIndex={}, count={}", startIndex, count);
        long startTid = System.currentTimeMillis();

        Page<ScimUser> userPage = userService.getUsers(startIndex, count);

        long kalltid = System.currentTimeMillis() - startTid;
        kallLoggHelper.loggInn(KallLogg.METHOD_GET, "/scim/v2/Users", 200, kalltid, null,
                "totalResults=" + userPage.getTotalElements());

        return new FilterResponse<>(userPage.getContent(), pageRequest, (int) userPage.getTotalElements());
    }

    @Override
    public ScimUser create(ScimUser resource) {
        log.info("CREATE User: userName={}", resource.getUserName());
        long startTid = System.currentTimeMillis();

        String userJson = toJson(resource);
        PlsqlProcedureResult result = plsqlRepository.executeInOutProcedure(PLSQL_PROCEDURE_NAME, userJson);
        long kalltid = System.currentTimeMillis() - startTid;

        log.info("CREATE User fullført: messageNumber={}, message={}", result.getMessageNumber(), result.getMessage());
        kallLoggHelper.loggUt(KallLogg.METHOD_POST, "/scim/v2/Users",
                result.getMessageNumber(), kalltid, userJson, result.getData(), result.getMessage());

        return resource;
    }

    @Override
    public ScimUser update(String id, String version, ScimUser resource,
                           Set<AttributeReference> includedAttributes,
                           Set<AttributeReference> excludedAttributes) {
        log.info("UPDATE User: id={}", id);
        long startTid = System.currentTimeMillis();

        String userJson = toJson(resource);
        PlsqlProcedureResult result = plsqlRepository.executeInOutProcedure(PLSQL_PROCEDURE_NAME, userJson);
        long kalltid = System.currentTimeMillis() - startTid;

        log.info("UPDATE User fullført: messageNumber={}, message={}", result.getMessageNumber(), result.getMessage());
        kallLoggHelper.loggUt(KallLogg.METHOD_PUT, "/scim/v2/Users/" + id,
                result.getMessageNumber(), kalltid, userJson, result.getData(), result.getMessage());

        return resource;
    }

    @Override
    public ScimUser patch(String id, String version, List<PatchOperation> patchOperations,
                          Set<AttributeReference> includedAttributes,
                          Set<AttributeReference> excludedAttributes) {
        throw new UnsupportedOperationException("PATCH is not supported for Users");
    }

    @Override
    public void delete(String id) {
        log.info("DELETE User: id={}", id);
        long startTid = System.currentTimeMillis();

        PlsqlProcedureResult result = plsqlRepository.executeInOutProcedure(PLSQL_PROCEDURE_NAME, id);
        long kalltid = System.currentTimeMillis() - startTid;

        log.info("DELETE User fullført: messageNumber={}, message={}", result.getMessageNumber(), result.getMessage());
        kallLoggHelper.loggUt(KallLogg.METHOD_DELETE, "/scim/v2/Users/" + id,
                result.getMessageNumber(), kalltid, id, result.getData(), result.getMessage());
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Kunne ikke serialisere til JSON", e);
            return null;
        }
    }
}

