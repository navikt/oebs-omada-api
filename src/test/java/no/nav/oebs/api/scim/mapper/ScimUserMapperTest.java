package no.nav.oebs.api.scim.mapper;

import no.nav.oebs.api.scim.ScimGroupMembershipEntity;
import no.nav.oebs.api.scim.ScimUserEntity;
import no.nav.oebs.api.scim.extension.NavOebsExtension;
import org.apache.directory.scim.spec.extension.EnterpriseExtension;
import org.apache.directory.scim.spec.resources.ScimUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScimUserMapperTest {

    private static final String BASE_URL = "http://localhost:8080";

    private ScimUserMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ScimUserMapper();
        ReflectionTestUtils.setField(mapper, "baseUrl", BASE_URL);
    }

    @Test
    void toScimUser_mapsIdAndUserName() {
        ScimUser user = mapper.toScimUser(enBruker(), List.of());
        assertThat(user.getId()).isEqualTo("S123456");
        assertThat(user.getExternalId()).isEqualTo("ABC1234");
        assertThat(user.getUserName()).isEqualTo("ABC1234");
    }

    @Test
    void toScimUser_mapsActive_trueForY() {
        ScimUserEntity e = enBruker();
        e.setActiveFlag("Y");
        assertThat(mapper.toScimUser(e, List.of()).getActive()).isTrue();
    }

    @Test
    void toScimUser_mapsActive_falseForN() {
        ScimUserEntity e = enBruker();
        e.setActiveFlag("N");
        assertThat(mapper.toScimUser(e, List.of()).getActive()).isFalse();
    }

    @Test
    void toScimUser_mapsName() {
        ScimUser user = mapper.toScimUser(enBruker(), List.of());
        assertThat(user.getName().getGivenName()).isEqualTo("Ola");
        assertThat(user.getName().getFamilyName()).isEqualTo("Nordmann");
        assertThat(user.getName().getFormatted()).isEqualTo("Ola Nordmann");
    }

    // --- E-post prioritet ---

    @Test
    void toScimUser_prefersNavEPostOverEPost() {
        ScimUserEntity e = enBruker();
        e.setNavEPost("ola@nav.no");
        e.setEPost("ola@adeo.no");
        assertThat(mapper.toScimUser(e, List.of()).getEmails().getFirst().getValue())
                .isEqualTo("ola@nav.no");
    }

    @Test
    void toScimUser_fallsBackToEPostWhenNavEPostBlank() {
        ScimUserEntity e = enBruker();
        e.setNavEPost("  ");
        e.setEPost("ola@adeo.no");
        assertThat(mapper.toScimUser(e, List.of()).getEmails().getFirst().getValue())
                .isEqualTo("ola@adeo.no");
    }

    @Test
    void toScimUser_noEmail_whenBothNull() {
        ScimUserEntity e = enBruker();
        e.setNavEPost(null);
        e.setEPost(null);
        assertThat(mapper.toScimUser(e, List.of()).getEmails()).isNullOrEmpty();
    }

    // --- Grupper ---

    @Test
    void toScimUser_mapsGroups() {
        ScimGroupMembershipEntity g = new ScimGroupMembershipEntity();
        g.setScimGroupId("GRP001");
        g.setScimDisplayName("NAV IT");

        ScimUser user = mapper.toScimUser(enBruker(), List.of(g));

        assertThat(user.getGroups()).hasSize(1);
        assertThat(user.getGroups().getFirst().getValue()).isEqualTo("GRP001");
        assertThat(user.getGroups().getFirst().getDisplay()).isEqualTo("NAV IT");
        assertThat(user.getGroups().getFirst().getRef())
                .isEqualTo(BASE_URL + "/scim/v2/Groups/GRP001");
    }

    @Test
    void toScimUser_noGroups_whenListEmpty() {
        assertThat(mapper.toScimUser(enBruker(), List.of()).getGroups()).isNullOrEmpty();
    }

    // --- Enterprise extension ---

    @Test
    void toScimUser_mapsEnterpriseExtension() {
        ScimUserEntity e = enBruker();
        e.setEnhetsId("1234");
        e.setArbeidsstedFylke("OSLO");

        EnterpriseExtension ext = (EnterpriseExtension) mapper.toScimUser(e, List.of())
                .getExtension(EnterpriseExtension.URN);

        assertThat(ext).isNotNull();
        assertThat(ext.getDepartment()).isEqualTo("1234");
        assertThat(ext.getDivision()).isEqualTo("OSLO");
    }

    @Test
    void toScimUser_noEnterpriseExtension_whenBothNull() {
        ScimUserEntity e = enBruker();
        e.setEnhetsId(null);
        e.setArbeidsstedFylke(null);
        assertThat(mapper.toScimUser(e, List.of()).getExtension(EnterpriseExtension.URN)).isNull();
    }

    // --- NavOebsExtension ---

    @Test
    void toScimUser_mapsFullmakt() {
        ScimUserEntity e = enBruker();
        e.setFullmakt("SAKSBEHANDLER");
        NavOebsExtension ext = (NavOebsExtension) mapper.toScimUser(e, List.of()).getExtension(NavOebsExtension.URN);
        assertThat(ext.getFullmakt()).isEqualTo("SAKSBEHANDLER");
    }

    @Test
    void toScimUser_egenansatt_true_whenFlagY() {
        ScimUserEntity e = enBruker();
        e.setEgenansattFlag("Y");
        NavOebsExtension ext = (NavOebsExtension) mapper.toScimUser(e, List.of()).getExtension(NavOebsExtension.URN);
        assertThat(ext.getEgenansatt()).isTrue();
    }

    @Test
    void toScimUser_egenansatt_false_whenFlagN() {
        ScimUserEntity e = enBruker();
        e.setEgenansattFlag("N");
        NavOebsExtension ext = (NavOebsExtension) mapper.toScimUser(e, List.of()).getExtension(NavOebsExtension.URN);
        assertThat(ext.getEgenansatt()).isFalse();
    }

    @Test
    void toScimUser_egenansatt_false_whenFlagNull() {
        ScimUserEntity e = enBruker();
        e.setEgenansattFlag(null);
        NavOebsExtension ext = (NavOebsExtension) mapper.toScimUser(e, List.of()).getExtension(NavOebsExtension.URN);
        assertThat(ext.getEgenansatt()).isFalse();
    }

    @Test
    void toScimUser_nyttPassord_erNullPaaGet() {
        // nyttPassord er WRITE_ONLY — skal aldri settes av mapperen på GET-responsen
        NavOebsExtension ext = (NavOebsExtension) mapper.toScimUser(enBruker(), List.of())
                .getExtension(NavOebsExtension.URN);
        assertThat(ext.getNyttPassord()).isNull();
    }

    // --- Meta ---

    @Test
    void toScimUser_mapsMetaLocationAndResourceType() {
        ScimUser user = mapper.toScimUser(enBruker(), List.of());
        assertThat(user.getMeta().getLocation())
                .isEqualTo(BASE_URL + "/scim/v2/Users/S123456");
        assertThat(user.getMeta().getResourceType()).isEqualTo("User");
    }

    // --- Hjelpemetode ---

    private ScimUserEntity enBruker() {
        ScimUserEntity e = new ScimUserEntity();
        e.setNavId("S123456");
        e.setBrukerId("ABC1234");
        e.setForNavn("Ola");
        e.setEtterNavn("Nordmann");
        e.setNavEPost("ola@nav.no");
        e.setActiveFlag("Y");
        e.setEnhetsId("1234");
        e.setArbeidsstedFylke("OSLO");
        return e;
    }
}

