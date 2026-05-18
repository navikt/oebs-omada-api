package no.nav.oebs.api.config;

import org.apache.directory.scim.core.repository.RepositoryRegistry;
import org.apache.directory.scim.core.schema.SchemaRegistry;
import org.apache.directory.scim.server.configuration.ServerConfiguration;
import org.apache.directory.scim.server.rest.EtagGenerator;
import org.apache.directory.scim.spec.resources.ScimUser;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.springframework.boot.web.servlet.ServletRegistrationBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockConstruction;

class ScimpleConfigTest {

    private final ScimpleConfig config = new ScimpleConfig();

    @Test
    void basicBeans_areCreated() {
        ServerConfiguration serverConfiguration = config.serverConfiguration();
        EtagGenerator etagGenerator = config.etagGenerator();
        SchemaRegistry schemaRegistry = config.schemaRegistry();

        assertThat(serverConfiguration).isNotNull();
        assertThat(etagGenerator).isNotNull();
        assertThat(schemaRegistry).isNotNull();
    }

    @Test
    void patchAndRepositoryBeans_areCreated() {
        SchemaRegistry schemaRegistry = config.schemaRegistry();
        RepositoryRegistry repositoryRegistry = config.repositoryRegistry(schemaRegistry, List.of());

        assertThat(config.patchHandler(schemaRegistry)).isNotNull();
        assertThat(repositoryRegistry).isNotNull();
    }

    @Test
    void scimpleServlet_isConfigured() {
        ResourceConfig resourceConfig = new ResourceConfig();

        ServletRegistrationBean<ServletContainer> bean = config.scimpleServlet(resourceConfig);

        assertThat(bean).isNotNull();
        assertThat(bean.getServletName()).isEqualTo("ScimpleServlet");
        assertThat(bean.getUrlMappings()).contains("/scim/v2/*");
    }

    @Test
    void logScimEndpoints_doesNotThrow() {
        assertDoesNotThrow(config::logScimEndpoints);
    }

    @Test
    void schemaRegistry_handlesRuntimeException_whenSchemaRegistrationFails() {
        try (MockedConstruction<SchemaRegistry> mocked = mockConstruction(SchemaRegistry.class, (mock, context) ->
                doThrow(new RuntimeException("boom")).when(mock).addSchema(eq(ScimUser.class), anyList()))) {
            SchemaRegistry registry = config.schemaRegistry();

            assertThat(registry).isNotNull();
            assertThat(mocked.constructed()).hasSize(1);
        }
    }
}

