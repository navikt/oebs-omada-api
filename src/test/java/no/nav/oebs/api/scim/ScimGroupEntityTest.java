package no.nav.oebs.api.scim;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScimGroupEntityTest {

    @Test
    void isGroup_and_isResponsibility_matchKildeType() {
        ScimGroupEntity entity = new ScimGroupEntity();

        entity.setKildeType("G");
        assertThat(entity.isGroup()).isTrue();
        assertThat(entity.isResponsibility()).isFalse();

        entity.setKildeType("A");
        assertThat(entity.isGroup()).isFalse();
        assertThat(entity.isResponsibility()).isTrue();
    }

    @Test
    void getExternalId_returnsStringValueOrNull() {
        ScimGroupEntity entity = new ScimGroupEntity();

        entity.setKildeId(123L);
        assertThat(entity.getExternalId()).isEqualTo("123");

        entity.setKildeId(null);
        assertThat(entity.getExternalId()).isNull();
    }
}

