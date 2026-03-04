package no.nav.oebs.api.config.common.mdc;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

/**
 * Jersey-filter som setter korrelasjons-ID i MDC for alle innkommende SCIM-requests.
 * Kjører i Jersey-konteksten (ikke Spring-filterkjeden) slik at MDC er tilgjengelig
 * i alle ressursklasser og KallLoggHelper.
 */
@Slf4j
@Provider
public class CorrelationIdFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String korrelasjonId = requestContext.getHeaderString(CORRELATION_ID_HEADER);
        if (korrelasjonId == null || korrelasjonId.isBlank()) {
            korrelasjonId = MdcOperations.generateCorrelationId();
        }
        MdcOperations.put(MdcOperations.MDC_CORRELATION_ID, korrelasjonId);
        log.debug("Korrelasjons-ID satt: {}", korrelasjonId);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        MdcOperations.remove(MdcOperations.MDC_CORRELATION_ID);
    }
}

