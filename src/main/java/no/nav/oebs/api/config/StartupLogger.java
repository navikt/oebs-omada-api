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

    private static final String LOG_SEPARATOR = "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
    private static final String STATUS_REGISTERED = "✓ registrert";
    private static final String STATUS_MISSING = "✗ MANGLER";
    private static final String OEBS_ENV_KEY = "OEBS_ENV";

    private final ApplicationContext applicationContext;
    private final ConfigurableEnvironment environment;

    @EventListener(ContextRefreshedEvent.class)
    public void onContextRefreshed(ContextRefreshedEvent event) {
        // Brann kun for rot-konteksten, ikke child-kontekster (f.eks. Jersey)
        if (event.getApplicationContext().getParent() != null) {
            return;
        }
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
        String oebsEnv    = environment.getProperty(OEBS_ENV_KEY,    "LOCAL");
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
        } catch (RuntimeException e) {
            log.warn("  Kunne ikke lese servlet-registreringer: {}", e.getMessage());
        }
        log.info(LOG_SEPARATOR);
    }

    private void logScimStatus() {
        log.info("━━━ SCIMple Status ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        boolean scimpleActive = applicationContext.containsBean("scimpleServlet");
        boolean registryActive = applicationContext.containsBean("repositoryRegistry");
        boolean schemaActive = applicationContext.containsBean("schemaRegistry");
        log.info("  ScimpleServlet bean     : {}", scimpleActive   ? STATUS_REGISTERED : STATUS_MISSING);
        log.info("  RepositoryRegistry bean : {}", registryActive  ? STATUS_REGISTERED : STATUS_MISSING);
        log.info("  SchemaRegistry bean     : {}", schemaActive     ? STATUS_REGISTERED : STATUS_MISSING);
        if (!scimpleActive) {
            log.warn("  ⚠ SCIMple-servlet er ikke aktiv — DataSource tilgjengelig? {}",
                    applicationContext.containsBean("dataSource"));
        }
        log.info(LOG_SEPARATOR);
    }

    private void logSummary() {
        long uptime;
        try {
            uptime = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime();
        } catch (RuntimeException e) {
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

    private static String padRight(String s) {
        if (s == null) s = "";
        if (s.length() >= 36) return s.substring(0, 36);
        return s + " ".repeat(36 - s.length());
    }
}


