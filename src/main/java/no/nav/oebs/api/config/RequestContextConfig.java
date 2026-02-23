package no.nav.oebs.api.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.filter.RequestContextFilter;

/**
 * Konfigurasjon for å eksponere Spring request context for Jersey servlet container.
 *
 * Dette er nødvendig fordi applikasjonen bruker både:
 * - Jersey (JAX-RS) for SCIM-endepunkter (Apache SCIMple)
 * - Spring DispatcherServlet for resten av applikasjonen
 * - NAV token validation som krever Spring request context
 *
 * RequestContextFilter sørger for at Spring's RequestContextHolder er tilgjengelig
 * også for requests som håndteres av Jersey servlet container.
 */
@Configuration
public class RequestContextConfig {

    /**
     * Registrerer RequestContextFilter for å eksponere request context til alle servlets.
     * Filteret kjører først (Integer.MIN_VALUE) for å sikre at context er tilgjengelig
     * før andre filtre (som token validation) kjører.
     */
    @Bean
    public FilterRegistrationBean<RequestContextFilter> requestContextFilter() {
        FilterRegistrationBean<RequestContextFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RequestContextFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Integer.MIN_VALUE); // Kjør først
        registration.setName("requestContextFilter");
        return registration;
    }

    @Bean
    public RequestContextListener requestContextListener() {
        return new RequestContextListener();
    }
}

