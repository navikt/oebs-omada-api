package no.nav.oebs.api.Group.v1;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Entity
@Immutable
@Table(name = "XXRTV_OEBS_SCIM_GROUPS_V")
public class OebsScimGroupView {

    @Id
    @Column(name = "SCIM_ID")
    private String scimId;

    @Column(name = "OBJECT_TYPE")
    private String objectType;  // 'GROUP' eller 'RESP'

    @Column(name = "OBJECT_ID")
    private Long objectId;

    @Column(name = "OBJECT_CODE")
    private String objectCode;  // group_number / responsibility_key

    @Column(name = "OBJECT_NAME")
    private String objectName;  // group_name / responsibility_name

    @Column(name = "OBJECT_DESC")
    private String objectDesc;  // group_desc, NULL for RESP

    @Column(name = "START_DATE")
    private LocalDateTime startDate;

    @Column(name = "END_DATE")
    private LocalDateTime endDate;

    protected OebsScimGroupView() {
        // JPA
    }

    public String getScimId() {
        return scimId;
    }

    public String getObjectType() {
        return objectType;
    }

    public Long getObjectId() {
        return objectId;
    }

    public String getObjectCode() {
        return objectCode;
    }

    public String getObjectName() {
        return objectName;
    }

    public String getObjectDesc() {
        return objectDesc;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }
}
