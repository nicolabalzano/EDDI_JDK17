package ai.labs.eddi.engine.impl;

import ai.labs.eddi.engine.ILogoutEndpoint;
import ai.labs.eddi.engine.security.AuthenticationService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.Map;

/**
 * Implementation of logout endpoint
 */
@ApplicationScoped
public class RestLogoutEndpoint implements ILogoutEndpoint {
    
    private static final Logger log = Logger.getLogger(RestLogoutEndpoint.class);
    
    @Inject
    AuthenticationService authenticationService;
    
    @Context
    ContainerRequestContext requestContext;
      @Override
    public Response isUserAuthenticated() {
        try {
            Map<String, Cookie> cookies = requestContext.getCookies();
            Cookie sessionCookie = cookies.get("EDDI_SESSION");
            
            boolean authenticated = sessionCookie != null && 
                                   authenticationService.isSessionValid(sessionCookie.getValue());
            
            return Response.ok(String.valueOf(authenticated)).build();
        } catch (Exception e) {
            log.error("Error checking authentication status", e);
            return Response.ok("false").build();
        }
    }
    
    @Override
    public Response getSecurityType() {
        // Return the current security configuration
        return Response.ok("FORM_BASED").build();
    }
    
    @Override
    public void logout() {
        try {
            Map<String, Cookie> cookies = requestContext.getCookies();
            Cookie sessionCookie = cookies.get("EDDI_SESSION");
            
            if (sessionCookie != null) {
                String sessionId = sessionCookie.getValue();
                String username = authenticationService.getUsernameFromSession(sessionId);
                authenticationService.invalidateSession(sessionId);
                log.info("User logged out: " + username);
                
                // Create cookie to expire the session
                NewCookie expiredCookie = new NewCookie.Builder("EDDI_SESSION")
                        .value("")
                        .path("/")
                        .maxAge(0) // Expire immediately
                        .build();
                
                // Note: We can't return the cookie from void method
                // The caller should handle cookie deletion in the endpoint
            }
        } catch (Exception e) {
            log.error("Error during logout", e);
        }
    }
}
