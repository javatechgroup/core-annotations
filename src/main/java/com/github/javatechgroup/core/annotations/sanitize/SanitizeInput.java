package com.github.javatechgroup.core.annotations.sanitize;

import java.lang.annotation.*;

/**
 * Annotation to mark fields, parameters, and methods that should be
 * automatically sanitized to prevent XSS (Cross-Site Scripting) attacks by
 * removing unsafe HTML content.
 * 
 * <p>
 * When applied to fields or method parameters, the annotated elements will be
 * automatically processed by the {@code SanitizeInputAspect} to sanitize HTML
 * content using configurable strategies.
 * </p>
 *
 * <p>
 * <b>Supported Targets:</b>
 * </p>
 * <ul>
 * <li><b>Fields:</b> Sanitized when processing request bodies in Spring MVC
 * controllers</li>
 * <li><b>Parameters:</b> Sanitized when passed to methods with
 * {@code @SanitizeInput} parameters</li>
 * <li><b>Methods:</b> Sanitized return values (future enhancement)</li>
 * </ul>
 *
 * <p>
 * <b>Usage Examples:</b>
 * </p>
 * 
 * <pre>
 * {@code
 * // Field-level sanitization
 * public class User {
 *     &#64;SanitizeInput
 *     private String bio; // HTML in bio will be sanitized automatically
 *     
 *     &#64;SanitizeInput(strategy = Strategy.NONE)
 *     private String comment; // All HTML tags will be removed
 *     
 *     &#64;SanitizeInput(strategy = Strategy.RELAXED, recursive = false)
 *     private Profile profile; // Relaxed sanitization, no recursion into Profile
 * }
 *
 * // Parameter-level sanitization
 * public void processContent(@SanitizeInput String htmlContent) {
 *     // htmlContent is automatically sanitized before method execution
 * }
 *
 * // Method-level sanitization (future)
 * @SanitizeInput
 * public String getUserGeneratedContent() {
 *     return potentiallyUnsafeHtml;
 * }
 * }
 * </pre>
 *
 * <p>
 * <b>Integration:</b>
 * </p>
 * <ul>
 * <li>Works with Spring MVC controllers automatically</li>
 * <li>Requires {@code SanitizeInputAspect} to be configured</li>
 * <li>Uses JSoup for HTML sanitization</li>
 * </ul>
 *
 * @see com.github.javatechgroup.core.annotations.sanitize.aspect.SanitizeInputAspect
 * @see com.github.javatechgroup.core.annotations.sanitize.util.SanitizerUtil
 * @author JavaTech Group
 * @version 1.0
 * @since 2024.1
 */
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SanitizeInput {

	/**
	 * Defines the sanitization strategy to be applied to the annotated element.
	 * Different strategies provide varying levels of HTML tag and attribute
	 * allowance while maintaining security against XSS attacks.
	 *
	 * @return the sanitization strategy to use, defaults to {@link Strategy#BASIC}
	 * @see Strategy
	 */
	Strategy strategy() default Strategy.BASIC;

	/**
	 * Controls whether nested objects should be recursively sanitized when this
	 * annotation is applied to a field. This setting only applies to field
	 * annotations and is ignored for parameter and method annotations.
	 *
	 * <p>
	 * <b>When true:</b> If the field contains an object, recursively scan and
	 * sanitize all {@code @SanitizeInput} annotated fields within that object.
	 * </p>
	 *
	 * <p>
	 * <b>When false:</b> Only sanitize the immediate field if it's a String, do not
	 * recurse into object fields.
	 * </p>
	 *
	 * @return {@code true} to enable recursive sanitization of nested objects,
	 *         {@code false} to sanitize only the immediate field. Defaults to
	 *         {@code true}.
	 *
	 *         <p>
	 *         <b>Example:</b>
	 *         </p>
	 * 
	 *         <pre>
	 *         {@code
	 * public class BlogPost {
	  *     &#64;SanitizeInput(recursive = true)
	  *     private Author author; // Will sanitize Author's @SanitizeInput fields
	  * 
	  *     @SanitizeInput(recursive = false)
	  *     private Metadata metadata; // Only sanitize if Metadata is String, no recursion
	  * }
	  * }
	 * </pre>
	 */
	boolean recursive() default true;

	/**
	 * Enumeration of available HTML sanitization strategies providing different
	 * levels of content filtering and tag allowance.
	 *
	 * <p>
	 * All strategies guarantee protection against common XSS vectors including
	 * script injection, iframe attacks, and other malicious content.
	 * </p>
	 */
	enum Strategy {
		/**
		 * Basic HTML sanitization strategy.
		 * 
		 * <p>
		 * <b>Allowed:</b> Basic text formatting tags
		 * ({@code <b>, <i>, <u>, <em>, <strong>}), links ({@code <a>}), lists ({@code 
		 * <ul>
		 * , 
		 * <ol>
		 * , 
		 * <li>}), block elements ({@code 
		 * <p>
		 * , <div>}), and headings ({@code 
		 * <h1>-
		 * <h6>}).
		 * </p>
		 * 
		 * <p>
		 * <b>Security:</b> All links automatically get {@code rel="nofollow"} and are
		 * restricted to {@code http}, {@code https}, and {@code mailto} protocols.
		 * </p>
		 * 
		 * <p>
		 * <b>Use Case:</b> User comments, blog posts, product descriptions where
		 * limited formatting is desired.
		 * </p>
		 */
		BASIC,

		/**
		 * Relaxed HTML sanitization strategy allowing more HTML content while
		 * maintaining safety.
		 * 
		 * <p>
		 * <b>Allowed:</b> All basic tags plus images ({@code <img>}), tables ({@code 
		 * <table>
		 * , 
		 * <tr>
		 * , 
		 * <td>}), and most common HTML attributes ({@code class, id, style, title}).
		 * </p>
		 * 
		 * <p>
		 * <b>Security:</b> Explicitly blocks dangerous tags
		 * ({@code <script>, <iframe>, <object>, <embed>, <form>}). Images restricted to
		 * safe protocols and links get {@code rel="nofollow"}.
		 * </p>
		 * 
		 * <p>
		 * <b>Use Case:</b> Rich text editors, CMS content, email templates where more
		 * HTML flexibility is needed.
		 * </p>
		 */
		RELAXED,

		/**
		 * Strict sanitization strategy that removes all HTML tags.
		 * 
		 * <p>
		 * <b>Allowed:</b> No HTML tags - all markup is stripped, only plain text
		 * remains.
		 * </p>
		 * 
		 * <p>
		 * <b>Security:</b> Maximum security level - no HTML execution possible.
		 * </p>
		 * 
		 * <p>
		 * <b>Use Case:</b> Usernames, titles, subject lines where no HTML should ever
		 * be allowed.
		 * </p>
		 */
		NONE,

		/**
		 * Custom sanitization strategy using a provider-defined safelist.
		 * 
		 * <p>
		 * <b>Note:</b> Requires additional configuration to register custom safelists
		 * with the {@code SanitizerUtil}.
		 * </p>
		 * 
		 * <p>
		 * <b>Use Case:</b> Application-specific HTML requirements that don't fit the
		 * predefined strategies.
		 * </p>
		 * 
		 * @see com.github.javatechgroup.core.annotations.sanitize.util.SanitizerUtil#registerCustomSafelist(String,
		 *      org.jsoup.safety.Safelist)
		 */
		CUSTOM
	}
}