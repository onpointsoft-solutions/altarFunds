package com.sanctum.auth;

import com.sanctum.api.SanctumApiClient;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;
import java.util.prefs.Preferences;

/**
 * Manages user authentication session and login state persistence
 */
public class SessionManager {
    
    private static final String SESSION_FILE = "sanctum_session.dat";
    private static SessionManager instance;
    private String currentUser;
    private String userRole;
    private long loginTime;
    private boolean rememberMe;
    
    private SessionManager() {
        loadSession();
    }
    
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }
    
    /**
     * Saves the current session to persistent storage
     */
    public void saveSession(String username, String role, boolean rememberMe) {
        this.currentUser = username;
        this.userRole = role;
        this.loginTime = System.currentTimeMillis();
        this.rememberMe = rememberMe;
        
        if (rememberMe) {
            try {
                Path sessionPath = getSessionFilePath();
                try (ObjectOutputStream oos = new ObjectOutputStream(
                        Files.newOutputStream(sessionPath))) {
                    
                    SessionData data = new SessionData(username, role, loginTime, 
                            SanctumApiClient.getAuthToken(), 
                            SanctumApiClient.getRefreshToken());
                    oos.writeObject(data);
                }
            } catch (IOException e) {
                System.err.println("Failed to save session: " + e.getMessage());
            }
        }
    }
    
    /**
     * Loads saved session from persistent storage
     */
    private void loadSession() {
        Path sessionPath = getSessionFilePath();
        if (!Files.exists(sessionPath)) {
            return;
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(
                Files.newInputStream(sessionPath))) {
            
            SessionData data = (SessionData) ois.readObject();
            
            // Check if session is still valid (24 hours)
            long sessionAge = System.currentTimeMillis() - data.getLoginTime();
            if (sessionAge < 24 * 60 * 60 * 1000) { // 24 hours
                this.currentUser = data.getUsername();
                this.userRole = data.getRole();
                this.loginTime = data.getLoginTime();
                this.rememberMe = true;
                
                // Restore API client tokens
                SanctumApiClient.setAuthToken(data.getAuthToken());
                SanctumApiClient.setRefreshToken(data.getRefreshToken());
                
                System.out.println("Session restored for user: " + currentUser);
            } else {
                // Session expired, clean up
                clearSession();
            }
        } catch (Exception e) {
            System.err.println("Failed to load session: " + e.getMessage());
            clearSession();
        }
    }
    
    /**
     * Clears the current session
     */
    public void clearSession() {
        this.currentUser = null;
        this.userRole = null;
        this.loginTime = 0;
        this.rememberMe = false;
        
        // Clear API client tokens
        SanctumApiClient.setAuthToken(null);
        SanctumApiClient.setRefreshToken(null);
        
        // Delete session file
        try {
            Files.deleteIfExists(getSessionFilePath());
        } catch (IOException e) {
            System.err.println("Failed to delete session file: " + e.getMessage());
        }
    }
    
    /**
     * Checks if user is currently logged in
     */
    public boolean isLoggedIn() {
        return currentUser != null && SanctumApiClient.isAuthenticated();
    }
    
    /**
     * Performs login with API and saves session if successful
     */
    public CompletableFuture<Boolean> login(String username, String password, boolean rememberMe) {
        return SanctumApiClient.login(username, password)
            .thenApply(success -> {
                if (success) {
                    // Get actual user role from API response
                    String role = SanctumApiClient.getCurrentUserRole();
                    if (role == null || role.isEmpty()) {
                        // Fallback to role determination if API doesn't provide role
                        role = determineUserRole(username);
                    }
                    saveSession(username, role, rememberMe);
                    return true;
                }
                return false;
            });
    }
    
    /**
     * Determines user role based on username (simplified for demo)
     */
    private String determineUserRole(String username) {
        // In production, this would come from the API response
        if (username.toLowerCase().contains("admin")) {
            return "ADMIN";
        } else if (username.toLowerCase().contains("pastor")) {
            return "PASTOR";
        } else if (username.toLowerCase().contains("treasurer")) {
            return "TREASURER";
        } else if (username.toLowerCase().contains("secretary")) {
            return "SECRETARY";
        } else if (username.toLowerCase().contains("usher")) {
            return "USHER";
        }
        return "MEMBER";
    }
    
    /**
     * Gets the path to the session file
     */
    private Path getSessionFilePath() {
        String userHome = System.getProperty("user.home");
        Path sanctumDir = Paths.get(userHome, ".sanctum");
        
        try {
            if (!Files.exists(sanctumDir)) {
                Files.createDirectories(sanctumDir);
            }
        } catch (IOException e) {
            System.err.println("Failed to create sanctum directory: " + e.getMessage());
        }
        
        return sanctumDir.resolve(SESSION_FILE);
    }
    
    // Getters
    public String getCurrentUser() {
        return currentUser;
    }
    
    public String getUserRole() {
        return userRole;
    }
    
    public long getLoginTime() {
        return loginTime;
    }
    
    public boolean isRememberMe() {
        return rememberMe;
    }
    
    /**
     * Session data holder class for serialization
     */
    private static class SessionData implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private final String username;
        private final String role;
        private final long loginTime;
        private final String authToken;
        private final String refreshToken;
        
        public SessionData(String username, String role, long loginTime, 
                          String authToken, String refreshToken) {
            this.username = username;
            this.role = role;
            this.loginTime = loginTime;
            this.authToken = authToken;
            this.refreshToken = refreshToken;
        }
        
        public String getUsername() { return username; }
        public String getRole() { return role; }
        public long getLoginTime() { return loginTime; }
        public String getAuthToken() { return authToken; }
        public String getRefreshToken() { return refreshToken; }
    }
}
