package no.nav.oebs.api.db.repository;

import no.nav.oebs.api.db.entity.KallLogg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(propagation = Propagation.REQUIRES_NEW)
public interface KallLoggRepository extends JpaRepository<KallLogg, Integer>, KallLoggRepositoryCustom {

}
