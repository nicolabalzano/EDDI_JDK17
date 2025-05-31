package ai.labs.eddi.engine.impl;

import ai.labs.eddi.engine.ISignupEndpoint;
import ai.labs.eddi.engine.dto.SignupRequest;
import ai.labs.eddi.engine.security.AuthenticationService;
import ai.labs.eddi.engine.security.CsrfTokenService;
import ai.labs.eddi.engine.security.HtmlSecurityUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static ai.labs.eddi.utils.RuntimeUtilities.getResourceAsStream;

/**
 * Implementation of signup endpoint with CSRF protection
 */
@ApplicationScoped
public class RestSignupEndpoint implements ISignupEndpoint {
    
    private static final Logger log = Logger.getLogger(RestSignupEndpoint.class);
    
    // Username validation pattern: alphanumeric, underscore, dash, 3-20 characters
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,20}$");
    
    // Password validation: at least 6 characters
    private static final int MIN_PASSWORD_LENGTH = 6;
    
    @Inject
    CsrfTokenService csrfTokenService;
    
    @Inject
    AuthenticationService authenticationService;
    
    @Override
    public Response getSignupPage() {
        try {
            // Generate CSRF token for the signup form
            String csrfToken = csrfTokenService.generateToken();
            
            // Load the signup page template
            InputStream signupPageStream = getResourceAsStream("/META-INF/resources/signup.html");
            if (signupPageStream == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("Signup page not found")
                    .build();
            }
            
            // Read the HTML content
            String htmlContent = new String(signupPageStream.readAllBytes(), StandardCharsets.UTF_8);              // Replace the CSRF token placeholder with HTML-escaped value
            String escapedToken = HtmlSecurityUtils.escapeHtml(csrfToken);
            htmlContent = htmlContent.replace("{{CSRF_TOKEN}}", escapedToken);
            
            return Response.ok(htmlContent)
                    .type("text/html")
                    .build();
                    
        } catch (Exception e) {
            log.error("Error generating signup page", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error loading signup page")
                    .build();
        }
    }    @Override
    public Response signup(SignupRequest signupRequest) {
        log.info("=== SIGNUP ATTEMPT START ===");
        log.info("Signup request: " + signupRequest);
        
        if (signupRequest == null) {
            log.warn("Null signup request received");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(createErrorResponse("Invalid request"))
                    .build();
        }
          String username = signupRequest.getUsername();
        String password = signupRequest.getPassword();
        String confirmPassword = signupRequest.getConfirmPassword();
        String email = signupRequest.getEmail();
        String csrfToken = signupRequest.getCsrfToken();
          log.info("Username: " + username);
        log.info("Email: " + email);
        log.info("Password present: " + (password != null && !password.isEmpty()));
        log.info("Confirm password present: " + (confirmPassword != null && !confirmPassword.isEmpty()));
        log.info("CSRF token: " + CsrfTokenService.getTokenLogRepresentation(csrfToken));
        log.info("CSRF token present: " + (csrfToken != null && !csrfToken.trim().isEmpty()));
        
        try {
            // Validate CSRF token
            if (!csrfTokenService.validateToken(csrfToken)) {
                log.warn("CSRF token validation FAILED for user: " + username + ", token: " + CsrfTokenService.getTokenLogRepresentation(csrfToken));
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(createErrorResponse("Invalid CSRF token"))
                        .build();
            }
            
            log.info("CSRF token validation PASSED for user: " + username);
              // Validate input parameters
            String validationError = validateSignupInput(username, password, confirmPassword);
            if (validationError != null) {
                log.warn("Input validation FAILED for user: " + username + " - " + validationError);
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(createErrorResponse(validationError))
                        .build();
            }
            
            log.info("Input validation PASSED for user: " + username);
            
            // Check if user already exists
            if (authenticationService.userExists(username)) {
                log.warn("User already exists: " + username);
                return Response.status(Response.Status.CONFLICT)
                        .entity(createErrorResponse("Username already exists"))
                        .build();
            }
              log.info("Username availability check PASSED for user: " + username);
            
            // Create new user
            authenticationService.addUser(username, password, email);
            
            log.info("New user successfully registered: " + username);
            log.info("=== SIGNUP ATTEMPT SUCCESS ===");
            
            // Return success response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Registration successful");
            response.put("redirectUrl", "/auth/login");
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            log.error("Error during signup process for user: " + username, e);
            log.info("=== SIGNUP ATTEMPT FAILED ===");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createErrorResponse("Internal server error"))
                    .build();
        }
    }
    
    private String validateSignupInput(String username, String password, String confirmPassword) {
        // Check if all fields are provided
        if (username == null || username.trim().isEmpty()) {
            return "Username is required";
        }
        
        if (password == null || password.isEmpty()) {
            return "Password is required";
        }
        
        if (confirmPassword == null || confirmPassword.isEmpty()) {
            return "Password confirmation is required";
        }
        
        // Validate username format
        if (!USERNAME_PATTERN.matcher(username.trim()).matches()) {
            return "Username must be 3-20 characters and contain only letters, numbers, underscore, and dash";
        }
        
        // Validate password length
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long";
        }
        
        // Check password confirmation
        if (!password.equals(confirmPassword)) {
            return "Passwords do not match";
        }
        
        return null; // No validation errors
    }    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}
