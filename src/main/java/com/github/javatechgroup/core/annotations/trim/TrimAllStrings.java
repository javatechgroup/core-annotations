package com.github.javatechgroup.core.annotations.trim;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to indicate that all String fields in a class should be
 * automatically trimmed when processed by StringTrimmer.
 * 
 * <p>
 * When applied at the class level, this annotation marks all String fields
 * within the class for automatic whitespace trimming. This is useful for data
 * cleaning, normalization, and ensuring consistent string formatting across
 * your application.
 * </p>
 * 
 * <h2>Usage Examples</h2>
 * 
 * <pre>{@code
 * // Example 1: Apply to DTO classes for trimming with StringTrimmer
 * &#064;TrimAllStrings
 * public class UserDTO {
 * 	private String name; // Will be trimmed when StringTrimmer.trim() is called
 * 	private String email; // Will be trimmed when StringTrimmer.trim() is called
 * 	private String phone; // Will be trimmed when StringTrimmer.trim() is called
 * }
 * 
 * // Example 2: Use with REST controller request bodies (requires explicit trimming)
 * &#064;RestController
 * public class UserController {
 * 
 * 	&#064;PostMapping("/users")
 * 	public ResponseEntity&lt;UserDTO&gt; createUser(&#064;RequestBody UserDTO user) {
 * 		// Must explicitly call StringTrimmer.trim() for trimming to occur
 * 		StringTrimmer.trim(user);
 * 		return ResponseEntity.ok(userService.save(user));
 * 	}
 * }
 * }</pre>
 * 
 * <h2>Important Implementation Note</h2>
 * <ul>
 * <li>This annotation <strong>does nothing by itself</strong> - it only marks
 * classes for processing</li>
 * <li>You must explicitly call {@code StringTrimmer.trim(object)} for trimming
 * to occur</li>
 * <li>The annotation is read by StringTrimmer to determine which fields to
 * trim</li>
 * </ul>
 * 
 * <h2>Integration Requirements</h2>
 * <ul>
 * <li><strong>Required:</strong> Must call {@code StringTrimmer.trim(object)}
 * explicitly</li>
 * <li><strong>Optional:</strong> Can be combined with Spring AOP using
 * TrimmedAspect</li>
 * <li><strong>Manual Processing:</strong> Use
 * {@code StringTrimmer.trim(object)} for direct control</li>
 * </ul>
 * 
 * <h2>Comparison with Trimmed</h2>
 * <table>
 * <caption>Annotation Comparison</caption>
 * <tr>
 * <th>Annotation</th>
 * <th>Scope</th>
 * <th>Use Case</th>
 * </tr>
 * <tr>
 * <td>TrimAllStrings</td>
 * <td>Class-level (all String fields)</td>
 * <td>When all string fields in a class should be trimmed</td>
 * </tr>
 * <tr>
 * <td>Trimmed</td>
 * <td>Field-level (individual fields)</td>
 * <td>When only specific fields should be trimmed</td>
 * </tr>
 * </table>
 * 
 * <h2>Best Practices</h2>
 * <ul>
 * <li>Use for data transfer objects (DTOs) that receive external input</li>
 * <li>Apply to entities where database consistency is important</li>
 * <li>Avoid using on classes that contain formatted text or
 * whitespace-sensitive data</li>
 * <li>Combine with validation annotations for comprehensive data cleaning</li>
 * <li>Call {@code StringTrimmer.trim()} at service boundaries or controller
 * methods</li>
 * </ul>
 * 
 * <h2>Example with Validation and Explicit Trimming</h2>
 * 
 * <pre>{@code
 * &#064;TrimAllStrings
 * public class RegistrationRequest {
 * 
 * 	&#064;NotBlank(message = "Name is required")
 * 	&#064;Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
 * 	private String name;
 * 
 * 	&#064;Email(message = "Invalid email format")
 * 	&#064;NotBlank(message = "Email is required")
 * 	private String email;
 * }
 * 
 * // In your service or controller:
 * public ResponseEntity&lt;?&gt; register(&#064;RequestBody RegistrationRequest request) {
 * 	StringTrimmer.trim(request); // Explicit trimming before validation
 * 	// Now validate the trimmed data
 * 	// ... validation logic
 * }
 * }</pre>
 * 
 * @see com.github.javatechgroup.core.annotations.trim.Trimmed
 * @see com.github.javatechgroup.core.annotations.trim.util.StringTrimmer
 * @since 1.0
 * @author JavaTech Group
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TrimAllStrings {
}