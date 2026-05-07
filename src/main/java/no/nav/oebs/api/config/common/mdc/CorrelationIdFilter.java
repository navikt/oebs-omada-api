package no.nav.oebs.api.config.common.mdc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Spring-filter som setter korrelasjons-ID i MDC for alle innkommende requests,
 * inkludert SCIM-endepunkter på /scim/v2/*.
 * Leser X-Correlation-ID header om den finnes, ellers genereres en ny ID.
 *
 * Kjører med HIGHEST_PRECEDENCE slik at den er det ytterste filteret og
 * MDC-verdien er tilgjengelig for alle indre filtre og Jersey-laget.
 * finally-blokken (som fjerner MDC) kjører derfor aller sist.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String korrelasjonId = request.getHeader(CORRELATION_ID_HEADER);
            if (korrelasjonId == null || korrelasjonId.isBlank()) {
                korrelasjonId = MdcOperations.generateCorrelationId();
            }
            MdcOperations.put(MdcOperations.MDC_CORRELATION_ID, korrelasjonId);
            response.setHeader(CORRELATION_ID_HEADER, korrelasjonId);
            log.debug("Korrelasjons-ID satt: {}", sanitizeForLog(korrelasjonId));

            filterChain.doFilter(request, response);
        } finally {
            MdcOperations.remove(MdcOperations.MDC_CORRELATION_ID);
        }
    }

    private String sanitizeForLog(String value) {
        if (value == null) return null;
        return value
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t")
                .replaceAll("\\p{Cntrl}", "_");
    }
}

