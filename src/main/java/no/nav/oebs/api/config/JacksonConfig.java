package no.nav.oebs.api.config;

import java.util.TimeZone;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.annotation.JsonInclude;

@Configuration
public class JacksonConfig {

	@Bean
	public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
		return builder.featuresToDisable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
				.timeZone(TimeZone.getDefault())
				.serializationInclusion(JsonInclude.Include.NON_NULL);
	}
}
