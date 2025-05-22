package no.nav.oebs.api.ping.v1;

import no.nav.security.token.support.core.api.Unprotected;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import no.nav.oebs.api.health.HealthCheckDbProbe;
//import no.nav.security.token.support.core.api.Unprotected;


 //@RestController
@RequestMapping(path = "/api/v1")
// @Api(tags = { SwaggerConfig.PING_TAG })
public class PingController {

	private HealthCheckDbProbe healthCheckDbProbe;

	public PingController(HealthCheckDbProbe healthCheckDbProbe) {
		this.healthCheckDbProbe = healthCheckDbProbe;
	}

	/**
	 * Sjekker om databasen er tilgjengelig.
	 */
	@Unprotected
	@GetMapping(path = "/ping")
	@PingSwagger
	public void ping() {
		healthCheckDbProbe.pingDatabase();
	}
}
