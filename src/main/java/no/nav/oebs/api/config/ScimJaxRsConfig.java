package no.nav.oebs.api.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.oebs.api.config.common.mdc.CorrelationIdFilter;
import no.nav.oebs.api.scim.rest.ScimGroupsResource;
import no.nav.oebs.api.scim.rest.ScimUsersResource;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * SCIM 2.0 JAX-RS configuration
 * Registrerer våre SCIM REST resources med Jersey
 *
 * Endepunkter:
 * - GET /scim/v2/Users
 * - GET /scim/v2/Users/{id}
 * - GET /scim/v2/Groups
 * - GET /scim/v2/Groups/{id}
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ScimJaxRsConfig {

    private final ScimUsersResource usersResource;
    private final ScimGroupsResource groupsResource;

    @PostConstruct
    public void logScimEndpoints() {
        log.info("╔══════════════════════════════════════════════════════╗");
        log.info("║         SCIM 2.0 API Endpoints                       ║");
        log.info("╠══════════════════════════════════════════════════════╣");
        log.info("║ GET /scim/v2/Users                                   ║");
        log.info("║ GET /scim/v2/Users/{id}                              ║");
        log.info("║ GET /scim/v2/Groups                                  ║");
        log.info("║ GET /scim/v2/Groups/{id}                             ║");
        log.info("╚══════════════════════════════════════════════════════╝");
    }

    @Bean
    public ResourceConfig jerseyConfig() {
        ResourceConfig config = new ResourceConfig();

        // Register SCIM resources
        config.register(usersResource);
        config.register(groupsResource);

        // Register MDC korrelasjons-ID filter
        config.register(CorrelationIdFilter.class);

        // Register JSON provider
        config.register(org.glassfish.jersey.jackson.JacksonFeature.class);

        log.info("✓ Registered SCIM JAX-RS resources");

        return config;
    }

    @Bean
    public ServletRegistrationBean<ServletContainer> jerseyServlet(ResourceConfig jerseyConfig) {
        ServletContainer servletContainer = new ServletContainer(jerseyConfig);
        ServletRegistrationBean<ServletContainer> registration =
            new ServletRegistrationBean<>(servletContainer, "/scim/v2/*");
        registration.setName("JerseyServlet");
        registration.setLoadOnStartup(1);

        log.info("✓ Registered Jersey servlet at /scim/v2/*");

        return registration;
    }
}



