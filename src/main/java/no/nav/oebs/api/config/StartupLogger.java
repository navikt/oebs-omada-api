package no.nav.oebs.api.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import java.util.Arrays;
import java.util.Map;

/**
 * Logger detaljert informasjon om applikasjonens oppstartstilstand.
 * Lytter på {@link ContextRefreshedEvent} og {@link ApplicationReadyEvent}
 * slik at alle beans, servlets og routes er registrert før logging skjer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupLogger {

    private final ApplicationContext applicationContext;
    private final ConfigurableEnvironment environment;

    @EventListener(ContextRefreshedEvent.class)
    public void onContextRefreshed(ContextRefreshedEvent event) {
        // Brann kun for rot-konteksten, ikke child-kontekster (f.eks. Jersey)
        if (event.getApplicationContext().getParent() != null) {
            return;
        }
        logEnvironment();
        logActiveProfile();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        logBanner();
        logServletMappings(event);
        logScimStatus();
        logSummary();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void logBanner() {
        String appName    = environment.getProperty("APP_NAME",    "oebs-omada-api");
        String appVersion = environment.getProperty("APP_VERSION", "?");
        String appUpdate  = environment.getProperty("APP_UPDATE",  "?");
        String oebsEnv    = environment.getProperty("OEBS_ENV",    "LOCAL");
        int    port       = Integer.parseInt(environment.getProperty("server.port", "8080"));

        log.info("╔══════════════════════════════════════════════════════╗");
        log.info("║             APPLIKASJON KLAR                         ║");
        log.info("╠══════════════════════════════════════════════════════╣");
        log.info("║  Navn     : {}",    padRight(appName)    + "║");
        log.info("║  Versjon  : {}",    padRight(appVersion) + "║");
        log.info("║  Oppdatert: {}",    padRight(appUpdate)  + "║");
        log.info("║  Miljø    : {}",    padRight(oebsEnv)    + "║");
        log.info("║  Port     : {}",    padRight(String.valueOf(port)) + "║");
        log.info("╚══════════════════════════════════════════════════════╝");
    }

    private void logActiveProfile() {
        String[] profiles = environment.getActiveProfiles();
        log.info("► Aktive profiler  : {}", profiles.length > 0 ? Arrays.toString(profiles) : "[default]");
        log.info("► Default profiler : {}", Arrays.toString(environment.getDefaultProfiles()));
    }

    private void logEnvironment() {
        log.info("━━━ Miljøvariabler (applikasjon) ━━━━━━━━━━━━━━━━━━━━━━");
        logMasked("DB_URL",              safeProperty("DB_URL"));
        logMasked("DB_USER",             safeProperty("APPS_USER"));
        logMasked("OEBS_ENV",            safeProperty("OEBS_ENV"));
        logMasked("AZURE_DISCOVERY_URL", safeProperty("no.nav.security.jwt.issuer.azure.discovery-url"));
        logMasked("AZURE_AUDIENCE",      safeProperty("no.nav.security.jwt.issuer.azure.accepted-audience"));
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void logServletMappings(ApplicationReadyEvent event) {
        log.info("━━━ Registrerte Servlets ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        try {
            if (event.getApplicationContext() instanceof ServletWebServerApplicationContext ctx) {
                ServletContext servletContext = ctx.getServletContext();
                if (servletContext != null) {
                    Map<String, ? extends ServletRegistration> registrations =
                            servletContext.getServletRegistrations();
                    if (registrations.isEmpty()) {
                        log.warn("  Ingen servlet-registreringer funnet i ServletContext");
                    } else {
                        registrations.forEach((name, reg) ->
                                log.info("  [{}]  mappings={}", name, reg.getMappings()));
                    }
                } else {
                    log.warn("  ServletContext er null — kan ikke lese servlet-registreringer");
                }
            } else {
                log.info("  (ikke en ServletWebServerApplicationContext — hopper over servlet-logging)");
            }
        } catch (Exception e) {
            log.warn("  Kunne ikke lese servlet-registreringer: {}", e.getMessage());
        }
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void logScimStatus() {
        log.info("━━━ SCIMple Status ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        boolean scimpleActive = applicationContext.containsBean("scimpleServlet");
        boolean registryActive = applicationContext.containsBean("repositoryRegistry");
        boolean schemaActive = applicationContext.containsBean("schemaRegistry");
        log.info("  ScimpleServlet bean     : {}", scimpleActive   ? "✓ registrert" : "✗ MANGLER");
        log.info("  RepositoryRegistry bean : {}", registryActive  ? "✓ registrert" : "✗ MANGLER");
        log.info("  SchemaRegistry bean     : {}", schemaActive     ? "✓ registrert" : "✗ MANGLER");
        if (!scimpleActive) {
            log.warn("  ⚠ SCIMple-servlet er ikke aktiv — DataSource tilgjengelig? {}",
                    applicationContext.containsBean("dataSource"));
        }
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void logSummary() {
        long uptime;
        try {
            uptime = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime();
        } catch (Exception e) {
            uptime = -1;
        }
        String port = environment.getProperty("server.port", "8080");
        log.info("► Applikasjon klar på {}ms", uptime);
        log.info("► Port        : {}", port);
        log.info("► Swagger UI  : /swagger-ui/index.html");
        log.info("► SCIM v2     : /scim/v2/Schemas");
        log.info("► Health      : /internal/isalive");
        log.info("► Actuator    : /actuator/health");
    }

    /** Leser en property uten å kaste exception hvis verdien inneholder en uresolvable placeholder. */
    private String safeProperty(String key) {
        try {
            return environment.getProperty(key, "<ikke satt>");
        } catch (Exception e) {
            return "<ikke tilgjengelig: " + e.getMessage() + ">";
        }
    }

    private void logMasked(String key, String value) {
        if (value == null) {
            log.info("  {} = <ikke satt>", key);
            return;
        }
        String lower = key.toLowerCase();
        if (lower.contains("password") || lower.contains("passord") ||
                lower.contains("secret") || lower.contains("pwd")) {
            log.info("  {} = ****", key);
        } else {
            log.info("  {} = {}", key, value);
        }
    }

    private static String padRight(String s) {
        if (s == null) s = "";
        if (s.length() >= 36) return s.substring(0, 36);
        return s + " ".repeat(36 - s.length());
    }
}


