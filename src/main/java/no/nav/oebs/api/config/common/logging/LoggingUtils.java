package no.nav.oebs.api.config.common.logging;

import java.io.PrintWriter;
import java.io.StringWriter;

public class LoggingUtils {

	private LoggingUtils() {

	}

	public static String formatExceptionAsString(Throwable exception) {
		if (exception == null) {
			return null;
		}
		var stringWriter = new StringWriter();
		exception.printStackTrace(new PrintWriter(stringWriter));

		return stringWriter.toString();
	}

	public static String maskIfFnr(String text) {
		return text != null //
				? text.replaceAll("(\\D+|^)(\\d{2})\\d{7}(\\d{2})(\\D+|$)", "$1$2" + "*******" + "$3$4") //
				: "(null)";
	}
}
