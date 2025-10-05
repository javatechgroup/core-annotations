package com.github.javatechgroup.core.annotations.trim.util;

import com.github.javatechgroup.core.annotations.trim.TrimAllStrings;
import com.github.javatechgroup.core.annotations.trim.Trimmed;
import java.lang.reflect.Field;

/**
 * Utility class for automatically trimming String fields based on annotations.
 * 
 * <p>
 * This class provides the core functionality for processing TrimAllStrings and
 * Trimmed annotations by automatically trimming whitespace from annotated
 * String fields in any Java object.
 * </p>
 * 
 * <h2>Basic Usage</h2>
 * 
 * <pre>{@code
 * // Simple usage - processes annotations automatically
 * UserDTO user = new UserDTO();
 * user.setName("  John Doe  ");
 * user.setEmail("  john@example.com  ");
 * 
 * StringTrimmer.trim(user);
 * 
 * // Results:
 * // user.getName() -> "John Doe"
 * // user.getEmail() -> "john@example.com"
 * }</pre>
 * 
 * <h2>Processing Logic</h2>
 * <ul>
 * <li>If class has TrimAllStrings → trims all String fields</li>
 * <li>If class has no TrimAllStrings → trims only fields with Trimmed</li>
 * <li>If both annotations present → TrimAllStrings takes precedence</li>
 * <li>Only processes String type fields</li>
 * <li>Preserves null values (doesn't convert to empty string)</li>
 * <li>Only modifies field if trimming actually changes the value</li>
 * </ul>
 * 
 * <h2>Common Integration Patterns</h2>
 * 
 * <pre>{@code
 * // Pattern 1: Trim in service methods
 * @Service
 * public class UserService {
 *     public UserDTO createUser(UserDTO user) {
 *         StringTrimmer.trim(user); // Explicit trim call
 *         // ... business logic
 *     }
 * }
 * 
 * // Pattern 2: Trim in controller methods  
 * @RestController
 * public class UserController {
 *     @PostMapping("/users")
 *     public UserDTO createUser(@RequestBody UserDTO user) {
 *         StringTrimmer.trim(user); // Explicit trim call
 *         return userService.save(user);
 *     }
 * }
 * }</pre>
 * 
 * @see com.github.javatechgroup.core.annotations.trim.TrimAllStrings
 * @see com.github.javatechgroup.core.annotations.trim.Trimmed
 * @since 1.0
 * @author JavaTech Group
 */
public final class StringTrimmer {

	/**
	 * Private constructor to prevent instantiation.
	 */
	private StringTrimmer() {
		// Utility class - no instantiation
	}

	/**
	 * Trims String fields in the specified object based on annotations.
	 * 
	 * <p>
	 * Processes the object's fields according to the following rules:
	 * <ul>
	 * <li>If the class is annotated with TrimAllStrings, all String fields are
	 * trimmed</li>
	 * <li>Otherwise, only fields annotated with Trimmed are trimmed</li>
	 * <li>Null objects are ignored</li>
	 * <li>Only modifies fields that actually change when trimmed</li>
	 * </ul>
	 * 
	 * @param obj the object whose String fields should be trimmed, may be null
	 * @throws RuntimeException if field access fails, with the underlying
	 *                          IllegalAccessException as cause
	 */
	public static void trim(Object obj) {
		if (obj == null)
			return;

		Class<?> clazz = obj.getClass();
		boolean trimAllStrings = clazz.isAnnotationPresent(TrimAllStrings.class);

		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			if (field.getType() == String.class) {
				boolean shouldTrim = trimAllStrings || field.isAnnotationPresent(Trimmed.class);
				if (shouldTrim) {
					trimField(obj, field);
				}
			}
		}
	}

	/**
	 * Trims a specific field in the target object.
	 * 
	 * @param obj   the target object containing the field
	 * @param field the field to trim
	 * @throws RuntimeException if field access fails
	 */
	private static void trimField(Object obj, Field field) {
		field.setAccessible(true);
		try {
			String value = (String) field.get(obj);
			if (value != null) {
				String trimmed = value.trim();
				if (!value.equals(trimmed)) {
					field.set(obj, trimmed);
				}
			}
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Failed to trim field: " + field.getName(), e);
		}
	}
}