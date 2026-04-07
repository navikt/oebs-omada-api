package no.nav.oebs.api.scim.mapper;

import no.nav.oebs.api.scim.ScimGroupMembershipEntity;
import no.nav.oebs.api.scim.ScimUserEntity;
import no.nav.oebs.api.scim.extension.NavOebsExtension;
import org.apache.directory.scim.spec.extension.EnterpriseExtension;
import org.apache.directory.scim.spec.resources.*;
import org.apache.directory.scim.spec.schema.Meta;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper som konverterer ScimUserEntity til Apache SCIMple User objekt
 */
@Component
public class ScimUserMapper {

    @Value("${oebs.scim.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Konverter entity til SCIM User objekt
     */
    public ScimUser toScimUser(ScimUserEntity entity, List<ScimGroupMembershipEntity> groups) {
        ScimUser user = new ScimUser();

        // Core attributes
        user.setId(entity.getNavId());
        user.setExternalId(entity.getBrukerId()); // externalId = brukerId (OeBS-brukernavn)
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
                    // $ref settes kun når vi har en gyldig gruppe-ID — ikke forventet i innkommende meldinger fra Omada
                    if (g.getScimGroupId() != null) {
                        gm.setRef(baseUrl + "/scim/v2/Groups/" + g.getScimGroupId());
                    }
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
        navExt.setFullmakt(entity.getFullmakt());
        navExt.setEgenansatt(entity.isEgenansatt());
        navExt.setNyttPassord(entity.isNyttPassord());
        user.addExtension(navExt);

        // Meta
        Meta meta = new Meta();
        meta.setResourceType("User");
        meta.setLocation(baseUrl + "/scim/v2/Users/" + entity.getNavId());
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
     * Lager et slim SCIM User-objekt med kun id (navId) og groups.
     * Brukes av Omada bulk-endepunktet /scim/v2/UserMemberships.
     */
    public ScimUser toSlimScimUser(String navId, List<ScimGroupMembershipEntity> groups) {
        ScimUser user = new ScimUser();
        user.setId(navId);

        if (groups != null && !groups.isEmpty()) {
            List<UserGroup> groupMemberships = groups.stream()
                .map(g -> {
                    UserGroup ug = new UserGroup();
                    ug.setValue(g.getScimGroupId());
                    ug.setDisplay(g.getScimDisplayName());
                    ug.setRef(baseUrl + "/scim/v2/Groups/" + g.getScimGroupId());
                    return ug;
                })
                .collect(Collectors.toList());
            user.setGroups(groupMemberships);
        }

        return user;
    }

}
