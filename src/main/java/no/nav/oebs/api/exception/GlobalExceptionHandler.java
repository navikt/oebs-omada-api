package no.nav.oebs.api.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import no.nav.oebs.api.db.entity.ApiError;
import no.nav.oebs.api.common.utils.ResponseEntityBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {


    @ExceptionHandler({ HttpClientErrorException.class })
    public ResponseEntity<Object> handleHttpClientErrorException(HttpClientErrorException ex) {

        List<String> details = new ArrayList<>();
        details.add(ex.getLocalizedMessage());

        ApiError err = new ApiError(LocalDateTime.now(),HttpStatus.UNAUTHORIZED, "Feil 401: Ugyldig Aksess token" ,details);

        return ResponseEntityBuilder.build(err);

    }

    // handleMethodArgumentTypeMismatch : triggers when a parameter's type does not match
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<Object> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        List<String> details = new ArrayList<>();
        details.add(ex.getMessage());

        ApiError err = new ApiError(LocalDateTime.now(),HttpStatus.BAD_REQUEST, "Mismatch Type" ,details);

        return ResponseEntityBuilder.build(err);
    }

    // handleConstraintViolationException : triggers when @Validated fails
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolationException(ConstraintViolationException ex) {

        List<String> details = new ArrayList<>();
        details.add(ex.getMessage());

        ApiError err = new ApiError(LocalDateTime.now(),HttpStatus.BAD_REQUEST, "Constraint Violation" ,details);

        return ResponseEntityBuilder.build(err);
    }

    // @ExceptionHandler({ Exception.class })
    public ResponseEntity<Object> handleAll(Exception ex) {

        List<String> details = new ArrayList<>();
        details.add(ex.getLocalizedMessage());

        ApiError err = new ApiError(LocalDateTime.now(),HttpStatus.BAD_REQUEST, ex.getMessage() ,details);

        return ResponseEntityBuilder.build(err);

    }

    // @ExceptionHandler({ Exception.class })
    @ExceptionHandler({ PlsqlException.class })
    public ResponseEntity<Object> handleDataNotFoundException(Exception ex){

        List<String> details = new ArrayList<>();
        details.add(ex.getLocalizedMessage());

        ApiError err = new ApiError(LocalDateTime.now(),HttpStatus.OK, "[]" ,details);

        return ResponseEntityBuilder.build(err);

    }

}

