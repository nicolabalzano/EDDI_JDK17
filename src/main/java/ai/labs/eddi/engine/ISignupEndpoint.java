package ai.labs.eddi.engine;

import ai.labs.eddi.engine.dto.SignupRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Endpoint for user registration
 */
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ISignupEndpoint {
    
    @GET
    @Path("/signup")
    @Produces(MediaType.TEXT_HTML)
    Response getSignupPage();
    
    @POST
    @Path("/signup")
    Response signup(SignupRequest signupRequest);
}
