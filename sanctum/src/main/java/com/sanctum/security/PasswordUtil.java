package com.sanctum.security;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Password utility class for hashing and validation using BCrypt
 */
public class PasswordUtil {
    private static final Logger logger = LoggerFactory.getLogger(PasswordUtil.class);
    
    // BCrypt workload factor (cost parameter)
    private static final int WORKLOAD = 12;
    
    /**
     * Hash password using BCrypt
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        
        try {
            String salt = BCrypt.gensalt(WORKLOAD);
            return BCrypt.hashpw(plainPassword, salt);
        } catch (Exception e) {
            logger.error("Failed to hash password", e);
            throw new RuntimeException("Password hashing failed", e);
        }
    }
    
    /**
     * Verify password against hash
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (Exception e) {
            logger.error("Password verification failed", e);
            return false;
        }
    }
    
    /**
     * Check if password meets minimum requirements
     */
    public static boolean isPasswordValid(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        
        // Minimum 8 characters
        if (password.length() < 8) {
            return false;
        }
        
        // At least one uppercase letter
        if (!password.matches(".*[A-Z].*")) {
            return false;
        }
        
        // At least one lowercase letter
        if (!password.matches(".*[a-z].*")) {
            return false;
        }
        
        // At least one digit
        if (!password.matches(".*\\d.*")) {
            return false;
        }
        
        // At least one special character
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Generate random password
     */
    public static String generateRandomPassword(int length) {
        if (length < 8) {
            length = 8;
        }
        
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * chars.length());
            password.append(chars.charAt(index));
        }
        
        return password.toString();
    }
    
    /**
     * Get password strength indicator
     */
    public static PasswordStrength getPasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return PasswordStrength.WEAK;
        }
        
        int score = 0;
        
        // Length bonus
        if (password.length() >= 8) score++;
        if (password.length() >= 12) score++;
        if (password.length() >= 16) score++;
        
        // Character variety bonus
        if (password.matches(".*[a-z].*")) score++;
        if (password.matches(".*[A-Z].*")) score++;
        if (password.matches(".*\\d.*")) score++;
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) score++;
        
        if (score <= 2) return PasswordStrength.WEAK;
        if (score <= 4) return PasswordStrength.MEDIUM;
        if (score <= 6) return PasswordStrength.STRONG;
        return PasswordStrength.VERY_STRONG;
    }
    
    /**
     * Password strength enumeration
     */
    public enum PasswordStrength {
        WEAK, MEDIUM, STRONG, VERY_STRONG
    }
    
    /**
     * Get password requirements description
     */
    public static String getPasswordRequirements() {
        return "Password must be at least 8 characters long and contain:\n" +
               "- At least one uppercase letter\n" +
               "- At least one lowercase letter\n" +
               "- At least one digit\n" +
               "- At least one special character (!@#$%^&*())";
    }
}
