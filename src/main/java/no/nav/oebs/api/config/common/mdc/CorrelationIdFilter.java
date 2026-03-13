package no.nav.oebs.api.config.common.mdc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Spring-filter som setter korrelasjons-ID i MDC for alle innkommende requests,
 * inkludert SCIM-endepunkter på /scim/v2/*.
 * Leser X-Correlation-ID header om den finnes, ellers genereres en ny ID.
 */
@Slf4j
@Component
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
            log.debug("Korrelasjons-ID satt: {}", korrelasjonId);

            filterChain.doFilter(request, response);
        } finally {
            MdcOperations.remove(MdcOperations.MDC_CORRELATION_ID);
        }
    }
}

