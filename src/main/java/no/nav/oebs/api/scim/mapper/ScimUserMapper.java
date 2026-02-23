package no.nav.oebs.api.scim.mapper;

import no.nav.oebs.api.scim.ScimGroupMembershipEntity;
import no.nav.oebs.api.scim.ScimUserEntity;
import org.apache.directory.scim.core.schema.SchemaRegistry;
import org.apache.directory.scim.spec.extension.EnterpriseExtension;
import org.apache.directory.scim.spec.resources.*;
import org.apache.directory.scim.spec.schema.Meta;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper som konverterer ScimUserEntity til Apache SCIMple User objekt
 */
@Component
public class ScimUserMapper {

    /**
     * Konverter entity til SCIM User objekt
     */
    public ScimUser toScimUser(ScimUserEntity entity, List<ScimGroupMembershipEntity> groups) {
        ScimUser user = new ScimUser();

        // Core attributes
        user.setId(entity.getBrukerId());
        user.setExternalId(String.valueOf(entity.getNavId()));
        user.setUserName(entity.getBrukerId());
        user.setActive(entity.isActive());

        // Name
        Name name = new Name();
        name.setGivenName(entity.getForNavn());
        name.setFamilyName(entity.getEtterNavn());
        name.setFormatted(entity.getFullName());
        user.setName(name);

        user.setDisplayName(entity.getFullName());

        // Email
        if (entity.getEPost() != null) {
            Email email = new Email();
            email.setValue(entity.getEPost());
            email.setType("work");
            email.setPrimary(true);
            user.setEmails(Collections.singletonList(email));
        }

        // Groups
        if (groups != null && !groups.isEmpty()) {
            List<GroupMembership> groupMemberships = groups.stream()
                .map(g -> {
                    GroupMembership gm = new GroupMembership();
                    gm.setValue(g.getScimGroupId());
                    gm.setDisplay(g.getScimDisplayName());
                    gm.setRef("https://example.com/scim/v2/Groups/" + g.getScimGroupId());
                    return gm;
                })
                .collect(Collectors.toList());
            user.setGroups(groupMemberships);
        }

        // Enterprise extension
        if (entity.getEnhetsId() != null || entity.getArbeidsstedFylke() != null) {
            EnterpriseExtension enterprise = new EnterpriseExtension();
            enterprise.setDepartment(entity.getEnhetsId());
            enterprise.setDivision(entity.getArbeidsstedFylke());
            user.addExtension(enterprise);
        }

        // TODO: custom extension
        // NavExtension navExt = new NavExtension();
        // navExt.setFullmakt(...);
        // user.addExtension(navExt);

        // Meta
        Meta meta = new Meta();
        meta.setResourceType("User");
        // TODO: Set created, lastModified from entity
        user.setMeta(meta);

        return user;
    }

    /**
     * Konverter liste av entities til SCIM Users
     */
    public List<ScimUser> toScimUsers(List<ScimUserEntity> entities) {
        return entities.stream()
            .map(entity -> toScimUser(entity, Collections.emptyList()))
            .collect(Collectors.toList());
    }
}
