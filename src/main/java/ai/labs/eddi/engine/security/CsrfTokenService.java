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
}
