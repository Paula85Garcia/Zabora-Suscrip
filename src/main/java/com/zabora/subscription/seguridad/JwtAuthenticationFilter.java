package com.zabora.subscription.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtService jwtService;
    /**
     * EXPLICACIÓN GENERAL:
     * Este filtro se ejecuta una sola vez por cada solicitud HTTP.
     * Su función es:
     * 
     * 1. Leer el encabezado Authorization
     * 2. Extraer el token JWT si está presente
     * 3. Validar el token
     * 4. Si es válido, autenticar al usuario en el contexto de Spring Security
     * 
     * Este proceso permite que las rutas protegidas reconozcan al usuario,
     * sin necesidad de volver a iniciar sesión en cada petición.
     */
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) 
            throws ServletException, IOException {

        // Extrae el encabezado Authorization de la petición
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;
        
        /**
         * Caso 1: No hay token o no empieza con "Bearer "
         * En este caso, se deja pasar la solicitud sin autenticar.
         * Esto permite que rutas públicas sigan funcionando.
         */
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        // Obtiene el token JWT (después de "Bearer ")
        jwt = authHeader.substring(7);
     // Extrae el nombre de usuario contenido dentro del token

        username = jwtService.extractUsername(jwt);
        
        /**
         * Caso 2: El usuario está presente en el token y aún no está autenticado
         * Esto evita autenticar dos veces al mismo usuario en la misma petición.
         */
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        	// Valida que el token sea correcto y no haya expirado
        	if (jwtService.validateToken(jwt, username)) {
        		 /**
                 * Si el token es válido, se construye un objeto de autenticación
                 * sin credenciales, ya que la verificación ocurrió mediante el JWT.
                 * 
                 * También se adjunta información adicional de la solicitud,
                 * como la IP o el User-Agent.
                 */
                UsernamePasswordAuthenticationToken authToken = 
                    new UsernamePasswordAuthenticationToken(username, null, null);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                // Se registra la autenticación en el contexto de seguridad
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
     // Continúa con el siguiente filtro de la cadena
        filterChain.doFilter(request, response);
    }
}