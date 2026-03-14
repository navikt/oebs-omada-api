package no.nav.oebs.api.scim.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.oebs.api.db.entity.KallLogg;
import no.nav.oebs.api.scim.KallLoggHelper;
import no.nav.oebs.api.scim.service.ScimGroupService;
import no.nav.security.token.support.core.api.Protected;
import no.nav.security.token.support.core.api.Unprotected;
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
 * GET/find krever token (@Protected), create/update/patch/delete er åpne (@Unprotected).
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
    @Protected
    public ScimGroup get(String id) throws ResourceException {
        log.debug("GET Group: id={}", id);
        long startTid = System.currentTimeMillis();

        Optional<ScimGroup> group = groupService.getGroup(id);

        long kalltid = System.currentTimeMillis() - startTid;
        if (group.isEmpty()) {
            kallLoggHelper.loggInn(KallLogg.METHOD_GET, "/scim/v2/Groups/" + id, 404, kalltid, null, "Group not found");
            return null;
        }

        kallLoggHelper.loggInn(KallLogg.METHOD_GET, "/scim/v2/Groups/" + id, 200, kalltid, null, null);
        return group.get();
    }

    @Override
    @Protected
    @SuppressWarnings("NullableProblems")
    public FilterResponse<ScimGroup> find(Filter filter, PageRequest pageRequest, SortRequest sortRequest) {
        int startIndex = pageRequest != null && pageRequest.getStartIndex() != null
                ? pageRequest.getStartIndex() : 1;
        int count = pageRequest != null && pageRequest.getCount() != null
                ? pageRequest.getCount() : 100;

        log.debug("LIST Groups: startIndex={}, count={}", startIndex, count);
        long startTid = System.currentTimeMillis();

        Page<ScimGroup> groupPage = groupService.getGroups(startIndex, count);

        long kalltid = System.currentTimeMillis() - startTid;
        kallLoggHelper.loggInn(KallLogg.METHOD_GET, "/scim/v2/Groups", 200, kalltid, null,
                "totalResults=" + groupPage.getTotalElements());

        return new FilterResponse<>(groupPage.getContent(), pageRequest, (int) groupPage.getTotalElements());
    }

    @Override
    @Unprotected
    public ScimGroup create(ScimGroup resource) {
        throw new UnsupportedOperationException("Groups er read-only — POST ikke støttet");
    }

    @Override
    @Unprotected
    public ScimGroup update(String id, String version, ScimGroup resource,
                            Set<AttributeReference> includedAttributes,
                            Set<AttributeReference> excludedAttributes) {
        throw new UnsupportedOperationException("Groups er read-only — PUT ikke støttet");
    }

    @Override
    @Unprotected
    public ScimGroup patch(String id, String version, List<PatchOperation> patchOperations,
                           Set<AttributeReference> includedAttributes,
                           Set<AttributeReference> excludedAttributes) {
        throw new UnsupportedOperationException("Groups er read-only — PATCH ikke støttet");
    }

    @Override
    @Unprotected
    public void delete(String id) {
        throw new UnsupportedOperationException("Groups er read-only — DELETE ikke støttet");
    }
}


