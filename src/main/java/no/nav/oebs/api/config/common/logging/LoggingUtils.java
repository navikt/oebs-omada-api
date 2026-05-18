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
		if (text == null) {
			return "(null)";
		}
		return maskFnrLinear(text);
	}

	private static String maskFnrLinear(String text) {
		StringBuilder masked = new StringBuilder(text.length());
		int i = 0;
		while (i < text.length()) {
			if (isFnrAt(text, i)) {
				masked.append(text, i, i + 2).append("*******").append(text, i + 9, i + 11);
				i += 11;
				continue;
			}
			masked.append(text.charAt(i));
			i++;
		}
		return masked.toString();
	}

	private static boolean isFnrAt(String text, int start) {
		if (start + 11 > text.length()) {
			return false;
		}
		if (start > 0 && Character.isDigit(text.charAt(start - 1))) {
			return false;
		}
		if (start + 11 < text.length() && Character.isDigit(text.charAt(start + 11))) {
			return false;
		}
		for (int i = start; i < start + 11; i++) {
			if (!Character.isDigit(text.charAt(i))) {
				return false;
			}
		}
		return true;
	}
}
