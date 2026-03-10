package no.nav.oebs.api.scim.mapper;

import no.nav.oebs.api.scim.ScimGroupMembershipEntity;
import no.nav.oebs.api.scim.ScimUserEntity;
import no.nav.oebs.api.scim.extension.NavOebsExtension;
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
        user.setExternalId(entity.getNavId());
        user.setUserName(entity.getBrukerId());
        user.setActive(entity.isActive());

        // Name
        Name name = new Name();
        name.setGivenName(entity.getForNavn());
        name.setFamilyName(entity.getEtterNavn());
        name.setFormatted(entity.getFullName());
        user.setName(name);

        user.setDisplayName(entity.getFullName());

        // Email - bruk NAV e-post (COUNTRY_OF_BIRTH) som primær om satt, ellers fallback til FND_USER e-post
        String epostVerdi = entity.getNavEPost() != null && !entity.getNavEPost().isBlank()
                ? entity.getNavEPost()
                : entity.getEPost();
        if (epostVerdi != null) {
            Email email = new Email();
            email.setValue(epostVerdi);
            email.setType("work");
            email.setPrimary(true);
            user.setEmails(Collections.singletonList(email));
        }

        // Groups
        if (groups != null && !groups.isEmpty()) {
            List<UserGroup> groupMemberships = groups.stream()
                .map(g -> {
                    UserGroup gm = new UserGroup();
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

        // NAV OeBS custom extension
        NavOebsExtension navExt = new NavOebsExtension();
        navExt.setFullmakt(null); // TODO: hent fullmakt fra kilde når tilgjengelig
        user.addExtension(navExt);

        // Meta
        Meta meta = new Meta();
        meta.setResourceType("User");
        if (entity.getCreationDate() != null) {
            meta.setCreated(entity.getCreationDate());
        }
        if (entity.getLastUpdateDate() != null) {
            meta.setLastModified(entity.getLastUpdateDate());
        }
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
