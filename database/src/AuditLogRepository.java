package database;

import java.sql.*;
import java.util.*;

public class AuditLogRepository {
    public void log(String actorId, String actorRole, String actionType, String entityType, String entityId, String details) {
        String q = "INSERT INTO audit_log (actor_id, actor_role, action_type, entity_type, entity_id, details) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(q)) {
            p.setString(1, actorId == null ? "SYSTEM" : actorId);
            p.setString(2, actorRole == null ? "system" : actorRole);
            p.setString(3, actionType);
            p.setString(4, entityType);
            p.setString(5, entityId);
            p.setString(6, details);
            p.executeUpdate();
        } catch (Exception e) {
            System.err.println("Failed to write audit log: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> findRecent(int limit) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        String q = "SELECT id, actor_id, actor_role, action_type, entity_type, entity_id, details, created_at FROM audit_log ORDER BY id DESC LIMIT ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(q)) {
            p.setInt(1, limit <= 0 ? 50 : limit);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getInt("id"));
                    row.put("actor_id", rs.getString("actor_id"));
                    row.put("actor_role", rs.getString("actor_role"));
                    row.put("action_type", rs.getString("action_type"));
                    row.put("entity_type", rs.getString("entity_type"));
                    row.put("entity_id", rs.getString("entity_id"));
                    row.put("details", rs.getString("details"));
                    row.put("created_at", rs.getTimestamp("created_at").toString());
                    result.add(row);
                }
            }
        }
        return result;
    }
}
