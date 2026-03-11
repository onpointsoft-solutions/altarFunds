package com.sanctum.service;

import com.sanctum.model.User;
import com.sanctum.repository.UserRepository;
import com.sanctum.security.JwtUtil;
import com.sanctum.security.PasswordUtil;
import com.sanctum.security.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * Authentication service for login, logout, and user management
 */
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 30;
    
    private final UserRepository userRepository;
    
    public AuthService() {
        this.userRepository = new UserRepository();
    }
    
    /**
     * Authenticate user and create session
     */
    public AuthResult authenticate(String username, String password) {
        try {
            // Find user by username
            User user = userRepository.findByUsername(username);
            if (user == null) {
                logger.warn("Login attempt with non-existent username: {}", username);
                return AuthResult.failure("Invalid username or password");
            }
            
            // Check if account is locked
            if (user.isLocked()) {
                logger.warn("Login attempt on locked account: {}", username);
                return AuthResult.failure("Account is locked. Please try again later.");
            }
            
            // Check if user is active
            if (!user.isActive()) {
                logger.warn("Login attempt on inactive account: {}", username);
                return AuthResult.failure("Account is inactive. Please contact administrator.");
            }
            
            // Verify password
            if (!PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
                // Increment failed attempts
                userRepository.incrementFailedAttempts(user.getId());
                
                // Check if account should be locked
                if (user.getFailedAttempts() + 1 >= MAX_LOGIN_ATTEMPTS) {
                    userRepository.lockAccount(user.getId(), LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
                    logger.warn("Account locked due to multiple failed attempts: {}", username);
                    return AuthResult.failure("Account locked due to multiple failed attempts. Please try again in 30 minutes.");
                }
                
                logger.warn("Failed login attempt for username: {}", username);
                return AuthResult.failure("Invalid username or password");
            }
            
            // Reset failed attempts on successful login
            userRepository.resetFailedAttempts(user.getId());
            
            // Generate JWT token
            String token = JwtUtil.generateToken(
                String.valueOf(user.getId()), 
                user.getRole(), 
                user.getChurchId()
            );
            
            // Create session
            SessionManager.createSession(token, username, user.getRole(), user.getChurchId());
            
            logger.info("User authenticated successfully: {}", username);
            return AuthResult.success(token, user);
            
        } catch (SQLException e) {
            logger.error("Database error during authentication", e);
            return AuthResult.failure("Authentication service unavailable");
        }
    }
    
    /**
     * Logout user and invalidate session
     */
    public boolean logout(String token) {
        try {
            SessionManager.invalidateSession(token);
            logger.info("User logged out successfully");
            return true;
        } catch (Exception e) {
            logger.error("Error during logout", e);
            return false;
        }
    }
    
    /**
     * Change user password
     */
    public PasswordChangeResult changePassword(String token, String currentPassword, String newPassword) {
        try {
            // Validate current session
            if (!SessionManager.isSessionValid(token)) {
                return PasswordChangeResult.failure("Session expired. Please login again.");
            }
            
            // Get current user
            SessionManager.UserSession session = SessionManager.getSession(token);
            User user = userRepository.findById(Integer.parseInt(session.getUsername()));
            
            if (user == null) {
                return PasswordChangeResult.failure("User not found");
            }
            
            // Verify current password
            if (!PasswordUtil.verifyPassword(currentPassword, user.getPasswordHash())) {
                return PasswordChangeResult.failure("Current password is incorrect");
            }
            
            // Validate new password
            if (!PasswordUtil.isPasswordValid(newPassword)) {
                return PasswordChangeResult.failure("New password does not meet requirements");
            }
            
            // Hash new password
            String newPasswordHash = PasswordUtil.hashPassword(newPassword);
            
            // Update password
            userRepository.updatePassword(user.getId(), newPasswordHash);
            
            // Invalidate all sessions for this user (force re-login)
            SessionManager.invalidateUserSessions(user.getUsername());
            
            logger.info("Password changed successfully for user: {}", user.getUsername());
            return PasswordChangeResult.success();
            
        } catch (SQLException e) {
            logger.error("Database error during password change", e);
            return PasswordChangeResult.failure("Password change failed. Please try again.");
        }
    }
    
    /**
     * Create new user (for registration - no token required)
     */
    public UserCreationResult createUserForRegistration(User newUser, String password) {
        try {
            // Validate password
            if (!PasswordUtil.isPasswordValid(password)) {
                return UserCreationResult.failure("Password does not meet requirements");
            }
            
            // Check if username already exists
            if (userRepository.findByUsername(newUser.getUsername()) != null) {
                return UserCreationResult.failure("Username already exists");
            }
            
            // Hash password
            String passwordHash = PasswordUtil.hashPassword(password);
            newUser.setPasswordHash(passwordHash);
            
            // Create user
            User createdUser = userRepository.create(newUser);
            
            logger.info("User created successfully for registration: " + createdUser.getUsername());
            return UserCreationResult.success(createdUser);
            
        } catch (SQLException e) {
            logger.error("Database error during user creation", e);
            return UserCreationResult.failure("User creation failed");
        }
    }
    
    /**
     * Create new user (requires authentication token)
     */
    public UserCreationResult createUser(String token, User newUser, String password) {
        try {
            // Validate session and permissions
            if (!SessionManager.isSessionValid(token)) {
                return UserCreationResult.failure("Session expired");
            }
            
            SessionManager.UserSession session = SessionManager.getSession(token);
            
            // Check permissions (only admin roles can create users)
            if (!SessionManager.hasRole(token, "CHURCH_ADMIN") && !session.getRole().equals("SUPER_ADMIN")) {
                return UserCreationResult.failure("Insufficient permissions to create users");
            }
            
            // Validate password
            if (!PasswordUtil.isPasswordValid(password)) {
                return UserCreationResult.failure("Password does not meet requirements");
            }
            
            // Check if username already exists
            if (userRepository.findByUsername(newUser.getUsername()) != null) {
                return UserCreationResult.failure("Username already exists");
            }
            
            // Hash password
            String passwordHash = PasswordUtil.hashPassword(password);
            newUser.setPasswordHash(passwordHash);
            
            // Set church ID from session
            newUser.setChurchId(session.getChurchId());
            
            // Create user
            User createdUser = userRepository.create(newUser);
            
            logger.info("User created successfully: {}", createdUser.getUsername());
            return UserCreationResult.success(createdUser);
            
        } catch (SQLException e) {
            logger.error("Database error during user creation", e);
            return UserCreationResult.failure("User creation failed");
        }
    }
    
    /**
     * Authentication result class
     */
    public static class AuthResult {
        private final boolean success;
        private final String token;
        private final User user;
        private final String message;
        
        private AuthResult(boolean success, String token, User user, String message) {
            this.success = success;
            this.token = token;
            this.user = user;
            this.message = message;
        }
        
        public static AuthResult success(String token, User user) {
            return new AuthResult(true, token, user, null);
        }
        
        public static AuthResult failure(String message) {
            return new AuthResult(false, null, null, message);
        }
        
        public boolean isSuccess() { return success; }
        public String getToken() { return token; }
        public User getUser() { return user; }
        public String getMessage() { return message; }
    }
    
    /**
     * Password change result class
     */
    public static class PasswordChangeResult {
        private final boolean success;
        private final String message;
        
        private PasswordChangeResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public static PasswordChangeResult success() {
            return new PasswordChangeResult(true, "Password changed successfully");
        }
        
        public static PasswordChangeResult failure(String message) {
            return new PasswordChangeResult(false, message);
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
    
    /**
     * User creation result class
     */
    public static class UserCreationResult {
        private final boolean success;
        private final User user;
        private final String message;
        
        private UserCreationResult(boolean success, User user, String message) {
            this.success = success;
            this.user = user;
            this.message = message;
        }
        
        public static UserCreationResult success(User user) {
            return new UserCreationResult(true, user, "User created successfully");
        }
        
        public static UserCreationResult failure(String message) {
            return new UserCreationResult(false, null, message);
        }
        
        public boolean isSuccess() { return success; }
        public User getUser() { return user; }
        public String getMessage() { return message; }
    }
}
