package no.nav.oebs.api.scim.provider;

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
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
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

    private final ScimGroupService groupService;
    private final KallLoggHelper kallLoggHelper;

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
                kallLoggHelper.loggInn(KallLogg.METHOD_GET, "/scim/v2/Groups/" + id, 404, kalltid, null, "Group not found");
                return null;
            }
            kallLoggHelper.loggInn(KallLogg.METHOD_GET, "/scim/v2/Groups/" + id, 200, kalltid, null, null);
            return group.get();
        } catch (Exception e) {
            long kalltid = System.currentTimeMillis() - startTid;
            log.error("GET Group FEIL: id={}", id, e);
            kallLoggHelper.loggInn(KallLogg.METHOD_GET, "/scim/v2/Groups/" + id, 500, kalltid,
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
            kallLoggHelper.loggInn(KallLogg.METHOD_GET, "/scim/v2/Groups", 200, kalltid, null,
                    "totalResults=" + groupPage.getTotalElements());
            return new FilterResponse<>(groupPage.getContent(), pageRequest, (int) groupPage.getTotalElements());
        } catch (Exception e) {
            long kalltid = System.currentTimeMillis() - startTid;
            log.error("LIST Groups FEIL", e);
            kallLoggHelper.loggInn(KallLogg.METHOD_GET, "/scim/v2/Groups", 500, kalltid,
                    errorJson(500, e.getMessage()), null);
            throw new RuntimeException("Intern feil: " + e.getMessage(), e);
        }
    }

    @Override
    public ScimGroup create(ScimGroup resource) {
        kallLoggHelper.loggInn(KallLogg.METHOD_POST, "/scim/v2/Groups", 405, 0,
                errorJson(405, "Groups er read-only — POST ikke støttet"), null);
        throw new UnsupportedOperationException("Groups er read-only — POST ikke støttet");
    }

    @Override
    public ScimGroup update(String id, String version, ScimGroup resource,
                            Set<AttributeReference> includedAttributes,
                            Set<AttributeReference> excludedAttributes) {
        kallLoggHelper.loggInn(KallLogg.METHOD_PUT, "/scim/v2/Groups/" + id, 405, 0,
                errorJson(405, "Groups er read-only — PUT ikke støttet"), null);
        throw new UnsupportedOperationException("Groups er read-only — PUT ikke støttet");
    }

    @Override
    public ScimGroup patch(String id, String version, List<PatchOperation> patchOperations,
                           Set<AttributeReference> includedAttributes,
                           Set<AttributeReference> excludedAttributes) {
        kallLoggHelper.loggInn(KallLogg.METHOD_PUT, "/scim/v2/Groups/" + id, 405, 0,
                errorJson(405, "Groups er read-only — PATCH ikke støttet"), null);
        throw new UnsupportedOperationException("Groups er read-only — PATCH ikke støttet");
    }

    @Override
    public void delete(String id) {
        kallLoggHelper.loggInn(KallLogg.METHOD_DELETE, "/scim/v2/Groups/" + id, 405, 0,
                errorJson(405, "Groups er read-only — DELETE ikke støttet"), null);
        throw new UnsupportedOperationException("Groups er read-only — DELETE ikke støttet");
    }

    private String errorJson(int status, String detail) {
        return String.format(
            "{\"schemas\":[\"urn:ietf:params:scim:api:messages:2.0:Error\"],\"status\":\"%d\",\"detail\":\"%s\"}",
            status, detail != null ? detail.replace("\"", "'") : "Ukjent feil");
    }
}


