package no.nav.oebs.api.common.utils;


import org.springframework.http.ResponseEntity;
import no.nav.oebs.api.db.entity.ApiError;

public class ResponseEntityBuilder {
    public static ResponseEntity<Object> build(ApiError apiError) {
        return new ResponseEntity<>(apiError, apiError.getStatus());
    }
}

