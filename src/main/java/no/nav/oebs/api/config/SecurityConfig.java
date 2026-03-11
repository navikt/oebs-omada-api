package no.nav.oebs.api.config;

import no.nav.security.token.support.core.configuration.MultiIssuerConfiguration;
import no.nav.security.token.support.jaxrs.servlet.JaxrsJwtTokenValidationFilter;
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableJwtTokenValidation(ignore = { "org.springframework", "org.springdoc" })
public class SecurityConfig {

    @Bean
    public FilterRegistrationBean<JaxrsJwtTokenValidationFilter> jaxrsJwtTokenValidationFilterBean(
            MultiIssuerConfiguration multiIssuerConfiguration) {
        var filter = new JaxrsJwtTokenValidationFilter(multiIssuerConfiguration);
        var registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/scim/v2/*");
        registration.setOrder(1);
        return registration;
    }
}
