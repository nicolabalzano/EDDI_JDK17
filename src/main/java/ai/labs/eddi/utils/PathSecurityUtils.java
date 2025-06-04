package ai.labs.eddi.utils;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class for secure path operations to prevent path traversal attacks.
 * 
 * @author Security Team
 */
public final class PathSecurityUtils {
    
    // Private constructor to prevent instantiation
    private PathSecurityUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    /**
     * Validates that the provided path components don't contain path traversal sequences.
     * 
     * @param pathComponents The path components to validate
     * @throws SecurityException if path traversal is detected
     */
    public static void validatePathComponents(String... pathComponents) {
        if (pathComponents == null) {
            return;
        }
        
        for (String component : pathComponents) {
            if (component == null) {
                continue;
            }
            
            // Check for directory traversal patterns
            if (component.contains("..") || 
                component.contains("./") || 
                component.contains(".\\") ||
                component.startsWith("/") ||
                component.startsWith("\\") ||
                component.contains(":") ||  // Windows drive letters or alternate data streams
                component.contains("~")) {  // Unix home directory shortcuts
                
                throw new SecurityException("Path traversal detected in component: " + component);
            }
            
            // Check for null bytes (can be used to bypass filters)
            if (component.contains("\0")) {
                throw new SecurityException("Null byte detected in path component: " + component);
            }
            
            // Check for suspicious patterns
            String normalized = component.toLowerCase();
            if (normalized.contains("..") ||
                normalized.contains("etc") ||
                normalized.contains("passwords") ||
                normalized.contains("home") ||
                normalized.contains("boot")) {
                
                throw new SecurityException("Suspicious path pattern detected: " + component);
            }
        }
    }
    
    /**
     * Safely builds a path by validating all components and ensuring the result 
     * stays within the base directory.
     * 
     * @param baseDir The base directory that the result must stay within
     * @param pathComponents The path components to append
     * @return A safe path string
     * @throws SecurityException if path traversal is detected or result escapes base directory
     */
    public static String buildSecurePath(String baseDir, String... pathComponents) {
        if (baseDir == null) {
            throw new IllegalArgumentException("Base directory cannot be null");
        }
        
        // Validate all path components first
        validatePathComponents(pathComponents);
          // Build the path using FileUtilities
        // Create a combined array with baseDir as first element
        String[] allComponents = new String[pathComponents.length + 1];
        allComponents[0] = baseDir;
        System.arraycopy(pathComponents, 0, allComponents, 1, pathComponents.length);
        String resultPath = FileUtilities.buildPath(allComponents);
        
        // Resolve to absolute paths for comparison
        try {
            Path basePath = Paths.get(baseDir).toAbsolutePath().normalize();
            Path resultAbsolutePath = Paths.get(resultPath).toAbsolutePath().normalize();
            
            // Ensure the result path starts with the base path (is contained within)
            if (!resultAbsolutePath.startsWith(basePath)) {
                throw new SecurityException("Path traversal detected: result path escapes base directory. " +
                    "Base: " + basePath + ", Result: " + resultAbsolutePath);
            }
            
            return resultPath;
        } catch (Exception e) {
            throw new SecurityException("Error validating path security: " + e.getMessage(), e);
        }
    }
    
    /**
     * Sanitizes a path component by removing dangerous characters.
     * 
     * @param component The path component to sanitize
     * @return Sanitized path component
     */
    public static String sanitizePathComponent(String component) {
        if (component == null) {
            return null;
        }
        
        // Remove dangerous characters
        String sanitized = component
            .replace("..", "_")
            .replace("/", "_")
            .replace("\\", "_")
            .replace(":", "_")
            .replace("~", "_")
            .replace("\0", "");
        
        // Limit length to prevent filesystem issues
        if (sanitized.length() > 255) {
            sanitized = sanitized.substring(0, 255);
        }
        
        return sanitized;
    }
    
    /**
     * Validates that a file path is safe and within expected boundaries.
     * 
     * @param filePath The file path to validate
     * @param allowedDirectory The directory that the file must be within
     * @return true if the path is safe, false otherwise
     */
    public static boolean isPathSafe(String filePath, String allowedDirectory) {
        if (filePath == null || allowedDirectory == null) {
            return false;
        }
        
        try {
            Path allowed = Paths.get(allowedDirectory).toAbsolutePath().normalize();
            Path target = Paths.get(filePath).toAbsolutePath().normalize();
            
            return target.startsWith(allowed);
        } catch (Exception e) {
            // If any exception occurs during path resolution, consider it unsafe
            return false;
        }
    }
}
