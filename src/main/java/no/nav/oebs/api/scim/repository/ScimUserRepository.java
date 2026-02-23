package no.nav.oebs.api.scim.repository;

import no.nav.oebs.api.scim.ScimUserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for å hente brukerdata fra V_OMADA_ACTIVE_USERS view
 */
@Repository
public interface ScimUserRepository extends JpaRepository<ScimUserEntity, Long> {

    /**
     * Finn bruker basert på brukerId (userName i SCIM)
     */
    Optional<ScimUserEntity> findByBrukerId(String brukerId);

    /**
     * Finn bruker basert på navId (externalId i SCIM)
     */
    Optional<ScimUserEntity> findByNavId(Long navId);

    /**
     * Hent alle aktive brukere (paginert)
     */
    @Query("SELECT u FROM ScimUserEntity u WHERE u.activeFlag = 'Y' ORDER BY u.brukerId")
    Page<ScimUserEntity> findAllActiveUsers(Pageable pageable);


    /**
     * Tell antall aktive brukere
     */
    @Query("SELECT COUNT(u) FROM ScimUserEntity u WHERE u.activeFlag = 'Y'")
    long countActiveUsers();
}
