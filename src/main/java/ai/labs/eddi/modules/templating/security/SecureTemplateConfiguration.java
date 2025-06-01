package ai.labs.eddi.modules.templating.security;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.expression.IExpressionObjectFactory;
import org.thymeleaf.standard.StandardDialect;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Set;
import java.util.Map;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Security configuration for Thymeleaf templates to prevent SSTI attacks.
 * This class implements strict sandboxing by:
 * 
 * 1. Disabling Type expressions (T() operator)
 * 2. Restricting access to dangerous Java classes and methods
 * 3. Implementing input validation and sanitization
 * 4. Providing secure expression evaluation
 * 
 * @author Security Team
 */
@ApplicationScoped
public class SecureTemplateConfiguration {
    
    private static final Logger LOGGER = Logger.getLogger(SecureTemplateConfiguration.class.getName());
    
    // Dangerous classes that should never be accessible in templates
    private static final Set<String> BLACKLISTED_CLASSES = Set.of(
        "java.lang.Runtime",
        "java.lang.Process", 
        "java.lang.ProcessBuilder",
        "java.lang.System",
        "java.lang.Class",
        "java.lang.ClassLoader",
        "java.lang.Thread",
        "java.io.File",
        "java.io.FileInputStream",
        "java.io.FileOutputStream",
        "java.net.URL",
        "java.net.URLClassLoader",
        "java.security.AccessController",
        "javax.script.ScriptEngine",
        "javax.script.ScriptEngineManager",
        "org.springframework.context.ApplicationContext",
        "freemarker.template.utility.Execute"
    );
      // Dangerous method patterns that should be blocked
    private static final Set<String> BLACKLISTED_METHODS = Set.of(
        "getClass",
        "getRuntime", 
        "exec",
        "getMethod",
        "getDeclaredMethod",
        "getConstructor",
        "newInstance",
        "forName",
        "getClassLoader",
        "defineClass",
        "getSystemProperty",
        "getenv"
    );
    
    /**
     * Configures a TemplateEngine with security restrictions to prevent SSTI.
     * 
     * @param templateEngine The template engine to secure
     */    public void configureSecureTemplate(TemplateEngine templateEngine) {
        LOGGER.info("Configuring secure Thymeleaf template engine with SSTI protections");
        
        // Create a secure dialect that replaces the standard dialect
        SecureStandardDialect secureDialect = new SecureStandardDialect();
        
        // Clear all dialects and add only our secure dialect
        // In Thymeleaf 3.1.3, we need to clear dialects properly
        templateEngine.clearDialects();
        templateEngine.addDialect(secureDialect);
        
        LOGGER.info("Secure Thymeleaf configuration applied successfully");
    }
    
    /**
     * Validates template content before processing to detect potential SSTI payloads.
     * 
     * @param template The template content to validate
     * @return true if template is safe, false if potentially malicious
     */
    public boolean validateTemplateContent(String template) {
        if (template == null || template.trim().isEmpty()) {
            return true;
        }
        
        // Check for dangerous type expressions (T() operator)
        if (template.contains("T(") || template.contains("#{T(")) {
            LOGGER.warning("Blocked template containing T() type expression: potential SSTI attempt");
            return false;
        }
        
        // Check for OGNL expressions which should not be present
        if (template.contains("@") && template.contains("@java.")) {
            LOGGER.warning("Blocked template containing OGNL @ expressions: potential SSTI attempt");
            return false;
        }
        
        // Check for dangerous class references
        for (String blacklistedClass : BLACKLISTED_CLASSES) {
            if (template.contains(blacklistedClass)) {
                LOGGER.warning("Blocked template containing blacklisted class: " + blacklistedClass);
                return false;
            }
        }
        
        // Check for dangerous method calls
        for (String blacklistedMethod : BLACKLISTED_METHODS) {
            if (template.contains(blacklistedMethod + "(")) {
                LOGGER.warning("Blocked template containing blacklisted method: " + blacklistedMethod);
                return false;
            }
        }
        
        // Check for script injections
        if (template.toLowerCase().contains("<script") || 
            template.toLowerCase().contains("javascript:") ||
            template.toLowerCase().contains("vbscript:")) {
            LOGGER.warning("Blocked template containing script injection attempt");
            return false;
        }
        
        return true;
    }
    
