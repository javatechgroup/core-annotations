package com.github.javatechgroup.core.annotations.monitoring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Performance monitoring annotation for method-level execution tracking.
 * 
 * <p>
 * This annotation enables comprehensive monitoring of method execution
 * including time tracking, memory usage monitoring, and exception tracking with
 * configurable thresholds and logging levels.
 * </p>
 * 
 * <p>
 * <b>Usage Example:</b>
 * </p>
 * 
 * <pre>{@code
 * @Service
 * public class UserService {
 * 
 * 	@MonitorPerformance(slowThreshold = 500, memoryThreshold = 50, logLevel = MonitorPerformance.LogLevel.INFO)
 * 	public List&lt;User&gt; getUsers() {
 * 		return userRepository.findAll();
 * 	}
 * 
 * 	@MonitorPerformance(trackMemory = false, logExceptions = false)
 * 	public User getUserById(Long id) {
 * 		return userRepository.findById(id).orElse(null);
 * 	}
 * }
 * }</pre>
 * 
 * <p>
 * <b>Expected Log Output:</b>
 * </p>
 * 
 * <pre>
 * UserService.getUsers | Time: 245ms | Memory: 12MB | Success: true
 * UserService.getUsers | Time: 1200ms | Memory: 65MB | Success: true | SLOW | HIGH_MEMORY
 * UserService.getUserById | Time: 15ms | Success: false | Exception: EntityNotFoundException
 * </pre>
 * 
 * @author JavaTech Group
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MonitorPerformance {

	/**
	 * Enable or disable execution time tracking. When enabled, the method execution
	 * time will be measured and logged.
	 * 
	 * <p>
	 * Default: {@code true}
	 * </p>
	 * 
	 * @return true to track execution time, false to disable
	 */
	boolean trackTime() default true;

	/**
	 * Enable or disable memory usage tracking. When enabled, the memory usage
	 * difference before and after method execution will be calculated and logged.
	 * 
	 * <p>
	 * Default: {@code true}
	 * </p>
	 * 
	 * @return true to track memory usage, false to disable
	 */
	boolean trackMemory() default true;

	/**
	 * Enable or disable exception tracking. When enabled, any exceptions thrown by
	 * the method will be captured and logged.
	 * 
	 * <p>
	 * Default: {@code true}
	 * </p>
	 * 
	 * @return true to track exceptions, false to disable
	 */
	boolean trackExceptions() default true;

	/**
	 * Threshold for slow execution detection in milliseconds. If execution time
	 * exceeds this value, a "SLOW" alert will be added to the log. Set to -1 to
	 * disable slow execution detection.
	 * 
	 * <p>
	 * Default: {@code 1000}
	 * </p>
	 * 
	 * @return slow threshold in milliseconds
	 */
	long slowThreshold() default 1000;

	/**
	 * Threshold for high memory usage detection in megabytes. If memory usage
	 * exceeds this value, a "HIGH_MEMORY" alert will be added to the log. Set to -1
	 * to disable high memory usage detection.
	 * 
	 * <p>
	 * Default: {@code 100}
	 * </p>
	 * 
	 * @return memory threshold in megabytes
	 */
	long memoryThreshold() default 100;

	/**
	 * Enable or disable exception logging. When disabled, exceptions will still be
	 * tracked but not logged. Useful for expected exceptions that shouldn't clutter
	 * the logs.
	 * 
	 * <p>
	 * Default: {@code true}
	 * </p>
	 * 
	 * @return true to log exceptions, false to suppress exception logging
	 */
	boolean logExceptions() default true;

	/**
	 * Enable or disable stack trace logging for exceptions. When enabled, the full
	 * stack trace will be logged along with the exception message. When disabled,
	 * only the exception type and message will be logged.
	 * 
	 * <p>
	 * Default: {@code false}
	 * </p>
	 * 
	 * @return true to log full stack traces, false to log only exception details
	 */
	boolean logStackTrace() default false;

	/**
	 * Logging level for performance metrics output. Determines at which log level
	 * the performance metrics will be reported.
	 * 
	 * <p>
	 * Default: {@link LogLevel#INFO}
	 * </p>
	 * 
	 * @return the logging level for performance output
	 * @see LogLevel
	 */
	LogLevel logLevel() default LogLevel.INFO;

	/**
	 * Logging levels available for performance metrics output.
	 */
	enum LogLevel {

		/**
		 * Debug level - use for frequently called methods or detailed debugging.
		 * Metrics will be logged at DEBUG level.
		 */
		DEBUG,

		/**
		 * Info level - use for normal method monitoring. Metrics will be logged at INFO
		 * level.
		 */
		INFO,

		/**
		 * Warn level - use for critical methods that should always be visible. Metrics
		 * will be logged at WARN level.
		 */
		WARN
	}
}