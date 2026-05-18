package no.nav.oebs.api.config;

import no.nav.security.token.support.core.configuration.MultiIssuerConfiguration;
import no.nav.security.token.support.jaxrs.servlet.JaxrsJwtTokenValidationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import static org.assertj.core.api.Assertions.assertThat;

class LocalSecurityConfigTest {

    private final LocalSecurityConfig config = new LocalSecurityConfig();

    @Test
    void multiIssuerConfiguration_returnsEmptyIssuersForLocalProfile() {
        MultiIssuerConfiguration issuerConfiguration = config.multiIssuerConfiguration();

        assertThat(issuerConfiguration.getIssuers()).isEmpty();
    }

    @Test
    void jaxrsJwtTokenValidationFilter_registersScimPatternWithOrderOne() {
        MultiIssuerConfiguration issuerConfiguration = config.multiIssuerConfiguration();

        FilterRegistrationBean<JaxrsJwtTokenValidationFilter> registration =
                config.jaxrsJwtTokenValidationFilter(issuerConfiguration);

        assertThat(registration.getFilter()).isInstanceOf(JaxrsJwtTokenValidationFilter.class);
        assertThat(registration.getUrlPatterns()).containsExactly("/scim/v2/*");
        assertThat(registration.getOrder()).isEqualTo(1);
    }
}

