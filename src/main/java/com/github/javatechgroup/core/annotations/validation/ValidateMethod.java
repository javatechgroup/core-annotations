package com.github.javatechgroup.core.annotations.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.groups.Default;

/**
 * Automatically validates method parameters and return values using Bean
 * Validation.
 * 
 * <p>
 * <b>What Problem Does This Solve?</b> Spring's built-in {@code @Valid} works
 * great in controllers, but what about service methods? This annotation brings
 * automatic validation to your service layer, ensuring data integrity
 * throughout your entire application.
 *
 * <p>
 * <b>Key Benefits:</b>
 * <ul>
 * <li>Service-layer validation - Works outside controllers</li>
 * <li>Comprehensive coverage - Validates all constrained parameters</li>
 * <li>Return value validation - Ensures methods return valid data</li>
 * <li>Conditional validation - Different rules for different scenarios</li>
 * <li>Consistent behavior - Same validation across all application layers</li>
 * </ul>
 *
 * <p>
 * <b>Basic Usage Examples:</b>
 * 
 * <pre>{@code
 * @Service
 * public class UserService {
 * 
 * 	// 1. Basic parameter validation
 * 	@ValidateMethod
 * 	public User createUser(@NotNull Long tenantId, @Valid User user) {
 * 		return userRepository.save(user);
 * 	}
 * 
 * 	// 2. Input AND output validation
 * 	@ValidateMethod(validateReturn = true)
 * 	public User findUser(@NotNull Long id) {
 * 		User user = userRepository.findById(id);
 * 		return user;
 * 	}
 * 
 * 	// 3. Collect all errors instead of failing fast
 * 	@ValidateMethod(failFast = false)
 * 	public User updateUser(@NotNull Long id, @Valid User user, @NotBlank String reason) {
 * 		return userRepository.update(id, user);
 * 	}
 * }
 * }</pre>
 *
 * <p>
 * <b>Understanding Validation Groups:</b> Validation groups let you apply
 * different validation rules for different scenarios.
 * 
 * <p>
 * <b>Example: Different Validation for Different User Roles:</b>
 * 
 * <pre>{@code
 * // Define groups (simple marker interfaces)
 * public interface BasicValidation {
 * }
 * 
 * public interface AdminValidation {
 * }
 * 
 * public interface StrictValidation {
 * }
 * 
 * public class BankAccount {
 * 	@NotNull
 * 	private String accountNumber;
 * 
 * 	@NotBlank(groups = BasicValidation.class)
 * 	private String accountName;
 * 
 * 	@Min(value = 100, groups = StrictValidation.class)
 * 	private BigDecimal minBalance;
 * 
 * 	@NotNull(groups = AdminValidation.class)
 * 	private String adminNotes;
 * }
 * 
 * @Service
 * public class BankService {
 * 	@ValidateMethod(groups = { BasicValidation.class, Default.class })
 * 	public void createAccount(@Valid BankAccount account) {
 * 	}
 * 
 * 	@ValidateMethod(groups = { StrictValidation.class, Default.class })
 * 	public void transferMoney(@Valid BankAccount account) {
 * 	}
 * 
 * 	@ValidateMethod(groups = { AdminValidation.class, Default.class })
 * 	public void auditAccount(@Valid BankAccount account) {
 * 	}
 * }
 * }</pre>
 *
 * <p>
 * <b>Integration with Existing Validation:</b> Works seamlessly with all Bean
 * Validation constraints:
 * 
 * <pre>{@code
 * @ValidateMethod
 * public void complexOperation(@NotNull Long id, @Size(min = 2, max = 50) String name, @Email String email,
 * 		@Valid Address address, @Min(18) Integer age, @Future Date startDate) {
 * }
 * }</pre>
 *
 * <p>
 * <b>Error Handling:</b> When validation fails, a
 * {@code ConstraintViolationException} is thrown with detailed information
 * about what failed and why.
 * 
 * <p>
 * <b>Prerequisites:</b>
 * <ul>
 * <li>Bean Validation API (jakarta.validation) on classpath</li>
 * <li>Validation implementation (Hibernate Validator recommended)</li>
 * <li>Spring AOP enabled (default in Spring Boot)</li>
 * </ul>
 *
 * @see jakarta.validation.ConstraintViolationException
 * @see jakarta.validation.Validator
 * @see org.springframework.validation.annotation.Validated
 * 
 * @author JavaTech Group
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ValidateMethod {

	/**
	 * Validation groups to use for constraint validation.
	 * 
	 * <p>
	 * Groups allow you to validate different sets of constraints for different
	 * scenarios.
	 * 
	 * <p>
	 * Example:
	 * 
	 * <pre>{@code
	 * public class User {
	 *     @NotNull
	 *     private Long id;
	 *     
	 *     @NotBlank(groups = BasicCheck.class)
	 *     private String name;
	 * }
	 * 
	 * @ValidateMethod(groups = {BasicCheck.class, Default.class})
	 * public void basicOperation(@Valid User user)
	 * }</pre>
	 * 
	 * @return validation groups to activate
	 */
	Class<?>[] groups() default { Default.class };

	/**
	 * Whether to validate the method return value.
	 * 
	 * <p>
	 * When enabled, the object returned by the method is validated against its
	 * constraints before being returned to the caller.
	 * 
	 * <p>
	 * Example:
	 * 
	 * <pre>{@code
	 * public class User {
	 * 	@NotNull
	 * 	private Long id;
	 * }
	 * 
	 * @ValidateMethod(validateReturn = true)
	 * public User findUser(Long id) {
	 * 	User user = userRepository.findById(id);
	 * 	return user;
	 * }
	 * }</pre>
	 * 
	 * @return true to validate return value, false to skip
	 */
	boolean validateReturn() default false;

	/**
	 * Whether to fail immediately on first validation error or collect all errors.
	 * 
	 * <p>
	 * When true (default), validation stops at the first error encountered. When
	 * false, all parameters are validated and all errors are collected.
	 * 
	 * <p>
	 * Example:
	 * 
	 * <pre>{@code
	 * @ValidateMethod(failFast = false)
	 * public void updateUser(@NotNull Long id, @NotBlank String name) {
	 * }
	 * }</pre>
	 * 
	 * @return true to stop at first error, false to collect all errors
	 */
	boolean failFast() default true;

	/**
	 * Custom message for validation exceptions.
	 * 
	 * <p>
	 * Supports the following placeholders:
	 * <ul>
	 * <li>{method} - The method name that failed validation</li>
	 * </ul>
	 * 
	 * <p>
	 * Example:
	 * 
	 * <pre>{@code
	 * @ValidateMethod(message = "Create user validation failed in {method}")
	 * public User createUser(@Valid User user)
	 * }</pre>
	 * 
	 * @return custom exception message template
	 */
	String message() default "Validation failed for method: {method}";
}