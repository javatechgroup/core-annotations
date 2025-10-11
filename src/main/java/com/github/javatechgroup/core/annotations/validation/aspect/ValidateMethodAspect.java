package com.github.javatechgroup.core.annotations.validation.aspect;

import com.github.javatechgroup.core.annotations.validation.ValidateMethod;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Aspect that performs method-level validation for methods annotated
 * with @ValidateMethod.
 * 
 * <p>
 * This aspect intercepts method executions and automatically validates:
 * <ul>
 * <li>All method parameters with validation constraints
 * (@NotNull, @Size, @Email, etc.)</li>
 * <li>Method return values when validateReturn = true</li>
 * <li>Parameters annotated with @Valid for nested object validation</li>
 * </ul>
 * 
 * <p>
 * <b>Key Features:</b>
 * <ul>
 * <li>Automatic parameter validation</li>
 * <li>Optional return value validation</li>
 * <li>Support for validation groups</li>
 * <li>Fail-fast or collect-all-errors modes</li>
 * <li>Comprehensive logging</li>
 * </ul>
 * 
 * <p>
 * <b>Throws:</b> ConstraintViolationException when validation fails, containing
 * detailed information about what constraints were violated.
 * 
 * @author JavaTech Group
 * @see ValidateMethod
 * @see ConstraintViolationException
 * @since 1.0.0
 */
@Aspect
@Component
public class ValidateMethodAspect {

	private static final Logger log = LoggerFactory.getLogger(ValidateMethodAspect.class);
	private final Validator validator;

	/**
	 * Constructs a new ValidateMethodAspect with the given Validator.
	 * 
	 * @param validator the Bean Validation validator to use for constraint checking
	 */
	public ValidateMethodAspect(Validator validator) {
		this.validator = validator;
	}

	/**
	 * Around advice that validates method parameters and optionally return values
	 * for methods annotated with @ValidateMethod.
	 * 
	 * @param joinPoint      the proceeding join point representing the method
	 *                       execution
	 * @param validateMethod the ValidateMethod annotation configuration
	 * @return the result of the method execution if validation passes
	 * @throws Throwable if the method execution fails or validation fails
	 */
	@Around("@annotation(validateMethod)")
	public Object validateMethodExecution(ProceedingJoinPoint joinPoint, ValidateMethod validateMethod)
			throws Throwable {
		Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
		String methodName = getMethodName(method);
		Object[] args = joinPoint.getArgs();
		Parameter[] parameters = method.getParameters();

		// Validate method parameters
		Set<ConstraintViolation<?>> violations = validateParameters(parameters, args, validateMethod);

		if (!violations.isEmpty()) {
			logValidationFailure(methodName, violations, false);
			throw new ConstraintViolationException(new HashSet<>(violations));
		}

		// Proceed with method execution
		Object result = joinPoint.proceed();

		// Validate return value if configured
		if (validateMethod.validateReturn()) {
			violations = validateReturnValue(result, validateMethod);
			if (!violations.isEmpty()) {
				logValidationFailure(methodName, violations, true);
				throw new ConstraintViolationException(new HashSet<>(violations));
			}
		}

		return result;
	}

	/**
	 * Validates all method parameters that have validation constraints.
	 * 
	 * @param parameters the method parameters to validate
	 * @param args       the actual argument values
	 * @param config     the validation configuration
	 * @return set of constraint violations, empty if validation passes
	 */
	private Set<ConstraintViolation<?>> validateParameters(Parameter[] parameters, Object[] args,
			ValidateMethod config) {
		Set<ConstraintViolation<?>> allViolations = new LinkedHashSet<>();

		for (int i = 0; i < parameters.length; i++) {
			Parameter parameter = parameters[i];
			Object argument = args[i];

			Set<ConstraintViolation<?>> parameterViolations = validateParameter(parameter, argument, config);
			allViolations.addAll(parameterViolations);

			// Fail fast if configured
			if (config.failFast() && !allViolations.isEmpty()) {
				break;
			}
		}

		return allViolations;
	}

	/**
	 * Validates a single parameter if it has validation constraints.
	 * 
	 * @param parameter the parameter metadata
	 * @param argument  the actual argument value
	 * @param config    the validation configuration
	 * @return set of constraint violations for this parameter
	 */
	@SuppressWarnings("unchecked")
	private Set<ConstraintViolation<?>> validateParameter(Parameter parameter, Object argument, ValidateMethod config) {
		if (!shouldValidateParameter(parameter)) {
			return Collections.emptySet();
		}

		if (argument == null) {
			// Validate null values against constraints like @NotNull
			return (Set<ConstraintViolation<?>>) (Set<?>) validator.validateValue(parameter.getType(),
					parameter.getName(), null, config.groups());
		}

		// Validate non-null values
		return (Set<ConstraintViolation<?>>) (Set<?>) validator.validate(argument, config.groups());
	}

	/**
	 * Validates the method return value against its constraints.
	 * 
	 * @param returnValue the value returned by the method
	 * @param config      the validation configuration
	 * @return set of constraint violations, empty if validation passes
	 */
	@SuppressWarnings("unchecked")
	private Set<ConstraintViolation<?>> validateReturnValue(Object returnValue, ValidateMethod config) {
		if (returnValue == null) {
			return Collections.emptySet();
		}

		return (Set<ConstraintViolation<?>>) (Set<?>) validator.validate(returnValue, config.groups());
	}

	/**
	 * Determines whether a parameter should be validated.
	 * 
	 * @param parameter the parameter to check
	 * @return true if the parameter has @Valid annotation or validation constraints
	 */
	private boolean shouldValidateParameter(Parameter parameter) {
		return parameter.isAnnotationPresent(Valid.class) || hasValidationConstraints(parameter);
	}

	/**
	 * Checks if a parameter has any Bean Validation constraints.
	 * 
	 * @param parameter the parameter to check
	 * @return true if the parameter has validation constraints from
	 *         javax.validation or jakarta.validation packages
	 */
	private boolean hasValidationConstraints(Parameter parameter) {
		for (Annotation annotation : parameter.getAnnotations()) {
			String annotationName = annotation.annotationType().getName();
			if (annotationName.startsWith("javax.validation.") || annotationName.startsWith("jakarta.validation.")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Logs validation failures at DEBUG level.
	 * 
	 * @param methodName    the name of the method that failed validation
	 * @param violations    the set of constraint violations
	 * @param isReturnValue true if validating return value, false for parameters
	 */
	private void logValidationFailure(String methodName, Set<ConstraintViolation<?>> violations,
			boolean isReturnValue) {
		if (log.isDebugEnabled()) {
			String type = isReturnValue ? "return value" : "parameters";
			String violationDetails = buildViolationDetails(violations);
			log.debug("Validation failed for {} {}: {}", methodName, type, violationDetails);
		}
	}

	/**
	 * Builds a concise string representation of constraint violations.
	 * 
	 * @param violations the set of constraint violations
	 * @return formatted string with violation details
	 */
	private String buildViolationDetails(Set<ConstraintViolation<?>> violations) {
		if (violations.isEmpty()) {
			return "No violations";
		}

		StringBuilder details = new StringBuilder();
		for (ConstraintViolation<?> violation : violations) {
			if (details.length() > 0) {
				details.append(", ");
			}
			details.append(violation.getPropertyPath()).append(": ").append(violation.getMessage());
		}
		return details.toString();
	}

	/**
	 * Gets the fully qualified method name for logging.
	 * 
	 * @param method the method
	 * @return class name + method name
	 */
	private String getMethodName(Method method) {
		return method.getDeclaringClass().getSimpleName() + "." + method.getName();
	}
}