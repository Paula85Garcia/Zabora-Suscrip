// package com.zabora.subscription.seguridad;

// import io.jsonwebtoken.Claims;
// import io.jsonwebtoken.Jwts;
// import io.jsonwebtoken.security.Keys;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Service;

// import javax.crypto.SecretKey;
// import java.util.Date;
// import java.util.HashMap;
// import java.util.Map;

// /**
//  * Servicio para generacion y validacion de tokens JWT
//  * Utiliza JJWT 0.12.x (sin metodos deprecated)
//  */
// @Service
// public class JwtService {
    
//     @Value("${jwt.secret:zabora-subscription-secret-key-2024-change-in-production-please}")
//     private String secret;
    
//     @Value("${jwt.expiration:86400000}") // 24 horas por defecto
//     private Long expiration;
    
//     /**
//      * Obtiene la clave de firma para JWT
//      */
//     private SecretKey getSigningKey() {
//         return Keys.hmacShaKeyFor(secret.getBytes());
//     }
    
//     /**
//      * Genera un token JWT para un usuario
//      * @param userId ID del usuario
//      * @param email Email del usuario
//      * @return Token JWT firmado
//      */
//     public String generateToken(String userId, String email) {
//         Map<String, Object> claims = new HashMap<>();
//         claims.put("userId", userId);
//         claims.put("email", email);
        
       
//         return Jwts.builder()
//             .claims(claims)
//             .subject(userId)
//             .issuedAt(new Date(System.currentTimeMillis()))
//             .expiration(new Date(System.currentTimeMillis() + expiration))
//             .signWith(getSigningKey()) 
//             .compact();
//     }
    
//     /**
//      * Extrae el userId del token
//      * @param token Token JWT
//      * @return ID del usuario
//      */
//     public String extractUserId(String token) {
//         Claims claims = extractAllClaims(token);
//         return claims.get("userId", String.class);
//     }
    
//     /**
//      * Extrae el email del token
//      * @param token Token JWT
//      * @return Email del usuario
//      */
//     public String extractEmail(String token) {
//         Claims claims = extractAllClaims(token);
//         return claims.get("email", String.class);
//     }
    
//     /**
//      * Valida si el token es valido para el usuario dado
//      * @param token Token JWT
//      * @param userId ID del usuario a validar
//      * @return true si el token es valido
//      */
//     public boolean isTokenValid(String token, String userId) {
//         final String tokenUserId = extractUserId(token);
//         return (tokenUserId.equals(userId) && !isTokenExpired(token));
//     }
    
//     /**
//      * Extrae todos los claims del token
//      * @param token Token JWT
//      * @return Claims del token
//      */
//     private Claims extractAllClaims(String token) {
//         return Jwts.parser()
//             .verifyWith(getSigningKey())
//             .build()
//             .parseSignedClaims(token)
//             .getPayload();
//     }
    
//     /**
//      * Verifica si el token ha expirado
//      * @param token Token JWT
//      * @return true si el token ha expirado
//      */
//     private boolean isTokenExpired(String token) {
//         return extractAllClaims(token).getExpiration().before(new Date());
//     }
// }