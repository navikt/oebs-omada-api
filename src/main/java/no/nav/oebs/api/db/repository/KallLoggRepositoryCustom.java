package no.nav.oebs.api.db.repository;

import java.time.LocalDateTime;

public interface KallLoggRepositoryCustom {

	void pingKallLogg();

	/**
	 * Sletter alle KallLogg-rader med tidspunkt eldre enn {@code grense}.
	 * @return antall slettede rader
	 */
	int slettGamleRader(LocalDateTime grense);

	/** Teller rader som ville blitt slettet — uten å slette dem. @return antall rader eldre enn {@code grense} */
	long tellGamleRader(LocalDateTime grense);

	/** Sletter samtlige rader i KallLogg-tabellen. @return antall slettede rader */
	int slettAlleRader();
}
