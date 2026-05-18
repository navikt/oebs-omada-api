package no.nav.oebs.api.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TechnicalPlsqlExceptionTest {

    @Test
    void constructor_withMessage_keepsMessage() {
        TechnicalPlsqlException ex = new TechnicalPlsqlException("plain message");

        assertThat(ex.getMessage()).isEqualTo("plain message");
    }

    @Test
    void constructor_withMessageNumber_formatsMessage() {
        TechnicalPlsqlException ex = new TechnicalPlsqlException(42, "boom");

        assertThat(ex.getMessage()).isEqualTo("PL/SQL-feil 42 (boom)");
    }
}

