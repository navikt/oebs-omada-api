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
     * Hent kun grupper (G$) for en bruker
     */
    @Query("SELECT g FROM ScimGroupMembershipEntity g WHERE g.brukerId = :brukerId AND g.groupType = 'GROUP'")
    List<ScimGroupMembershipEntity> findGroupsByBrukerId(@Param("brukerId") String brukerId);

    /**
     * Hent kun ansvarsområder (A$) for en bruker
     */
    @Query("SELECT g FROM ScimGroupMembershipEntity g WHERE g.brukerId = :brukerId AND g.groupType = 'RESPONSIBILITY'")
    List<ScimGroupMembershipEntity> findResponsibilitiesByBrukerId(@Param("brukerId") String brukerId);
}
