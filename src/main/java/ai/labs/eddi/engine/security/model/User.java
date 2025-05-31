package ai.labs.eddi.engine.security.model;

import java.util.Date;

/**
 * User entity for authentication system
 * 
 * @author eddi-system
 */
public class User {
    private String username;
    private String passwordHash;
    private String email;
    private Date createdAt;
    private Date lastLoginAt;
    private boolean active;

    public User() {
        this.active = true;
        this.createdAt = new Date();
    }

    public User(String username, String passwordHash) {
        this();
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public User(String username, String passwordHash, String email) {
        this(username, passwordHash);
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Date lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", active=" + active +
                ", createdAt=" + createdAt +
                ", lastLoginAt=" + lastLoginAt +
                '}';
    }
}
