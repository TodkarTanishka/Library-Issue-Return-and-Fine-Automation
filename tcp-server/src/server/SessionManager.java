package server;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private static final SecureRandom random = new SecureRandom();
    private static final long SESSION_DURATION_MS = 24 * 60 * 60 * 1000L; // 24 hours

    public static class SessionInfo {
        private final String token;
        private final String userId;
        private final String email;
        private final String role;
        private final long createdAt;
        private final long expiresAt;

        public SessionInfo(String token, String userId, String email, String role) {
            this.token = token;
            this.userId = userId;
            this.email = email;
            this.role = role;
            this.createdAt = System.currentTimeMillis();
            this.expiresAt = this.createdAt + SESSION_DURATION_MS;
        }

        public String getToken() { return token; }
        public String getUserId() { return userId; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
        public boolean isExpired() { return System.currentTimeMillis() > expiresAt; }
    }

    private final ConcurrentHashMap<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    public SessionInfo createSession(String userId, String email, String role) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        SessionInfo info = new SessionInfo(token, userId, email, role);
        sessions.put(token, info);
        return info;
    }

    public SessionInfo getSession(String token) {
        if (token == null || token.isBlank()) return null;
        SessionInfo info = sessions.get(token.trim());
        if (info == null) return null;
        if (info.isExpired()) {
            sessions.remove(token);
            return null;
        }
        return info;
    }

    public boolean invalidateSession(String token) {
        if (token == null) return false;
        return sessions.remove(token) != null;
    }

    public void cleanupExpired() {
        sessions.entrySet().removeIf(e -> e.getValue().isExpired());
    }

    public int getActiveSessionCount() {
        cleanupExpired();
        return sessions.size();
    }
}
