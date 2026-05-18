package no.nav.oebs.api.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsonMappingExceptionTest {

    @Test
    void constructor_withCause_setsCauseAndNullMessage() {
        Exception cause = new Exception("root");

        JsonMappingException ex = new JsonMappingException(cause);

        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(ex.getMessage()).contains("root");
    }

    @Test
    void constructor_withMessageAndCause_setsBoth() {
        Exception cause = new Exception("root");

        JsonMappingException ex = new JsonMappingException("mapping failed", cause);

        assertThat(ex.getMessage()).isEqualTo("mapping failed");
        assertThat(ex.getCause()).isSameAs(cause);
    }
}

