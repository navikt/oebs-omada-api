package no.nav.oebs.api.health;

import no.nav.oebs.api.db.repository.KallLoggRepository;
import org.springframework.stereotype.Component;

@Component
public class HealthCheckDbProbe {

	private KallLoggRepository kallLoggRepository;

	HealthCheckDbProbe(KallLoggRepository kallLoggRepository) {
		this.kallLoggRepository = kallLoggRepository;
	}

	public void pingDatabase() {
		kallLoggRepository.pingKallLogg();
	}
}
