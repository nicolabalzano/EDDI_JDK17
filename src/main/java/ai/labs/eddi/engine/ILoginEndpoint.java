package ai.labs.eddi.engine;

import ai.labs.eddi.engine.dto.LoginRequest;
import org.eclipse.microprofile.openapi.annotations.Operation;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
public interface ILoginEndpoint {
    
    @GET
    @Path("/login")
    @Produces(MediaType.TEXT_HTML)
    @Operation(description = "Display login page.")
    Response getLoginPage();
      @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Authenticate user with username and password.")
    Response login(LoginRequest loginRequest);
    
    @GET
    @Path("/csrf-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Generate CSRF token for authentication.")
    Response generateCsrfToken();
}
