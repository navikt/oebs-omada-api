package no.nav.oebs.api.config;

import no.nav.security.token.support.core.configuration.MultiIssuerConfiguration;
import no.nav.security.token.support.jaxrs.servlet.JaxrsJwtTokenValidationFilter;
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

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
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
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

    /**
     * Explicitly permits only known public paths (Swagger UI, webjars, OpenAPI docs, actuator health).
     * All other requests require authentication, preventing ResourceHttpRequestHandler from
     * exposing static resources without protection.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers(
                "/scim/v2/**",
                "/internal/**",
                "/actuator/**",
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/webjars/**"
            ))
            .authorizeHttpRequests(auth -> auth
                // SCIM-endepunkter beskyttes av JaxrsJwtTokenValidationFilter (servlet-filter) og
                // JwtTokenContainerRequestFilter (Jersey) — Spring Security skal ikke interferere her.
                .requestMatchers("/scim/v2/**").permitAll()
                // Swagger UI / OpenAPI-dokumentasjon — åpent
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/webjars/**",
                    "/v3/api-docs/**",
                    "/v3/api-docs"
                ).permitAll()
                // Actuator — åpne helse- og metrikk-endepunkter
                .requestMatchers(
                    "/internal/isready",
                    "/internal/isalive",
                    "/actuator/health",
                    "/actuator/health/**",
                    "/actuator/info",
                    "/actuator/prometheus"
                ).permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }
}