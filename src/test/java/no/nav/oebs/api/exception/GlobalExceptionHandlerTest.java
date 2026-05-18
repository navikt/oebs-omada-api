package no.nav.oebs.api.exception;

import jakarta.validation.ConstraintViolationException;
import no.nav.oebs.api.db.entity.ApiError;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleHttpClientErrorException_returnsUnauthorizedApiError() {
        HttpClientErrorException ex = HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "Unauthorized", null, null, null);

        ResponseEntity<Object> response = handler.handleHttpClientErrorException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        ApiError body = (ApiError) response.getBody();
        assertThat(body.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(body.getMessage()).isEqualTo("Feil 401: Ugyldig Aksess token");
        assertThat(body.getErrors()).isNotEmpty();
    }

    @Test
    void handleMethodArgumentTypeMismatch_returnsBadRequestApiError() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getMessage()).thenReturn("type mismatch");

        ResponseEntity<Object> response = handler.handleMethodArgumentTypeMismatch(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiError body = (ApiError) response.getBody();
        assertThat(body.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.getMessage()).isEqualTo("Mismatch Type");
        assertThat(body.getErrors()).containsExactly("type mismatch");
    }

    @Test
    void handleConstraintViolationException_returnsBadRequestApiError() {
        ConstraintViolationException ex = new ConstraintViolationException("constraint failed", Set.of());

        ResponseEntity<Object> response = handler.handleConstraintViolationException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiError body = (ApiError) response.getBody();
        assertThat(body.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.getMessage()).isEqualTo("Constraint Violation");
        assertThat(body.getErrors()).containsExactly("constraint failed");
    }

    @Test
    void handleAll_returnsBadRequestApiError() {
        Exception ex = new Exception("boom");

        ResponseEntity<Object> response = handler.handleAll(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiError body = (ApiError) response.getBody();
        assertThat(body.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.getMessage()).isEqualTo("boom");
        assertThat(body.getErrors()).containsExactly("boom");
    }

    @Test
    void handleDataNotFoundException_returnsOkWithEmptyPayloadMarker() {
        Exception ex = new TechnicalPlsqlException("not found");

        ResponseEntity<Object> response = handler.handleDataNotFoundException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ApiError body = (ApiError) response.getBody();
        assertThat(body.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(body.getMessage()).isEqualTo("[]");
        assertThat(body.getErrors()).isNotEmpty();
    }
}

