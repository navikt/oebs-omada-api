package no.nav.oebs.api.scim.provider;

import no.nav.oebs.api.db.repository.PlsqlMessageCodes;
import no.nav.oebs.api.db.repository.PlsqlProcedureResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static no.nav.oebs.api.scim.provider.ScimUserResourceProvider.isSyncPending;
import static no.nav.oebs.api.scim.provider.ScimUserResourceProvider.mapSyncRetcodeToHttpStatus;
import static no.nav.oebs.api.scim.provider.ScimUserResourceProvider.syncSuccessHttpStatus;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tester retcode-til-HTTP-status-mapping og pending-logikk for sync-prosedyren.
 *
 * Oracle concurrent program-konvensjon:
 *   retcode=0 → suksess
 *   retcode=1 → advarsel → HTTP 422
 *   retcode=2+ → feil    → HTTP 500
 *
 * Pending-konvensjon:
 *   dev_phase=PENDING/RUNNING + dev_status=STANDBY/NORMAL → HTTP 202 Accepted
 */
class ScimUserResourceProviderRetcodeTest {

    // =========================================================================
    // mapSyncRetcodeToHttpStatus
    // =========================================================================

    @Test
    void retcode_1_advarsel_gir_422() {
        assertThat(mapSyncRetcodeToHttpStatus("1")).isEqualTo(422);
    }

    @Test
    void retcode_2_feil_gir_500() {
        assertThat(mapSyncRetcodeToHttpStatus("2")).isEqualTo(500);
    }

    @ParameterizedTest(name = "retcode={0} → 500")
    @ValueSource(strings = {"3", "99", "UKJENT", "FEIL"})
    void retcode_over2_eller_ugyldig_gir_500(String retcode) {
        assertThat(mapSyncRetcodeToHttpStatus(retcode)).isEqualTo(500);
    }

    @NullSource
    @ParameterizedTest(name = "retcode=null → 500")
    void retcode_null_gir_500(String retcode) {
        assertThat(mapSyncRetcodeToHttpStatus(retcode)).isEqualTo(500);
    }

    @ParameterizedTest(name = "HTTP {0} brukes direkte")
    @ValueSource(strings = {"200", "201", "400", "404", "422", "500", "503"})
    void gyldige_http_statuskoder_brukes_direkte(String retcode) {
        assertThat(mapSyncRetcodeToHttpStatus(retcode)).isEqualTo(Integer.parseInt(retcode));
    }

    // =========================================================================
    // syncSuccessHttpStatus — operasjonstype → HTTP-suksess-statuskode
    // =========================================================================

    @Test
    void create_gir_201() {
        assertThat(syncSuccessHttpStatus("CREATE")).isEqualTo(201);
    }

    @Test
    void delete_gir_200() {
        assertThat(syncSuccessHttpStatus("DELETE")).isEqualTo(200);
    }

    @ParameterizedTest(name = "operasjon={0} → 200")
    @ValueSource(strings = {"UPDATE", "ENDRE", "UKJENT", ""})
    void update_og_ukjente_operasjoner_gir_200(String operasjon) {
        assertThat(syncSuccessHttpStatus(operasjon)).isEqualTo(200);
    }

    @NullSource
    @ParameterizedTest(name = "operasjon=null → 200")
    void null_operasjon_gir_200(String operasjon) {
        assertThat(syncSuccessHttpStatus(operasjon)).isEqualTo(200);
    }

    @ParameterizedTest(name = "case-insensitive: {0} → 201")
    @ValueSource(strings = {"create", "Create", "CREATE"})
    void create_er_case_insensitive(String operasjon) {
        assertThat(syncSuccessHttpStatus(operasjon)).isEqualTo(201);
    }

    @ParameterizedTest(name = "case-insensitive: {0} → 200")
    @ValueSource(strings = {"delete", "Delete", "DELETE"})
    void delete_er_case_insensitive(String operasjon) {
        assertThat(syncSuccessHttpStatus(operasjon)).isEqualTo(200);
    }

    private static PlsqlProcedureResult syncResult(String retcode, String devPhase, String devStatus) {
        int messageNumber = "0".equals(retcode) ? PlsqlMessageCodes.OK : PlsqlMessageCodes.EXCEPTION;
        return new PlsqlProcedureResult(null, messageNumber, null, null, retcode, devPhase, devStatus);
    }

    @ParameterizedTest(name = "phase={0} + NORMAL → pending")
    @ValueSource(strings = {"PENDING", "RUNNING", "pending", "running"})
    void pending_phase_med_normal_status_gir_pending(String phase) {
        assertThat(isSyncPending(syncResult("0", phase, "NORMAL"))).isTrue();
    }

    @ParameterizedTest(name = "phase={0} + STANDBY → pending")
    @ValueSource(strings = {"PENDING", "RUNNING"})
    void pending_phase_med_standby_status_gir_pending(String phase) {
        assertThat(isSyncPending(syncResult("0", phase, "STANDBY"))).isTrue();
    }

    @ParameterizedTest(name = "retcode=1 + phase={0} + NORMAL → pending")
    @ValueSource(strings = {"PENDING", "RUNNING"})
    void retcode_1_med_pending_phase_gir_pending(String phase) {
        assertThat(isSyncPending(syncResult("1", phase, "NORMAL"))).isTrue();
    }

    @ParameterizedTest(name = "retcode=1 + phase={0} + STANDBY → pending")
    @ValueSource(strings = {"PENDING", "RUNNING"})
    void retcode_1_med_pending_phase_og_standby_gir_pending(String phase) {
        assertThat(isSyncPending(syncResult("1", phase, "STANDBY"))).isTrue();
    }

    @Test
    void retcode_2_trumfer_pending() {
        // retcode=2 er ekte feil — skal ikke gi pending selv om phase/status tilsier det
        assertThat(isSyncPending(syncResult("2", "PENDING", "NORMAL"))).isFalse();
    }

    @ParameterizedTest(name = "retcode={0} trumfer pending")
    @ValueSource(strings = {"3", "99"})
    void hoey_retcode_trumfer_pending(String retcode) {
        assertThat(isSyncPending(syncResult(retcode, "PENDING", "NORMAL"))).isFalse();
    }

    @Test
    void complete_phase_er_ikke_pending() {
        assertThat(isSyncPending(syncResult("0", "COMPLETE", "NORMAL"))).isFalse();
    }

    @ParameterizedTest(name = "status={0} med PENDING phase → ikke pending")
    @ValueSource(strings = {"PAUSED", "ERROR", "CANCELLED", "COMPLETE"})
    void ukjent_status_er_ikke_pending(String status) {
        assertThat(isSyncPending(syncResult("0", "PENDING", status))).isFalse();
    }

    @Test
    void null_phase_og_status_er_ikke_pending() {
        assertThat(isSyncPending(syncResult("0", null, null))).isFalse();
    }
}

