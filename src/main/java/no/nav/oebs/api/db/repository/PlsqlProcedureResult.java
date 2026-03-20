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
	private Long interfaceMsgId;
	private String retcode;


	public PlsqlProcedureResult(String data, Integer messageNumber, String message, Long interfaceMsgId, String retcode) {
		this.data           = data;
		this.messageNumber  = messageNumber != null ? messageNumber : PlsqlMessageCodes.OK;
		this.message        = message;
		this.interfaceMsgId = interfaceMsgId;
		this.retcode        = retcode;
	}

	public PlsqlProcedureResult(Clob clob, BigDecimal messageNumber, String message, Long interfaceMsgId, String retcode) {
		try {
			this.data           = clob != null ? clob.getSubString(1, (int) clob.length()) : null;
			this.messageNumber  = messageNumber != null ? messageNumber.intValue() : PlsqlMessageCodes.OK;
			this.message        = message;
			this.interfaceMsgId = interfaceMsgId;
			this.retcode        = retcode;
		} catch (SQLException e) {
			throw new DataRetrievalFailureException("Feil ved lesing av clob-verdi", e);
		}
	}
}
