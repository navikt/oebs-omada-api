package no.nav.oebs.api.scim.mapper;

import no.nav.oebs.api.scim.ScimGroupEntity;
import no.nav.oebs.api.scim.ScimGroupMembershipEntity;
import org.apache.directory.scim.spec.resources.ScimGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScimGroupMapperTest {

    private static final String BASE_URL = "http://localhost:8080";
    private ScimGroupMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ScimGroupMapper();
        ReflectionTestUtils.setField(mapper, "baseUrl", BASE_URL);
    }

    @Test
    void toScimGroup_mapsIdAndDisplayName() {
        ScimGroup group = mapper.toScimGroup(enGruppe(), List.of());
        assertThat(group.getId()).isEqualTo("G$1234");
        assertThat(group.getDisplayName()).isEqualTo("NAV IT");
    }

    @Test
    void toScimGroup_mapsExternalId() {
        assertThat(mapper.toScimGroup(enGruppe(), List.of()).getExternalId()).isEqualTo("1234");
    }

    @Test
    void toScimGroup_mapsMetaLocationAndResourceType() {
        ScimGroup group = mapper.toScimGroup(enGruppe(), List.of());
        assertThat(group.getMeta().getResourceType()).isEqualTo("Group");
        assertThat(group.getMeta().getLocation()).isEqualTo(BASE_URL + "/scim/v2/Groups/G$1234");
    }

    @Test
    void toScimGroup_mapsMembers() {
        ScimGroupMembershipEntity member = new ScimGroupMembershipEntity();
        member.setNavId("S123456");
        ScimGroup group = mapper.toScimGroup(enGruppe(), List.of(member));
        assertThat(group.getMembers()).hasSize(1);
        assertThat(group.getMembers().getFirst().getValue()).isEqualTo("S123456");
        assertThat(group.getMembers().getFirst().getRef())
                .isEqualTo(BASE_URL + "/scim/v2/Users/S123456");
    }

    @Test
    void toScimGroup_noMembers_whenListEmpty() {
        assertThat(mapper.toScimGroup(enGruppe(), List.of()).getMembers()).isNullOrEmpty();
    }

    @Test
    void toScimGroup_multipleMembers() {
        ScimGroupMembershipEntity m1 = new ScimGroupMembershipEntity();
        m1.setNavId("S111111");
        ScimGroupMembershipEntity m2 = new ScimGroupMembershipEntity();
        m2.setNavId("S222222");
        assertThat(mapper.toScimGroup(enGruppe(), List.of(m1, m2)).getMembers()).hasSize(2);
    }

    @Test
    void toScimGroup_mapsMetaDates() {
        ScimGroupEntity entity = enGruppe();
        LocalDateTime created = LocalDateTime.of(2024, 1, 2, 3, 4, 5);
        LocalDateTime updated = LocalDateTime.of(2025, 6, 7, 8, 9, 10);
        entity.setOpprettetDato(created);
        entity.setSistOppdatert(updated);

        ScimGroup group = mapper.toScimGroup(entity, List.of());

        assertThat(group.getMeta().getCreated()).isEqualTo(created);
        assertThat(group.getMeta().getLastModified()).isEqualTo(updated);
    }

    private ScimGroupEntity enGruppe() {
        ScimGroupEntity e = new ScimGroupEntity();
        e.setScimId("G$1234");
        e.setScimDisplayName("NAV IT");
        e.setKildeType("G");
        e.setKildeId(1234L);
        return e;
    }
}
