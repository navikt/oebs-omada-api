package no.nav.oebs.api.db.repository;

import java.math.BigDecimal;
import java.sql.Clob;
import java.sql.SQLException;

import org.springframework.dao.DataRetrievalFailureException;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
public class PlsqlProcedureResult {

	private String data;

	private Integer messageNumber;

	private String message;

	public PlsqlProcedureResult(String data, Integer messageNumber, String message) {
		this.data = data;
		this.messageNumber = messageNumber != null ? messageNumber : Integer.valueOf(PlsqlMessageCodes.OK);
		this.message = message;
	}

	public PlsqlProcedureResult(String errbuf, String retcode) {
		this.message = errbuf;
		this.data = retcode;
		try {
			this.messageNumber = retcode != null ? Integer.parseInt(retcode.trim()) : PlsqlMessageCodes.OK;
		} catch (NumberFormatException e) {
			this.messageNumber = PlsqlMessageCodes.OK;
		}
	}

	public PlsqlProcedureResult(Clob clob, BigDecimal messageNumber, String message) {
		try {
			this.data = clob != null ? clob.getSubString(1, (int) clob.length()) : null;
			this.messageNumber = messageNumber != null ? messageNumber.intValue() : Integer.valueOf(PlsqlMessageCodes.OK);
			this.message = message;
		} catch (SQLException e) {
			throw new DataRetrievalFailureException("Feil ved lesing av clob-verdi", e);
		}
	}

	public static Integer resolveMessageNumber(PlsqlProcedureResult result) {
		return result != null ? result.getMessageNumber() : Integer.valueOf(PlsqlMessageCodes.OK);
	}

	public static String resolveMessage(PlsqlProcedureResult result) {
		return result != null ? result.getMessage() : null;
	}
}
