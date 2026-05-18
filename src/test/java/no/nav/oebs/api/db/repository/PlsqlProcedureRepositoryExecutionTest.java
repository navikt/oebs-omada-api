package no.nav.oebs.api.db.repository;

import no.nav.oebs.api.exception.UgyldigInputException;
import no.nav.oebs.api.scim.KallLoggHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.UncategorizedDataAccessException;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlsqlProcedureRepositoryExecutionTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private KallLoggHelper kallLoggHelper;

    @Mock
    private SimpleJdbcCall jdbcCall;

    private PlsqlProcedureRepository repository;
    private ConcurrentMap<String, SimpleJdbcCall> jdbcCallCache;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        repository = new PlsqlProcedureRepository(dataSource, kallLoggHelper);
        ReflectionTestUtils.setField(repository, "orgId", 123L);
        jdbcCallCache = (ConcurrentMap<String, SimpleJdbcCall>) ReflectionTestUtils.getField(repository, "jdbcCallCache");
    }

    @Test
    void executeInOutProcedure_returnsResult_whenRetcodeIsOk() {
        jdbcCallCache.put("pkg.proc", jdbcCall);
        when(jdbcCall.execute(any(MapSqlParameterSource.class))).thenReturn(Map.of(
                "errbuf", "ok",
                "retcode", "0",
                "x_interface_msg_id", BigDecimal.valueOf(77)
        ));

        PlsqlProcedureResult result = repository.executeInOutProcedure("pkg.proc", PlsqlProcedureRepository.Operasjon.NY, "{}");

        assertThat(result.getMessageNumber()).isEqualTo(PlsqlMessageCodes.OK);
        assertThat(result.getRetcode()).isEqualTo("0");
        assertThat(result.getInterfaceMsgId()).isEqualTo(77L);
    }

    @Test
    void executeInOutProcedure_mapsMissingInterfaceMsgIdToNull() {
        jdbcCallCache.put("pkg.proc", jdbcCall);
        when(jdbcCall.execute(any(MapSqlParameterSource.class))).thenReturn(Map.of(
                "errbuf", "ok",
                "retcode", "0"
        ));

        PlsqlProcedureResult result = repository.executeInOutProcedure("pkg.proc", PlsqlProcedureRepository.Operasjon.NY, "{}");

        assertThat(result.getInterfaceMsgId()).isNull();
    }

    @Test
    void executeInOutProcedure_treatsBlankRetcodeAsOk() {
        jdbcCallCache.put("pkg.proc", jdbcCall);
        when(jdbcCall.execute(any(MapSqlParameterSource.class))).thenReturn(Map.of(
                "errbuf", "ok",
                "retcode", " "
        ));

        PlsqlProcedureResult result = repository.executeInOutProcedure("pkg.proc", PlsqlProcedureRepository.Operasjon.NY, "{}");

        assertThat(result.getMessageNumber()).isEqualTo(PlsqlMessageCodes.OK);
    }

    @Test
    void executeInOutProcedure_treatsMissingRetcodeAsOk() {
        jdbcCallCache.put("pkg.proc", jdbcCall);
        when(jdbcCall.execute(any(MapSqlParameterSource.class))).thenReturn(Map.of(
                "errbuf", "ok"
        ));

        PlsqlProcedureResult result = repository.executeInOutProcedure("pkg.proc", PlsqlProcedureRepository.Operasjon.NY, "{}");

        assertThat(result.getMessageNumber()).isEqualTo(PlsqlMessageCodes.OK);
        assertThat(result.getRetcode()).isNull();
    }

    @Test
    void executeInOutProcedure_throwsUgyldigInputException_whenMappedMessageNumberIsNegative() {
        jdbcCallCache.put("pkg.proc", jdbcCall);
        when(jdbcCall.execute(any(MapSqlParameterSource.class))).thenReturn(Map.of(
                "errbuf", "failed",
                "retcode", "2",
                "x_interface_msg_id", BigDecimal.ZERO
        ));

        assertThrows(UgyldigInputException.class,
                () -> repository.executeInOutProcedure("pkg.proc", PlsqlProcedureRepository.Operasjon.NY, "{}"));
    }

    @Test
    void executeInOutProcedure_rethrowsNonOra04068DataAccessException() {
        jdbcCallCache.put("pkg.proc", jdbcCall);
        SQLException cause = new SQLException("db", "state", 9999);
        UncategorizedDataAccessException ex = new UncategorizedDataAccessException("boom", cause) { };
        when(jdbcCall.execute(any(MapSqlParameterSource.class))).thenThrow(ex);

        UncategorizedDataAccessException thrown = assertThrows(UncategorizedDataAccessException.class,
                () -> repository.executeInOutProcedure("pkg.proc", PlsqlProcedureRepository.Operasjon.NY, "{}"));

        assertThat(thrown).isSameAs(ex);
    }

    @Test
    void executeInOutProcedure_retriesWhenOra04068_thenSucceeds() {
        jdbcCallCache.put("pkg.proc", jdbcCall);
        SQLException ora4068 = new SQLException("package state discarded", "state", 4068);
        UncategorizedDataAccessException firstFail = new UncategorizedDataAccessException("boom", ora4068) { };
        when(jdbcCall.execute(any(MapSqlParameterSource.class))).thenThrow(firstFail);

        try (MockedConstruction<SimpleJdbcCall> ignored = mockConstruction(SimpleJdbcCall.class, (mock, context) -> {
            when(mock.withCatalogName(anyString())).thenReturn(mock);
            when(mock.withProcedureName(anyString())).thenReturn(mock);
            when(mock.withSchemaName(anyString())).thenReturn(mock);
            when(mock.withoutProcedureColumnMetaDataAccess()).thenReturn(mock);
            when(mock.declareParameters(any(SqlParameter[].class))).thenReturn(mock);
            when(mock.execute(any(MapSqlParameterSource.class))).thenReturn(Map.of(
                    "errbuf", "ok",
                    "retcode", "0",
                    "x_interface_msg_id", BigDecimal.valueOf(11)
            ));
        })) {
            PlsqlProcedureResult result = repository.executeInOutProcedure("pkg.proc", PlsqlProcedureRepository.Operasjon.NY, "{}");

            assertThat(result.getMessageNumber()).isEqualTo(PlsqlMessageCodes.OK);
            assertThat(result.getInterfaceMsgId()).isEqualTo(11L);
            verify(kallLoggHelper).loggUt(anyString(), anyString(), anyInt(), anyLong(), any(), any(), any());
        }
    }

    @Test
    void executeSyncProcedure_mapsWarningRetcodeToExceptionMessageNumber() {
        jdbcCallCache.put("pkg.sync", jdbcCall);
        when(jdbcCall.execute(any(MapSqlParameterSource.class))).thenReturn(Map.of(
                "errbuf", "warn",
                "retcode", BigDecimal.ONE,
                "phase", "COMPLETE",
                "status", "NORMAL",
                "dev_phase", "COMPLETE",
                "dev_status", "NORMAL",
                "message", "sync warning"
        ));

        PlsqlProcedureResult result = repository.executeSyncProcedure("pkg.sync", 55L);

        assertThat(result.getMessageNumber()).isEqualTo(PlsqlMessageCodes.EXCEPTION);
        assertThat(result.getRetcode()).isEqualTo("1");
        assertThat(result.getDevPhase()).isEqualTo("COMPLETE");
        assertThat(result.getDevStatus()).isEqualTo("NORMAL");
    }

    @Test
    void executeSyncProcedure_defaultsNullRetcodeToZero() {
        jdbcCallCache.put("pkg.sync", jdbcCall);
        when(jdbcCall.execute(any(MapSqlParameterSource.class))).thenReturn(Map.of(
                "errbuf", "ok",
                "phase", "PENDING",
                "status", "NORMAL",
                "dev_phase", "PENDING",
                "dev_status", "NORMAL",
                "message", "sync started"
        ));

        PlsqlProcedureResult result = repository.executeSyncProcedure("pkg.sync", 99L);

        assertThat(result.getMessageNumber()).isEqualTo(PlsqlMessageCodes.OK);
        assertThat(result.getRetcode()).isEqualTo("0");
        assertThat(result.getDevPhase()).isEqualTo("PENDING");
    }

    @Test
    void executeSyncProcedure_mapsRetcodeGte2ToException() {
        jdbcCallCache.put("pkg.sync", jdbcCall);
        when(jdbcCall.execute(any(MapSqlParameterSource.class))).thenReturn(Map.of(
                "errbuf", "failed",
                "retcode", BigDecimal.valueOf(2),
                "phase", "COMPLETE",
                "status", "ERROR",
                "dev_phase", "COMPLETE",
                "dev_status", "ERROR",
                "message", "sync failed"
        ));

        PlsqlProcedureResult result = repository.executeSyncProcedure("pkg.sync", 100L);

        assertThat(result.getMessageNumber()).isEqualTo(PlsqlMessageCodes.EXCEPTION);
        assertThat(result.getRetcode()).isEqualTo("2");
    }

    @Test
    void executeSyncProcedure_rethrowsNonOra04068DataAccessException() {
        jdbcCallCache.put("pkg.sync", jdbcCall);
        SQLException cause = new SQLException("db", "state", 9999);
        UncategorizedDataAccessException ex = new UncategorizedDataAccessException("boom", cause) { };
        when(jdbcCall.execute(any(MapSqlParameterSource.class))).thenThrow(ex);

        UncategorizedDataAccessException thrown = assertThrows(UncategorizedDataAccessException.class,
                () -> repository.executeSyncProcedure("pkg.sync", 123L));

        assertThat(thrown).isSameAs(ex);
    }

    @Test
    void executeSyncProcedure_retriesWhenOra04068_thenSucceeds() {
        jdbcCallCache.put("pkg.sync", jdbcCall);
        SQLException ora4068 = new SQLException("package state discarded", "state", 4068);
        UncategorizedDataAccessException firstFail = new UncategorizedDataAccessException("boom", ora4068) { };
        when(jdbcCall.execute(any(MapSqlParameterSource.class))).thenThrow(firstFail);

        try (MockedConstruction<SimpleJdbcCall> ignored = mockConstruction(SimpleJdbcCall.class, (mock, context) -> {
            when(mock.withCatalogName(anyString())).thenReturn(mock);
            when(mock.withProcedureName(anyString())).thenReturn(mock);
            when(mock.withSchemaName(anyString())).thenReturn(mock);
            when(mock.withoutProcedureColumnMetaDataAccess()).thenReturn(mock);
            when(mock.declareParameters(any(SqlParameter[].class))).thenReturn(mock);
            when(mock.execute(any(MapSqlParameterSource.class))).thenReturn(Map.of(
                    "errbuf", "ok",
                    "retcode", BigDecimal.ZERO,
                    "phase", "COMPLETE",
                    "status", "NORMAL",
                    "dev_phase", "COMPLETE",
                    "dev_status", "NORMAL",
                    "message", "sync ok"
            ));
        })) {
            PlsqlProcedureResult result = repository.executeSyncProcedure("pkg.sync", 12L);

            assertThat(result.getMessageNumber()).isEqualTo(PlsqlMessageCodes.OK);
            assertThat(result.getRetcode()).isEqualTo("0");
            verify(kallLoggHelper, times(1)).loggUt(anyString(), anyString(), anyInt(), anyLong(), any(), any(), any());
        }
    }

    @Test
    void getJdbcCall_createsAndCachesOnMiss_andReusesOnHit() {
        Object first = ReflectionTestUtils.invokeMethod(
                repository,
                "getJdbcCall",
                "pkg.proc",
                new SqlParameter[0]
        );
        assertThat(first).isNotNull();
        assertThat(jdbcCallCache).containsKey("pkg.proc");

        Object second = ReflectionTestUtils.invokeMethod(
                repository,
                "getJdbcCall",
                "pkg.proc",
                new SqlParameter[0]
        );
        assertThat(second).isSameAs(first);
    }

    @Test
    void buildJdbcCall_supportsThreePartProcedureName() {
        Object call = ReflectionTestUtils.invokeMethod(
                repository,
                "buildJdbcCall",
                "schema.pkg.proc",
                new SqlParameter[0]
        );
        assertThat(call).isNotNull();
    }

    @Test
    void isOra04068_detectsSqlErrorCodeInCauseChain() {
        SQLException ora = new SQLException("ora", "state", 4068);
        Exception wrapper = new RuntimeException(new RuntimeException(ora));

        Boolean detected = ReflectionTestUtils.invokeMethod(repository, "isOra04068", wrapper);

        assertThat(detected).isTrue();
    }

    @Test
    void isOra04068_returnsFalseWhenNotPresent() {
        SQLException sql = new SQLException("other", "state", 9999);
        Exception wrapper = new RuntimeException(sql);

        Boolean detected = ReflectionTestUtils.invokeMethod(repository, "isOra04068", wrapper);

        assertThat(detected).isFalse();
    }

    @Test
    void executeInOutProcedure_acceptsThreePartProcedureName() {
        jdbcCallCache.put("schema.pkg.proc", jdbcCall);
        when(jdbcCall.execute(any(MapSqlParameterSource.class))).thenReturn(Map.of(
                "errbuf", "ok",
                "retcode", "0"
        ));

        assertDoesNotThrow(() -> repository.executeInOutProcedure("schema.pkg.proc", PlsqlProcedureRepository.Operasjon.NY, "{}"));
    }

    @Test
    void evictAndRebuildJdbcCall_replacesCacheAndLogsRetry() {
        jdbcCallCache.put("schema.pkg.proc", jdbcCall);

        Object rebuilt = ReflectionTestUtils.invokeMethod(
                repository,
                "evictAndRebuildJdbcCall",
                "schema.pkg.proc",
                new SqlParameter[0]
        );

        assertThat(rebuilt).isNotNull();
        assertThat(jdbcCallCache.get("schema.pkg.proc")).isNotNull();
        verify(kallLoggHelper).loggUt(anyString(), anyString(), anyInt(), anyLong(), any(), any(), any());
    }
}

