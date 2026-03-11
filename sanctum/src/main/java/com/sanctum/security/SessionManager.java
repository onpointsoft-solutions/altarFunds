package com.sanctum.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Session manager for JWT tokens and user sessions
 */
public class SessionManager {
    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);
    
    // In-memory session storage
    private static final Map<String, UserSession> activeSessions = new ConcurrentHashMap<>();
    
    // Current user session (for desktop application)
    private static UserSession currentUserSession;
    
    /**
     * Create and store user session
     */
    public static void createSession(String token, String username, String role, Integer churchId) {
        UserSession session = new UserSession(token, username, role, churchId);
        activeSessions.put(token, session);
        currentUserSession = session;
        
        logger.info("Session created for user: {} with role: {}", username, role);
    }
    
    /**
     * Get current user session
     */
    public static UserSession getCurrentSession() {
        return currentUserSession;
    }
    
    /**
     * Get session by token
     */
    public static UserSession getSession(String token) {
        return activeSessions.get(token);
    }
    
    /**
     * Validate session
     */
    public static boolean isSessionValid(String token) {
        UserSession session = activeSessions.get(token);
        if (session == null) {
            return false;
        }
        
        // Check if token is still valid
        if (JwtUtil.isTokenExpired(token)) {
            invalidateSession(token);
            return false;
        }
        
        return true;
    }
    
    /**
     * Invalidate session
     */
    public static void invalidateSession(String token) {
        UserSession session = activeSessions.remove(token);
        if (session != null) {
            logger.info("Session invalidated for user: {}", session.getUsername());
        }
        
        if (currentUserSession != null && token.equals(currentUserSession.getToken())) {
            currentUserSession = null;
        }
    }
    
    /**
     * Invalidate all sessions for user
     */
    public static void invalidateUserSessions(String username) {
        activeSessions.entrySet().removeIf(entry -> {
            UserSession session = entry.getValue();
            if (session.getUsername().equals(username)) {
                logger.info("Session invalidated for user: {}", username);
                if (currentUserSession != null && currentUserSession.getUsername().equals(username)) {
                    currentUserSession = null;
                }
                return true;
            }
            return false;
        });
    }
    
    /**
     * Refresh session token
     */
    public static String refreshSession(String oldToken) {
        UserSession session = activeSessions.get(oldToken);
        if (session == null) {
            return null;
        }
        
        String newToken = JwtUtil.refreshToken(oldToken);
        if (newToken != null) {
            // Remove old session and create new one
            activeSessions.remove(oldToken);
            createSession(newToken, session.getUsername(), session.getRole(), session.getChurchId());
            return newToken;
        }
        
        return null;
    }
    
    /**
     * Check if user has required role
     */
    public static boolean hasRole(String token, String requiredRole) {
        UserSession session = activeSessions.get(token);
        if (session == null) {
            return false;
        }
        
        return hasRoleHierarchy(session.getRole(), requiredRole);
    }
    
    /**
     * Check if current user has required role
     */
    public static boolean currentUserHasRole(String requiredRole) {
        if (currentUserSession == null) {
            return false;
        }
        
        return hasRoleHierarchy(currentUserSession.getRole(), requiredRole);
    }
    
    /**
     * Check role hierarchy
     */
    private static boolean hasRoleHierarchy(String userRole, String requiredRole) {
        Map<String, Integer> roleHierarchy = new HashMap<>();
        roleHierarchy.put("MEMBER", 1);
        roleHierarchy.put("SECRETARY", 2);
        roleHierarchy.put("TREASURER", 3);
        roleHierarchy.put("PASTOR", 4);
        roleHierarchy.put("CHURCH_ADMIN", 5);
        roleHierarchy.put("SUPER_ADMIN", 6);
        
        Integer userLevel = roleHierarchy.get(userRole);
        Integer requiredLevel = roleHierarchy.get(requiredRole);
        
        return userLevel != null && requiredLevel != null && userLevel >= requiredLevel;
    }
    
    /**
     * Get active session count
     */
    public static int getActiveSessionCount() {
        return activeSessions.size();
    }
    
    /**
     * Clean up expired sessions
     */
    public static void cleanupExpiredSessions() {
        activeSessions.entrySet().removeIf(entry -> {
            String token = entry.getKey();
            if (JwtUtil.isTokenExpired(token)) {
                logger.info("Expired session removed for user: {}", entry.getValue().getUsername());
                return true;
            }
            return false;
        });
    }
    
    /**
     * User session class
     */
    public static class UserSession {
        private final String token;
        private final String username;
        private final String role;
        private final Integer churchId;
        private final LocalDateTime createdAt;
        
        public UserSession(String token, String username, String role, Integer churchId) {
            this.token = token;
            this.username = username;
            this.role = role;
            this.churchId = churchId;
            this.createdAt = LocalDateTime.now();
        }
        
        public String getToken() { return token; }
        public String getUsername() { return username; }
        public String getRole() { return role; }
        public Integer getChurchId() { return churchId; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        
        public boolean isExpired() {
            return JwtUtil.isTokenExpired(token);
        }
        
        public long getTimeUntilExpiration() {
            return JwtUtil.getTimeUntilExpiration(token);
        }
    }
}
