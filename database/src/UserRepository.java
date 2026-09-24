package database;

import model.User;
import java.sql.*;
import java.util.*;

public class UserRepository {
    public List<User> findAll() throws SQLException {
        List<User> r = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT id,user_code,full_name,email,password,department,division,designation,role FROM users")) {
            while (rs.next()) {
                r.add(new User(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9)));
            }
        }
        return r;
    }

    public User findByEmail(String email) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT id,user_code,full_name,email,password,department,division,designation,role FROM users WHERE email=?")) {
            p.setString(1, email);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) return new User(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9));
            }
        }
        return null;
    }

    public User findStaff(String email, String password, String role) throws SQLException {
        String sql = "SELECT id, full_name, email, password, role FROM librarian WHERE (id=? OR email=?) AND role=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, email);
            p.setString(2, email);
            p.setString(3, role);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) {
                    String stored = rs.getString(4);
                    if (security.PasswordUtil.verifyPassword(password, stored)) {
                        if (!stored.startsWith("$pbkdf2$")) {
                            updateLibrarianPassword(rs.getString(1), security.PasswordUtil.hashPassword(password));
                        }
                        return new User(0, rs.getString(1), rs.getString(2), rs.getString(3), stored, "Central Library", "", "", rs.getString(5));
                    }
                }
            }
        }
        return null;
    }

    public User findLibrarianByEmail(String email) throws SQLException {
        String sql = "SELECT id, full_name, email, password, role FROM librarian WHERE email=? OR id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, email);
            p.setString(2, email);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) return new User(0, rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), "Central Library", "", "", rs.getString(5));
            }
        }
        return null;
    }

    public User findAdminByEmail(String email) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT id, full_name, email, password, role FROM admins WHERE email=? OR id=?")) {
            p.setString(1, email);
            p.setString(2, email);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) return new User(0, rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), "Administration", "", "", rs.getString(5));
            }
        }
        return null;
    }

    public User findAdmin(String id, String password) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT id, full_name, email, password, role FROM admins WHERE id=? OR email=?")) {
            p.setString(1, id);
            p.setString(2, id);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) {
                    String stored = rs.getString(4);
                    if (security.PasswordUtil.verifyPassword(password, stored)) {
                        if (!stored.startsWith("$pbkdf2$")) {
                            updateAdminPassword(rs.getString(1), security.PasswordUtil.hashPassword(password));
                        }
                        return new User(0, rs.getString(1), rs.getString(2), rs.getString(3), stored, "Administration", "", "", rs.getString(5));
                    }
                }
            }
        }
        return null;
    }

    public List<Map<String, Object>> findAllDirectory() throws SQLException {
        List<Map<String, Object>> out = new ArrayList<>();
        String userSql = "SELECT user_code, full_name, email, department, division, designation, role FROM users ORDER BY role, full_name";
        try (Connection c = DBConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(userSql)) {
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", rs.getString(1)); m.put("name", rs.getString(2)); m.put("email", rs.getString(3));
                m.put("department", rs.getString(4)); m.put("division", rs.getString(5)); m.put("designation", rs.getString(6)); m.put("role", rs.getString(7));
                out.add(m);
            }
        }
        String adminSql = "SELECT id, full_name, email, role FROM admins ORDER BY full_name";
        try (Connection c = DBConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(adminSql)) {
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", rs.getString(1)); m.put("name", rs.getString(2)); m.put("email", rs.getString(3));
                m.put("department", "Administration"); m.put("division", ""); m.put("designation", "System Administrator"); m.put("role", rs.getString(4));
                out.add(m);
            }
        }
        String libSql = "SELECT id, full_name, email, role FROM librarian ORDER BY full_name";
        try (Connection c = DBConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(libSql)) {
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", rs.getString(1)); m.put("name", rs.getString(2)); m.put("email", rs.getString(3));
                m.put("department", "Central Library"); m.put("division", ""); m.put("designation", "Librarian"); m.put("role", rs.getString(4));
                out.add(m);
            }
        }
        return out;
    }

    public void updateUserPassword(String email, String hashed) {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement("UPDATE users SET password=? WHERE email=? OR user_code=?")) {
            p.setString(1, hashed); p.setString(2, email); p.setString(3, email); p.executeUpdate();
        } catch (Exception ignored) {}
    }

    public void updateLibrarianPassword(String id, String hashed) {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement("UPDATE librarian SET password=? WHERE id=? OR email=?")) {
            p.setString(1, hashed); p.setString(2, id); p.setString(3, id); p.executeUpdate();
        } catch (Exception ignored) {}
    }

    public void updateAdminPassword(String id, String hashed) {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement("UPDATE admins SET password=? WHERE id=? OR email=?")) {
            p.setString(1, hashed); p.setString(2, id); p.setString(3, id); p.executeUpdate();
        } catch (Exception ignored) {}
    }

    public void migratePlaintextPasswords() {
        // 1. Users table migration
        try (Connection c = DBConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT id, email, password FROM users")) {
            List<Map<String, String>> toUpdate = new ArrayList<>();
            while (rs.next()) {
                String pwd = rs.getString(3);
                if (pwd != null && !pwd.startsWith("$pbkdf2$")) {
                    Map<String, String> item = new HashMap<>();
                    item.put("id", String.valueOf(rs.getInt(1)));
                    item.put("pwd", pwd);
                    toUpdate.add(item);
                }
            }
            for (Map<String, String> item : toUpdate) {
                String hashed = security.PasswordUtil.hashPassword(item.get("pwd"));
                try (PreparedStatement p = c.prepareStatement("UPDATE users SET password=? WHERE id=?")) {
                    p.setString(1, hashed);
                    p.setInt(2, Integer.parseInt(item.get("id")));
                    p.executeUpdate();
                }
            }
        } catch (Exception e) {
            System.err.println("User password migration error: " + e.getMessage());
        }

        // 2. Admins table migration
        try (Connection c = DBConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT id, password FROM admins")) {
            List<Map<String, String>> toUpdate = new ArrayList<>();
            while (rs.next()) {
                String pwd = rs.getString(2);
                if (pwd != null && !pwd.startsWith("$pbkdf2$")) {
                    Map<String, String> item = new HashMap<>();
                    item.put("id", rs.getString(1));
                    item.put("pwd", pwd);
                    toUpdate.add(item);
                }
            }
            for (Map<String, String> item : toUpdate) {
                String hashed = security.PasswordUtil.hashPassword(item.get("pwd"));
                updateAdminPassword(item.get("id"), hashed);
            }
        } catch (Exception e) {
            System.err.println("Admin password migration error: " + e.getMessage());
        }

        // 3. Librarian table migration
        try (Connection c = DBConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT id, password FROM librarian")) {
            List<Map<String, String>> toUpdate = new ArrayList<>();
            while (rs.next()) {
                String pwd = rs.getString(2);
                if (pwd != null && !pwd.startsWith("$pbkdf2$")) {
                    Map<String, String> item = new HashMap<>();
                    item.put("id", rs.getString(1));
                    item.put("pwd", pwd);
                    toUpdate.add(item);
                }
            }
            for (Map<String, String> item : toUpdate) {
                String hashed = security.PasswordUtil.hashPassword(item.get("pwd"));
                updateLibrarianPassword(item.get("id"), hashed);
            }
        } catch (Exception e) {
            System.err.println("Librarian password migration error: " + e.getMessage());
        }
    }
}
