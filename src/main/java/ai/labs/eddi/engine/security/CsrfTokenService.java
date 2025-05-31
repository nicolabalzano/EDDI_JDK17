package ai.labs.eddi.engine.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.servlet.http.HttpSession;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Service for managing CSRF tokens
 */
@ApplicationScoped
public class CsrfTokenService {
    
    private static final String CSRF_TOKEN_SESSION_ATTRIBUTE = "csrfToken";
    private static final int TOKEN_LENGTH = 32;
    
    private final SecureRandom secureRandom = new SecureRandom();
    private final ConcurrentMap<String, Long> tokenStore = new ConcurrentHashMap<>();
    private static final long TOKEN_VALIDITY_DURATION = 30 * 60 * 1000; // 30 minutes
    
    public String generateToken() {
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        
        // Store token with timestamp
        tokenStore.put(token, System.currentTimeMillis());
        cleanupExpiredTokens();
        
        return token;
    }
      public boolean validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        
        // Validate token format to prevent XSS attacks
        if (!isValidTokenFormat(token)) {
            return false;
        }
        
        Long timestamp = tokenStore.get(token);
        if (timestamp == null) {
            return false;
        }
        
        // Check if token is still valid
        boolean isValid = (System.currentTimeMillis() - timestamp) < TOKEN_VALIDITY_DURATION;
        
        if (isValid) {
            // Remove token after successful validation (single use)
            tokenStore.remove(token);
        }
        
        return isValid;
    }
    
    public void storeTokenInSession(HttpSession session, String token) {
        if (session != null) {
            session.setAttribute(CSRF_TOKEN_SESSION_ATTRIBUTE, token);
        }
    }
    
    public String getTokenFromSession(HttpSession session) {
        if (session != null) {
            return (String) session.getAttribute(CSRF_TOKEN_SESSION_ATTRIBUTE);
        }
        return null;
    }
      private void cleanupExpiredTokens() {
        long currentTime = System.currentTimeMillis();
        tokenStore.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) > TOKEN_VALIDITY_DURATION);
    }
    
    /**
     * Validates token format to prevent XSS attacks.
     * CSRF tokens should only contain Base64 URL-safe characters.
     * 
     * @param token The token to validate
     * @return true if token format is safe, false otherwise
     */
    private boolean isValidTokenFormat(String token) {
        if (token == null || token.length() == 0) {
            return false;
        }
        
        // Check length bounds (Base64 encoded 32 bytes should be ~43 characters)
        if (token.length() < 20 || token.length() > 100) {
            return false;
        }
        
        // Check for Base64 URL-safe characters only: A-Z, a-z, 0-9, -, _
        // This prevents XSS through malicious characters
        return token.matches("^[A-Za-z0-9_-]+$");
    }
    
    /**
     * Safely logs token information without exposing the full token value.
     * Used to prevent XSS attacks through log injection.
     * 
     * @param token The token to log safely
     * @return A safe representation of the token for logging
     */
    public static String getTokenLogRepresentation(String token) {
        if (token == null) {
            return "[NULL]";
        }
        if (token.trim().isEmpty()) {
            return "[EMPTY]";
        }
        if (token.length() < 6) {
            return "[TOO_SHORT]";
        }
        
        // Show only first 6 characters to prevent log-based XSS
        return "[" + token.substring(0, 6) + "..." + token.length() + "chars]";
    }
}
