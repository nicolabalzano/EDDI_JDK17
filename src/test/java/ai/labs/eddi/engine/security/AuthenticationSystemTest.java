package ai.labs.eddi.engine.security;

import ai.labs.eddi.datastore.IResourceStore;
import ai.labs.eddi.engine.security.model.User;
import ai.labs.eddi.utils.SecurityUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for authentication system components
 */
class AuthenticationSystemTest {

    private CsrfTokenService csrfTokenService;
    private AuthenticationService authenticationService;
    private TestUserStore testUserStore;
    
    @BeforeEach
    void setUp() {
        csrfTokenService = new CsrfTokenService();
        testUserStore = new TestUserStore();
        authenticationService = new AuthenticationService(testUserStore);
    }
    
    /**
     * Test implementation of IUserStore for testing purposes
     */
    private static class TestUserStore implements IUserStore {
        private final ConcurrentMap<String, User> users = new ConcurrentHashMap<>();
          public TestUserStore() {
            // Initialize with test users using BCrypt
            try {
                String adminHashedPassword = SecurityUtilities.hashPassword("admin");
                User admin = new User("admin", adminHashedPassword, "admin@test.com");
                users.put("admin", admin);
                
                String userHashedPassword = SecurityUtilities.hashPassword("user");
                User user = new User("user", userHashedPassword, "user@test.com");
                users.put("user", user);
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize test users", e);
            }
        }
        
        @Override
        public User findUserByUsername(String username) throws IResourceStore.ResourceStoreException {
            return users.get(username);
        }
        
        @Override
        public void createUser(User user) throws IResourceStore.ResourceAlreadyExistsException, IResourceStore.ResourceStoreException {
            if (users.containsKey(user.getUsername())) {
                throw new IResourceStore.ResourceAlreadyExistsException("User already exists: " + user.getUsername());
            }
            users.put(user.getUsername(), user);
        }
        
        @Override
        public void updateUser(User user) throws IResourceStore.ResourceStoreException, IResourceStore.ResourceNotFoundException {
            if (!users.containsKey(user.getUsername())) {
                throw new IResourceStore.ResourceNotFoundException("User not found: " + user.getUsername());
            }
            users.put(user.getUsername(), user);
        }
        
        @Override
        public void deleteUser(String username) throws IResourceStore.ResourceStoreException {
            users.remove(username);
        }
        
        @Override
        public void updateLastLogin(String username) throws IResourceStore.ResourceStoreException, IResourceStore.ResourceNotFoundException {
            User user = users.get(username);
            if (user == null) {
                throw new IResourceStore.ResourceNotFoundException("User not found: " + username);
            }
            user.setLastLoginAt(new Date());
        }
    }

    @Test
    void testCsrfTokenGeneration() {
        String token1 = csrfTokenService.generateToken();
        String token2 = csrfTokenService.generateToken();
        
        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);
        assertTrue(token1.length() > 0);
        assertTrue(token2.length() > 0);
    }

    @Test
    void testCsrfTokenValidation() {
        String token = csrfTokenService.generateToken();
        
        // Valid token should pass validation
        assertTrue(csrfTokenService.validateToken(token));
        
        // Same token should fail on second validation (single use)
        assertFalse(csrfTokenService.validateToken(token));
        
        // Invalid token should fail
        assertFalse(csrfTokenService.validateToken("invalid-token"));
        
        // Null token should fail
        assertFalse(csrfTokenService.validateToken(null));
        
        // Empty token should fail
        assertFalse(csrfTokenService.validateToken(""));
    }

    @Test
    void testAuthenticationService() {
        // Test valid credentials
        assertTrue(authenticationService.authenticateUser("admin", "admin"));
        assertTrue(authenticationService.authenticateUser("user", "user"));
        
        // Test invalid credentials
        assertFalse(authenticationService.authenticateUser("admin", "wrong"));
        assertFalse(authenticationService.authenticateUser("wrong", "admin"));
        assertFalse(authenticationService.authenticateUser("nonexistent", "password"));
        
        // Test null credentials
        assertFalse(authenticationService.authenticateUser(null, "password"));
        assertFalse(authenticationService.authenticateUser("admin", null));
        assertFalse(authenticationService.authenticateUser(null, null));
    }    @Test
    void testAddUser() {
        String testUsername = "testuser";
        String testPassword = "testpass";
        String testEmail = "test@example.com";
        
        // Initially user shouldn't exist
        assertFalse(authenticationService.authenticateUser(testUsername, testPassword));
        assertFalse(authenticationService.userExists(testUsername));
        
        // Add user with email
        authenticationService.addUser(testUsername, testPassword, testEmail);
        
        // Now user should exist
        assertTrue(authenticationService.userExists(testUsername));
        
        // Authentication should work
        assertTrue(authenticationService.authenticateUser(testUsername, testPassword));
        
        // Wrong password should still fail
        assertFalse(authenticationService.authenticateUser(testUsername, "wrongpass"));
        
        // Verify user was stored correctly
        try {
            User user = testUserStore.findUserByUsername(testUsername);
            assertNotNull(user);
            assertEquals(testUsername, user.getUsername());
            assertEquals(testEmail, user.getEmail());
            assertTrue(user.isActive());
            assertNotNull(user.getCreatedAt());
        } catch (Exception e) {
            fail("Failed to retrieve user: " + e.getMessage());
        }
    }
    
    @Test
    void testSessionManagement() {
        String username = "admin";
        
        // Create session
        String sessionId = authenticationService.createSession(username);
        assertNotNull(sessionId);
        assertFalse(sessionId.isEmpty());
        
        // Session should be valid
        assertTrue(authenticationService.isSessionValid(sessionId));
        
        // Should return correct username
        assertEquals(username, authenticationService.getUsernameFromSession(sessionId));
        
        // Invalid session should return false
        assertFalse(authenticationService.isSessionValid("invalid-session"));
        assertNull(authenticationService.getUsernameFromSession("invalid-session"));
        
        // Invalidate session
        authenticationService.invalidateSession(sessionId);
        assertFalse(authenticationService.isSessionValid(sessionId));
        assertNull(authenticationService.getUsernameFromSession(sessionId));
    }
    
    @Test
    void testUserExistsCheck() {
        // Test existing users
        assertTrue(authenticationService.userExists("admin"));
        assertTrue(authenticationService.userExists("user"));
        
        // Test non-existing user
        assertFalse(authenticationService.userExists("nonexistent"));
        
        // Test null username
        assertFalse(authenticationService.userExists(null));
    }
    
    @Test
    void testDuplicateUserCreation() {
        String testUsername = "duplicatetest";
        String testPassword = "testpass";
        
        // First creation should succeed
        authenticationService.addUser(testUsername, testPassword);
        assertTrue(authenticationService.userExists(testUsername));
        
        // Second creation should fail
        assertThrows(RuntimeException.class, () -> {
            authenticationService.addUser(testUsername, testPassword);
        });
    }
}
