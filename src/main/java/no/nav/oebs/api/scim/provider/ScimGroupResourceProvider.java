package no.nav.oebs.api.scim.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.oebs.api.db.entity.KallLogg;
import no.nav.oebs.api.scim.KallLoggHelper;
import no.nav.oebs.api.scim.service.ScimGroupService;
import org.apache.directory.scim.core.repository.Repository;
import org.apache.directory.scim.spec.exception.ResourceException;
import org.apache.directory.scim.spec.filter.Filter;
import org.apache.directory.scim.spec.filter.FilterResponse;
import org.apache.directory.scim.spec.filter.PageRequest;
import org.apache.directory.scim.spec.filter.SortRequest;
import org.apache.directory.scim.spec.filter.attribute.AttributeReference;
import org.apache.directory.scim.spec.patch.PatchOperation;
import org.apache.directory.scim.spec.resources.ScimGroup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * SCIMple Repository<ScimGroup> — read-only.
 * Registreres automatisk i SCIMples RepositoryRegistry via Spring autoconfiguration.
 * Tilgangskontroll håndteres av ScimTokenValidationFilter i Jersey-laget.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScimGroupResourceProvider implements Repository<ScimGroup> {

    @Value("${oebs.scim.groups-path:/scim/v2/Groups}")
    private String scimGroupsPath = "/scim/v2/Groups";

    @Value("${oebs.scim.group-item-path-prefix:/scim/v2/Groups/}")
    private String scimGroupsItemPathPrefix = "/scim/v2/Groups/";

    private final ScimGroupService groupService;
    private final KallLoggHelper kallLoggHelper;

    private static final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @Override
    public Class<ScimGroup> getResourceClass() {
        return ScimGroup.class;
    }

    @Override
    public ScimGroup get(String id) throws ResourceException {
        log.debug("GET Group: id={}", id);
        long startTid = System.currentTimeMillis();
        try {
            Optional<ScimGroup> group = groupService.getGroup(id);
            long kalltid = System.currentTimeMillis() - startTid;
            if (group.isEmpty()) {
                kallLoggHelper.loggInn(KallLogg.METHOD_GET, groupPath(id), 404, kalltid, null, "Group not found");
                return null;
            }
            String responseJson = toJson(group.get());
            kallLoggHelper.loggInn(KallLogg.METHOD_GET, groupPath(id), 200, kalltid, responseJson, null);
            return group.get();
        } catch (RuntimeException e) {
            long kalltid = System.currentTimeMillis() - startTid;
            log.error("GET Group FEIL: id={}", id, e);
            kallLoggHelper.loggInn(KallLogg.METHOD_GET, groupPath(id), 500, kalltid,
                    errorJson(500, e.getMessage()), null);
            throw new ResourceException(500, "Intern feil: " + e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public FilterResponse<ScimGroup> find(Filter filter, PageRequest pageRequest, SortRequest sortRequest) {
        int startIndex = pageRequest != null && pageRequest.getStartIndex() != null ? pageRequest.getStartIndex() : 1;
        int count      = pageRequest != null && pageRequest.getCount()      != null ? pageRequest.getCount()      : 100;
        log.debug("LIST Groups: startIndex={}, count={}", startIndex, count);
        long startTid = System.currentTimeMillis();
        try {
            Page<ScimGroup> groupPage = groupService.getGroups(startIndex, count);
            long kalltid = System.currentTimeMillis() - startTid;
            kallLoggHelper.loggInn(KallLogg.METHOD_GET, scimGroupsPath, 200, kalltid, null,
                    "totalResults=" + groupPage.getTotalElements() + " returnert=" + groupPage.getNumberOfElements());
            return new FilterResponse<>(groupPage.getContent(), pageRequest, (int) groupPage.getTotalElements());
        } catch (RuntimeException e) {
            long kalltid = System.currentTimeMillis() - startTid;
            log.error("LIST Groups FEIL", e);
            kallLoggHelper.loggInn(KallLogg.METHOD_GET, scimGroupsPath, 500, kalltid,
                    errorJson(500, e.getMessage()), null);
            throw new IllegalStateException("Intern feil: " + e.getMessage(), e);
        }
    }

    @Override
    public ScimGroup create(ScimGroup resource) throws ResourceException {
        kallLoggHelper.loggInn(KallLogg.METHOD_POST, scimGroupsPath, 405, 0,
                errorJson(405, "Groups er read-only — POST ikke støttet"), null);
        throw new ResourceException(405, "Groups er read-only — POST ikke støttet");
    }

    @Override
    public ScimGroup update(String id, String version, ScimGroup resource,
                            Set<AttributeReference> includedAttributes,
                            Set<AttributeReference> excludedAttributes) throws ResourceException {
        kallLoggHelper.loggInn(KallLogg.METHOD_PUT, groupPath(id), 405, 0,
                errorJson(405, "Groups er read-only — PUT ikke støttet"), null);
        throw new ResourceException(405, "Groups er read-only — PUT ikke støttet");
    }

    @Override
    public ScimGroup patch(String id, String version, List<PatchOperation> patchOperations,
                           Set<AttributeReference> includedAttributes,
                           Set<AttributeReference> excludedAttributes) throws ResourceException {
        kallLoggHelper.loggInn(KallLogg.METHOD_PUT, groupPath(id), 405, 0,
                errorJson(405, "Groups er read-only — PATCH ikke støttet"), null);
        throw new ResourceException(405, "Groups er read-only — PATCH ikke støttet");
    }

    @Override
    public void delete(String id) throws ResourceException {
        kallLoggHelper.loggInn(KallLogg.METHOD_DELETE, groupPath(id), 405, 0,
                errorJson(405, "Groups er read-only — DELETE ikke støttet"), null);
        throw new ResourceException(405, "Groups er read-only — DELETE ikke støttet");
    }

    private String groupPath(String id) {
        return scimGroupsItemPathPrefix + id;
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
