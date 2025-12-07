package com.zabora.subscription.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

public class FiltroAutenticacionSimulada extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) 
            throws ServletException, IOException {
        
        // Simulación de autenticación para pruebas
        String usuarioId = request.getHeader("X-Usuario-Id");
        
        if (usuarioId == null || usuarioId.isEmpty()) {
            // Para endpoints públicos, continuar sin usuario
            // Para endpoints protegidos, generar un usuario mock
            if (request.getRequestURI().startsWith("/api/suscripciones") ||
                request.getRequestURI().startsWith("/api/pagos")) {
                usuarioId = "usuario_mock_" + System.currentTimeMillis();
            }
        }
        
        // Agregar usuario al contexto de la solicitud
        request.setAttribute("usuarioId", usuarioId);
        
        filterChain.doFilter(request, response);
    }
}