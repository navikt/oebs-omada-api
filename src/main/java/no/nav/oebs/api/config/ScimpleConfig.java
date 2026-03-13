package no.nav.oebs.api.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;import org.apache.directory.scim.core.repository.DefaultPatchHandler;
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
import org.apache.directory.scim.spec.resources.ScimGroup;
import org.apache.directory.scim.spec.resources.ScimResource;
import org.apache.directory.scim.spec.resources.ScimUser;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * Konfigurasjon av Apache SCIMple-komponenter.
 * Følger mønsteret fra den originale {@code ScimpleSpringConfiguration} i SCIMple:
 * bruker {@code @AutoConfiguration} og {@code @ConditionalOnMissingBean} slik at
 * conditions evalueres korrekt etter at alle bruker-definerte beans er registrert.
 * Erstatter ScimpleSpringConfiguration (som refererer til den fjernede JerseyAutoConfiguration).
 */
@Slf4j
@AutoConfiguration
public class ScimpleConfig {

    @PostConstruct
    public void logScimEndpoints() {
        log.info("━━━ SCIMple konfigurasjon starter ━━━━━━━━━━━━━━━━━━━━━━");
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
    @ConditionalOnMissingBean
    public ServerConfiguration serverConfiguration() {
        log.info("  [1/6] ServerConfiguration OK");
        return new ServerConfiguration();
    }

    @Bean
    @ConditionalOnMissingBean
    public EtagGenerator etagGenerator() {
        log.info("  [2/6] EtagGenerator OK");
        return new EtagGenerator();
    }

    @Bean
    @ConditionalOnMissingBean
    public SchemaRegistry schemaRegistry() {
        log.info("  [3/6] SchemaRegistry — registrerer ScimUser og ScimGroup skjemaer...");
        SchemaRegistry registry = new SchemaRegistry();
        try {
            registry.addSchema(ScimUser.class, List.of());
            registry.addSchema(ScimGroup.class, List.of());
            log.info("  [3/6] SchemaRegistry OK — ScimUser og ScimGroup registrert");
        } catch (Exception e) {
            log.error("  [3/6] SchemaRegistry FEIL ved skjemaregistrering: {}", e.getMessage(), e);
        }
        return registry;
    }

    @Bean
    @ConditionalOnMissingBean
    public PatchHandler patchHandler(SchemaRegistry schemaRegistry) {
        log.info("  [4/6] PatchHandler OK");
        return new DefaultPatchHandler(schemaRegistry);
    }

    /**
     * Spring samler automatisk alle beans av typen {@code Repository<? extends ScimResource>}
     * — dvs. ScimUserResourceProvider og ScimGroupResourceProvider.
     */
    @Bean
    @ConditionalOnMissingBean
    public RepositoryRegistry repositoryRegistry(
            SchemaRegistry schemaRegistry,
            List<Repository<? extends ScimResource>> repositories) {
        log.info("  [5/6] RepositoryRegistry — registrerer {} repositories: {}",
                repositories.size(),
                repositories.stream().map(r -> r.getClass().getSimpleName()).toList());
        RepositoryRegistry registry = new RepositoryRegistry(schemaRegistry);
        registry.registerRepositories(repositories);
        log.info("  [5/6] RepositoryRegistry OK");
        return registry;
    }

    @Bean
    @ConditionalOnMissingBean
    public ResourceConfig scimpleJerseyConfig(
            SchemaRegistry schemaRegistry,
            RepositoryRegistry repositoryRegistry,
            ServerConfiguration serverConfiguration,
            EtagGenerator etagGenerator) {

        log.info("  [6/6] Konfigurerer Jersey ResourceConfig — JAX-RS klasser: {}",
                ScimResourceHelper.scimpleFeatureAndResourceClasses().stream()
                        .map(Class::getSimpleName).sorted().toList());

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
                bind(UserResourceImpl.class).to(UserResource.class);
                bind(GroupResourceImpl.class).to(GroupResource.class);
                bind(schemaRegistry).to(SchemaRegistry.class);
                bind(repositoryRegistry).to(RepositoryRegistry.class);
                bind(serverConfiguration).to(ServerConfiguration.class);
                bind(etagGenerator).to(EtagGenerator.class);
            }
        });

        log.info("  [6/6] Jersey ResourceConfig OK");
        return config;
    }

    @Bean
    @ConditionalOnMissingBean
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
