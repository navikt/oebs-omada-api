package no.nav.oebs.api.config;

import org.springframework.context.annotation.Configuration;

/**
 * Konfigurasjon for å eksponere Spring request context for Jersey servlet container.
 *
 * NOTE: JWT token validation er fullstendig deaktivert via:
 * - @EnableJwtTokenValidation er kommentert ut i SecurityConfig
 * - EnableJwtTokenValidationConfiguration er ekskludert i application.yaml
 *   (spring.autoconfigure.exclude)
 *
 * RequestContextFilter og RequestContextListener er derfor ikke nødvendige
 * siden de primært brukes av JWT token validation.
 *
 * Når JWT validation aktiveres igjen, må både:
 * 1. @EnableJwtTokenValidation aktiveres i SecurityConfig
 * 2. EnableJwtTokenValidationConfiguration fjernes fra exclude-listen
 * 3. RequestContextFilter/Listener aktiveres her
 */
@Configuration
public class RequestContextConfig {

    // RequestContextFilter er kommentert ut - ikke nødvendig uten JWT validation
    /*
    @Bean
    public FilterRegistrationBean<RequestContextFilter> requestContextFilter() {
        FilterRegistrationBean<RequestContextFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RequestContextFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Integer.MIN_VALUE);
        registration.setName("requestContextFilter");
        return registration;
    }
    */
}

