package no.nav.oebs.api.scim.repository;

import no.nav.oebs.api.scim.ScimGroupEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for XXRTV_OMADA_AKTIVE_GRUPPER_OG_ANSV_V
 * Henter grupper (G$) og ansvarsområder (A$) via det kombinerte viewet
 */
@Repository
public interface ScimGroupRepository extends JpaRepository<ScimGroupEntity, String> {

    Optional<ScimGroupEntity> findByScimId(String scimId);

    Page<ScimGroupEntity> findAllByOrderByScimIdAsc(Pageable pageable);

    @Query("SELECT COUNT(e) FROM ScimGroupEntity e")
    long countAllGroupsAndResponsibilities();
}
