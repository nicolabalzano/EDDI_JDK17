package ai.labs.eddi.utils;

import org.apache.commons.codec.digest.DigestUtils;
import org.mindrot.jbcrypt.BCrypt;

import javax.security.auth.Subject;
import java.security.Principal;
import java.security.SecureRandom;
import java.util.Set;

/**
 * @author ginccc
 */
public class SecurityUtilities {
    
    /**
     * Hash a password using BCrypt with automatic salt generation.
     * BCrypt automatically generates a unique salt for each password.
     * 
     * @param password The plain text password to hash
     * @return The BCrypt hash containing salt and hashed password
     */
    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    /**
     * Verify a password against a BCrypt hash.
     * BCrypt automatically extracts the salt from the stored hash.
     * 
     * @param password The plain text password to verify
     * @param hashedPassword The stored BCrypt hash
     * @return true if the password matches, false otherwise
     */
    public static boolean verifyPassword(String password, String hashedPassword) {
        try {
            return BCrypt.checkpw(password, hashedPassword);
        } catch (Exception e) {
            return false;
        }
    }

    public static Principal getPrincipal(Subject subject) {
        if (subject == null) {
            return null;
        }

        Set<Principal> principals = subject.getPrincipals();
        if (principals != null && !principals.isEmpty()) {
            return principals.toArray(new Principal[principals.size()])[0];
        } else {
            return null;
        }
    }
}
