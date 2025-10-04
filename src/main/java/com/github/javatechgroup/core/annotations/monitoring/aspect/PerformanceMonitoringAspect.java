package com.github.javatechgroup.core.annotations.monitoring.aspect;

import com.github.javatechgroup.core.annotations.monitoring.MonitorPerformance;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Aspect implementation for performance monitoring that intercepts methods
 * annotated with {@link MonitorPerformance} to track execution time, memory
 * usage, and exception handling.
 *
 * <p>
 * This aspect provides comprehensive performance monitoring capabilities
 * including:
 * </p>
 * <ul>
 * <li>Execution time tracking with configurable slow thresholds</li>
 * <li>Memory usage monitoring with configurable memory thresholds</li>
 * <li>Exception tracking with optional stack trace logging</li>
 * <li>Configurable log levels and output formatting</li>
 * <li>Non-intrusive memory measurement without forcing garbage collection</li>
 * </ul>
 *
 * <p>
 * <b>Important Notes on Memory Monitoring:</b>
 * </p>
 * <ul>
 * <li>Memory measurements are indicative and may include JVM overhead</li>
 * <li>For accurate memory analysis, use dedicated profiling tools</li>
 * <li>Memory thresholds should be set conservatively to avoid false
 * positives</li>
 * </ul>
 *
 * <p>
 * <b>Usage Example:</b>
 * </p>
 * 
 * <pre>
 * {
 * 	&#64;code
 * 	&#64;Service
 * 	public class UserService {
 * 
 * 		&#64;MonitorPerformance(slowThreshold = 100, memoryThreshold = 10)
 * 		public User findUser(String userId) {
 * 			// Method implementation
 * 		}
 * 
 * 		@MonitorPerformance(trackExceptions = false, logLevel = LogLevel.DEBUG)
 * 		public void quickOperation() {
 * 			// Fast operation that doesn't need exception logging
 * 		}
 * 	}
 * }
 * </pre>
 *
 * <p>
 * <b>Log Output Examples:</b>
 * </p>
 * 
 * <pre>
 * UserService.findUser | Time: 85ms | Memory: 8MB | Success: true
 * ReportService.generate | Time: 150ms | Memory: 15MB | Success: true | ⚠️ SLOW (threshold: 100ms)
 * PaymentService.process | Time: 25ms | Memory: 2MB | Success: false | Exception: PaymentFailedException: Insufficient funds
 * </pre>
 *
 * @author JavaTech Group
 * @see MonitorPerformance
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
public class PerformanceMonitoringAspect {

	/**
	 * Around advice that intercepts method execution for performance monitoring.
	 *
	 * <p>
	 * This method is triggered for any method annotated with
	 * {@link MonitorPerformance}. It captures performance metrics including
	 * execution time, memory usage, and success status, then logs the results
	 * according to the annotation configuration.
	 * </p>
	 *
	 * <p>
	 * <b>Execution Flow:</b>
	 * </p>
	 * <ol>
	 * <li>Capture start time and initial memory usage (if memory tracking is
	 * enabled)</li>
	 * <li>Execute the target method via {@link ProceedingJoinPoint#proceed()}</li>
	 * <li>Capture end time and final memory usage (if memory tracking is
	 * enabled)</li>
	 * <li>Calculate and log performance metrics</li>
	 * <li>Handle exceptions according to configuration</li>
	 * </ol>
	 *
	 * <p>
	 * <b>Performance Considerations:</b>
	 * </p>
	 * <ul>
	 * <li>No forced garbage collection is performed to avoid performance
	 * degradation</li>
	 * <li>Memory measurements are taken as-is from JVM runtime metrics</li>
	 * <li>Overhead is minimized by checking configuration flags before
	 * measurements</li>
	 * </ul>
	 *
	 * @param joinPoint the proceeding join point representing the intercepted
	 *                  method execution
	 * @param config    the {@link MonitorPerformance} annotation configuration from
	 *                  the intercepted method
	 * @return the result of the method execution, passed through unchanged
	 * @throws Throwable any exception thrown by the intercepted method, rethrown
	 *                   after logging
	 *
	 * @example
	 * 
	 *          <pre>{@code
	 * // When a method is annotated with @MonitorPerformance(slowThreshold = 100)
	 * // and takes 150ms to execute, the aspect will log:
	 * // "UserService.slowMethod | Time: 150ms | Memory: 5MB | Success: true | ⚠️ SLOW (threshold: 100ms)"
	 * }</pre>
	 *
	 * @see ProceedingJoinPoint
	 * @see MonitorPerformance
	 */
	@Around("@annotation(config)")
	public Object monitorPerformance(ProceedingJoinPoint joinPoint, MonitorPerformance config) throws Throwable {
		// Capture initial metrics based on configuration
		long startTime = config.trackTime() ? System.currentTimeMillis() : 0;
		long startMemory = config.trackMemory() ? getUsedMemory() : 0;

		String className = joinPoint.getTarget().getClass().getSimpleName();
		String methodName = joinPoint.getSignature().getName();
		String fullMethodName = className + "." + methodName;

		try {
			Object result = joinPoint.proceed();
			logPerformance(fullMethodName, startTime, startMemory, true, null, config);
			return result;

		} catch (Throwable throwable) {
			logPerformance(fullMethodName, startTime, startMemory, false, throwable, config);
			throw throwable;
		}
	}

