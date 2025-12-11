package com.zabora.subscription.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro que simula autenticación para pruebas.
 * 
 * - Obtiene el usuario desde el header "X-Usuario-Id".
 * - Si no existe, genera un usuario mock para endpoints protegidos.
 * - Agrega el usuario al contexto de la solicitud.
 */
public class FiltroAutenticacionSimulada extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Obtener usuario del header
        String usuarioId = request.getHeader("X-Usuario-Id");

        // Generar usuario mock si no existe y el endpoint es protegido
        if (usuarioId == null || usuarioId.isEmpty()) {
            String path = request.getRequestURI();
            if (path.startsWith("/api/suscripciones") || path.startsWith("/api/pagos")) {
                usuarioId = "usuario_mock_" + System.currentTimeMillis();
            }
        }

        // Guardar usuario en el contexto de la solicitud
        request.setAttribute("usuarioId", usuarioId);

        // Continuar con la cadena de filtros
        filterChain.doFilter(request, response);
    }
}
