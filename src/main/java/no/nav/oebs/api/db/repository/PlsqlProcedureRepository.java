package no.nav.oebs.api.db.repository;

import java.math.BigDecimal;
import java.sql.Clob;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.sql.DataSource;

import no.nav.oebs.api.config.common.mdc.MdcOperations;
import no.nav.oebs.api.exception.UgyldigInputException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;

import no.nav.oebs.api.config.common.logging.LoggingUtils;
import no.nav.oebs.api.db.entity.KallLogg;
import static no.nav.oebs.api.config.common.mdc.MdcOperations.generateCorrelationId;


@Slf4j
@Repository
public class PlsqlProcedureRepository {

	// Parameternavn matcher InsertOmadaMessage-signaturen eksakt
	private static final String ERRBUF_PARAM         = "errbuf";
	private static final String RETCODE_PARAM        = "retcode";
	private static final String ORG_ID_PARAM         = "p_org_id";
	private static final String JSON_MESSAGE_PARAM   = "p_json_message";
	private static final String OPERASJON_PARAM      = "p_operasjon";

	private KallLoggRepository kallLoggRepository;

	private JdbcTemplate jdbcTemplate;

	private ConcurrentMap<String, SimpleJdbcCall> jdbcCallCache = new ConcurrentHashMap<>();

	@Autowired
	public PlsqlProcedureRepository(DataSource dataSource, KallLoggRepository kallLoggRepository) {
		jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.setResultsMapCaseInsensitive(true);

		this.kallLoggRepository = kallLoggRepository;
	}

	public PlsqlProcedureResult executeInOutProcedure(String procedureName, String operasjon, Long orgId, String jsonMessage) {
		long startTime = System.currentTimeMillis();

		try {
			validateProcedureName(procedureName);

			SimpleJdbcCall jdbcCall = getJdbcCall(procedureName,
					new SqlOutParameter(ERRBUF_PARAM,       Types.VARCHAR),
					new SqlOutParameter(RETCODE_PARAM,      Types.VARCHAR),
					new SqlParameter(ORG_ID_PARAM,          Types.NUMERIC),
					new SqlParameter(JSON_MESSAGE_PARAM,    Types.CLOB),
					new SqlParameter(OPERASJON_PARAM,       Types.VARCHAR));

			SqlParameterSource inParams = new MapSqlParameterSource()
					.addValue(ORG_ID_PARAM,       orgId)
					.addValue(JSON_MESSAGE_PARAM, jsonMessage)
					.addValue(OPERASJON_PARAM,    operasjon);

			return executeProcedure(jdbcCall, inParams);

		} catch (Exception e) {
			throw e;
		} finally {
			long endTime = System.currentTimeMillis();
			// logProcedureCall(procedureName, jsonMessage, result, endTime - startTime, null);
		}
	}

	private void validateProcedureName(String procedureName) {
		if (procedureName.split("\\.").length != 2) {
			throw new IllegalArgumentException(
					"Feil format på PL/SQL-prosedyrenavnet '" + procedureName + "'; skal ha format 'pakkenavn.prosedyrenavn'");
		}
	}

	private SimpleJdbcCall getJdbcCall(String procedureName, SqlParameter... declaredParameters) {
		SimpleJdbcCall jdbcCall = jdbcCallCache.get(procedureName);
		if (jdbcCall == null) {
			String[] tokens = procedureName.split("\\.");

			jdbcCall = new SimpleJdbcCall(jdbcTemplate) //
					.withCatalogName(tokens[0]) //
					.withProcedureName(tokens[1]) //
					.withoutProcedureColumnMetaDataAccess() //
					.declareParameters(declaredParameters);

			jdbcCallCache.put(procedureName, jdbcCall);

			log.debug("Oppretter og cacher SimpleJdbcCall-objekt for '" + procedureName + "'");
		} else {
			log.debug("Gjenbruker cachet SimpleJdbcCall-objekt for '" + procedureName + "'");
		}
		return jdbcCall;
	}

	private PlsqlProcedureResult executeProcedure(SimpleJdbcCall jdbcCall, SqlParameterSource inParams) {
		Map<String, Object> outParams = jdbcCall.execute(inParams);

		String errbuf  = (String) outParams.get(ERRBUF_PARAM);
		String retcode = (String) outParams.get(RETCODE_PARAM);

		return new PlsqlProcedureResult(errbuf, retcode);
	}

	private void logProcedureCall(String procedureName, String dataIn, PlsqlProcedureResult result, long executionTime,
			Exception exception) {

		String correlationId = MdcOperations.get(MdcOperations.MDC_CORRELATION_ID);

		if (MdcOperations.get(MdcOperations.MDC_CORRELATION_ID) == null) {
			KallLogg kallLogg = KallLogg.builder() //
					.korrelasjonId(generateCorrelationId())
					// .korrelasjonId(MdcOperations.get(MdcOperations.MDC_CORRELATION_ID)) //
					.tidspunkt(LocalDateTime.now()) //
					.type(KallLogg.TYPE_PLSQL) //
					.kallRetning(KallLogg.RETNING_UT) //
					.operation(procedureName) //
					.status(exception != null //
							? Integer.valueOf(PlsqlMessageCodes.EXCEPTION) //
							: PlsqlProcedureResult.resolveMessageNumber(result)) //
					.kalltid(executionTime) //
					.request(dataIn) //
					.response(result != null ? result.getData() : null) //
					.logginfo(exception != null //
							? LoggingUtils.formatExceptionAsString(exception) //
							: PlsqlProcedureResult.resolveMessage(result)) //
					.build();

			log.debug("Correlation ID:  '" + correlationId + "'");

			// if (correlationId == null)  {
			   saveKallLogg(kallLogg);
		}
	}

	private void saveKallLogg(KallLogg kallLogg) {
		try {
			kallLoggRepository.save(kallLogg);
		} catch (Exception e) {
			log.error("Feil ved logging av kalloggdata til databasen; feilmelding=" + e.getMessage(), e);
		}
	}
}
