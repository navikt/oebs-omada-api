package no.nav.oebs.api.scim.repository;

import no.nav.oebs.api.scim.ScimGroupEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for å hente grupper og ansvarsområder
 */
@Repository
public interface ScimGroupRepository extends JpaRepository<ScimGroupEntity, String> {

    /**
     * Finn gruppe eller ansvarsområde basert på SCIM ID
     */
    Optional<ScimGroupEntity> findByScimId(String scimId);

    /**
     * Hent alle grupper (G$)
     */
    @Query(value = """
        SELECT scim_id, scim_display_name, group_id, NULL as responsibility_id,
               group_name, NULL as responsibility_name
        FROM XXRTV.XXRTV_OMADA_AKTIVE_GRUPPER_V
        ORDER BY scim_id
        """, nativeQuery = true)
    Page<ScimGroupEntity> findAllGroups(Pageable pageable);

    /**
     * Hent alle ansvarsområder (A$)
     */
    @Query(value = """
        SELECT scim_id, scim_display_name, NULL as group_id, responsibility_id,
               NULL as group_name, responsibility_name
        FROM XXRTV.XXRTV_OMADA_AKTIVE_ANSVARSOMRAADER_V
        ORDER BY scim_id
        """, nativeQuery = true)
    Page<ScimGroupEntity> findAllResponsibilities(Pageable pageable);

    /**
     * Hent alle grupper og ansvarsområder (union)
     */
    @Query(value = """
        SELECT scim_id, scim_display_name, group_id, NULL as responsibility_id,
               group_name, NULL as responsibility_name
        FROM XXRTV.XXRTV_OMADA_AKTIVE_GRUPPER_V
        UNION ALL
        SELECT scim_id, scim_display_name, NULL as group_id, responsibility_id,
               NULL as group_name, responsibility_name
        FROM XXRTV.XXRTV_OMADA_AKTIVE_ANSVARSOMRAADER_V
        ORDER BY scim_id
        """, nativeQuery = true)
    Page<ScimGroupEntity> findAllGroupsAndResponsibilities(Pageable pageable);

    /**
     * Tell totalt antall grupper og ansvarsområder
     */
    @Query(value = """
        SELECT COUNT(*) FROM (
            SELECT scim_id FROM XXRTV.XXRTV_OMADA_AKTIVE_GRUPPER_V
            UNION ALL
            SELECT scim_id FROM XXRTV.XXRTV_OMADA_AKTIVE_ANSVARSOMRAADER_V
        )
        """, nativeQuery = true)
    long countAllGroupsAndResponsibilities();
}
