package no.nav.oebs.api.scim.repository;

import no.nav.oebs.api.scim.ScimGroupMembershipEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for å hente gruppe-medlemskap fra V_OMADA_USER_ALL_GROUPS view
 */
@Repository
public interface ScimGroupMembershipRepository extends JpaRepository<ScimGroupMembershipEntity, Long> {

    /**
     * Hent alle grupper for en bruker
     */
    List<ScimGroupMembershipEntity> findByBrukerId(String brukerId);

    /**
     * Hent alle medlemmer av en gruppe
     */
    List<ScimGroupMembershipEntity> findByScimGroupId(String scimGroupId);

    /**
     * Hent alle grupper og ansvarsområder for en bruker via navId
     */
    List<ScimGroupMembershipEntity> findByNavId(String navId);

    /**
     * Hent alle grupper for en bruker via navId
     */
    @Query("SELECT g FROM ScimGroupMembershipEntity g WHERE g.navId = :navId AND g.groupType = 'GROUP'")
    List<ScimGroupMembershipEntity> findGroupsByNavId(@Param("navId") String navId);

    /**
     * Hent kun ansvarsområder (A$) for en bruker via navId
     */
    @Query("SELECT g FROM ScimGroupMembershipEntity g WHERE g.navId = :navId AND g.groupType = 'RESPONSIBILITY'")
    List<ScimGroupMembershipEntity> findResponsibilitiesByNavId(@Param("navId") String navId);
}
