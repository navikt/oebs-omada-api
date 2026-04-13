package no.nav.oebs.api.config.common.security;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;
import no.nav.oebs.api.scim.KallLoggHelper;
import no.nav.security.token.support.core.configuration.MultiIssuerConfiguration;
import no.nav.security.token.support.core.http.HttpRequest;
import no.nav.security.token.support.core.validation.JwtTokenValidationHandler;
import org.apache.directory.scim.protocol.data.ErrorResponse;
import java.time.Instant;
import java.util.Base64;

/**
 * Jersey ContainerRequestFilter som håndhever token-validering per HTTP-metode og path.
 * Validerer JWT-token direkte via JwtTokenValidationHandler — uavhengig av servlet-filter-chain.
 *
 * Endepunkt                          | Metode | Krav
 * -----------------------------------|--------|---------------------
 * GET /Schemas, /ResourceTypes, /SPC | GET    | @Unprotected
 * Alt annet (Users og Groups)        | alle   | Gyldig Bearer-token påkrevd
 */
@Slf4j
@Provider
public class ScimTokenValidationFilter implements ContainerRequestFilter {

    private final KallLoggHelper kallLoggHelper;
    private final JwtTokenValidationHandler validationHandler;
    private final MultiIssuerConfiguration multiIssuerConfiguration;

    @Inject
    @SuppressWarnings("CdiInjectionPointsInspection")
    public ScimTokenValidationFilter(KallLoggHelper kallLoggHelper,
                                     JwtTokenValidationHandler validationHandler,
                                     MultiIssuerConfiguration multiIssuerConfiguration) {
        this.kallLoggHelper = kallLoggHelper;
        this.validationHandler = validationHandler;
        this.multiIssuerConfiguration = multiIssuerConfiguration;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String method = requestContext.getMethod().toUpperCase();
        String path   = requestContext.getUriInfo().getPath();

        if (isUnprotected(method, path)) {
            log.debug("ScimTokenValidationFilter: @Unprotected — {} {}", method, path);
            return;
        }

        String rawAuthHeader = requestContext.getHeaderString("Authorization");
        String authHeader = rawAuthHeader != null ? rawAuthHeader.trim() : null;

        log.debug("ScimTokenValidationFilter: validerer {} {} — Authorization: {}",
                method, path, authHeader == null ? "MANGLER" : "present");

        if (authHeader == null || !authHeader.toLowerCase().startsWith("bearer ")) {
            String grunn = authHeader == null
                    ? "Authorization-header mangler"
                    : "Authorization-header har ugyldig format (forventet 'Bearer <token>')";
            log.warn("ScimTokenValidationFilter: 401 — {} {} — {}", method, path, grunn);
            avvis(requestContext, method, path, grunn);
            return;
        }

        HttpRequest httpRequest = headerName -> "Authorization".equalsIgnoreCase(headerName) ? authHeader : null;
        var tokenContext = validationHandler.getValidatedTokens(httpRequest);

        boolean hasValidToken = tokenContext.getIssuers().stream()
                .anyMatch(issuer -> tokenContext.getJwtToken(issuer) != null);

        if (!hasValidToken) {
            String grunn = utledUgyldigTokenGrunn(authHeader);
            log.warn("ScimTokenValidationFilter: 401 — {} {} — {} [konfigurerte issuers={}]",
                    method, path, grunn, multiIssuerConfiguration.getIssuers().keySet());
            avvis(requestContext, method, path, grunn);
        }
    }

    /**
     * Undersøker JWT-payload og returnerer en menneskelig lesbar grunn til at tokenet er ugyldig.
     */
    private String utledUgyldigTokenGrunn(String authHeader) {
        try {
            String token = authHeader.substring(7).trim();
            String[] parts = token.split("\\.");
            if (parts.length < 2) return "Token er ikke et gyldig JWT-format";

            String padded = parts[1];
            if (padded.length() % 4 != 0) padded += "=".repeat(4 - padded.length() % 4);
            String payload = new String(Base64.getUrlDecoder().decode(padded));

            // exp-sjekk
            java.util.regex.Matcher expMatcher = java.util.regex.Pattern
                    .compile("\"exp\"\\s*:\\s*(\\d+)").matcher(payload);
            if (expMatcher.find()) {
                long exp = Long.parseLong(expMatcher.group(1));
                if (Instant.now().getEpochSecond() > exp) {
                    return "Token er utløpt (exp=" + Instant.ofEpochSecond(exp) + ")";
                }
            } else {
                return "Token mangler exp-claim";
            }

            // Hent iss og aud fra token for diagnostikk
            java.util.regex.Matcher issMatcher = java.util.regex.Pattern
                    .compile("\"iss\"\\s*:\\s*\"([^\"]+)\"").matcher(payload);
            String tokenIss = issMatcher.find() ? issMatcher.group(1) : "(mangler)";

            java.util.regex.Matcher audMatcher = java.util.regex.Pattern
                    .compile("\"aud\"\\s*:\\s*[\"\\[]([^\"]*)").matcher(payload);
            String tokenAud = audMatcher.find() ? audMatcher.group(1) : "(mangler)";

            // Sjekk iss mot konfigurerte issuers (via metadata)
            boolean kjentIssuer = multiIssuerConfiguration.getIssuers().values().stream()
                    .anyMatch(ic -> {
                        try {
                            String konfIss = ic.getMetadata().getIssuer().getValue();
                            return konfIss != null && konfIss.equals(tokenIss);
                        } catch (Exception e) {
                            return false;
                        }
                    });
            if (!kjentIssuer) {
                var konfigurertNavn = multiIssuerConfiguration.getIssuerShortNames();
                return "Token har ukjent issuer: " + tokenIss
                        + " (konfigurerte issuers: " + konfigurertNavn + ")";
            }

            // Sjekk aud mot aksepterte audiences
            boolean gyldigAud = multiIssuerConfiguration.getIssuers().values().stream()
                    .anyMatch(ic -> ic.getAcceptedAudience().stream()
                            .anyMatch(a -> a.equals(tokenAud)));
            if (!gyldigAud) {
                var aksepterteAud = multiIssuerConfiguration.getIssuers().values().stream()
                        .flatMap(ic -> ic.getAcceptedAudience().stream())
                        .toList();
                return "Token har feil audience: " + tokenAud
                        + " (aksepterte audiences: " + aksepterteAud + ")";
            }

            return "Token er ugyldig — iss=" + tokenIss + ", aud=" + tokenAud;

        } catch (Exception e) {
            return "Token er ugyldig (kunne ikke lese innhold: " + e.getMessage() + ")";
        }
    }

    private void avvis(ContainerRequestContext requestContext, String method, String path, String grunn) {
        ErrorResponse errorResponse = new ErrorResponse(401, "Ugyldig token: " + grunn);
        kallLoggHelper.loggInn(
                method,
                "/scim/v2/" + path,
                401,
                0,
                errorResponse.getDetail(),   // response  — svar returnert til Omada
                grunn);                       // logginfo  — teknisk grunn til at tokenet ble avvist
        requestContext.abortWith(ErrorResponse.toResponse(errorResponse));
    }

    /**
     * Kun SCIM metadata-endepunkter er åpne uten token.
     */
    private boolean isUnprotected(String method, String path) {
        boolean isGet = "GET".equals(method);
        return isGet && (
                path.startsWith("Schemas")               || path.startsWith("/Schemas") ||
                path.startsWith("ResourceTypes")         || path.startsWith("/ResourceTypes") ||
                path.startsWith("ServiceProviderConfig") || path.startsWith("/ServiceProviderConfig"));
    }
}
