package no.nav.oebs.api.config;

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import org.springframework.context.annotation.Configuration;

/**
 * Sikkerhetskonfigurasjon.
 * {@code @EnableJwtTokenValidation} aktiverer JWT-validering for alle endepunkter unntatt
 * de som er annotert med {@code @Unprotected} eller er i ignored packages.
 * SCIMple-endepunkter beskyttes via {@code @Protected} / {@code @Unprotected} på ressursklassene.
 */
@Configuration
@EnableJwtTokenValidation(ignore = { "org.springframework", "org.springdoc" })
public class SecurityConfig {
}