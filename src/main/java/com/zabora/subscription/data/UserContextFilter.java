package com.zabora.subscription.data;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Filtro que extrae los headers del API Gateway y los coloca en UserContext.
 * Se registra solo vía {@link com.zabora.subscription.config.SecurityConfig} (addFilterBefore)
 * para evitar doble registro como filtro servlet y orden incorrecto frente a Spring Security.
 */
@Slf4j
@RequiredArgsConstructor
public class UserContextFilter extends OncePerRequestFilter {

    private final BearerJwtUserContextFallback bearerJwtUserContextFallback;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String userIdHeader = request.getHeader("X-User-Id");
            String emailHeader = request.getHeader("X-User-Email");
            if (emailHeader == null || emailHeader.isBlank()) {
                emailHeader = request.getHeader("X-Email");
            }
            String roleHeader = request.getHeader("X-User-Role");
            if (roleHeader == null || roleHeader.isBlank()) {
                roleHeader = request.getHeader("X-Rol");
            }

            Optional<UserData> fromGateway = resolveFromGatewayHeaders(userIdHeader, emailHeader, roleHeader);
            Optional<UserData> fromBearer = bearerJwtUserContextFallback.tryResolve(request);
            Optional<UserData> context = fromGateway.or(() -> fromBearer);

            context.ifPresent(userData -> {
                UserContext.set(userData);
                if (fromBearer.isPresent() && fromGateway.isEmpty()) {
                    log.info("UserContext desde Bearer JWT (X-User-Id del gateway ausente o vacío)");
                } else {
                    log.debug("UserContext desde headers gateway — userId: {}", userData.getUserId());
                }
            });

            if (context.isEmpty() && debeTenerUsuario(request)) {
                log.warn(
                    "Sin UserContext para {} {} — X-User-Id='{}', Authorization={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    userIdHeader,
                    request.getHeader("Authorization") != null ? "presente" : "ausente"
                );
            }

            filterChain.doFilter(request, response);

        } finally {
            UserContext.clear();
        }
    }

    private Optional<UserData> resolveFromGatewayHeaders(String userIdHeader, String emailHeader, String roleHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            return Optional.empty();
        }
        try {
            int userId = Integer.parseInt(userIdHeader.trim());
            return Optional.of(UserData.builder()
                .userId(userId)
                .email(emailHeader)
                .role(roleHeader)
                .build());
        } catch (NumberFormatException e) {
            log.warn("Header X-User-Id invalido (no numerico): '{}' — se intentara Bearer JWT si aplica",
                userIdHeader);
            return Optional.empty();
        }
    }

    /** Rutas que requieren usuario autenticado (excluye catálogo público y webhooks). */
    private static boolean debeTenerUsuario(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        if (uri == null) {
            return false;
        }
        if (uri.startsWith("/api/webhooks/")) {
            return false;
        }
        if ("GET".equalsIgnoreCase(method) && uri.endsWith("/api/suscripciones/planes")) {
            return false;
        }
        if ("GET".equalsIgnoreCase(method) && uri.contains("/api/pagos/bricks/public-key")) {
            return false;
        }
        if (uri.startsWith("/api/suscripciones/") || uri.startsWith("/api/pagos/")) {
            return true;
        }
        if (uri.startsWith("/api/admin/")) {
            return true;
        }
        return false;
    }
}
