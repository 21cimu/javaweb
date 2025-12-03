package com.carrental.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT utility class for token generation and validation
 */
public class JwtUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    private static final String SECRET;
    private static final long EXPIRATION;
    private static final SecretKey KEY;

    static {
        SECRET = DatabaseUtil.getProperty("jwt.secret", "default-secret-key-for-development-only");
        EXPIRATION = Long.parseLong(DatabaseUtil.getProperty("jwt.expiration", "86400000"));
        KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate JWT token for user
     */
    public static String generateToken(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(KEY)
                .compact();
    }

    /**
     * Validate and parse JWT token
     */
    public static Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            logger.warn("Invalid JWT token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check if token is valid
     */
    public static boolean isTokenValid(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return false;
        }
        return claims.getExpiration().after(new Date());
    }

    /**
     * Get user ID from token
     */
    public static Long getUserId(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        Object userId = claims.get("userId");
        if (userId instanceof Number) {
            return ((Number) userId).longValue();
        }
        return null;
    }

    /**
     * Get username from token
     */
    public static String getUsername(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.getSubject() : null;
    }

    /**
     * Get role from token
     */
    public static String getRole(String token) {
        Claims claims = parseToken(token);
        return claims != null ? (String) claims.get("role") : null;
    }
}
