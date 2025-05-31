package ai.labs.eddi.engine.dto;

/**
 * Request DTO for user signup
 */
public class SignupRequest {
    private String username;
    private String password;
    private String confirmPassword;
    private String email;
    private String csrfToken;
    
    // Default constructor for JSON deserialization
    public SignupRequest() {}
    
    public SignupRequest(String username, String password, String confirmPassword, String email, String csrfToken) {
        this.username = username;
        this.password = password;
        this.confirmPassword = confirmPassword;
        this.email = email;
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
    
    public String getConfirmPassword() {
        return confirmPassword;
    }
      public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getCsrfToken() {
        return csrfToken;
    }
    
    public void setCsrfToken(String csrfToken) {
        this.csrfToken = csrfToken;
    }
      @Override
    public String toString() {
        return "SignupRequest{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", password='[PROTECTED]'" +
                ", confirmPassword='[PROTECTED]'" +
                ", csrfToken='" + (csrfToken != null ? csrfToken.substring(0, Math.min(10, csrfToken.length())) + "..." : "null") + '\'' +
                '}';
    }
}
