package no.nav.oebs.api.scim;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ScimUserEntityTest {

    // -------------------------------------------------------------------------
    // isActive()
    // -------------------------------------------------------------------------

    @Test
    void isActive_Y_returnsTrue() {
        ScimUserEntity entity = new ScimUserEntity();
        entity.setActiveFlag("Y");
        assertThat(entity.isActive()).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"N", "y", "yes", "true", "1"})
    void isActive_nonY_returnsFalse(String flag) {
        ScimUserEntity entity = new ScimUserEntity();
        entity.setActiveFlag(flag);
        assertThat(entity.isActive()).isFalse();
    }

    // -------------------------------------------------------------------------
    // isEgenansatt()
    // -------------------------------------------------------------------------

    @Test
    void isEgenansatt_Y_returnsTrue() {
        ScimUserEntity entity = new ScimUserEntity();
        entity.setEgenansattFlag("Y");
        assertThat(entity.isEgenansatt()).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"N", "y", "yes", "true"})
    void isEgenansatt_nonY_returnsFalse(String flag) {
        ScimUserEntity entity = new ScimUserEntity();
        entity.setEgenansattFlag(flag);
        assertThat(entity.isEgenansatt()).isFalse();
    }

    // -------------------------------------------------------------------------
    // getFullName()
    // -------------------------------------------------------------------------

    @Test
    void getFullName_combinesForNavnAndEtterNavn() {
        ScimUserEntity entity = new ScimUserEntity();
        entity.setForNavn("Ola");
        entity.setEtterNavn("Nordmann");
        assertThat(entity.getFullName()).isEqualTo("Ola Nordmann");
    }
}

