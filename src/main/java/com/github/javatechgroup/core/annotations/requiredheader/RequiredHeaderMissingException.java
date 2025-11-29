package com.github.javatechgroup.core.annotations.requiredheader;

/**
 * Exception thrown when a required HTTP header, as specified by the
 * {@link RequireHeader} annotation, is missing or contains a blank value in the
 * incoming HTTP request.
 *
 * <p>
 * This exception is typically raised by the
 * {@link com.github.javatechgroup.core.annotations.requiredheader.RequireHeaderAspect}
 * during pre-execution validation of controller methods. When thrown, request
 * processing is halted and should be handled by a global exception handler to
 * return an appropriate HTTP response to the client.
 * </p>
 *
 * <p>
 * <b>Single-header usage example:</b>
 * </p>
 * 
 * <pre>
 * &#64;RequireHeader("X-CLIENT-ID")
 * public String sample() { ... }
 * </pre>
 * 
 * When the above endpoint is invoked without {@code X-CLIENT-ID}, this
 * exception will be thrown.
 *
 * <p>
 * <b>Multiple-headers usage example:</b>
 * </p>
 * 
 * <pre>
 * &#64;RequireHeader({ "X-CLIENT-ID", "X-AUTH-TOKEN" })
 * public String secureAction() { ... }
 * </pre>
 * 
 * The exception will be thrown if <u>either one</u> of the required headers is
 * missing or blank.
 *
 * <p>
 * <b>Handling the exception in Spring Boot:</b>
 * </p>
 * 
 * <pre>
 * &#64;RestControllerAdvice
 * public class GlobalExceptionHandler {
 *
 * 	&#64;ExceptionHandler(RequiredHeaderMissingException.class)
 * 	public ResponseEntity&lt;Map&lt;String, String&gt;&gt; handle(RequiredHeaderMissingException ex) {
 * 		return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
 * 	}
 * }
 * </pre>
 *
 * @see RequireHeader
 * @see com.github.javatechgroup.core.annotations.requiredheader.RequireHeaderAspect
 */
public class RequiredHeaderMissingException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new {@code RequiredHeaderMissingException} describing the name of
	 * the missing header.
	 *
	 * @param header the required HTTP header that was not present in the request
	 */
	public RequiredHeaderMissingException(String header) {
		super("Missing required header: " + header);
	}
}
