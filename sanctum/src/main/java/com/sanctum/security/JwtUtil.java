package com.sanctum.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT utility class for token generation and validation
 */
public class JwtUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    
    // Secret key for signing JWT tokens
    private static final SecretKey SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    
    // Token expiration time (24 hours)
    private static final long EXPIRATION_TIME = 24 * 60 * 60 * 1000;
    
    /**
     * Generate JWT token for user
     */
    public static String generateToken(String userId, String role, Integer churchId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("churchId", churchId);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SECRET_KEY)
                .compact();
    }
    
    /**
     * Extract username from token
     */
    public static String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }
    
    /**
     * Extract role from token
     */
    public static String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }
    
    /**
     * Extract church ID from token
     */
    public static Integer extractChurchId(String token) {
        return extractClaims(token).get("churchId", Integer.class);
    }
    
    /**
     * Extract expiration date from token
     */
    public static Date extractExpiration(String token) {
        return extractClaims(token).getExpiration();
    }
    
    /**
     * Extract all claims from token
     */
    private static Claims extractClaims(String token) {
        return Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    /**
     * Check if token is expired
     */
    public static boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }
    
    /**
     * Validate token
     */
    public static boolean validateToken(String token, String username) {
        try {
            final String extractedUsername = extractUsername(token);
            return (extractedUsername.equals(username) && !isTokenExpired(token));
        } catch (JwtException e) {
            logger.warn("JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Validate token format and signature
     */
    public static boolean isValidToken(String token) {
        try {
            Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get remaining time until token expires (in milliseconds)
     */
    public static long getTimeUntilExpiration(String token) {
        try {
            Date expiration = extractExpiration(token);
            return expiration.getTime() - System.currentTimeMillis();
        } catch (JwtException e) {
            return 0;
        }
    }
    
    /**
     * Refresh token (generate new token with same claims)
     */
    public static String refreshToken(String token) {
        try {
            Claims claims = extractClaims(token);
            return Jwts.builder()
                    .setClaims(claims)
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                    .signWith(SECRET_KEY)
                    .compact();
        } catch (JwtException e) {
            logger.error("Failed to refresh token: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Get token header information
     */
    public static Map<String, Object> getTokenHeader(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJwt(token.substring(0, token.lastIndexOf('.') + 1))
                    .getHeader();
        } catch (JwtException e) {
            logger.warn("Failed to extract token header: {}", e.getMessage());
            return new HashMap<>();
        }
    }
}
