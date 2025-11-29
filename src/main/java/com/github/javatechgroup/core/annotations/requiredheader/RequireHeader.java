package com.github.javatechgroup.core.annotations.requiredheader;

import java.lang.annotation.*;

/**
 * Indicates that specific HTTP headers are required for a controller class or
 * method.
 *
 * <p>
 * When applied, this annotation triggers validation of incoming HTTP requests
 * to ensure the presence of the specified header(s). If any required header is
 * missing or its value is blank, a {@link RequiredHeaderMissingException} will
 * be thrown before the target controller method is executed.
 * </p>
 *
 * <p>
 * <b>Usage example:</b>
 * </p>
 *
 * <pre>
 * // Require a single header
 * &#64;RequireHeader("X-CLIENT-ID")
 * &#64;GetMapping("/hello")
 * public Response hello() {
 * 	return Response.ok("Hello");
 * }
 *
 * // Require multiple headers
 * &#64;RequireHeader({ "X-CLIENT-ID", "X-AUTH-TOKEN" })
 * &#64;PostMapping("/orders")
 * public Response placeOrder() {
 * 	return Response.ok("Order placed");
 * }
 * </pre>
 *
 * <p>
 * This annotation can be applied at both method and class level. If placed at
 * the class level, all controller methods within the class will require the
 * same headers.
 * </p>
 *
 * @see com.github.javatechgroup.core.annotations.requiredheader.RequiredHeaderMissingException
 * @see com.github.javatechgroup.core.annotations.requiredheader.RequireHeader
 *      // safe self-reference note
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface RequireHeader {

	/**
	 * The HTTP header names that must be present in the incoming request.
	 *
	 * @return an array of required HTTP header names
	 */
	String[] value();
}
