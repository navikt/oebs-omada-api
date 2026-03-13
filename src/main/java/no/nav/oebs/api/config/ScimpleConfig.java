package no.nav.oebs.api.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.oebs.api.scim.provider.ScimGroupResourceProvider;
import no.nav.oebs.api.scim.provider.ScimUserResourceProvider;
import org.apache.directory.scim.core.repository.DefaultPatchHandler;
import org.apache.directory.scim.core.repository.PatchHandler;
import org.apache.directory.scim.core.repository.Repository;
import org.apache.directory.scim.core.repository.RepositoryRegistry;
import org.apache.directory.scim.core.schema.SchemaRegistry;
import org.apache.directory.scim.protocol.UserResource;
import org.apache.directory.scim.server.configuration.ServerConfiguration;
import org.apache.directory.scim.server.rest.EtagGenerator;
import org.apache.directory.scim.server.rest.ScimResourceHelper;
import org.apache.directory.scim.server.rest.UserResourceImpl;
import org.apache.directory.scim.spec.resources.ScimResource;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.List;

/**
 * Manuell konfigurasjon av Apache SCIMple-komponenter.
 * Erstatter ScimpleSpringConfiguration (som ikke fungerer med Spring Boot 4
 * fordi den refererer til den fjernede JerseyAutoConfiguration).
 * Kun aktiv når en DataSource er konfigurert (ikke i unit-tester uten DB).
 */
@Slf4j
@Configuration
@ConditionalOnBean(DataSource.class)
@RequiredArgsConstructor
public class ScimpleConfig {

    private final ScimUserResourceProvider userRepository;
    private final ScimGroupResourceProvider groupRepository;

    @PostConstruct
    public void logScimEndpoints() {
        log.info("╔══════════════════════════════════════════════════════╗");
        log.info("║         SCIM 2.0 API Endpoints (via SCIMple)         ║");
        log.info("╠══════════════════════════════════════════════════════╣");
        log.info("║ GET    /scim/v2/Users                                ║");
        log.info("║ GET    /scim/v2/Users/{id}                           ║");
        log.info("║ POST   /scim/v2/Users                                ║");
        log.info("║ PUT    /scim/v2/Users/{id}                           ║");
        log.info("║ DELETE /scim/v2/Users/{id}                           ║");
        log.info("║ GET    /scim/v2/Groups                               ║");
        log.info("║ GET    /scim/v2/Groups/{id}                          ║");
        log.info("║ GET    /scim/v2/ServiceProviderConfig                ║");
        log.info("║ GET    /scim/v2/Schemas                              ║");
        log.info("║ GET    /scim/v2/ResourceTypes                        ║");
        log.info("╚══════════════════════════════════════════════════════╝");
    }

    @Bean
    public ServerConfiguration serverConfiguration() {
        return new ServerConfiguration();
    }

    @Bean
    public EtagGenerator etagGenerator() {
        return new EtagGenerator();
    }

    @Bean
    public SchemaRegistry schemaRegistry() {
        return new SchemaRegistry();
    }

    @Bean
    public PatchHandler patchHandler(SchemaRegistry schemaRegistry) {
        return new DefaultPatchHandler(schemaRegistry);
    }

    @Bean
    public RepositoryRegistry repositoryRegistry(SchemaRegistry schemaRegistry) {
        List<Repository<? extends ScimResource>> repositories = List.of(userRepository, groupRepository);
        RepositoryRegistry registry = new RepositoryRegistry(schemaRegistry);
        registry.registerRepositories(repositories);
        log.info("✓ SCIMple RepositoryRegistry: registrert {} repositories", repositories.size());
        return registry;
    }

    @Bean
    public ResourceConfig scimpleJerseyConfig() {
        ResourceConfig config = ResourceConfig.forApplication(new jakarta.ws.rs.core.Application() {
            @Override
            public java.util.Set<Class<?>> getClasses() {
                return ScimResourceHelper.scimpleFeatureAndResourceClasses();
            }
        });

        config.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(UserResourceImpl.class).to(UserResource.class);
            }
        });

        log.info("✓ SCIMple Jersey ResourceConfig konfigurert");
        return config;
    }

    @Bean
    @SuppressWarnings("NullableProblems")
    public ServletRegistrationBean<ServletContainer> scimpleServlet(ResourceConfig scimpleJerseyConfig) {
        ServletContainer container = new ServletContainer(scimpleJerseyConfig);
        ServletRegistrationBean<ServletContainer> registration =
                new ServletRegistrationBean<>(container, "/scim/v2/*");
        registration.setName("ScimpleServlet");
        registration.setLoadOnStartup(1);
        log.info("✓ SCIMple servlet registrert på /scim/v2/*");
        return registration;
    }
}
