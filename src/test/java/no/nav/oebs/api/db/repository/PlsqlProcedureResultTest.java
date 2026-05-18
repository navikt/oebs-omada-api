package no.nav.oebs.api.db.repository;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataRetrievalFailureException;

import java.math.BigDecimal;
import java.sql.Clob;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlsqlProcedureResultTest {

    @Test
    void stringConstructor_defaultsMessageNumberWhenNull() {
        PlsqlProcedureResult result = new PlsqlProcedureResult("data", null, "msg", 1L, "0");

        assertThat(result.getData()).isEqualTo("data");
        assertThat(result.getMessageNumber()).isEqualTo(PlsqlMessageCodes.OK);
        assertThat(result.getMessage()).isEqualTo("msg");
        assertThat(result.getInterfaceMsgId()).isEqualTo(1L);
        assertThat(result.getRetcode()).isEqualTo("0");
        assertThat(result.getDevPhase()).isNull();
        assertThat(result.getDevStatus()).isNull();
    }

    @Test
    void clobConstructor_readsClobAndConvertsBigDecimal() throws Exception {
        Clob clob = mock(Clob.class);
        when(clob.length()).thenReturn(4L);
        when(clob.getSubString(1, 4)).thenReturn("json");

        PlsqlProcedureResult result = new PlsqlProcedureResult(
                clob,
                BigDecimal.valueOf(5),
                "ok",
                2L,
                "1",
                "RUNNING",
                "NORMAL"
        );

        assertThat(result.getData()).isEqualTo("json");
        assertThat(result.getMessageNumber()).isEqualTo(5);
        assertThat(result.getMessage()).isEqualTo("ok");
        assertThat(result.getInterfaceMsgId()).isEqualTo(2L);
        assertThat(result.getRetcode()).isEqualTo("1");
        assertThat(result.getDevPhase()).isEqualTo("RUNNING");
        assertThat(result.getDevStatus()).isEqualTo("NORMAL");
        verify(clob).length();
        verify(clob).getSubString(1, 4);
    }

    @Test
    void fiveArgClobConstructor_delegatesAndSetsNullSyncFields() throws Exception {
        Clob clob = mock(Clob.class);
        when(clob.length()).thenReturn(3L);
        when(clob.getSubString(1, 3)).thenReturn("abc");

        PlsqlProcedureResult result = new PlsqlProcedureResult(clob, BigDecimal.ONE, "ok", 9L, "0");

        assertThat(result.getData()).isEqualTo("abc");
        assertThat(result.getMessageNumber()).isEqualTo(1);
        assertThat(result.getDevPhase()).isNull();
        assertThat(result.getDevStatus()).isNull();
    }

    @Test
    void clobConstructor_handlesNullClobAndNullMessageNumber() {
        PlsqlProcedureResult result = new PlsqlProcedureResult(
                (Clob) null,
                null,
                "ok",
                null,
                "0",
                null,
                null
        );

        assertThat(result.getData()).isNull();
        assertThat(result.getMessageNumber()).isEqualTo(PlsqlMessageCodes.OK);
    }

    @Test
    void clobConstructor_wrapsSqlExceptionAsDataRetrievalFailureException() throws Exception {
        Clob clob = mock(Clob.class);
        when(clob.length()).thenThrow(new SQLException("CLOB read failed"));

        DataRetrievalFailureException ex = assertThrows(DataRetrievalFailureException.class,
                () -> new PlsqlProcedureResult(clob, BigDecimal.ONE, "msg", 1L, "1", null, null));

        assertThat(ex.getMessage()).contains("Feil ved lesing av clob-verdi");
        assertThat(ex.getCause()).isInstanceOf(SQLException.class);
    }
}

