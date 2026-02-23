package no.nav.oebs.api.scim.mapper;

import no.nav.oebs.api.scim.ScimGroupEntity;
import no.nav.oebs.api.scim.ScimGroupMembershipEntity;
import org.apache.directory.scim.spec.resources.ScimGroup;
import org.apache.directory.scim.spec.resources.GroupMembership;
import org.apache.directory.scim.spec.schema.Meta;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ScimGroupMapper {

    /**
     * Konverter entity til SCIM Group objekt
     */
    public ScimGroup toScimGroup(ScimGroupEntity entity, List<ScimGroupMembershipEntity> members) {
        ScimGroup group = new ScimGroup();

        // Core attributes
        group.setId(entity.getScimId());
        group.setDisplayName(entity.getScimDisplayName());
        group.setExternalId(entity.getExternalId());

        // Members
        if (members != null && !members.isEmpty()) {
            List<GroupMembership> scimMembers = members.stream()
                .map(m -> {
                    GroupMembership member = new GroupMembership();
                    member.setValue(m.getBrukerId());  // userName (MSF4711)
                    member.setDisplay(m.getFulltNavn() != null ? m.getFulltNavn() : m.getBrukerId());
                    member.setRef("https://example.com/scim/v2/Users/" + m.getBrukerId());
                    return member;
                })
                .collect(Collectors.toList());
            group.setMembers(scimMembers);
        }

        // Meta
        Meta meta = new Meta();
        meta.setResourceType("Group");
        if (entity.getCreationDate() != null) {
            meta.setCreated(entity.getCreationDate());
        }
        if (entity.getLastUpdateDate() != null) {
            meta.setLastModified(entity.getLastUpdateDate());
        }
        group.setMeta(meta);

        return group;
    }

    /**
     * Konverter liste av entities til SCIM Groups
     */
    public List<ScimGroup> toScimGroups(List<ScimGroupEntity> entities) {
        return entities.stream()
            .map(entity -> toScimGroup(entity, null))
            .collect(Collectors.toList());
    }
}
