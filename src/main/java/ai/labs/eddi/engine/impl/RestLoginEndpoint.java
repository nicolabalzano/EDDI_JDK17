package ai.labs.eddi.engine.impl;

import ai.labs.eddi.engine.ILoginEndpoint;
import ai.labs.eddi.engine.dto.LoginRequest;
import ai.labs.eddi.engine.security.AuthenticationService;
import ai.labs.eddi.engine.security.CsrfTokenService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static ai.labs.eddi.utils.RuntimeUtilities.getResourceAsStream;

/**
 * Implementation of login endpoint with CSRF protection
 */
@ApplicationScoped
public class RestLoginEndpoint implements ILoginEndpoint {
    
    private static final Logger log = Logger.getLogger(RestLoginEndpoint.class);
    
    @Inject
    CsrfTokenService csrfTokenService;
    
    @Inject
    AuthenticationService authenticationService;
      @Override
    public Response getLoginPage() {
        try {
            // Generate CSRF token for the login form
            String csrfToken = csrfTokenService.generateToken();
            
            // Load the login page template
            InputStream loginPageStream = getResourceAsStream("/META-INF/resources/login.html");
            if (loginPageStream == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("Login page not found")
                    .build();
            }
            
            // Read the HTML content
            String htmlContent = new String(loginPageStream.readAllBytes(), StandardCharsets.UTF_8);
            
            // Replace the CSRF token placeholder
            htmlContent = htmlContent.replace("{{CSRF_TOKEN}}", csrfToken);
            
            return Response.ok(htmlContent)
                    .type("text/html")
                    .build();
                    
        } catch (Exception e) {
            log.error("Error generating login page", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error loading login page")
                    .build();
        }
    }    @Override
    public Response login(LoginRequest loginRequest) {
        log.info("=== LOGIN ATTEMPT START ===");
        log.info("Login request: " + loginRequest);
        
        if (loginRequest == null) {
            log.warn("Null login request received");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(createErrorResponse("Invalid request"))
                    .build();
        }
        
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();
        String csrfToken = loginRequest.getCsrfToken();
        
        try {
            // Validate CSRF token
            if (!csrfTokenService.validateToken(csrfToken)) {
                log.warn("Invalid CSRF token for login attempt: " + username);
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(createErrorResponse("Invalid CSRF token"))
                        .build();
            }
            
            // Authenticate user
            if (authenticationService.authenticateUser(username, password)) {
                // Create session for authenticated user
                String sessionId = authenticationService.createSession(username);
                
                log.info("User successfully authenticated: " + username);
                
                // Create session cookie
                NewCookie sessionCookie = new NewCookie.Builder("EDDI_SESSION")
                        .value(sessionId)
                        .path("/")
                        .maxAge(3600) // 1 hour
                        .httpOnly(true)
                        .secure(false) // Set to true in production with HTTPS
                        .sameSite(NewCookie.SameSite.STRICT)
                        .build();
                
                // Return success response with redirect
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Login successful");
                response.put("redirectUrl", "/");
                
                return Response.ok(response)
                        .cookie(sessionCookie)
                        .build();                
            } else {
                log.warn("Authentication failed for user: " + username);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Invalid username or password");
                
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(response)
                        .build();
            }
            
        } catch (Exception e) {
            log.error("Error during login process", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createErrorResponse("Internal server error"))
                    .build();
        }
    }
      @Override
    public Response generateCsrfToken() {
        try {
            String csrfToken = csrfTokenService.generateToken();
            
            Map<String, String> response = new HashMap<>();
            response.put("csrfToken", csrfToken);
              return Response.ok(response).build();
            
        } catch (Exception e) {
            log.error("Error generating CSRF token", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createErrorResponse("Failed to generate CSRF token"))
                    .build();
        }
    }
    
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}
