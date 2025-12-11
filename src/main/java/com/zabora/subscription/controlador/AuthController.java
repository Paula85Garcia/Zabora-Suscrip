package com.zabora.subscription.controlador;

import com.zabora.subscription.seguridad.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador para la autenticación usando JWT (JSON Web Tokens).
 * Permite obtener un token al iniciar sesión y validar tokens existentes.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Endpoints para autenticación JWT")
public class AuthController {

    // Servicio encargado de generar y validar JWT
    private final JwtService jwtService;

    /**
     * Endpoint para iniciar sesión y obtener un token JWT.
     *
     * @param authRequest Mapa con 'username' y 'password'.
     * @return Token JWT si las credenciales son correctas, error 401 si no.
     */
    @PostMapping("/login")
    @Operation(summary = "Obtener token JWT")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> authRequest) {
        String username = authRequest.get("username");
        String password = authRequest.get("password");

        // En producción, validar contra base de datos
        // Por ahora, simulamos autenticación exitosa con admin/admin123
        if ("admin".equals(username) && "admin123".equals(password)) {
            String token = jwtService.generateToken(username);
            return ResponseEntity.ok(Map.of("token", token));
        }

        // Credenciales incorrectas
        return ResponseEntity.status(401).body(Map.of("error", "Credenciales inválidas"));
    }

    /**
     * Endpoint para validar un token JWT.
     *
     * @param authHeader Header Authorization con el token Bearer.
     * @return Validación del token y nombre de usuario si es válido, o 401 si no.
     */
    @GetMapping("/validate")
    @Operation(summary = "Validar token JWT")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("valid", false));
        }

        String token = authHeader.substring(7); // Quita "Bearer "
        try {
            String username = jwtService.extractUsername(token);
            boolean isValid = jwtService.validateToken(token, username);
            return ResponseEntity.ok(Map.of(
                "valid", isValid,
                "username", username
            ));
        } catch (Exception e) {
            // Token inválido o expirado
            return ResponseEntity.status(401).body(Map.of("valid", false));
        }
    }
}
