package ai.labs.eddi.ui;

import ai.labs.eddi.engine.security.CsrfTokenService;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/api/review")
public class ReviewResource {
    // SQLite database path - configurable via environment variable
    @Inject
    @ConfigProperty(name = "sqlite.db.path", defaultValue = "/tmp/reviews.db")
    String dbPath;
    
    // SQLite password - configurable via environment variable
    @Inject
    @ConfigProperty(name = "sqlite.password", defaultValue = "")
    String dbPassword;
    
    @Inject
    CsrfTokenService csrfTokenService;
    // Initialize database table (vulnerabile a SQL injection per design)
    public ReviewResource() {
        initializeDatabase();
    }    private void initializeDatabase() {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "CREATE TABLE IF NOT EXISTS reviews (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT, email TEXT, review TEXT)")) {
            stmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
        }
    }
    
    private Connection getConnection() throws Exception {
        String connectionString = "jdbc:sqlite:" + dbPath;
        
        // Add password to connection string if configured
        if (dbPassword != null && !dbPassword.trim().isEmpty()) {
            connectionString += "?password=" + dbPassword;
        }
        
        return DriverManager.getConnection(connectionString);
    }@POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response submitReview(@FormParam("username") String username,
                                 @FormParam("email") String email,
                                 @FormParam("review") String review,
                                 @FormParam("csrfToken") String csrfToken) {
        // Validazione CSRF
        if (csrfToken == null || !csrfTokenService.validateToken(csrfToken)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("Invalid or missing CSRF token.")
                    .build();
        }
        // Input validation
        if (username == null || username.trim().isEmpty() || username.length() > 100) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid username (max 100 characters)")
                    .build();
        }
        if (email == null || email.trim().isEmpty() || email.length() > 255) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid email (max 255 characters)")
                    .build();
        }
        if (review == null || review.trim().isEmpty() || review.length() > 1000) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid review (max 1000 characters)")
                    .build();
        }
          // Trim inputs
        username = escapeHtml(username.trim());
        email = escapeHtml(email.trim());
        review = escapeHtml(review.trim());
          // Use PreparedStatement to prevent SQL injection
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO reviews (username, email, review) VALUES (?, ?, ?)") ) {
            stmt.setString(1, username);
            stmt.setString(2, email);
            stmt.setString(3, review);
            stmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("Database error: " + e.getMessage());
            return Response.serverError()
                    .entity("Error while inserting review")
                    .build();
        }
        Response.ResponseBuilder responseBuilder = Response.ok("Review submitted successfully!")
                .header("Content-Type", "text/plain; charset=UTF-8")
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; script-src 'none'; connect-src 'self'; img-src 'self'; style-src 'self';");
        return responseBuilder.build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getReviews() {
        List<Map<String, String>> reviews = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
             PreparedStatement stmt = conn.prepareStatement("SELECT username, email, review FROM reviews ORDER BY id DESC");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Map<String, String> row = new HashMap<>();
                row.put("username", escapeHtml(rs.getString("username")));
                row.put("email", escapeHtml(rs.getString("email")));
                row.put("review", escapeHtml(rs.getString("review")));
                reviews.add(row);
            }
        } catch (Exception e) {
            System.err.println("Database error: " + e.getMessage());
            // Return empty list on error instead of exposing error details
        }
        Response.ResponseBuilder responseBuilder = Response.ok(reviews)
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; script-src 'none'; connect-src 'self'; img-src 'self'; style-src 'self';");
        return responseBuilder.build();
    }
    // Escape HTML to prevent XSS
    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;")
                   .replace("javascript:", "&#x6A;&#x61;&#x76;&#x61;&#x73;&#x63;&#x72;&#x69;&#x70;&#x74;:");
    }
}
