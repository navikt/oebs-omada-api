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
import org.apache.directory.scim.protocol.GroupResource;
import org.apache.directory.scim.protocol.UserResource;
import org.apache.directory.scim.server.configuration.ServerConfiguration;
import org.apache.directory.scim.server.rest.EtagGenerator;
import org.apache.directory.scim.server.rest.GroupResourceImpl;
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
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ScimpleConfig {

    private final ScimUserResourceProvider userRepository;
    private final ScimGroupResourceProvider groupRepository;

    @PostConstruct
    public void logScimEndpoints() {
        log.info("━━━ SCIMple konfigurasjon starter ━━━━━━━━━━━━━━━━━━━━━━");
        log.info("  UserRepository  : {}", userRepository.getClass().getSimpleName());
        log.info("  GroupRepository : {}", groupRepository.getClass().getSimpleName());
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
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
        log.info("  [1/6] Oppretter ServerConfiguration...");
        ServerConfiguration cfg = new ServerConfiguration();
        log.info("  [1/6] ServerConfiguration OK");
        return cfg;
    }

    @Bean
    public EtagGenerator etagGenerator() {
        log.info("  [2/6] Oppretter EtagGenerator...");
        EtagGenerator gen = new EtagGenerator();
        log.info("  [2/6] EtagGenerator OK");
        return gen;
    }

    @Bean
    public SchemaRegistry schemaRegistry() {
        log.info("  [3/6] Oppretter SchemaRegistry...");
        SchemaRegistry registry = new SchemaRegistry();
        log.info("  [3/6] SchemaRegistry OK");
        return registry;
    }

    @Bean
    public PatchHandler patchHandler(SchemaRegistry schemaRegistry) {
        log.info("  [4/6] Oppretter PatchHandler (DefaultPatchHandler)...");
        PatchHandler handler = new DefaultPatchHandler(schemaRegistry);
        log.info("  [4/6] PatchHandler OK");
        return handler;
    }

    @Bean
    @ConditionalOnBean(DataSource.class)
    public RepositoryRegistry repositoryRegistry(SchemaRegistry schemaRegistry) {
        log.info("  [5/6] Oppretter RepositoryRegistry...");
        List<Repository<? extends ScimResource>> repositories = List.of(userRepository, groupRepository);
        log.info("  [5/6] Registrerer {} repositories: {}",
                repositories.size(),
                repositories.stream()
                        .map(r -> r.getClass().getSimpleName())
                        .toList());
        RepositoryRegistry registry = new RepositoryRegistry(schemaRegistry);
        registry.registerRepositories(repositories);
        log.info("  [5/6] RepositoryRegistry OK — {} repositories registrert", repositories.size());
        return registry;
    }

    @Bean
    @ConditionalOnBean(DataSource.class)
    public ResourceConfig scimpleJerseyConfig(
            SchemaRegistry schemaRegistry,
            RepositoryRegistry repositoryRegistry,
            ServerConfiguration serverConfiguration,
            EtagGenerator etagGenerator) {

        log.info("  [6/6] Konfigurerer Jersey ResourceConfig for SCIMple...");
        log.info("  [6/6] Registrerer JAX-RS ressursklasser: {}",
                ScimResourceHelper.scimpleFeatureAndResourceClasses().stream()
                        .map(Class::getSimpleName)
                        .sorted()
                        .toList());

        ResourceConfig config = ResourceConfig.forApplication(new jakarta.ws.rs.core.Application() {
            @Override
            public java.util.Set<Class<?>> getClasses() {
                return ScimResourceHelper.scimpleFeatureAndResourceClasses();
            }
        });

        // Bridge Spring Beans inn i HK2 slik at Jersey kan injisere dem
        // i UserResourceImpl og GroupResourceImpl sine konstruktører
        config.register(new AbstractBinder() {
            @Override
            protected void configure() {
                log.info("  [6/6] HK2 binder: UserResourceImpl → UserResource");
                bind(UserResourceImpl.class).to(UserResource.class);
                log.info("  [6/6] HK2 binder: GroupResourceImpl → GroupResource");
                bind(GroupResourceImpl.class).to(GroupResource.class);
                log.info("  [6/6] HK2 binder: SchemaRegistry (instance)");
                bind(schemaRegistry).to(SchemaRegistry.class);
                log.info("  [6/6] HK2 binder: RepositoryRegistry (instance)");
                bind(repositoryRegistry).to(RepositoryRegistry.class);
                log.info("  [6/6] HK2 binder: ServerConfiguration (instance)");
                bind(serverConfiguration).to(ServerConfiguration.class);
                log.info("  [6/6] HK2 binder: EtagGenerator (instance)");
                bind(etagGenerator).to(EtagGenerator.class);
            }
        });

        log.info("  [6/6] Jersey ResourceConfig OK");
        return config;
    }

    @Bean
    @ConditionalOnBean(DataSource.class)
    @SuppressWarnings("NullableProblems")
    public ServletRegistrationBean<ServletContainer> scimpleServlet(ResourceConfig scimpleJerseyConfig) {
        log.info("  [7/7] Registrerer SCIMple Jersey-servlet på /scim/v2/*...");
        ServletContainer container = new ServletContainer(scimpleJerseyConfig);
        ServletRegistrationBean<ServletContainer> registration =
                new ServletRegistrationBean<>(container, "/scim/v2/*");
        registration.setName("ScimpleServlet");
        registration.setLoadOnStartup(1);
        registration.setOrder(1);
        registration.setAsyncSupported(true);
        log.info("  [7/7] ScimpleServlet OK — mappet til /scim/v2/*");
        return registration;
    }
}
