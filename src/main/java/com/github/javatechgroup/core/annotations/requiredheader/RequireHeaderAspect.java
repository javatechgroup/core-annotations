package com.github.javatechgroup.core.annotations.requiredheader;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Aspect responsible for enforcing the {@link RequireHeader} annotation on
 * controller classes and methods.
 *
 * <p>
 * This aspect intercepts the execution of annotated controller methods and
 * validates the presence of HTTP headers declared in {@link RequireHeader}. If
 * a required header is missing or blank, a
 * {@link RequiredHeaderMissingException} is thrown before the method executes.
 * </p>
 *
 * <p>
 * <b>How validation works:</b>
 * </p>
 * <ul>
 * <li>Detect method or class annotated with {@link RequireHeader}</li>
 * <li>Read the header names defined in the annotation</li>
 * <li>Fetch values from {@link HttpServletRequest}</li>
 * <li>Throw {@code RequiredHeaderMissingException} if any value is missing or
 * blank</li>
 * </ul>
 *
 * <p>
 * <b>Typical usage:</b>
 * </p>
 * 
 * <pre>
 * &#64;RestController
 * &#64;RequireHeader("X-CLIENT-ID")
 * public class HelloController {
 *
 * 	&#64;GetMapping("/hello")
 * 	public String hello() {
 * 		return "Hello";
 * 	}
 *
 * 	// Overrides class annotation — requires two headers
 * 	&#64;RequireHeader({ "X-CLIENT-ID", "X-AUTH-TOKEN" })
 * 	&#64;GetMapping("/secure")
 * 	public String secureEndpoint() {
 * 		return "Secure Hello";
 * 	}
 * }
 * </pre>
 *
 * @see RequireHeader
 * @see RequiredHeaderMissingException
 */
@Aspect
@Component
public class RequireHeaderAspect {

	/** Provides access to incoming HTTP request headers. */
	@Autowired
	private HttpServletRequest request;

	/**
	 * Advice executed before any controller method annotated with
	 * {@link RequireHeader} or belonging to a class annotated with
	 * {@link RequireHeader}.
	 *
	 * @param requireHeader annotation instance with required header names
	 * @throws RequiredHeaderMissingException if any required header is missing or
	 *                                        blank
	 */
	@Before("@within(requireHeader) || @annotation(requireHeader)")
	public void checkHeaders(RequireHeader requireHeader) {
		for (String header : requireHeader.value()) {
			final String headerValue = request.getHeader(header);
			if (headerValue == null || headerValue.isBlank()) {
				throw new RequiredHeaderMissingException(header);
			}
		}
	}
}
