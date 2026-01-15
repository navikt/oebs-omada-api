package no.nav.oebs.api.config;

import java.util.TimeZone;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.DeserializationFeature;

@Configuration
public class JacksonConfig {

	@Bean
	public JsonMapperBuilderCustomizer jacksonCustomizer() {
		return builder -> builder.disable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY) //
				.defaultTimeZone(TimeZone.getDefault()); // Bruk plattform default som default, ikke UTC.
	}
}
