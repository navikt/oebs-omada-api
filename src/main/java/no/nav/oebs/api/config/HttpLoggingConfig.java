package no.nav.oebs.api.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import no.nav.oebs.api.config.common.logging.HttpLoggingFilter;
import no.nav.oebs.api.db.repository.KallLoggRepository;

/**
 * Registrerer HttpLoggingFilter for /api/* og /scim/v2/*.
 * MDC-livssyklus (correlationId) håndteres av CorrelationIdFilter (@Order(HIGHEST_PRECEDENCE)).
 */
@Configuration
public class HttpLoggingConfig {


	@Bean
	public FilterRegistrationBean<HttpLoggingFilter> httpLoggingFilterRegistrationBean(KallLoggRepository kallLoggRepository) {
		FilterRegistrationBean<HttpLoggingFilter> registrationBean = new FilterRegistrationBean<>();
		registrationBean.setFilter(new HttpLoggingFilter(kallLoggRepository));
		registrationBean.addUrlPatterns("/api/*", "/scim/v2/*");
		registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
		return registrationBean;
	}
}
