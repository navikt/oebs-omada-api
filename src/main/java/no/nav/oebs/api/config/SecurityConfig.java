package no.nav.oebs.api.config;

import no.nav.security.token.support.core.configuration.MultiIssuerConfiguration;
import no.nav.security.token.support.jaxrs.servlet.JaxrsJwtTokenValidationFilter;
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Sikkerhetskonfigurasjon.
 * {@code @EnableJwtTokenValidation} aktiverer JWT-validering for alle endepunkter unntatt
 * de som er annotert med {@code @Unprotected} eller er i ignored packages.
 * SCIMple-endepunkter beskyttes via {@code @Protected} / {@code @Unprotected} på ressursklassene.
 * JWT-validering for SCIMple (Jersey) bruker token-validation-jaxrs:
 *   - JaxrsJwtTokenValidationFilter registreres som servlet-filter for /scim/v2/*
 *   - JwtTokenContainerRequestFilter registreres i Jersey ResourceConfig
 *   - @Protected / @Unprotected annoteres direkte på SCIMple provider-klassene
 * Spring MVC-endepunkter beskyttes via @EnableJwtTokenValidation som vanlig.
 */

@Configuration
@Profile("!local")
@EnableJwtTokenValidation(ignore = { "org.springframework", "org.springdoc" })
public class SecurityConfig {

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