package ai.labs.eddi.engine.dto;

/**
 * Request DTO for user login
 */
public class LoginRequest {
    private String username;
    private String password;
    private String csrfToken;
    
    // Default constructor for JSON deserialization
    public LoginRequest() {}
    
    public LoginRequest(String username, String password, String csrfToken) {
        this.username = username;
        this.password = password;
        this.csrfToken = csrfToken;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getCsrfToken() {
        return csrfToken;
    }
    
    public void setCsrfToken(String csrfToken) {
        this.csrfToken = csrfToken;
    }
    
    @Override
    public String toString() {
        return "LoginRequest{" +
                "username='" + username + '\'' +
                ", password='[PROTECTED]'" +
                ", csrfToken='" + (csrfToken != null ? csrfToken.substring(0, Math.min(10, csrfToken.length())) + "..." : "null") + '\'' +
                '}';
    }
}
