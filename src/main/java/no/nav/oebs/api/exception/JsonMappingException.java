package no.nav.oebs.api.exception;

public class JsonMappingException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public JsonMappingException(Exception cause) {
		super(cause);
	}

	public JsonMappingException(String message, Exception cause) {
		super(message, cause);
	}
}