    /**
     * Sanitizes template variables to prevent injection attacks.
     * 
     * @param variables Template variables map
     * @return Sanitized variables map
     */
    public Map<String, Object> sanitizeTemplateVariables(Map<String, Object> variables) {
        if (variables == null) {
            return Collections.emptyMap();
        }
        
        // Create a new map to avoid modifying the original
        Map<String, Object> sanitized = new java.util.HashMap<>();
        
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // Validate key names
            if (key == null || key.trim().isEmpty()) {
                LOGGER.warning("Skipping variable with null/empty key");
                continue;
            }
            
            // Block dangerous variable names
            if (key.toLowerCase().contains("class") || 
                key.toLowerCase().contains("runtime") ||
                key.toLowerCase().contains("system")) {
                LOGGER.warning("Blocked dangerous variable name: " + key);
                continue;
            }
            
            // Sanitize string values
            if (value instanceof String) {
                String stringValue = (String) value;
                if (!validateTemplateContent(stringValue)) {
                    // Replace with safe placeholder
                    sanitized.put(key, "[SANITIZED]");
                    LOGGER.warning("Sanitized dangerous string value for variable: " + key);
                } else {
                    sanitized.put(key, stringValue);
                }
            } else {
                // For non-string values, ensure they're not dangerous classes
                if (value != null && isDangerousObject(value)) {
                    LOGGER.warning("Blocked dangerous object type for variable: " + key + " (" + value.getClass().getName() + ")");
                    continue;
                }
                sanitized.put(key, value);
            }
        }
        
        return sanitized;
    }
    
    /**
     * Checks if an object is of a dangerous type that should not be accessible in templates.
     */
    private boolean isDangerousObject(Object obj) {
        if (obj == null) {
            return false;
        }
        
        Class<?> clazz = obj.getClass();
        String className = clazz.getName();
        
        // Check against blacklisted classes
        for (String blacklisted : BLACKLISTED_CLASSES) {
            if (className.equals(blacklisted) || className.startsWith(blacklisted + "$")) {
                return true;
            }
        }
        
        // Check for reflection-related classes
        if (className.startsWith("java.lang.reflect.") ||
            className.startsWith("java.lang.Class") ||
            className.startsWith("java.security.") ||
            className.startsWith("javax.script.")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Custom secure dialect that restricts dangerous operations
     */
    private static class SecureStandardDialect extends StandardDialect {
        
        public SecureStandardDialect() {
            super("SecureStandard", "sec", 1000);
        }
        
        @Override
        public IExpressionObjectFactory getExpressionObjectFactory() {
            return new SecureExpressionObjectFactory();
        }
    }
    
    /**
     * Secure expression object factory that filters out dangerous objects
     */
    private static class SecureExpressionObjectFactory implements IExpressionObjectFactory {
        
        @Override
        public Set<String> getAllExpressionObjectNames() {
            // Return only safe expression objects
            return Set.of("strings", "numbers", "bools", "arrays", "lists", "sets", "maps");
        }        @Override
        public Object buildObject(IExpressionContext context, String expressionObjectName) {
            // Only allow safe expression objects
            switch (expressionObjectName) {
                case "strings":
                    return new org.thymeleaf.expression.Strings(context.getLocale());
                case "numbers":
                    return new org.thymeleaf.expression.Numbers(context.getLocale());
                case "bools":
                    return new org.thymeleaf.expression.Bools();
                case "arrays":
                    return new org.thymeleaf.expression.Arrays();
                case "lists":
                    return new org.thymeleaf.expression.Lists();
                case "sets":
                    return new org.thymeleaf.expression.Sets();
                case "maps":
                    return new org.thymeleaf.expression.Maps();
                default:
                    return null; // Block access to other expression objects
            }
        }
        
        @Override
        public boolean isCacheable(String expressionObjectName) {
            return true;
        }
    }
}
