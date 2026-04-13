package no.nav.oebs.api.scim.mapper;

import no.nav.oebs.api.scim.ScimGroupEntity;
import no.nav.oebs.api.scim.ScimGroupMembershipEntity;
import org.apache.directory.scim.spec.resources.ScimGroup;
import org.apache.directory.scim.spec.resources.GroupMembership;
import org.apache.directory.scim.spec.schema.Meta;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ScimGroupMapper {

    @Value("${oebs.scim.base-url:http://localhost:8080}")
    private String baseUrl;

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
                    member.setValue(m.getNavId());
                    member.setDisplay(m.getNavId());
                    if (m.getNavId() != null) {
                        member.setRef(baseUrl + "/scim/v2/Users/" + m.getNavId());
                    }
                    return member;
                })
                .collect(Collectors.toList());
            group.setMembers(scimMembers);
        }

        // Meta
        Meta meta = new Meta();
        meta.setResourceType("Group");
        meta.setLocation(baseUrl + "/scim/v2/Groups/" + entity.getScimId());
        if (entity.getOpprettetDato() != null) {
            meta.setCreated(entity.getOpprettetDato());
        }
        if (entity.getSistOppdatert() != null) {
            meta.setLastModified(entity.getSistOppdatert());
        }
        group.setMeta(meta);

        return group;
    }

}
