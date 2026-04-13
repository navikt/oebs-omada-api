package no.nav.oebs.api.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
/**
 * Aktiverer Spring {@code @Scheduled}-støtte for applikasjonen.
 * KallLoggOppryddingService bruker dette for daglig opprydding.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
