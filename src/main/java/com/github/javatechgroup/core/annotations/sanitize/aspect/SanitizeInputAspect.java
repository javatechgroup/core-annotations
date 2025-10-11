package com.github.javatechgroup.core.annotations.sanitize.aspect;

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;

import com.github.javatechgroup.core.annotations.sanitize.SanitizeInput;
import com.github.javatechgroup.core.annotations.sanitize.util.SanitizerUtil;

/**
 * Spring AOP aspect that automatically sanitizes input parameters and request
 * body fields annotated with {@link SanitizeInput} to prevent XSS attacks.
 * 
 * <p>
 * This aspect provides three main sanitization strategies:
 * </p>
 * 
 * <ol>
 * <li><b>Parameter Sanitization:</b> Sanitizes method parameters annotated with
 * {@code @SanitizeInput}</li>
 * <li><b>Request Parameter Sanitization:</b> Sanitizes Spring MVC
 * {@code @RequestParam} parameters annotated with {@code @SanitizeInput}</li>
 * <li><b>Request Body Sanitization:</b> Sanitizes fields in Spring MVC
 * controller request bodies</li>
 * </ol>
 *
 * <p>
 * <b>Key Features:</b>
 * </p>
 * <ul>
 * <li>Automatic detection of {@code @SanitizeInput} annotated elements</li>
 * <li>Performance optimization through reflection caching</li>
 * <li>Recursive sanitization of nested objects with depth limiting</li>
 * <li>JDK type detection to avoid unnecessary processing</li>
 * <li>Thread-safe implementation</li>
 * </ul>
 *
 * <p>
 * <b>Supported Spring Mapping Annotations:</b>
 * </p>
 * <ul>
 * <li>{@code @RequestMapping}</li>
 * <li>{@code @GetMapping}</li>
 * <li>{@code @PostMapping}</li>
 * <li>{@code @PutMapping}</li>
 * <li>{@code @DeleteMapping}</li>
 * <li>{@code @PatchMapping}</li>
 * </ul>
 *
 * <p>
 * <b>Usage Examples:</b>
 * </p>
 * 
 * <pre>{@code
 * // Parameter sanitization in service methods
 * public void updateUserProfile(@SanitizeInput String bio, String username) {
 * 	// 'bio' parameter is automatically sanitized, 'username' is not
 * }
 *
 * // Request parameter sanitization in controllers
 * @GetMapping("/content")
 * public String getContent(@SanitizeInput @RequestParam String html) {
 * 	// 'html' parameter is automatically sanitized
 * 	return html;
 * }
 *
 * // Request body sanitization in controllers
 * @PostMapping("/users")
 * public User createUser(@RequestBody User user) {
 * 	// All @SanitizeInput fields in User will be sanitized
 * 	return userService.save(user);
 * }
 * }</pre>
 *
 * @see SanitizeInput
 * @see SanitizerUtil
 * @author JavaTech Group
 * @version 1.1
 * @since 2024.1
 */
@Aspect
@Component
public class SanitizeInputAspect {

	private static final Logger log = LoggerFactory.getLogger(SanitizeInputAspect.class);

	/**
	 * Reflection cache mapping classes to their sanitizable fields.
	 * 
	 * <p>
	 * <b>Cache Semantics:</b>
	 * </p>
	 * <ul>
	 * <li><b>Empty list:</b> No sanitizable fields found in the class</li>
	 * <li><b>{@link #JDK_TYPE_MARKER}:</b> Class is a JDK type (skip
	 * processing)</li>
	 * <li><b>Field list:</b> List of {@code @SanitizeInput} annotated String
	 * fields</li>
	 * </ul>
	 */
	private final Map<Class<?>, List<Field>> cache = new ConcurrentHashMap<>();

	/**
	 * Marker indicating that a class is a JDK type and should not be processed for
	 * sanitizable fields.
	 */
	private static final List<Field> JDK_TYPE_MARKER = Collections.emptyList();

	/**
	 * Maximum recursion depth for nested object sanitization. Prevents infinite
	 * recursion and StackOverflowErrors when processing complex object graphs.
	 */
	private static final int MAX_RECURSION_DEPTH = 5;

