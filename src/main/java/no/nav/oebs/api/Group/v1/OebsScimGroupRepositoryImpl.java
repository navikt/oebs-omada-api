package no.nav.oebs.api.Group.v1;

import org.apache.directory.scim.core.repository.Repository;
import org.apache.directory.scim.core.schema.SchemaRegistry;
import org.apache.directory.scim.spec.exception.ResourceException;
import org.apache.directory.scim.spec.filter.Filter;
import org.apache.directory.scim.spec.filter.FilterExpressions;
import org.apache.directory.scim.spec.filter.FilterResponse;
import org.apache.directory.scim.spec.filter.PageRequest;
import org.apache.directory.scim.spec.filter.SortRequest;
import org.apache.directory.scim.spec.filter.attribute.AttributeReference;
import org.apache.directory.scim.spec.patch.PatchOperation;
import org.apache.directory.scim.spec.resources.ScimExtension;
import org.apache.directory.scim.spec.resources.ScimGroup;
import org.apache.directory.scim.spec.resources.ScimResource;
import org.apache.directory.scim.spec.schema.Schema;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class OebsScimGroupRepositoryImpl implements Repository<ScimGroup> {

    private final OebsScimGroupViewRepository viewRepository;
    private final OebsScimGroupMapper mapper;
    private final SchemaRegistry schemaRegistry;

    public OebsScimGroupRepositoryImpl(OebsScimGroupViewRepository viewRepository,
                                       OebsScimGroupMapper mapper,
                                       SchemaRegistry schemaRegistry) {
        this.viewRepository = viewRepository;
        this.mapper = mapper;
        this.schemaRegistry = schemaRegistry;
    }

    @Override
    public Class<ScimGroup> getResourceClass() {
        return ScimGroup.class;
    }

    // ---------- WRITE: ikke støttet (view er read-only) ----------

    @Override
    public ScimGroup create(ScimGroup resource) throws ResourceException {
        throw new ResourceException(405, "Create group is not supported – underlying source is a read-only view");
    }

    @Override
    public ScimGroup update(String id,
                            String version,
                            ScimGroup resource,
                            Set<AttributeReference> includedAttributeReferences,
                            Set<AttributeReference> excludedAttributeReferences) throws ResourceException {
        throw new ResourceException(405, "Update group is not supported – underlying source is a read-only view");
    }

    @Override
    public ScimGroup patch(String id,
                           String version,
                           List<PatchOperation> patchOperations,
                           Set<AttributeReference> includedAttributeReferences,
                           Set<AttributeReference> excludedAttributeReferences) throws ResourceException {
        throw new ResourceException(405, "Patch group is not supported – underlying source is a read-only view");
    }

    @Override
    public void delete(String id) throws ResourceException {
        throw new ResourceException(405, "Delete group is not supported – underlying source is a read-only view");
    }

    // ---------- READ: GET /Groups/{id} ----------

    @Override
    public ScimGroup get(String id) throws ResourceException {
        OebsScimGroupView row = viewRepository.findById(id)
                .orElseThrow(() -> new ResourceException(404, "Group not found: " + id));

        return mapper.toScim(row);
    }

    // ---------- READ: GET /Groups (med filter/paging) ----------

    @Override
    public FilterResponse<ScimGroup> find(Filter filter,
                                          PageRequest pageRequest,
                                          SortRequest sortRequest) {

        // 1) Hent alle rader (viewet er allerede filtrert på dato = "aktive")
        List<OebsScimGroupView> allRows = viewRepository.findAll();

        List<ScimGroup> allGroups = allRows.stream()
                .map(mapper::toScim)
                .toList();

        // 2) Bruk SCIMple sin in-memory-filtering, likt som InMemoryGroupService gjør
        Schema groupSchema = schemaRegistry.getSchema(ScimGroup.SCHEMA_URI);

        Predicate<ScimResource> predicate = (filter != null)
                ? FilterExpressions.inMemory(filter, groupSchema)
                : r -> true;

        List<ScimGroup> filtered = allGroups.stream()
                .filter(predicate)
                .toList();

        // 3) Paging: SCIM bruker startIndex (1-basert) + count
        long startIndex = (pageRequest != null && pageRequest.getStartIndex() != null)
                ? pageRequest.getStartIndex() - 1    // SCIM → 0-basert
                : 0L;

        long count = (pageRequest != null && pageRequest.getCount() != null)
                ? pageRequest.getCount()
                : filtered.size();

        List<ScimGroup> page = filtered.stream()
                .skip(startIndex)
                .limit(count)
                .collect(Collectors.toList());

        return new FilterResponse<>(page, pageRequest, filtered.size());
    }

    @Override
    public List<Class<? extends ScimExtension>> getExtensionList() {
        // Ingen custom extensions (endnu)
        return Collections.emptyList();
    }
}
