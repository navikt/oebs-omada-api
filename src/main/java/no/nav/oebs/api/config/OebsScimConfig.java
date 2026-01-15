package no.nav.oebs.api.config;

import org.apache.directory.scim.server.configuration.ServerConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OebsScimConfig {

    @Bean
    public ServerConfiguration serverConfiguration() {

        // Denne beannen brukes av SCIMple for /ServiceProviderConfig osv.
        return new ServerConfiguration()
                .setId("oebs-scim")
                .setDocumentationUri("https://nav.no")
                // her kan du senere legge til auth-schemas, patch/bulk-config osv
                ;
    }
}
