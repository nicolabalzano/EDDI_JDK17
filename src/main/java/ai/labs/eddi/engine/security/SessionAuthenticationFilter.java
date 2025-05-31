package ai.labs.eddi.engine.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Security filter to validate authenticated sessions
 * Only applies when authorization is enabled
 */
@Provider
@PreMatching
@ApplicationScoped
public class SessionAuthenticationFilter implements ContainerRequestFilter {
    
    private static final Logger log = Logger.getLogger(SessionAuthenticationFilter.class);
    
    @Inject
    AuthenticationService authenticationService;
    
    @ConfigProperty(name = "authorization.enabled", defaultValue = "false")
    boolean authorizationEnabled;
      // Paths that don't require authentication
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
        "/auth/login",
        "/auth/signup",
        "/auth/logout", 
        "/auth/csrf-token",
        "/q/metrics",
        "/q/health",
        "/chat/unrestricted",
        "/bots/unrestricted",
        "/managedbots",
        "/css",
        "/js",
        "/img",
        "/openapi",
        "/q/swagger-ui"
    );
    
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // Skip filter if authorization is disabled
        if (!authorizationEnabled) {
            return;
        }
        
        String path = requestContext.getUriInfo().getPath();
        String method = requestContext.getMethod();
        
        // Allow OPTIONS requests (CORS preflight)
        if ("OPTIONS".equals(method)) {
            return;
        }
        
        // Check if path is public
        if (isPublicPath(path)) {
            return;
        }
          // Validate session for protected paths
        if (!isAuthenticated(requestContext)) {
            log.warn("Unauthorized access attempt to: " + path);
            
            // Return 401 Unauthorized with redirect to login
            Response response = Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\": \"Authentication required\", \"redirectUrl\": \"/auth/login\"}")
                    .type("application/json")
                    .build();
            
            requestContext.abortWith(response);
        }
    }
    
    private boolean isPublicPath(String path) {
        // Normalize path
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        
        // Check if path starts with any public path
        for (String publicPath : PUBLIC_PATHS) {
            if (publicPath.startsWith("/")) {
                publicPath = publicPath.substring(1);
            }
            
            if (path.startsWith(publicPath)) {
                return true;
            }
        }
        
        return false;
    }
      private boolean isAuthenticated(ContainerRequestContext requestContext) {
        try {
            // Get session cookie
            Map<String, Cookie> cookies = requestContext.getCookies();
            Cookie sessionCookie = cookies.get("EDDI_SESSION");
            
            if (sessionCookie == null) {
                log.debug("No session cookie found");
                return false;
            }
            
            String sessionId = sessionCookie.getValue();
            if (sessionId == null || sessionId.trim().isEmpty()) {
                log.debug("Empty session ID");
                return false;
            }
              // Validate session with authentication service
            boolean isValid = authenticationService.isSessionValid(sessionId);
            log.debug("Session validation result for sessionId " + sessionId + ": " + isValid);
            
            return isValid;
            
        } catch (Exception e) {
            log.error("Error checking authentication status", e);
            return false;
        }
    }
}
