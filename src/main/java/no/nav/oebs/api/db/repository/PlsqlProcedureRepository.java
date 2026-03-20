package no.nav.oebs.api.db.repository;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.sql.DataSource;

import no.nav.oebs.api.exception.UgyldigInputException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Repository
public class PlsqlProcedureRepository {

	// Parameternavn matcher XXRTV_INT_OMADA_INSERT_MESSAGE.InsertOmadaMessage
	private static final String PARAM_ERRBUF              = "errbuf";
	private static final String PARAM_RETCODE             = "retcode";
	private static final String PARAM_ORG_ID              = "p_org_id";
	private static final String PARAM_JSON_MESSAGE        = "p_json_message";
	private static final String PARAM_OPERASJON           = "p_operasjon";
	private static final String PARAM_X_INTERFACE_MSG_ID  = "x_interface_msg_id";
	private static final String PARAM_P_INTERFACE_MSG_ID  = "p_interface_msg_id";

	/** Gyldige operasjonsverdier */
	public enum Operasjon {
		NY, ENDRE, SLETTE
	}

	private final JdbcTemplate jdbcTemplate;
	private final ConcurrentMap<String, SimpleJdbcCall> jdbcCallCache = new ConcurrentHashMap<>();

	@Value("${oebs.plsql.org-id:0}")
	private long orgId;

	@Autowired
	public PlsqlProcedureRepository(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
		this.jdbcTemplate.setResultsMapCaseInsensitive(true);
	}

	public PlsqlProcedureResult executeInOutProcedure(String procedureName, Operasjon operasjon, String dataIn) {
		long startTime = System.currentTimeMillis();
		try {
			validateProcedureName(procedureName);

			SimpleJdbcCall jdbcCall = getJdbcCall(procedureName,
					new SqlOutParameter(PARAM_ERRBUF,              Types.VARCHAR),
					new SqlOutParameter(PARAM_RETCODE,             Types.VARCHAR),
					new SqlOutParameter(PARAM_X_INTERFACE_MSG_ID,  Types.NUMERIC),
					new SqlParameter(  PARAM_ORG_ID,               Types.NUMERIC),
					new SqlParameter(  PARAM_JSON_MESSAGE,         Types.CLOB),
					new SqlParameter(  PARAM_OPERASJON,            Types.VARCHAR));

			SqlParameterSource inParams = new MapSqlParameterSource()
					.addValue(PARAM_ORG_ID,      orgId)
					.addValue(PARAM_JSON_MESSAGE, dataIn)
					.addValue(PARAM_OPERASJON,    operasjon.name());

			PlsqlProcedureResult result = executeProcedure(jdbcCall, inParams);

			if (result.getMessageNumber() < 0) {
				throw new UgyldigInputException("Ingen data funnet");
			}
			return result;

		} finally {
			log.debug("executeInOutProcedure: procedure={}, operasjon={}, tid={}ms",
					procedureName, operasjon, System.currentTimeMillis() - startTime);
		}
	}

	private void validateProcedureName(String procedureName) {
		int parts = procedureName.split("\\.").length;
		if (parts != 2 && parts != 3) {
			throw new IllegalArgumentException(
					"Feil format på PL/SQL-prosedyrenavnet '" + procedureName + "'; skal ha format 'pakkenavn.prosedyrenavn' eller 'schema.pakkenavn.prosedyrenavn'");
		}
	}

	private SimpleJdbcCall getJdbcCall(String procedureName, SqlParameter... declaredParameters) {
		SimpleJdbcCall jdbcCall = jdbcCallCache.get(procedureName);
		if (jdbcCall == null) {
			String[] tokens = procedureName.split("\\.");

			jdbcCall = new SimpleJdbcCall(jdbcTemplate);

			if (tokens.length == 3) {
				// Format: SCHEMA.PAKKE.PROSEDYRE — brukes når kalleren ikke eier pakken
				jdbcCall.withSchemaName(tokens[0])
						.withCatalogName(tokens[1])
						.withProcedureName(tokens[2]);
			} else {
				// Format: PAKKE.PROSEDYRE
				jdbcCall.withCatalogName(tokens[0])
						.withProcedureName(tokens[1]);
			}

			jdbcCall.withoutProcedureColumnMetaDataAccess()
					.declareParameters(declaredParameters);

			jdbcCallCache.put(procedureName, jdbcCall);
			log.debug("Oppretter og cacher SimpleJdbcCall-objekt for '{}'", procedureName);
		} else {
			log.debug("Gjenbruker cachet SimpleJdbcCall-objekt for '{}'", procedureName);
		}
		return jdbcCall;
	}