	/**
	 * Intercepts method executions that have parameters annotated with
	 * {@code @SanitizeInput} and sanitizes those parameters before method
	 * execution.
	 *
	 * <p>
	 * This advice works for regular method parameters as well as Spring MVC
	 * {@code @RequestParam} parameters.
	 * </p>
	 *
	 * @param pjp the proceeding join point representing the method execution
	 * @return the result of the method execution
	 * @throws Throwable if the proceeded method throws an exception
	 * 
	 * @example
	 * 
	 *          <pre>{@code
	 * // This method call will trigger parameter sanitization
	 * service.processContent("<script>alert('xss')</script>Safe content");
	 * // The parameter will be sanitized to "Safe content" before method execution
	 * 
	 * // This HTTP request will also trigger parameter sanitization
	 * GET /api/content?html=<script>alert('xss')</script>Hello
	 * // The @RequestParam will be sanitized to "Hello" before controller execution
	 * }</pre>
	 */
	@Around("execution(* *(.., @com.github.javatechgroup.core.annotations.sanitize.SanitizeInput (*), ..))")
	public Object sanitizeAnnotatedParameters(ProceedingJoinPoint pjp) throws Throwable {
		Object[] args = pjp.getArgs();
		MethodSignature signature = (MethodSignature) pjp.getSignature();
		Parameter[] parameters = signature.getMethod().getParameters();

		for (int i = 0; i < parameters.length; i++) {
			SanitizeInput annotation = parameters[i].getAnnotation(SanitizeInput.class);
			if (annotation != null && args[i] instanceof String) {
				String original = (String) args[i];
				String sanitized = SanitizerUtil.sanitize(original, annotation.strategy());
				args[i] = sanitized;
				log.info("Sanitized parameter: '{}' -> '{}'", original, sanitized);
			}
		}
		return pjp.proceed(args);
	}

	/**
	 * Intercepts Spring MVC controller methods and sanitizes all request parameters
	 * and request body fields annotated with {@code @SanitizeInput}.
	 *
	 * <p>
	 * This advice applies to methods annotated with any Spring web mapping
	 * annotation and handles both {@code @RequestParam} and {@code @RequestBody}
	 * parameters.
	 * </p>
	 *
	 * @param pjp the proceeding join point representing the controller method
	 *            execution
	 * @return the result of the controller method execution
	 * @throws Throwable if the proceeded method throws an exception
	 * 
	 * @example
	 * 
	 *          <pre>{@code
	 *          @GetMapping("/search")
	 *          public String searchContent(@SanitizeInput @RequestParam String query) {
	 *          // 'query' parameter is automatically sanitized
	 *          return searchService.find(query);
	 *          }
	 * 
	 *          @PostMapping("/blog/posts")
	 *          public BlogPost createPost(@RequestBody BlogPost post) {
	 *          // All @SanitizeInput fields in BlogPost and its nested objects
	 *          	// are automatically sanitized before this method executes
	 *          return repository.save(post);
	 *          }
	 * }</pre>
	 */
	@Around("@annotation(org.springframework.web.bind.annotation.RequestMapping) || "
			+ "@annotation(org.springframework.web.bind.annotation.GetMapping) || "
			+ "@annotation(org.springframework.web.bind.annotation.PostMapping) || "
			+ "@annotation(org.springframework.web.bind.annotation.PutMapping) || "
			+ "@annotation(org.springframework.web.bind.annotation.DeleteMapping) || "
			+ "@annotation(org.springframework.web.bind.annotation.PatchMapping)")
	public Object sanitizeControllerMethods(ProceedingJoinPoint pjp) throws Throwable {
		Object[] args = pjp.getArgs();
		MethodSignature signature = (MethodSignature) pjp.getSignature();
		Parameter[] parameters = signature.getMethod().getParameters();

		// First, sanitize @RequestParam parameters
		for (int i = 0; i < parameters.length; i++) {
			SanitizeInput sanitizeAnnotation = parameters[i].getAnnotation(SanitizeInput.class);
			RequestParam requestParamAnnotation = parameters[i].getAnnotation(RequestParam.class);

			if (sanitizeAnnotation != null && requestParamAnnotation != null && args[i] instanceof String) {
				String original = (String) args[i];
				String sanitized = SanitizerUtil.sanitize(original, sanitizeAnnotation.strategy());
				args[i] = sanitized;

				log.info("Sanitized @RequestParam: '{}' -> '{}'", original, sanitized);
			}
		}

		// Then, sanitize @RequestBody object fields
		for (Object arg : args) {
			if (arg != null && !isJdkType(arg.getClass())) {
				sanitizeObjectFields(arg, 0);
			}
		}

		return pjp.proceed(args);
	}

