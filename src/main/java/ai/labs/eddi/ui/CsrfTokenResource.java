package ai.labs.eddi.ui;

import ai.labs.eddi.engine.security.CsrfTokenService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/csrf-token")
public class CsrfTokenResource {
    @Inject
    CsrfTokenService csrfTokenService;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getCsrfToken() {
        String token = csrfTokenService.generateToken();
        return Response.ok(token)
                .header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0")
                .build();
    }
}
