package com.zabora.subscription.data;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Optional;

/**
 * Cuando el gateway envía {@code X-User-Id} vacío (p. ej. {@code userId} en el JWT como Long),
 * intenta obtener usuario desde {@code Authorization: Bearer} usando el mismo secreto HMAC que auth-service.
 */
@Slf4j
@Component
public class BearerJwtUserContextFallback {

    @Value("${zabora.subscription.jwt.secret:}")
    private String secretBase64;

    private SecretKey signingKey;

    @PostConstruct
    void init() {
        if (secretBase64 == null || secretBase64.isBlank()) {
            log.warn("zabora.subscription.jwt.secret vacío — no habrá respaldo Bearer→userId");
            return;
        }
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secretBase64.trim());
            signingKey = Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            log.error("No se pudo inicializar la clave JWT del servicio de suscripciones: {}", e.getMessage());
            signingKey = null;
        }
    }

    public Optional<UserData> tryResolve(HttpServletRequest request) {
        if (signingKey == null) {
            return Optional.empty();
        }
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return Optional.empty();
        }
        String token = auth.substring(7).trim();
        if (token.isEmpty()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .clockSkewSeconds(60)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Integer userId = claimToInteger(claims.get("userId"));
            if (userId == null) {
                return Optional.empty();
            }
            String email = claims.getSubject();
            Object rolObj = claims.get("rol");
            String role = rolObj != null ? rolObj.toString() : null;

            return Optional.of(
                    UserData.builder()
                            .userId(userId)
                            .email(email)
                            .role(role)
                            .build()
            );
        } catch (JwtException e) {
            log.debug("Bearer JWT no válido para contexto de usuario: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static Integer claimToInteger(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number n) {
            long v = n.longValue();
            if (v < Integer.MIN_VALUE || v > Integer.MAX_VALUE) {
                return null;
            }
            return (int) v;
        }
        try {
            return Integer.parseInt(raw.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
