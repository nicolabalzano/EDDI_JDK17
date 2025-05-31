package ai.labs.eddi.engine.security;

import ai.labs.eddi.datastore.IResourceStore;
import ai.labs.eddi.engine.security.model.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import ai.labs.eddi.utils.SecurityUtilities;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.UUID;

/**
 * Authentication service with MongoDB backend for user persistence
 * Sessions are still kept in memory for performance
 */
@ApplicationScoped
public class AuthenticationService {
    
    private static final Logger log = Logger.getLogger(AuthenticationService.class);
    
    private final IUserStore userStore;
    
    // In-memory session store for performance - in production consider Redis
    private final ConcurrentMap<String, SessionInfo> sessions = new ConcurrentHashMap<>();
    
    private static class SessionInfo {
        final String username;
        final LocalDateTime createdAt;
        final LocalDateTime lastAccessed;
        
        SessionInfo(String username) {
            this.username = username;
            this.createdAt = LocalDateTime.now();
            this.lastAccessed = LocalDateTime.now();
        }
        
        SessionInfo updateLastAccessed() {
            return new SessionInfo(this.username, this.createdAt, LocalDateTime.now());
        }
        
        SessionInfo(String username, LocalDateTime createdAt, LocalDateTime lastAccessed) {
            this.username = username;
            this.createdAt = createdAt;
            this.lastAccessed = lastAccessed;
        }
        
        boolean isExpired() {
            // Session expires after 1 hour of inactivity
            return lastAccessed.isBefore(LocalDateTime.now().minusHours(1));
        }
    }
    
    @Inject
    public AuthenticationService(IUserStore userStore) {
        this.userStore = userStore;
        initializeDefaultUsers();
    }
      private void initializeDefaultUsers() {
        try {
            // Create default admin user if it doesn't exist
            User adminUser = userStore.findUserByUsername("admin");
            if (adminUser == null) {
                String hashedPassword = SecurityUtilities.hashPassword("admin");
                
                User admin = new User("admin", hashedPassword, "admin@eddi.ai");
                userStore.createUser(admin);
                log.info("Default admin user created");
            }
            
            // Create default user if it doesn't exist
            User demoUser = userStore.findUserByUsername("user");
            if (demoUser == null) {
                String hashedPassword = SecurityUtilities.hashPassword("user");
                
                User user = new User("user", hashedPassword, "user@eddi.ai");
                userStore.createUser(user);
                log.info("Default demo user created");
            }
        } catch (Exception e) {
            log.error("Error initializing default users", e);
        }
    }
      public boolean authenticateUser(String username, String password) {
        if (username == null || password == null) {
            return false;
        }
        
        try {
            User user = userStore.findUserByUsername(username);
            if (user == null || !user.isActive()) {
                return false;
            }
            
            String storedPasswordHash = user.getPasswordHash();
            if (storedPasswordHash == null) {
                return false;
            }
            
            boolean authenticated;
            
            // Check if this is a legacy password format (salt:hash)
            if (storedPasswordHash.contains(":")) {
                // Legacy format - use old method for backward compatibility
                String[] parts = storedPasswordHash.split(":", 2);
                if (parts.length != 2) {
                    return false;
                }
                
                String salt = parts[0];
                String storedHash = parts[1];
                String providedHash = SecurityUtilities.hashPassword(password, salt);
                authenticated = storedHash.equals(providedHash);
                
                // If authentication successful, migrate to BCrypt
                if (authenticated) {
                    try {
                        String newBcryptHash = SecurityUtilities.hashPassword(password);
                        user.setPasswordHash(newBcryptHash);
                        userStore.updateUser(user);
                        log.info("Migrated user password to BCrypt: " + username);
                    } catch (Exception e) {
                        log.warn("Failed to migrate password to BCrypt for user: " + username, e);
                    }
                }
            } else {
                // BCrypt format
                authenticated = SecurityUtilities.verifyPassword(password, storedPasswordHash);
            }
            
            if (authenticated) {
                // Update last login timestamp
                try {
                    userStore.updateLastLogin(username);
                } catch (Exception e) {
                    log.warn("Failed to update last login for user: " + username, e);
                }
            }
            
            return authenticated;
            
        } catch (Exception e) {
            log.error("Error during authentication for user: " + username, e);
            return false;
        }
    }
    
    public void addUser(String username, String password) {
        addUser(username, password, null);
    }
      public void addUser(String username, String password, String email) {
        try {
            String hashedPassword = SecurityUtilities.hashPassword(password);
            
            User user = new User(username, hashedPassword, email);
            userStore.createUser(user);
            log.info("User created successfully: " + username);
            
        } catch (IResourceStore.ResourceAlreadyExistsException e) {
            log.warn("User already exists: " + username);
            throw new RuntimeException("User already exists: " + username, e);
        } catch (Exception e) {
            log.error("Error creating user: " + username, e);
            throw new RuntimeException("Error creating user: " + username, e);
        }
    }
    
    public boolean userExists(String username) {
        try {
            User user = userStore.findUserByUsername(username);
            return user != null;
        } catch (Exception e) {
            log.error("Error checking if user exists: " + username, e);
            return false;
        }
    }
    
    public String createSession(String username) {
        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, new SessionInfo(username));
        cleanupExpiredSessions();
        return sessionId;
    }
    
    public boolean isSessionValid(String sessionId) {
        if (sessionId == null) {
            return false;
        }
        
        SessionInfo session = sessions.get(sessionId);
        if (session == null) {
            return false;
        }
        
        if (session.isExpired()) {
            sessions.remove(sessionId);
            return false;
        }
        
        // Update last accessed time
        sessions.put(sessionId, session.updateLastAccessed());
        return true;
    }
    
    public void invalidateSession(String sessionId) {
        if (sessionId != null) {
            sessions.remove(sessionId);
        }
    }
    
    public String getUsernameFromSession(String sessionId) {
        SessionInfo session = sessions.get(sessionId);
        return session != null ? session.username : null;
    }
    
    private void cleanupExpiredSessions() {
        sessions.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}
