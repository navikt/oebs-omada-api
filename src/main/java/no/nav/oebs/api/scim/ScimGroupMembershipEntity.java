package no.nav.oebs.api.scim;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Entity som mapper til XXRTV_OMADA_SCIM_GRPS_BRUKER_V view
 * Representerer gruppe-medlemskap for en bruker
 */
@Data
@Entity
@Table(name = "XXRTV_OMADA_SCIM_GRPS_BRUKER_V", schema = "APPS")
@IdClass(ScimGroupMembershipId.class)
public class ScimGroupMembershipEntity {

    @Id
    @Column(name = "USER_ID")
    private Long userId;

    @Id
    @Column(name = "SCIM_GROUP_ID")
    private String scimGroupId;  // G$... eller A$...

    @Column(name = "BRUKER_ID")
    private String brukerId;

    @Column(name = "NAV_ID")
    private String navId;

    @Column(name = "SCIM_DISPLAY_NAME")
    private String scimDisplayName;

    @Column(name = "GROUP_TYPE")
    private String groupType;  // GROUP eller RESPONSIBILITY
}
