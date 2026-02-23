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
@Table(name = "XXRTV_OMADA_AKTIVE_BRUKERE_V", schema = "XXRTV")
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
    private String ePost;  // SCIM email (work)

    @Column(name = "E_POST_2")
    private String EPost2;  // Email from per_all_people_f (not used in SCIM yet)

    @Column(name = "START_DATO")
    private LocalDate startDato;

    @Column(name = "SLUTT_DATO")
    private LocalDate sluttDato;

    @Column(name = "ACTIVE_FLAG")
    private String activeFlag;  // Y/N

    // Enterprise extension data - now part of V_OMADA_ACTIVE_USERS
    @Column(name = "ENHETS_ID")
    private String enhetsId;  // SCIM department (last 4 digits of userName)

    @Column(name = "ARBEIDSSTED_FYLKE")
    private String arbeidsstedFylke;  // SCIM division (location_code)

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
