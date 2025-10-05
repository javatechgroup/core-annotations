package com.github.javatechgroup.core.annotations.sanitize.util;

import com.github.javatechgroup.core.annotations.sanitize.SanitizeInput;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Thread-safe HTML sanitization utility with performance optimizations
 * including caching and configurable sanitization strategies.
 * 
 * <p>
 * This utility provides XSS protection by sanitizing HTML input using JSoup's
 * safelist-based approach. It includes multiple pre-configured security levels
 * and supports custom safelists for application-specific requirements.
 * </p>
 *
 * <p>
 * <b>Key Features:</b>
 * </p>
 * <ul>
 * <li>Thread-safe implementation with proper synchronization</li>
 * <li>LRU cache for performance optimization</li>
 * <li>Multiple sanitization strategies (BASIC, RELAXED, NONE, CUSTOM)</li>
 * <li>Custom safelist registry for extended functionality</li>
 * <li>Input size validation and safety limits</li>
 * <li>Fail-safe error handling</li>
 * </ul>
 *
 * <p>
 * <b>Usage Examples:</b>
 * </p>
 * 
 * <pre>{@code
 * // Basic sanitization
 * String safeHtml = SanitizerUtil.sanitize(untrustedInput, SanitizeInput.Strategy.BASIC);
 * 
 * // Custom safelist usage
 * Safelist customSafelist = Safelist.basic().addTags("custom-tag");
 * SanitizerUtil.registerCustomSafelist("mySafelist", customSafelist);
 * String customSanitized = SanitizerUtil.sanitize(input, SanitizeInput.Strategy.CUSTOM, "mySafelist");
 * 
 * // Default sanitization (BASIC strategy)
 * String defaultSanitized = SanitizerUtil.sanitize(untrustedInput);
 * }</pre>
 *
 * <p>
 * <b>Performance Notes:</b>
 * </p>
 * <ul>
 * <li>Uses LRU cache with {@value #MAX_CACHE_SIZE} entry limit</li>
 * <li>Skips caching for very short ({@code < 5 chars}) and very long
 * ({@code > 5000 chars}) inputs</li>
 * <li>Uses hash-based keys for large inputs to conserve memory</li>
 * </ul>
 *
 * @see SanitizeInput
 * @see org.jsoup.Jsoup
 * @see org.jsoup.safety.Safelist
 * @author JavaTech Group
 * @version 1.0
 * @since 2024.1
 */
public class SanitizerUtil {

	private static final Logger logger = LoggerFactory.getLogger(SanitizerUtil.class);

	/**
	 * Maximum number of entries in the sanitization cache. When exceeded, least
	 * recently used entries are evicted.
	 */
	private static final int MAX_CACHE_SIZE = 5000;

	/**
	 * Maximum input length allowed for sanitization (100KB). Inputs exceeding this
	 * limit are rejected for safety.
	 */
	private static final int MAX_INPUT_LENGTH = 100000;

	// Safelist configurations

	/**
	 * BASIC strategy safelist configuration.
	 * <p>
	 * Allows: Basic text formatting, links, lists, block elements, headings
	 * </p>
	 * <p>
	 * Security: Links get {@code rel="nofollow"}, restricted to safe protocols
	 * </p>
	 */
	private static final Safelist BASIC = Safelist.basic().addTags("h1", "h2", "h3", "h4", "h5", "h6", "br", "hr")
			.addAttributes("a", "href", "target", "rel").addProtocols("a", "href", "http", "https", "mailto")
			.addEnforcedAttribute("a", "rel", "nofollow");

	/**
	 * RELAXED strategy safelist configuration.
	 * <p>
	 * Allows: All BASIC tags plus images, tables, and common attributes
	 * </p>
	 * <p>
	 * Security: Explicitly blocks dangerous tags (script, iframe, etc.)
	 * </p>
	 */
	private static final Safelist RELAXED = Safelist.relaxed().addAttributes(":all", "class", "id", "style", "title")
			.addAttributes("img", "alt", "width", "height").addProtocols("img", "src", "http", "https", "data")
			.addEnforcedAttribute("a", "rel", "nofollow").removeTags("script", "iframe", "object", "embed", "form");

	/**
	 * NONE strategy safelist configuration.
	 * <p>
	 * Removes all HTML tags, leaving only plain text
	 * </p>
	 */
	private static final Safelist NONE = Safelist.none();

	/**
	 * LRU Cache for sanitization results.
	 * <p>
	 * Uses access-order LinkedHashMap to automatically evict least recently used
	 * entries when the size exceeds {@value #MAX_CACHE_SIZE}.
	 * </p>
	 */
	private static final Map<String, String> sanitizationCache = new LinkedHashMap<String, String>(MAX_CACHE_SIZE,
			0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
			return size() > MAX_CACHE_SIZE;
		}
	};

	/**
	 * Registry for custom safelists.
	 * <p>
	 * Allows applications to register their own Safelist configurations for use
	 * with the {@link SanitizeInput.Strategy#CUSTOM} strategy.
	 * </p>
	 */
	private static final Map<String, Safelist> customSafelists = new LinkedHashMap<>();

	/**
	 * Sanitizes the input string using the specified strategy.
	 * 
	 * <p>
	 * This method applies HTML sanitization to prevent XSS attacks while preserving
	 * allowed content based on the chosen strategy. Results are cached for
	 * performance when appropriate.
	 * </p>
	 *
	 * @param input    the input string to sanitize, may be {@code null}
	 * @param strategy the sanitization strategy to apply
	 * @return the sanitized string, or {@code null} if input was {@code null}, or
	 *         empty string if input was too large or sanitization failed
	 * @throws IllegalArgumentException if strategy is {@code null}
	 * 
	 * @example
	 * 
	 *          <pre>{@code
	 * String userInput = "<script>alert('xss')</script><p>Safe content</p>";
	 * String safe = SanitizerUtil.sanitize(userInput, SanitizeInput.Strategy.BASIC);
	 * // Result: "<p>Safe content</p>" - script tag removed
	 * }</pre>
	 */
	public static String sanitize(String input, SanitizeInput.Strategy strategy) {
		return sanitize(input, strategy, null);
	}

	/**
	 * Sanitizes the input string using a custom safelist strategy.
	 * 
	 * <p>
	 * This variant allows using a pre-registered custom safelist by name. The
	 * custom safelist must be registered using
	 * {@link #registerCustomSafelist(String, Safelist)} before use.
	 * </p>
	 *
	 * @param input              the input string to sanitize, may be {@code null}
	 * @param strategy           the sanitization strategy, must be
	 *                           {@link SanitizeInput.Strategy#CUSTOM} for custom
	 *                           safelist to be used
	 * @param customSafelistName the name of the registered custom safelist, may be
	 *                           {@code null} if not using CUSTOM strategy
	 * @return the sanitized string, or {@code null} if input was {@code null}, or
	 *         empty string if input was too large, sanitization failed, or custom
	 *         safelist not found
	 * @throws IllegalArgumentException if strategy is {@code null}
	 * 
	 * @example
	 * 
	 *          <pre>{@code
	 * // Register custom safelist first
	 * Safelist mySafelist = Safelist.basic().addTags("mark");
	 * SanitizerUtil.registerCustomSafelist("highlight", mySafelist);
	 * 
	 * // Use custom safelist
	 * String result = SanitizerUtil.sanitize(
	 *     "<mark>Important</mark><script>alert('xss')</script>", 
	 *     SanitizeInput.Strategy.CUSTOM, 
	 *     "highlight"
	 * );
	 * // Result: "<mark>Important</mark>" - script removed, mark preserved
	 * }</pre>
	 */
	public static String sanitize(String input, SanitizeInput.Strategy strategy, String customSafelistName) {
		if (input == null) {
			return null;
		}

		// Validate input size
		if (input.length() > MAX_INPUT_LENGTH) {
			logger.warn("Input too large for sanitization: {} characters", input.length());
			return ""; // Return empty instead of processing huge input
		}

		// Skip cache for very short strings or very long strings
		if (shouldSkipCache(input)) {
			return doSanitize(input, strategy, customSafelistName);
		}

		String cacheKey = generateCacheKey(input, strategy, customSafelistName);

		// Use synchronized block for thread-safe cache access
		synchronized (sanitizationCache) {
			return sanitizationCache.computeIfAbsent(cacheKey, k -> doSanitize(input, strategy, customSafelistName));
		}
	}

	/**
	 * Registers a custom safelist for use with the
	 * {@link SanitizeInput.Strategy#CUSTOM} strategy.
	 *
	 * @param name     the unique name to identify the safelist, must not be
	 *                 {@code null} or empty
	 * @param safelist the JSoup Safelist configuration, must not be {@code null}
	 * @throws IllegalArgumentException if name is {@code null}/empty or safelist is
	 *                                  {@code null}
	 * 
	 * @example
	 * 
	 *          <pre>{@code
	 * Safelist custom = Safelist.none()
	 *     .addTags("b", "i", "u")
	 *     .addAttributes("b", "class");
	 * SanitizerUtil.registerCustomSafelist("simple-format", custom);
	 * }</pre>
	 */
	public static void registerCustomSafelist(String name, Safelist safelist) {
		if (name == null || name.trim().isEmpty()) {
			throw new IllegalArgumentException("Safelist name cannot be null or empty");
		}
		if (safelist == null) {
			throw new IllegalArgumentException("Safelist cannot be null");
		}

		synchronized (customSafelists) {
			customSafelists.put(name, safelist);
			logger.debug("Registered custom safelist: {}", name);
		}
	}

	/**
	 * Removes a previously registered custom safelist.
	 *
	 * @param name the name of the safelist to remove
	 * @return {@code true} if the safelist was found and removed, {@code false}
	 *         otherwise
	 */
	public static boolean removeCustomSafelist(String name) {
		synchronized (customSafelists) {
			boolean removed = customSafelists.remove(name) != null;
			if (removed) {
				logger.debug("Removed custom safelist: {}", name);
			}
			return removed;
		}
	}

	/**
	 * Performs the actual sanitization using JSoup.
	 */
	private static String doSanitize(String input, SanitizeInput.Strategy strategy, String customSafelistName) {
		try {
			Safelist safelist = getSafelist(strategy, customSafelistName);
			return Jsoup.clean(input, safelist);
		} catch (Exception e) {
			logger.error("Sanitization failed for strategy: {}", strategy, e);
			return ""; // Fail safe - return empty string
		}
	}

	/**
	 * Retrieves the appropriate Safelist for the given strategy.
	 */
	private static Safelist getSafelist(SanitizeInput.Strategy strategy, String customSafelistName) {
		switch (strategy) {
		case BASIC:
			return BASIC;
		case RELAXED:
			return RELAXED;
		case NONE:
			return NONE;
		case CUSTOM:
			Safelist custom = customSafelists.get(customSafelistName);
			if (custom == null) {
				logger.warn("Custom safelist not found: {}, falling back to BASIC", customSafelistName);
				return BASIC;
			}
			return custom;
		default:
			logger.warn("Unknown strategy: {}, falling back to BASIC", strategy);
			return BASIC;
		}
	}

	/**
	 * Determines if caching should be skipped for the given input. Caching is
	 * skipped for very short or very long strings to optimize performance.
	 */
	private static boolean shouldSkipCache(String input) {
		return input.length() < 5 || input.length() > 5000;
	}

	/**
	 * Generates a cache key for the input and strategy. For large inputs, uses hash
	 * code to conserve memory.
	 */
	private static String generateCacheKey(String input, SanitizeInput.Strategy strategy, String customSafelistName) {
		if (input.length() > 1000) {
			return strategy.name() + ":" + (customSafelistName != null ? customSafelistName + ":" : "")
					+ Integer.toHexString(input.hashCode());
		}
		return strategy.name() + ":" + (customSafelistName != null ? customSafelistName + ":" : "") + input;
	}

	/**
	 * Clears both the sanitization cache and custom safelist registry.
	 * 
	 * <p>
	 * <b>Note:</b> This operation affects all threads and should be used carefully
	 * in production environments.
	 * </p>
	 */
	public static void clear() {
		synchronized (sanitizationCache) {
			sanitizationCache.clear();
		}
		synchronized (customSafelists) {
			customSafelists.clear();
		}
		logger.debug("Cleared all sanitization caches and custom safelists");
	}

	/**
	 * Returns the current number of entries in the sanitization cache.
	 *
	 * @return the current cache size
	 */
	public static int getCacheSize() {
		synchronized (sanitizationCache) {
			return sanitizationCache.size();
		}
	}

	/**
	 * Returns the number of registered custom safelists.
	 *
	 * @return the count of custom safelists
	 */
	public static int getCustomSafelistCount() {
		synchronized (customSafelists) {
			return customSafelists.size();
		}
	}

	/**
	 * Returns the maximum allowed input length.
	 *
	 * @return the maximum input length in characters
	 */
	public static int getMaxInputLength() {
		return MAX_INPUT_LENGTH;
	}

	/**
	 * Returns the maximum cache size.
	 *
	 * @return the maximum number of cache entries
	 */
	public static int getMaxCacheSize() {
		return MAX_CACHE_SIZE;
	}
	
	/**
     * Default constructor.
     */
    private SanitizerUtil() {
        // Aspect initialization
    }
}