	private PlsqlProcedureResult executeProcedure(SimpleJdbcCall jdbcCall, SqlParameterSource inParams) {
		Map<String, Object> outParams = jdbcCall.execute(inParams);

		String errbuf  = (String) outParams.get(PARAM_ERRBUF);
		String retcode = (String) outParams.get(PARAM_RETCODE);

		log.info("InsertOmadaMessage returnerte retcode='{}', errbuf='{}'", retcode, errbuf);

		if (retcode != null && !retcode.isBlank() && !retcode.equals("0")) {
			log.warn("InsertOmadaMessage returnerte retcode={}, errbuf={}", retcode, errbuf);
		}

		int messageNumber = mapInsertRetcode(retcode);

		BigDecimal rawMsgId = (BigDecimal) outParams.get(PARAM_X_INTERFACE_MSG_ID);
		Long interfaceMsgId = rawMsgId != null ? rawMsgId.longValue() : null;

		return new PlsqlProcedureResult(null, BigDecimal.valueOf(messageNumber), errbuf, interfaceMsgId, retcode);
	}

	/**
	 * Mapper retcode (varchar2) fra InsertOmadaMessage til messageNumber.
	 * Konvensjon: null/blank/"0" = OK, "1" = advarsel, "2"/annet = feil.
	 * HTTP-statuskoder (2xx/3xx/4xx/5xx) støttes også.
	 */
	static int mapInsertRetcode(String retcode) {
		try {
			int code = Integer.parseInt(retcode == null || retcode.isBlank() ? "0" : retcode);
			if      (code >= 200 && code < 300) return 0;
			else if (code >= 300 && code < 400) return 1;
			else if (code >= 400)               return -1;
			return switch (retcode == null || retcode.isBlank() ? "0" : retcode) {
				case "0"    -> 0;
				case "1"    -> 1;
				default     -> -1;
			};
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	/**
	 * Mapper retcode (number) fra start_import_ident_melding til messageNumber.
	 * Konvensjon: 0/null = OK, alt annet (1=advarsel, 2+=feil) = EXCEPTION.
	 */
	static int mapSyncRetcode(int retcodeInt) {
		return retcodeInt == 0 ? PlsqlMessageCodes.OK : PlsqlMessageCodes.EXCEPTION;
	}
    
	public PlsqlProcedureResult executeSyncProcedure(String procedureName, Long interfaceMsgId) {
		long startTime = System.currentTimeMillis();
		try {
			validateProcedureName(procedureName);

			SimpleJdbcCall jdbcCall = getJdbcCall(procedureName,
					new SqlOutParameter(PARAM_ERRBUF,              Types.VARCHAR),
					new SqlOutParameter(PARAM_RETCODE,             Types.NUMERIC),  // number, ikke varchar2
					new SqlParameter(   PARAM_P_INTERFACE_MSG_ID, Types.NUMERIC));

			SqlParameterSource inParams = new MapSqlParameterSource()
					.addValue(PARAM_P_INTERFACE_MSG_ID, interfaceMsgId);

			Map<String, Object> outParams = jdbcCall.execute(inParams);

			String     errbuf      = (String)     outParams.get(PARAM_ERRBUF);
			BigDecimal retcodeRaw  = (BigDecimal) outParams.get(PARAM_RETCODE);
			int        retcodeInt  = retcodeRaw != null ? retcodeRaw.intValue() : 0;

			log.info("start_import_ident_melding returnerte retcode='{}', errbuf='{}'", retcodeInt, errbuf);

			// Oracle concurrent program-konvensjon: 0/null=suksess, 1=advarsel, 2=feil
			// Både advarsel og feil behandles som EXCEPTION — sync-prosedyren skal alltid fullføre OK
			if (retcodeInt == 1) {
				log.warn("start_import_ident_melding returnerte retcode=1 (advarsel), errbuf={}", errbuf);
			} else if (retcodeInt >= 2) {
				log.error("start_import_ident_melding returnerte retcode={} (feil), errbuf={}", retcodeInt, errbuf);
			}

			int messageNumber = mapSyncRetcode(retcodeInt);

			return new PlsqlProcedureResult(null, messageNumber, errbuf, null, String.valueOf(retcodeInt));
		} finally {
			log.debug("executeSyncProcedure: procedure={}, interfaceMsgId={}, tid={}ms",
					procedureName, interfaceMsgId, System.currentTimeMillis() - startTime);
		}
	}
}
