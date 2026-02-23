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
     * Hent bruker med enterprise extension data
     */
    @Query(value = """
        SELECT u.user_id, u.bruker_id, u.nav_id, u.for_navn, u.etter_navn, 
               u.e_post, u.start_dato, u.slutt_dato, u.active_flag,
               e.enhets_id, e.arbeidsted_fylke
        FROM V_OMADA_ACTIVE_USERS u
        LEFT JOIN V_OMADA_USER_ENTERPRISE_EXT e ON u.user_id = e.user_id
        WHERE u.bruker_id = :brukerId
        """, nativeQuery = true)
    Optional<Object[]> findUserWithEnterpriseExtension(@Param("brukerId") String brukerId);

    /**
     * Tell antall aktive brukere
     */
    @Query("SELECT COUNT(u) FROM ScimUserEntity u WHERE u.activeFlag = 'Y'")
    long countActiveUsers();
}
