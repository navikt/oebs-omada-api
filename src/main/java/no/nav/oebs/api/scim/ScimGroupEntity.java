package no.nav.oebs.api.scim;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity som mapper til XXRTV_OMADA_AKTIVE_GRUPPER_OG_ANSV_V
 * Representerer både grupper (G$) og ansvarsområder (A$)
 */
@Data
@Entity
@Table(name = "XXRTV_OMADA_AKTIVE_GRUPPER_OG_ANSV_V", schema = "XXRTV")
public class ScimGroupEntity {

    @Id
    @Column(name = "SCIM_ID")
    private String scimId;

    @Column(name = "SCIM_DISPLAY_NAME")
    private String scimDisplayName;

    @Column(name = "KILDE_TYPE")
    private String kildeType;  // G = gruppe, A = ansvarsområde

    @Column(name = "KILDE_ID")
    private Long kildeId;

    @Column(name = "KILDE_NAVN")
    private String kildeNavn;

    @Column(name = "BESKRIVELSE")
    private String beskrivelse;

    @Column(name = "START_DATO")
    private LocalDateTime startDato;

    @Column(name = "SLUTT_DATO")
    private LocalDateTime sluttDato;

    @Column(name = "OPPRETTET_DATO")
    private LocalDateTime opprettetDato;

    @Column(name = "SIST_OPPDATERT")
    private LocalDateTime sistOppdatert;

    public boolean isGroup() {
        return "G".equals(kildeType);
    }

    public boolean isResponsibility() {
        return "A".equals(kildeType);
    }

    public String getExternalId() {
        return kildeId != null ? String.valueOf(kildeId) : null;
    }
}
