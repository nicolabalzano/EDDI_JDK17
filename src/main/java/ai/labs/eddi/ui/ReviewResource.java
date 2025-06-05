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
import java.sql.Statement;
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
        try {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            Statement stmt = conn.createStatement();
            String createTableSQL = "CREATE TABLE IF NOT EXISTS reviews (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT, email TEXT, review TEXT)";
            stmt.executeUpdate(createTableSQL);
            stmt.close();
            conn.close();
        } catch (Exception e) {
            // Ignore initialization errors for now
            System.err.println("Failed to initialize database: " + e.getMessage());
        }
    }    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response submitReview(@FormParam("username") String username,
                                 @FormParam("email") String email,
                                 @FormParam("review") String review) {
        // Inserimento non sicuro (vulnerabile a SQL Injection)
        try {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            Statement stmt = conn.createStatement();
            String sql = "INSERT INTO reviews (username, email, review) VALUES ('" + username + "', '" + email + "', '" + review + "')";
            stmt.executeUpdate(sql);
            stmt.close();
            conn.close();
        } catch (Exception e) {
            return Response.serverError().entity("Errore: " + e.getMessage()).build();
        }
        return Response.ok("Recensione inserita!").build();
    }    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, String>> getReviews() {
        List<Map<String, String>> reviews = new ArrayList<>();
        try {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT username, email, review FROM reviews");
            while (rs.next()) {
                Map<String, String> row = new HashMap<>();
                row.put("username", rs.getString("username"));
                row.put("email", rs.getString("email"));
                row.put("review", rs.getString("review"));
                reviews.add(row);
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception ignored) {
            // Ignore query errors
        }
        return reviews;
    }
}
