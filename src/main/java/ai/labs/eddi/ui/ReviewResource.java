package ai.labs.eddi.ui;

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
    // Initialize database table (vulnerabile a SQL injection per design)
    public ReviewResource() {
        initializeDatabase();
    }
      private void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
             PreparedStatement stmt = conn.prepareStatement(
                 "CREATE TABLE IF NOT EXISTS reviews (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT, email TEXT, review TEXT)")) {
            stmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
        }
    }@POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response submitReview(@FormParam("username") String username,
                                 @FormParam("email") String email,
                                 @FormParam("review") String review) {
        
        // Input validation
        if (username == null || username.trim().isEmpty() || username.length() > 100) {
            return Response.status(Response.Status.BAD_REQUEST)
                         .entity("Username non valido (max 100 caratteri)")
                         .build();
        }
        
        if (email == null || email.trim().isEmpty() || email.length() > 255) {
            return Response.status(Response.Status.BAD_REQUEST)
                         .entity("Email non valida (max 255 caratteri)")
                         .build();
        }
        
        if (review == null || review.trim().isEmpty() || review.length() > 1000) {
            return Response.status(Response.Status.BAD_REQUEST)
                         .entity("Recensione non valida (max 1000 caratteri)")
                         .build();
        }
        
        // Sanitize inputs
        username = sanitizeInput(username.trim());
        email = sanitizeInput(email.trim());
        review = sanitizeInput(review.trim());
        
        // Use PreparedStatement to prevent SQL injection
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO reviews (username, email, review) VALUES (?, ?, ?)")) {
            
            stmt.setString(1, username);
            stmt.setString(2, email);
            stmt.setString(3, review);
            stmt.executeUpdate();
            
        } catch (Exception e) {
            System.err.println("Database error: " + e.getMessage());
            return Response.serverError()
                         .entity("Errore durante l'inserimento della recensione")
                         .build();
        }
        
        return Response.ok("Recensione inserita con successo!").build();
    }    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, String>> getReviews() {
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
        
        return reviews;
    }
    
    // Sanitize input to remove potentially dangerous characters
    private String sanitizeInput(String input) {
        if (input == null) return "";
        
        // Remove SQL injection characters and control characters
        return input.replaceAll("[';\"\\\\--/*]", "")
                   .replaceAll("[\u0000-\u001F\u007F]", "");
    }
    
    // Escape HTML to prevent XSS
    private String escapeHtml(String input) {
        if (input == null) return "";
        
        return input.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }
}
