package no.nav.oebs.api.scim;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity som mapper til XXRTV.XXRTV_OMADA_AKTIVE_BRUKERE_V
 * Brukes for å hente brukerdata fra OeBS (read-only, kun GET-operasjoner).
 */
@Data
@Entity
@Table(name = "XXRTV_OMADA_AKTIVE_BRUKERE_V", schema = "APPS")
public class ScimUserEntity {

    @Id
    @Column(name = "USER_ID")
    private Long userId;

    @Column(name = "BRUKER_ID")
    private String brukerId;  // SCIM userName

    @Column(name = "NAV_ID")
    private String navId;  // SCIM externalId - del av e-post før @, f.eks. K105317

    @Column(name = "FOR_NAVN")
    private String forNavn;  // SCIM givenName

    @Column(name = "ETTER_NAVN")
    private String etterNavn;  // SCIM familyName

    @Column(name = "E_POST")
    private String ePost;  // SCIM email (work) - FND_USER.EMAIL_ADDRESS, fallback

    @Column(name = "E_POST_2")
    private String EPost2;  // Email from per_all_people_f (not used in SCIM yet)

    @Column(name = "NAV_E_POST")
    private String navEPost;  // SCIM email (work) - NAV e-post fra PER_ALL_PEOPLE_F.COUNTRY_OF_BIRTH - primær e-post i SCIM om satt

    @Column(name = "START_DATO")
    private LocalDate startDato;

    @Column(name = "SLUTT_DATO")
    private LocalDate sluttDato;

    @Column(name = "ACTIVE_FLAG")
    private String activeFlag;  // Y/N

    @Column(name = "PERMISJON")
    private String permisjon;

    @Column(name = "LAST_UPDATE_DATE")
    private LocalDateTime lastUpdateDate;

    @Column(name = "CREATION_DATE")
    private LocalDateTime creationDate;

    @Column(name = "EFFECTIVE_START_DATE")
    private LocalDate effectiveStartDate;

    @Column(name = "EFFECTIVE_END_DATE")
    private LocalDate effectiveEndDate;

    // Enterprise extension data - now part of V_OMADA_ACTIVE_USERS
    @Column(name = "ENHETS_ID")
    private String enhetsId;  // SCIM department (last 4 digits of userName)

    @Column(name = "ARBEIDSSTED_FYLKE")
    private String arbeidsstedFylke;  // SCIM division (location_code)

    @Column(name = "FULLMAKT")
    private String fullmakt;  // per_job_definitions.segment1 - fullmakt-tittel fra per_jobs/per_job_definitions via assignment

    @Column(name = "EGENANSATT")
    private String egenansattFlag;  // 'Y' dersom bruker_id finnes i XXRTV_SKJERMING_TILGANG flex-verdisettet

    @Transient
    private List<ScimGroupMembershipEntity> groups;

    public boolean isActive() {
        return "Y".equals(activeFlag);
    }

    public boolean isEgenansatt() {
        return "Y".equals(egenansattFlag);
    }


    public String getFullName() {
        return forNavn + " " + etterNavn;
    }
}
