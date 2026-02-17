// package com.zabora.subscription.controlador;

// import com.zabora.subscription.seguridad.JwtService;
// import io.swagger.v3.oas.annotations.Operation;
// import io.swagger.v3.oas.annotations.tags.Tag;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.*;

// import java.util.Map;
// import java.util.UUID;

// @RestController
// @RequestMapping("/api/auth")
// @RequiredArgsConstructor
// @Slf4j
// @Tag(name = "Autenticación", description = "Endpoints para autenticación JWT temporal")
// public class AuthController {
    
//     private final JwtService jwtService;
    
//     @PostMapping("/register")
//     @Operation(summary = "Registrar nuevo usuario (temporal - solo para pruebas)")
//     public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> request) {
//         String email = request.get("email");
//         String password = request.get("password");
        
//         if (email == null || password == null) {
//             return ResponseEntity.badRequest()
//                 .body(Map.of("error", "Email y password son requeridos"));
//         }
        
//         // Generar ID único para el usuario
//         String userId = "user_" + UUID.randomUUID().toString();
        
//         // Generar token JWT
//         String token = jwtService.generateToken(userId, email);
        
//         log.info("✅ Usuario registrado: {} - {}", userId, email);
        
//         return ResponseEntity.ok(Map.of(
//             "success", true,
//             "userId", userId,
//             "email", email,
//             "token", token,
//             "message", "Usuario registrado exitosamente"
//         ));
//     }
    
//     @PostMapping("/login")
//     @Operation(summary = "Login de usuario (temporal - acepta cualquier credencial)")
//     public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
//         String email = request.get("email");
//         String password = request.get("password");
        
//         if (email == null || password == null) {
//             return ResponseEntity.badRequest()
//                 .body(Map.of("error", "Email y password son requeridos"));
//         }
        
//         // Por ahora, generar un userId basado en el email
//         String userId = "user_" + email.hashCode();
        
//         String token = jwtService.generateToken(userId, email);
        
//         log.info("Usuario logueado: {} - {}", userId, email);
        
//         return ResponseEntity.ok(Map.of(
//             "success", true,
//             "userId", userId,
//             "email", email,
//             "token", token,
//             "message", "Login exitoso"
//         ));
//     }
    
//     @GetMapping("/validate")
//     @Operation(summary = "Validar token JWT")
//     public ResponseEntity<Map<String, Object>> validateToken(
//             @RequestHeader("Authorization") String authHeader) {
        
//         if (authHeader == null || !authHeader.startsWith("Bearer ")) {
//             return ResponseEntity.status(401)
//                 .body(Map.of("valid", false, "error", "Token no proporcionado"));
//         }
        
//         String token = authHeader.substring(7);
        
//         try {
//             String userId = jwtService.extractUserId(token);
//             String email = jwtService.extractEmail(token);
//             boolean isValid = jwtService.isTokenValid(token, userId);
            
//             if (isValid) {
//                 return ResponseEntity.ok(Map.of(
//                     "valid", true,
//                     "userId", userId,
//                     "email", email
//                 ));
//             } else {
//                 return ResponseEntity.status(401)
//                     .body(Map.of("valid", false, "error", "Token expirado"));
//             }
//         } catch (Exception e) {
//             return ResponseEntity.status(401)
//                 .body(Map.of("valid", false, "error", "Token inválido"));
//         }
//     }
// }