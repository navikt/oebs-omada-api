package no.nav.oebs.api.db.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import lombok.extern.slf4j.Slf4j;
import no.nav.oebs.api.db.entity.KallLogg;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
public class KallLoggRepositoryImpl implements KallLoggRepositoryCustom {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public void pingKallLogg() {
		log.info("Ping database kall logg");
		List<KallLogg> resultList = entityManager.createQuery("SELECT k FROM KallLogg k WHERE k.id = 0", KallLogg.class) //
				.getResultList();
		log.info("Ping database kall logg OK: " + resultList);
	}
}
