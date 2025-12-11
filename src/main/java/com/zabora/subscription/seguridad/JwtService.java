package com.zabora.subscription.seguridad;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {
	 /**
     * Clave secreta usada para firmar los tokens.
     * Se obtiene del archivo de configuración.
     * En producción debe ser larga y segura.
     */
    @Value("${jwt.secret:zabora-subscription-secret-key-2024-change-in-production}")
    private String secret;
    /**
     * Tiempo de expiración del token en milisegundos.
     * Valor por defecto: 24 horas.
     */
    @Value("${jwt.expiration:86400000}")
    private Long expiration;
    /**
     * Genera la llave criptográfica usada para firmar y validar los tokens.
     * Convierte el valor secreto en un SecretKey válido para HMAC-SHA.
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
    /**
     * Extrae el nombre de usuario almacenado en el token JWT.
     * El "subject" es el campo estándar donde se guarda el usuario.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    /**
     * Obtiene la fecha de expiración del token.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    /**
     * Permite extraer cualquier tipo de información almacenada en el token,
     * recibiendo una función que indica qué dato se quiere obtener.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    /**
     * Decodifica el token completo y retorna todos los "claims".
     * Utiliza la llave secreta para verificar que la firma sea válida.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    /**
     * Verifica si el token ya pasó su fecha de expiración.
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
   /*
    * Genera un token JWT para el usuario dado.
    * El token no contiene información adicional (claims vacíos).
    */
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username);
    }
    /**
     * Crea y firma un token JWT.
     * Incluye:
     * - Claims personalizados
     * - Nombre de usuario (subject)
     * - Fecha de creación
     * - Fecha de expiración
     * - Firma criptográfica HMAC-SHA usando la clave secreta
     */
    
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }
    /**
     * Valida que:
     * 1. El nombre dentro del token coincide con el usuario esperado
     * 2. El token no esté expirado
     */
    public Boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }
}