	/**
	 * Recursively sanitizes all {@code @SanitizeInput} annotated String fields in
	 * the given object and its nested objects.
	 *
	 * @param obj   the object to sanitize
	 * @param depth the current recursion depth (starts at 0)
	 * @throws IllegalAccessException if field access is denied by security manager
	 * 
	 * @example
	 * 
	 *          <pre>
	 *          {
	 *          	@code
	 *          	public class User {
	 *          		@SanitizeInput
	 *          		private String bio; // Will be sanitized
	 *          		private Profile profile; // Will be recursively processed
	 *          	}
	 * 
	 *          	public class Profile {
	 *          		@SanitizeInput
	 *          		private String description; // Will be sanitized recursively
	 *          		private String name; // Not annotated, won't be sanitized
	 *          	}
	 *          }
	 *          </pre>
	 */
	private void sanitizeObjectFields(Object obj, int depth) throws IllegalAccessException {
		if (obj == null || depth > MAX_RECURSION_DEPTH) {
			return; // Safety: prevent infinite recursion
		}

		List<Field> fields = getSanitizableFields(obj.getClass());
		if (fields.isEmpty()) {
			return; // No sanitizable fields or JDK type
		}

		for (Field field : fields) {
			field.setAccessible(true);
			Object fieldValue = field.get(obj);

			if (fieldValue instanceof String) {
				SanitizeInput annotation = field.getAnnotation(SanitizeInput.class);
				String original = (String) fieldValue;
				String sanitized = SanitizerUtil.sanitize(original, annotation.strategy());
				field.set(obj, sanitized);

				if (!original.equals(sanitized)) {
					log.info("Sanitized field '{}' : '{}' -> '{}'", field.getName(), original, sanitized);
				}
			} else if (fieldValue != null && !isJdkType(fieldValue.getClass())) {
				// Recursively process nested objects (only non-JDK types)
				sanitizeObjectFields(fieldValue, depth + 1);
			}
		}
	}

	/**
	 * Retrieves the list of sanitizable fields for the given class, using cache for
	 * performance optimization.
	 *
	 * @param clazz the class to inspect for sanitizable fields
	 * @return list of {@code @SanitizeInput} annotated String fields, or
	 *         {@link #JDK_TYPE_MARKER} if class is a JDK type, or empty list if no
	 *         sanitizable fields found
	 */
	private List<Field> getSanitizableFields(Class<?> clazz) {
		return cache.computeIfAbsent(clazz, this::computeSanitizableFields);
	}

	/**
	 * Computes the list of sanitizable fields for a class by scanning all fields in
	 * the class hierarchy (excluding JDK types and Object.class).
	 *
	 * @param clazz the class to compute sanitizable fields for
	 * @return list of sanitizable fields, or appropriate marker
	 */
	private List<Field> computeSanitizableFields(Class<?> clazz) {
		// Check if it's a JDK type
		if (isJdkType(clazz)) {
			return JDK_TYPE_MARKER;
		}

		List<Field> sanitizableFields = new ArrayList<>();
		Class<?> currentClass = clazz;

		while (currentClass != null && currentClass != Object.class && !isJdkType(currentClass)) {
			for (Field field : currentClass.getDeclaredFields()) {
				if (field.isAnnotationPresent(SanitizeInput.class) && field.getType() == String.class) {
					sanitizableFields.add(field);
				}
			}
			currentClass = currentClass.getSuperclass();
		}

		return sanitizableFields.isEmpty() ? JDK_TYPE_MARKER : sanitizableFields;
	}

	/**
	 * Determines if the given class is a JDK or framework type that should be
	 * excluded from sanitization processing.
	 *
	 * @param clazz the class to check
	 * @return {@code true} if the class is a JDK or Spring framework type,
	 *         {@code false} otherwise
	 */
	private boolean isJdkType(Class<?> clazz) {
		String className = clazz.getName();
		return className.startsWith("java.") || className.startsWith("javax.")
				|| className.startsWith("org.springframework.");
	}

	/**
	 * Clears the reflection cache.
	 * 
	 * <p>
	 * This method is useful during development for hot-reloading scenarios where
	 * class definitions may change. In production environments, clearing the cache
	 * may cause temporary performance degradation until the cache is repopulated.
	 * </p>
	 */
	public void clearCache() {
		cache.clear();
	}

	/**
	 * Returns the current number of entries in the reflection cache.
	 *
	 * @return the number of cached class field mappings
	 */
	public int getCacheSize() {
		return cache.size();
	}

	/**
	 * Returns the maximum recursion depth used for nested object sanitization.
	 *
	 * @return the maximum recursion depth
	 */
	public static int getMaxRecursionDepth() {
		return MAX_RECURSION_DEPTH;
	}
	
	/**
     * Default constructor.
     */
    public SanitizeInputAspect() {
        // Aspect initialization
    }
}