	/**
	 * Calculates the currently used memory by the JVM.
	 *
	 * <p>
	 * This method computes memory usage by subtracting free memory from total
	 * memory. The result represents the memory currently occupied by Java objects.
	 * </p>
	 *
	 * <p>
	 * <b>Note:</b> This measurement includes all objects in the heap and may be
	 * influenced by JVM garbage collection cycles. It provides an indicative
	 * measure of memory usage rather than precise allocation tracking.
	 * </p>
	 *
	 * @return the amount of used memory in bytes
	 *
	 * @see Runtime#totalMemory()
	 * @see Runtime#freeMemory()
	 */
	private long getUsedMemory() {
		Runtime runtime = Runtime.getRuntime();
		return runtime.totalMemory() - runtime.freeMemory();
	}

	/**
	 * Processes and logs performance metrics based on the monitoring configuration.
	 *
	 * <p>
	 * This method calculates the final performance metrics after method execution,
	 * formats the log message, and outputs it at the appropriate log level.
	 * </p>
	 *
	 * <p>
	 * <b>Metrics Calculated:</b>
	 * </p>
	 * <ul>
	 * <li>Execution time (if time tracking is enabled)</li>
	 * <li>Memory usage delta (if memory tracking is enabled)</li>
	 * <li>Success/failure status</li>
	 * <li>Exception information (if method failed and exception logging is
	 * enabled)</li>
	 * </ul>
	 *
	 * <p>
	 * <b>Threshold Checking:</b>
	 * </p>
	 * <ul>
	 * <li>Slow execution: When execution time exceeds
	 * {@link MonitorPerformance#slowThreshold()}</li>
	 * <li>High memory usage: When memory usage exceeds
	 * {@link MonitorPerformance#memoryThreshold()} MB</li>
	 * </ul>
	 *
	 * <p>
	 * <b>Memory Measurement Notes:</b>
	 * </p>
	 * <ul>
	 * <li>Memory delta calculation uses
	 * {@code Math.max(0, endMemory - startMemory)} to avoid negative values</li>
	 * <li>Results are converted to MB for readability</li>
	 * <li>This is an indicative measurement, not precise memory allocation
	 * tracking</li>
	 * </ul>
	 *
	 * @param fullMethodName the fully qualified method name in format
	 *                       "ClassName.methodName"
	 * @param startTime      the system time in milliseconds when method execution
	 *                       started, or 0 if time tracking is disabled
	 * @param startMemory    the memory usage in bytes before method execution
	 *                       started, or 0 if memory tracking is disabled
	 * @param success        indicates whether the method executed successfully
	 *                       (true) or threw an exception (false)
	 * @param exception      the exception thrown by the method, or null if
	 *                       execution was successful
	 * @param config         the monitoring configuration from the
	 *                       {@link MonitorPerformance} annotation
	 *
	 * @see MonitorPerformance
	 */
	private void logPerformance(String fullMethodName, long startTime, long startMemory, boolean success,
			Throwable exception, MonitorPerformance config) {
		// Calculate final metrics based on configuration
		long endTime = config.trackTime() ? System.currentTimeMillis() : 0;
		long endMemory = config.trackMemory() ? getUsedMemory() : 0;

		long executionTime = config.trackTime() ? endTime - startTime : 0;
		long memoryUsed = config.trackMemory() ? Math.max(0, endMemory - startMemory) : 0;
		long memoryUsedMB = config.trackMemory() ? memoryUsed / (1024 * 1024) : 0;

		// Build log message
		StringBuilder message = new StringBuilder();
		message.append(fullMethodName);

		if (config.trackTime()) {
			message.append(" | Time: ").append(executionTime).append("ms");
		}

		if (config.trackMemory()) {
			message.append(" | Memory: ").append(memoryUsedMB).append("MB");
		}

		message.append(" | Success: ").append(success);

		if (!success && exception != null) {
			message.append(" | Exception: ").append(exception.getClass().getSimpleName());
			if (config.logExceptions() && exception.getMessage() != null) {
				message.append(": ").append(exception.getMessage());
			}
		}

		// Check thresholds only if the respective tracking is enabled
		boolean isSlow = config.trackTime() && executionTime > config.slowThreshold();
		boolean isHighMemory = config.trackMemory() && memoryUsedMB > config.memoryThreshold();

		if (isSlow) {
			message.append(" | ⚠️ SLOW (threshold: ").append(config.slowThreshold()).append("ms)");
		}
		if (isHighMemory) {
			message.append(" | 💾 HIGH_MEMORY (threshold: ").append(config.memoryThreshold()).append("MB)");
		}

		// Log with appropriate level
		logWithLevel(config, message.toString(), exception, isSlow || isHighMemory);
	}

