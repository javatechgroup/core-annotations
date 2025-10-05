package com.github.javatechgroup.core.annotations.trim;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to indicate that a specific String field should be automatically
 * trimmed.
 * 
 * <p>
 * When applied at the field level, this annotation marks individual String
 * fields for automatic whitespace trimming. This provides granular control over
 * which fields should be processed, allowing you to preserve whitespace in
 * fields where it's meaningful.
 * </p>
 * 
 * <h2>Usage Examples</h2>
 * 
 * <pre>{@code
 * // Example 1: Selective field trimming in a DTO
 * public class UserProfileDTO {
 * 	&#064;Trimmed
 * 	private String username; // Will be automatically trimmed
 * 
 * 	&#064;Trimmed
 * 	private String email; // Will be automatically trimmed
 * 
 * 	private String bio; // Will NOT be trimmed (preserves formatting)
 * }
 * }</pre>
 * 
 * <h2>When to Use Trimmed vs TrimAllStrings</h2>
 * <table>
 * <caption>Annotation Selection Guide</caption>
 * <tr>
 * <th>Scenario</th>
 * <th>Use Trimmed</th>
 * <th>Use TrimAllStrings</th>
 * </tr>
 * <tr>
 * <td>Mixed field requirements</td>
 * <td>Yes (Selective control)</td>
 * <td>No (All or nothing)</td>
 * </tr>
 * <tr>
 * <td>All fields need trimming</td>
 * <td>Verbose</td>
 * <td>Yes (Concise)</td>
 * </tr>
 * </table>
 * 
 * <h2>Processing Behavior</h2>
 * <ul>
 * <li>Trims leading and trailing whitespace using {@link String#trim()}</li>
 * <li>Only affects fields of type {@code String}</li>
 * <li>Null values are preserved (not converted to empty strings)</li>
 * <li>Internal whitespace within strings is preserved</li>
 * </ul>
 * 
 * <h2>Best Practices</h2>
 * <ul>
 * <li>Apply to fields that receive user input (names, emails, usernames)</li>
 * <li>Use on identifier fields (codes, SKUs, IDs) for consistency</li>
 * <li>Avoid on fields containing formatted text, addresses, or
 * descriptions</li>
 * <li>Combine with validation annotations for complete data cleaning</li>
 * <li>Process before validation to ensure clean data for validation rules</li>
 * </ul>
 * 
 * @see com.github.javatechgroup.core.annotations.trim.TrimAllStrings
 * @see com.github.javatechgroup.core.annotations.trim.util.StringTrimmer
 * @since 1.0
 * @author JavaTech Group
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Trimmed {
}