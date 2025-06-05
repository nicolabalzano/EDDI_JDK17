package ai.labs.eddi.ui;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/api/review")
public class ReviewResource {
    // Connessione e creazione tabella (non sicuro, solo per test SQLi)
    static {
        try {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:reviews.db");
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS reviews (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT, email TEXT, review TEXT)");
            stmt.close();
            conn.close();
        } catch (Exception ignored) {}
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response submitReview(@FormParam("username") String username,
                                 @FormParam("email") String email,
                                 @FormParam("review") String review) {
        // Inserimento non sicuro (vulnerabile a SQL Injection)
        try {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:reviews.db");
            Statement stmt = conn.createStatement();
            String sql = "INSERT INTO reviews (username, email, review) VALUES ('" + username + "', '" + email + "', '" + review + "')";
            stmt.executeUpdate(sql);
            stmt.close();
            conn.close();
        } catch (Exception e) {
            return Response.serverError().entity("Errore: " + e.getMessage()).build();
        }
        return Response.ok("Recensione inserita!").build();
    }

    @jakarta.ws.rs.GET
    @jakarta.ws.rs.Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
    public List<Map<String, String>> getReviews() {
        List<Map<String, String>> reviews = new ArrayList<>();
        try {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:reviews.db");
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
        } catch (Exception ignored) {}
        return reviews;
    }
}
