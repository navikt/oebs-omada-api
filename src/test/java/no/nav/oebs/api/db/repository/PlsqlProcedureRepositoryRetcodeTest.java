package no.nav.oebs.api.db.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static no.nav.oebs.api.db.repository.PlsqlProcedureRepository.mapInsertRetcode;
import static no.nav.oebs.api.db.repository.PlsqlProcedureRepository.mapSyncRetcode;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tester retcode-mapping-logikken for InsertOmadaMessage og start_import_ident_melding.
 * Disse testene dokumenterer og sikrer Oracle concurrent program-konvensjonen:
 *   0 / null / blank = suksess
 *   1               = advarsel (kun insert, sync feiler på alt != 0)
 *   2+              = feil
 */
class PlsqlProcedureRepositoryRetcodeTest {

    // =========================================================================
    // mapInsertRetcode — varchar2 retcode fra InsertOmadaMessage
    // =========================================================================

    @ParameterizedTest(name = "retcode={0} → OK")
    @NullAndEmptySource
    @ValueSource(strings = {"0", " ", "  "})
    void mapInsertRetcode_success(String retcode) {
        assertThat(mapInsertRetcode(retcode)).isEqualTo(PlsqlMessageCodes.OK);
    }

    @Test
    void mapInsertRetcode_1_isWarning() {
        assertThat(mapInsertRetcode("1")).isEqualTo(1);
    }

    @ParameterizedTest(name = "retcode={0} → EXCEPTION")
    @ValueSource(strings = {"2", "3", "99", "FEIL", "ERROR"})
    void mapInsertRetcode_nonZeroNonOne_isException(String retcode) {
        assertThat(mapInsertRetcode(retcode)).isEqualTo(PlsqlMessageCodes.EXCEPTION);
    }

    // HTTP-statuskoder (brukes av noen OeBS-prosedyrer)

    @ParameterizedTest(name = "HTTP {0} → OK")
    @ValueSource(strings = {"200", "201", "204", "299"})
    void mapInsertRetcode_http2xx_isOk(String retcode) {
        assertThat(mapInsertRetcode(retcode)).isEqualTo(PlsqlMessageCodes.OK);
    }

    @ParameterizedTest(name = "HTTP {0} → advarsel")
    @ValueSource(strings = {"300", "301", "399"})
    void mapInsertRetcode_http3xx_isWarning(String retcode) {
        assertThat(mapInsertRetcode(retcode)).isEqualTo(1);
    }

    @ParameterizedTest(name = "HTTP {0} → EXCEPTION")
    @ValueSource(strings = {"400", "404", "500", "503"})
    void mapInsertRetcode_http4xxAnd5xx_isException(String retcode) {
        assertThat(mapInsertRetcode(retcode)).isEqualTo(PlsqlMessageCodes.EXCEPTION);
    }

    // =========================================================================
    // mapSyncRetcode — number retcode fra start_import_ident_melding
    // Både advarsel (1) og feil (2) skal gi EXCEPTION
    // =========================================================================

    @Test
    void mapSyncRetcode_0_isOk() {
        assertThat(mapSyncRetcode(0)).isEqualTo(PlsqlMessageCodes.OK);
    }

    @ParameterizedTest(name = "retcode={0} → EXCEPTION")
    @ValueSource(ints = {1, 2, 3, 99})
    void mapSyncRetcode_nonZero_isException(int retcode) {
        assertThat(mapSyncRetcode(retcode)).isEqualTo(PlsqlMessageCodes.EXCEPTION);
    }
}

