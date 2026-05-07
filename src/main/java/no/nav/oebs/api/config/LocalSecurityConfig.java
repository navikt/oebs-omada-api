package no.nav.oebs.api.config;

import com.nimbusds.jose.util.DefaultResourceRetriever;
import lombok.extern.slf4j.Slf4j;
import no.nav.security.token.support.core.configuration.MultiIssuerConfiguration;
import no.nav.security.token.support.jaxrs.servlet.JaxrsJwtTokenValidationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Map;

/**
 * Lokal sikkerhetskonfigurasjon — aktiv kun med profilen {@code local}.
 * Erstatter {@link SecurityConfig} (deaktivert via {@code @Profile("!local")}):
 * <ul>
 *   <li>Tom {@link MultiIssuerConfiguration} → ingen OIDC discovery-kall ved oppstart</li>
 *   <li>No-op {@link JaxrsJwtTokenValidationFilter} → ingen 401 på /scim/v2/*</li>
 *   <li>{@link no.nav.oebs.api.config.common.security.ScimTokenValidationFilter} passerer
 *       alle requests gjennom når ingen issuers er konfigurert</li>
 * </ul>
 */
@Slf4j
@Configuration
@Profile("local")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class LocalSecurityConfig {

    @Bean
    public MultiIssuerConfiguration multiIssuerConfiguration() {
        log.info("[LocalSecurity] Ingen JWT-issuers konfigurert — alle SCIM-kall er åpne lokalt");
        return new MultiIssuerConfiguration(Map.of(), new DefaultResourceRetriever());
    }

    @Bean
    public FilterRegistrationBean<JaxrsJwtTokenValidationFilter> jaxrsJwtTokenValidationFilter(
            MultiIssuerConfiguration multiIssuerConfiguration) {
        FilterRegistrationBean<JaxrsJwtTokenValidationFilter> registration =
                new FilterRegistrationBean<>(new JaxrsJwtTokenValidationFilter(multiIssuerConfiguration));
        registration.addUrlPatterns("/scim/v2/*");
        registration.setOrder(1);
        return registration;
    }
}
