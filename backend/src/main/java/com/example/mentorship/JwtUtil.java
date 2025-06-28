package com.example.mentorship;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.Date;
import java.util.UUID;

/**
 * Utility class for JWT token generation and validation.
 * Contains all RFC 7519 required claims.
 */
public class JwtUtil {
    // Secret key for JWT signature
    private static final String SECRET_KEY = "mentor_mentee_app_secret_key";
    
    // Algorithm for JWT signature
    private static final Algorithm ALGORITHM = Algorithm.HMAC256(SECRET_KEY);
    
    // JWT issuer
    private static final String ISSUER = "mentor-mentee-app";
    
    // JWT audience
    private static final String AUDIENCE = "api-users";
    
    // Token validity (1 hour)
    private static final long TOKEN_VALIDITY = 3600000; // 1 hour in milliseconds
    
    /**
     * Generate a JWT token for a user
     * Includes all RFC 7519 required claims:
     * - iss (Issuer)
     * - sub (Subject - user ID)
     * - aud (Audience)
     * - exp (Expiration Time)
     * - nbf (Not Before)
     * - iat (Issued At)
     * - jti (JWT ID)
     * Plus application-specific claims:
     * - name
     * - email
     * - role
     */
    public static String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + TOKEN_VALIDITY);
        
        return JWT.create()
                // Required claims per RFC 7519
                .withIssuer(ISSUER)                     // iss claim
                .withSubject(user.getId().toString())   // sub claim
                .withAudience(AUDIENCE)                 // aud claim
                .withExpiresAt(expiryDate)              // exp claim
                .withNotBefore(now)                     // nbf claim
                .withIssuedAt(now)                      // iat claim
                .withJWTId(UUID.randomUUID().toString()) // jti claim
                
                // Custom claims
                .withClaim("name", user.getName())
                .withClaim("email", user.getEmail())
                .withClaim("role", user.getRole())
                
                .sign(ALGORITHM);
    }
    
    /**
     * Verify and extract user ID from a JWT token
     */
    public static Integer getUserIdFromToken(String token) {
        try {
            JWTVerifier verifier = JWT.require(ALGORITHM)
                    .withIssuer(ISSUER)
                    .withAudience(AUDIENCE)
                    .build();
            
            DecodedJWT jwt = verifier.verify(token);
            return Integer.parseInt(jwt.getSubject());
        } catch (JWTVerificationException | NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * Extract the user's role from a JWT token
     */
    public static String getRoleFromToken(String token) {
        try {
            JWTVerifier verifier = JWT.require(ALGORITHM)
                    .withIssuer(ISSUER)
                    .withAudience(AUDIENCE)
                    .build();
            
            DecodedJWT jwt = verifier.verify(token);
            return jwt.getClaim("role").asString();
        } catch (JWTVerificationException e) {
            return null;
        }
    }
    
    /**
     * Validate a JWT token
     */
    public static boolean validateToken(String token) {
        try {
            JWTVerifier verifier = JWT.require(ALGORITHM)
                    .withIssuer(ISSUER)
                    .withAudience(AUDIENCE)
                    .build();
            
            verifier.verify(token);
            return true;
        } catch (JWTVerificationException e) {
            return false;
        }
    }
    
    /**
     * Extract token from Authorization header
     */
    public static String extractTokenFromHeader(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }
}