	/**
	 * Logs the performance message at the appropriate log level based on
	 * configuration and execution results.
	 *
	 * <p>
	 * <b>Log Level Determination:</b>
	 * </p>
	 * <ul>
	 * <li><b>ERROR:</b> Method failed and exception logging is enabled (with
	 * optional stack trace)</li>
	 * <li><b>WARN:</b> Method succeeded but exceeded thresholds, or general warning
	 * conditions</li>
	 * <li><b>Configured Level:</b> Method succeeded within thresholds (DEBUG, INFO,
	 * or WARN from annotation)</li>
	 * </ul>
	 *
	 * <p>
	 * <b>Exception Handling:</b>
	 * </p>
	 * <ul>
	 * <li>If {@link MonitorPerformance#logExceptions()} is false, exceptions are
	 * not logged</li>
	 * <li>If {@link MonitorPerformance#logStackTrace()} is true, full stack traces
	 * are included</li>
	 * <li>Otherwise, only exception class and message are logged</li>
	 * </ul>
	 *
	 * @param config    the monitoring configuration from {@link MonitorPerformance}
	 * @param message   the formatted log message to output
	 * @param exception the exception thrown by the method, or null if successful
	 * @param isWarning indicates if the execution triggered any warning conditions
	 *                  (slow or high memory)
	 *
	 * @see MonitorPerformance#logLevel()
	 * @see MonitorPerformance#logExceptions()
	 * @see MonitorPerformance#logStackTrace()
	 */
	private void logWithLevel(MonitorPerformance config, String message, Throwable exception, boolean isWarning) {
		if (exception != null && config.logExceptions()) {
			if (config.logStackTrace()) {
				log.error(message, exception);
			} else {
				log.error(message);
			}
		} else if (isWarning) {
			log.warn(message);
		} else {
			switch (config.logLevel()) {
			case DEBUG -> log.debug(message);
			case WARN -> log.warn(message);
			case INFO -> log.info(message);
			default -> log.info(message);
			}
		}
	}
}