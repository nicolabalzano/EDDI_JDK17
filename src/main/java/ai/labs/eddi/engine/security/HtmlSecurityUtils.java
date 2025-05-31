package ai.labs.eddi.engine.security;

/**
 * Utility class for HTML security operations, specifically XSS prevention
 */
public final class HtmlSecurityUtils {
    
    // Private constructor to prevent instantiation
    private HtmlSecurityUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    /**
     * Escapes HTML special characters to prevent XSS attacks.
     * This is especially important when embedding user input or tokens in HTML templates.
     * 
     * @param input The string to escape
     * @return HTML-escaped string safe for embedding in HTML
     */
    public static String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        
        return input.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;")
                   .replace("/", "&#x2F;")
                   .replace("`", "&#x60;")
                   .replace("(", "&#x28;")
                   .replace(")", "&#x29;");
    }
    
    /**
     * Escapes JavaScript special characters to prevent injection in JavaScript contexts.
     * 
     * @param input The string to escape for JavaScript
     * @return JavaScript-escaped string safe for embedding in JavaScript
     */
    public static String escapeJavaScript(String input) {
        if (input == null) {
            return "";
        }
        
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("'", "\\'")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t")
                   .replace("`", "\\`");
    }
    
    /**
     * Validates that a string contains only safe characters for HTML attribute values.
     * 
     * @param input The string to validate
     * @return true if the string is safe for HTML attributes, false otherwise
     */
    public static boolean isSafeForHtmlAttribute(String input) {
        if (input == null) {
            return true;
        }
        
        // Check for dangerous characters that could break out of attributes
        return !input.contains("<") && 
               !input.contains(">") && 
               !input.contains("\"") && 
               !input.contains("'") &&
               !input.contains("`") &&
               !input.contains("&");
    }
}
