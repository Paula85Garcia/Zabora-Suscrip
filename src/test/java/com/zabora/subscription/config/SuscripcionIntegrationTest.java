//package com.zabora.subscription.config;
//
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.lang.NonNull;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//import com.zabora.subscription.seguridad.JwtService;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//
///**
// * Authentication Filter with dual mode:
// * 1. API Gateway Mode: Reads user data from headers (X-User-Id, X-Email, X-Rol)
// * 2. JWT Mode: Validates JWT tokens when gateway is not available
// * 
// * Configuration controlled by gateway.enabled property
// */
//@Component
//@Slf4j
//public class SuscripcionIntegrationTest extends OncePerRequestFilter {
//    
//    @Value("${gateway.enabled:false}")
//    private boolean gatewayEnabled;
//    
//    @Value("${gateway.header.userId:X-User-Id}")
//    private String headerUserId;
//    
//    @Value("${gateway.header.email:X-Email}")
//    private String headerEmail;
//    
//    @Value("${gateway.header.role:X-Rol}")
//    private String headerRole;
//    
//    private final JwtService jwtService;
//    
//    // Public endpoints that don't require authentication
//    private static final List<String> PUBLIC_ENDPOINTS = Arrays.asList(
//        "/api/auth/login",
//        "/api/auth/register",
//        "/api/suscripciones/planes",
//        "/api/webhooks/stripe",
//        "/swagger-ui",
//        "/v3/api-docs",
//        "/actuator/health",
//        "/h2-console"
//    );
//    
//    public SuscripcionIntegrationTest(JwtService jwtService) {
//        this.jwtService = jwtService;
//    }
//    
//    @Override
//    protected void doFilterInternal(
//            @NonNull HttpServletRequest request,
//            @NonNull HttpServletResponse response,
//            @NonNull FilterChain filterChain) throws ServletException, IOException {
//        
//        String requestPath = request.getRequestURI();
//        
//        // Skip authentication for public endpoints
//        if (isPublicEndpoint(requestPath)) {
//            filterChain.doFilter(request, response);
//            return;
//        }
//        
//        try {
//            if (gatewayEnabled) {
//                // MODE 1: API Gateway - Read from headers
//                authenticateFromGateway(request);
//            } else {
//                // MODE 2: JWT - Validate token
//                authenticateFromJwt(request);
//            }
//        } catch (Exception e) {
//            log.error("Authentication error: {}", e.getMessage());
//            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//            response.setContentType("application/json");
//            response.getWriter().write(
//                "{\"error\": \"Unauthorized\", \"message\": \"" + e.getMessage() + "\"}"
//            );
//            return;
//        }
//        
//        filterChain.doFilter(request, response);
//    }
//    
//    /**
//     * Authenticate using API Gateway headers
//     */
//    private void authenticateFromGateway(HttpServletRequest request) {
//        String userId = request.getHeader(headerUserId);
//        String email = request.getHeader(headerEmail);
//        String role = request.getHeader(headerRole);
//        
//        if (userId == null || userId.isEmpty()) {
//            throw new RuntimeException("User ID header not found from gateway");
//        }
//        
//        log.debug("Gateway authentication - User ID: {}, Email: {}, Role: {}", 
//                  userId, email, role);
//        
//        // Create authentication token
//        UsernamePasswordAuthenticationToken authToken = 
//            new UsernamePasswordAuthenticationToken(userId, null, new ArrayList<>());
//        
//        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//        SecurityContextHolder.getContext().setAuthentication(authToken);
//        
//        // Store in request attributes for easy access in controllers
//        request.setAttribute("usuarioId", userId);
//        request.setAttribute("email", email);
//        request.setAttribute("rol", role);
//    }
//    
//    /**
//     * Authenticate using JWT token
//     */
//    private void authenticateFromJwt(HttpServletRequest request) {
//        String authHeader = request.getHeader("Authorization");
//        
//        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
//            throw new RuntimeException("JWT token required");
//        }
//        
//        String jwt = authHeader.substring(7);
//        String userId = jwtService.extractUserId(jwt);
//        
//        if (userId == null || !jwtService.isTokenValid(jwt, userId)) {
//            throw new RuntimeException("Invalid or expired JWT token");
//        }
//        
//        String email = jwtService.extractEmail(jwt);
//        
//        log.debug("JWT authentication - User ID: {}, Email: {}", userId, email);
//        
//        // Create authentication token
//        UsernamePasswordAuthenticationToken authToken = 
//            new UsernamePasswordAuthenticationToken(userId, null, new ArrayList<>());
//        
//        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//        SecurityContextHolder.getContext().setAuthentication(authToken);
//        
//        // Store in request attributes
//        request.setAttribute("usuarioId", userId);
//        request.setAttribute("email", email);
//    }
//    
//    /**
//     * Check if the endpoint is public
//     */
//    private boolean isPublicEndpoint(String path) {
//        return PUBLIC_ENDPOINTS.stream()
//            .anyMatch(path::startsWith);
//    }
//}