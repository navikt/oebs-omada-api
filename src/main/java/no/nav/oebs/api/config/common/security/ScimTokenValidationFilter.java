package no.nav.oebs.api.config.common.security;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;
import no.nav.oebs.api.scim.KallLoggHelper;
import no.nav.security.token.support.core.http.HttpRequest;
import no.nav.security.token.support.core.validation.JwtTokenValidationHandler;
import org.apache.directory.scim.protocol.data.ErrorResponse;

/**
 * Jersey ContainerRequestFilter som håndhever token-validering per HTTP-metode og path.
 * Validerer JWT-token direkte via JwtTokenValidationHandler — uavhengig av servlet-filter-chain.
 *
 * Endepunkt                          | Metode | Krav
 * -----------------------------------|--------|---------------------
 * GET /Schemas, /ResourceTypes, /SPC | GET    | @Unprotected
 * Alt annet (Users og Groups)        | alle   | @Protected
 */
@Slf4j
@Provider
public class ScimTokenValidationFilter implements ContainerRequestFilter {

    private final KallLoggHelper kallLoggHelper;
    private final JwtTokenValidationHandler validationHandler;

    @Inject
    @SuppressWarnings("CdiInjectionPointsInspection")
    public ScimTokenValidationFilter(KallLoggHelper kallLoggHelper,
                                     JwtTokenValidationHandler validationHandler) {
        this.kallLoggHelper = kallLoggHelper;
        this.validationHandler = validationHandler;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String method = requestContext.getMethod().toUpperCase();
        String path   = requestContext.getUriInfo().getPath();

        if (isUnprotected(method, path)) {
            log.debug("ScimTokenValidationFilter: @Unprotected — {} {}", method, path);
            return;
        }

        // Les Authorization-header direkte fra JAX-RS request og valider via JwtTokenValidationHandler
        String authHeader = requestContext.getHeaderString("Authorization");
        HttpRequest httpRequest = headerName -> "Authorization".equalsIgnoreCase(headerName) ? authHeader : null;

        log.info("ScimTokenValidationFilter: validerer {} {} — Authorization: {}",
                method, path,
                authHeader == null ? "MANGLER" : authHeader.substring(0, Math.min(authHeader.length(), 30)) + "...");

        var tokenContext = validationHandler.getValidatedTokens(httpRequest);
        boolean hasValidToken = tokenContext != null
                && tokenContext.getIssuers().stream().anyMatch(issuer -> tokenContext.getJwtToken(issuer) != null);

        log.info("ScimTokenValidationFilter: tokenContext={} issuers={} hasValidToken={}",
                tokenContext == null ? "null" : "present",
                tokenContext == null ? "-" : tokenContext.getIssuers(),
                hasValidToken);

        if (!hasValidToken) {
            log.warn("ScimTokenValidationFilter: 401 Unauthorized — {} {} [issuers={}]",
                    method, path,
                    tokenContext == null ? "-" : tokenContext.getIssuers());
            ErrorResponse errorResponse = new ErrorResponse(401, "Token mangler eller er ugyldig");
            kallLoggHelper.loggInn(method, "/scim/v2/" + path,
                    401, 0, null, errorResponse.getDetail());
            requestContext.abortWith(ErrorResponse.toResponse(errorResponse));
        }
    }

    /**
     * Kun SCIM metadata-endepunkter er åpne uten token.
     */
    private boolean isUnprotected(String method, String path) {
        boolean isGet = method.equals("GET");
        return isGet && (
                path.startsWith("Schemas")               || path.startsWith("/Schemas") ||
                path.startsWith("ResourceTypes")         || path.startsWith("/ResourceTypes") ||
                path.startsWith("ServiceProviderConfig") || path.startsWith("/ServiceProviderConfig"));
    }
}
