package com.zabora.subscription.controlador;

import com.zabora.subscription.seguridad.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Endpoints para autenticación JWT")
public class AuthController {
    
    private final JwtService jwtService;
    
    @PostMapping("/login")
    @Operation(summary = "Obtener token JWT")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> authRequest) {
        String username = authRequest.get("username");
        String password = authRequest.get("password");
        
        // En producción, validar contra base de datos
        // Por ahora, simulamos autenticación exitosa
        if ("admin".equals(username) && "admin123".equals(password)) {
            String token = jwtService.generateToken(username);
            return ResponseEntity.ok(Map.of("token", token));
        }
        
        return ResponseEntity.status(401).body(Map.of("error", "Credenciales inválidas"));
    }
    
    @GetMapping("/validate")
    @Operation(summary = "Validar token JWT")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("valid", false));
        }
        
        String token = authHeader.substring(7);
        try {
            String username = jwtService.extractUsername(token);
            boolean isValid = jwtService.validateToken(token, username);
            return ResponseEntity.ok(Map.of(
                "valid", isValid,
                "username", username
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("valid", false));
        }
    }
}