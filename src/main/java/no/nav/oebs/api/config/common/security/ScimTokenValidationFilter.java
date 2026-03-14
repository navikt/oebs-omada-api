package no.nav.oebs.api.config.common.security;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;
import no.nav.oebs.api.scim.KallLoggHelper;
import no.nav.security.token.support.jaxrs.JaxrsTokenValidationContextHolder;
import org.apache.directory.scim.protocol.data.ErrorResponse;

/**
 * Jersey ContainerRequestFilter som håndhever token-validering per HTTP-metode og path.
 * Logger alle avviste kall (401) til KallLogg.
 * Endepunkt                             | Metode                  | Krav
 * --------------------------------------|-------------------------|------------------
 * GET /scim/v2/Users/{id}               | GET med id-segment      | @Unprotected
 * GET /scim/v2/Users (liste)            | GET uten id-segment     | @Protected
 * POST/PUT/PATCH/DELETE /scim/v2/Users  | write                   | @Protected
 * GET /scim/v2/Groups                   | GET                     | @Protected
 * POST/PUT/PATCH/DELETE /scim/v2/Groups | write                   | @Unprotected
 */
@Slf4j
@Provider
public class ScimTokenValidationFilter implements ContainerRequestFilter {

    private final KallLoggHelper kallLoggHelper;

    @Inject
    @SuppressWarnings("CdiInjectionPointsInspection") // KallLoggHelper bindes via HK2 AbstractBinder i ScimpleConfig
    public ScimTokenValidationFilter(KallLoggHelper kallLoggHelper) {
        this.kallLoggHelper = kallLoggHelper;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String method = requestContext.getMethod().toUpperCase();
        String path   = requestContext.getUriInfo().getPath();

        if (isUnprotected(method, path)) {
            log.debug("ScimTokenValidationFilter: @Unprotected — {} {}", method, path);
            return;
        }

        var tokenContext = JaxrsTokenValidationContextHolder.INSTANCE.getTokenValidationContext();
        //noinspection ConstantConditions — getTokenValidationContext() kan returnere null ved manglende token tross Kotlin @NotNull
        boolean hasValidToken = tokenContext != null && !tokenContext.getIssuers().isEmpty();

        if (!hasValidToken) {
            log.warn("ScimTokenValidationFilter: 401 Unauthorized — {} {}", method, path);
            ErrorResponse errorResponse = new ErrorResponse(401, "Token mangler eller er ugyldig");
            kallLoggHelper.loggInn(method, "/scim/v2/" + path,
                    401, 0, null, errorResponse.getDetail());
            requestContext.abortWith(ErrorResponse.toResponse(errorResponse));
        }
    }

    /**
     * Returnerer true hvis kallet er åpent uten token.
     * Kun SCIM metadata-endepunkter er åpne:
     *   GET /Schemas
     *   GET /ResourceTypes
     *   GET /ServiceProviderConfig
     * Alt annet (Users og Groups alle metoder) krever gyldig token.
     */
    private boolean isUnprotected(String method, String path) {
        boolean isGet = method.equals("GET");
        return isGet && (
                path.startsWith("Schemas")               || path.startsWith("/Schemas") ||
                path.startsWith("ResourceTypes")         || path.startsWith("/ResourceTypes") ||
                path.startsWith("ServiceProviderConfig") || path.startsWith("/ServiceProviderConfig"));
    }
}

