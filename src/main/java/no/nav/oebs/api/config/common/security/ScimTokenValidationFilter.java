package no.nav.oebs.api.config.common.security;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;
import no.nav.security.token.support.jaxrs.JaxrsTokenValidationContextHolder;

/**
 * Jersey ContainerRequestFilter som håndhever token-validering per HTTP-metode og path:
 *
 * Endepunkt                         | Metode              | Krav
 * ----------------------------------|---------------------|------------------
 * /scim/v2/Groups (GET, find)       | GET                 | @Protected (token kreves)
 * /scim/v2/Groups (create/upd/del)  | POST/PUT/PATCH/DEL  | @Unprotected (åpent for Omada)
 * /scim/v2/Users  (alle)            | GET/POST/PUT/PATCH/DEL | @Protected (token kreves)
 */
@Slf4j
@Provider
public class ScimTokenValidationFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String method = requestContext.getMethod().toUpperCase();
        String path   = requestContext.getUriInfo().getPath(); // relativ til Jersey-servlet, f.eks. "Groups" eller "Users/123"

        if (isUnprotected(method, path)) {
            log.debug("ScimTokenValidationFilter: @Unprotected — {} {}", method, path);
            return;
        }

        // Sjekk at JaxrsJwtTokenValidationFilter har satt et gyldig token i konteksten
        var tokenContext = JaxrsTokenValidationContextHolder.INSTANCE.getTokenValidationContext();
        boolean hasValidToken = tokenContext != null && !tokenContext.getIssuers().isEmpty();

        if (!hasValidToken) {
            log.warn("ScimTokenValidationFilter: 401 Unauthorized — {} {}", method, path);
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"detail\":\"Token mangler eller er ugyldig\"}")
                    .type("application/json")
                    .build()
            );
        }
    }

    /**
     * Returnerer true hvis kallet er åpent uten token.
     *
     * Groups:
     *   POST / PUT / PATCH / DELETE  → @Unprotected  (Omada skriver grupper til oss)
     *   GET                          → @Protected
     *
     * Users:
     *   GET /{id}                    → @Unprotected  (oppslag på enkelt bruker)
     *   GET (liste)                  → @Protected
     *   POST / PUT / PATCH / DELETE  → @Protected
     */
    private boolean isUnprotected(String method, String path) {
        boolean isGroupsPath  = path.startsWith("Groups")  || path.startsWith("/Groups");
        boolean isUsersPath   = path.startsWith("Users")   || path.startsWith("/Users");
        boolean isWriteMethod = method.equals("POST") || method.equals("PUT")
                             || method.equals("PATCH") || method.equals("DELETE");
        boolean isGet         = method.equals("GET");

        // Users GET /{id} — path inneholder "/" etter "Users", f.eks. "Users/abc123"
        boolean isUsersById   = isUsersPath && isGet && path.replaceFirst("^/?Users/?", "").contains("/") == false
                             && !path.replaceFirst("^/?Users/?", "").isBlank();

        return (isGroupsPath && isWriteMethod) || isUsersById;
    }
}

