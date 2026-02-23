package no.nav.oebs.api.scim;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/**
 * Entity som mapper til V_OMADA_ACTIVE_USERS view
 * Brukes for å hente brukerdata fra OeBS
 */
@Data
@Entity
@Table(name = "V_OMADA_ACTIVE_USERS", schema = "XXRTV")
public class ScimUserEntity {

    @Id
    @Column(name = "USER_ID")
    private Long userId;

    @Column(name = "BRUKER_ID")
    private String brukerId;  // SCIM userName

    @Column(name = "NAV_ID")
    private Long navId;  // SCIM externalId

    @Column(name = "FOR_NAVN")
    private String forNavn;  // SCIM givenName

    @Column(name = "ETTER_NAVN")
    private String etterNavn;  // SCIM familyName

    @Column(name = "E_POST")
    private String ePost;  // SCIM email

    @Column(name = "START_DATO")
    private LocalDate startDato;

    @Column(name = "SLUTT_DATO")
    private LocalDate sluttDato;

    @Column(name = "ACTIVE_FLAG")
    private String activeFlag;  // Y/N

    // Enterprise extension data - join med V_OMADA_USER_ENTERPRISE_EXT
    @Transient
    private String enhetsId;  // SCIM department

    @Transient
    private String arbeidsstedFylke;  // SCIM division

    // Groups - hentes separat fra V_OMADA_USER_ALL_GROUPS
    @Transient
    private List<ScimGroupMembershipEntity> groups;

    public boolean isActive() {
        return "Y".equals(activeFlag);
    }

    public String getFullName() {
        return forNavn + " " + etterNavn;
    }
}
