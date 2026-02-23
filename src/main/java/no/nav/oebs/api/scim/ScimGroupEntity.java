package no.nav.oebs.api.scim;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity som mapper til V_OMADA_ACTIVE_GROUPS og V_OMADA_ACTIVE_RESPONSIBILITIES views
 * Representerer både grupper (G$) og ansvarsområder (A$)
 */
@Data
@Entity
@Table(name = "XXRTV_OMADA_AKTIVE_GRUPPER_V", schema = "XXRTV")
public class ScimGroupEntity {

    @Id
    @Column(name = "SCIM_ID")
    private String scimId;  // G$... eller A$...

    @Column(name = "SCIM_DISPLAY_NAME")
    private String scimDisplayName;

    @Column(name = "GROUP_ID")
    private Long groupId;  // For grupper

    @Column(name = "RESPONSIBILITY_ID")
    private Long responsibilityId;  // For ansvarsområder

    @Column(name = "GROUP_NAME")
    private String groupName;

    @Column(name = "RESPONSIBILITY_NAME")
    private String responsibilityName;

    @Column(name = "CREATION_DATE")
    private LocalDateTime creationDate;

    @Column(name = "LAST_UPDATE_DATE")
    private LocalDateTime lastUpdateDate;

    @Transient
    private String groupType;  // GROUP eller RESPONSIBILITY

    public String getExternalId() {
        if (scimId.startsWith("G$")) {
            return groupId != null ? String.valueOf(groupId) : null;
        } else if (scimId.startsWith("A$")) {
            return responsibilityId != null ? String.valueOf(responsibilityId) : null;
        }
        return null;
    }

    public boolean isGroup() {
        return scimId != null && scimId.startsWith("G$");
    }

    public boolean isResponsibility() {
        return scimId != null && scimId.startsWith("A$");
    }
}
