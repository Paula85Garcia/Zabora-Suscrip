package com.zabora.subscription.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SeguridadConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails admin = User.builder()
                .username("admin")
                .password(passwordEncoder().encode("admin123"))
                .roles("ADMIN")
                .build();

        UserDetails usuario = User.builder()
                .username("usuario")
                .password(passwordEncoder().encode("usuario123"))
                .roles("USER")
                .build();

        UserDetails test = User.builder()
                .username("test")
                .password(passwordEncoder().encode("test123"))
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(admin, usuario, test);
    }

    @Bean
    @Profile("!prod") // Solo para desarrollo y pruebas
    public SecurityFilterChain devSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
            // ========== PARA PRUEBAS EN POSTMAN ==========
            .csrf(AbstractHttpConfigurer::disable) // CRÍTICO para Postman y APIs REST
            
            // ========== CONFIGURACIÓN DE AUTORIZACIÓN ==========
            .authorizeHttpRequests(authz -> authz
                // ===== H2 CONSOLE (Solo desarrollo) =====
                .requestMatchers(new AntPathRequestMatcher("/h2-console/**")).permitAll()
                
                // ===== DOCUMENTACIÓN API =====
                .requestMatchers(new AntPathRequestMatcher("/swagger-ui/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/swagger-ui.html")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/v3/api-docs/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api-docs/**")).permitAll()
                
                // ===== ENDPOINTS PÚBLICOS =====
                .requestMatchers(new AntPathRequestMatcher("/api/suscripciones/datos-mock")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/suscripciones/verificar/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/suscripciones/planes")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/pagos/pago-prueba")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/auth/**")).permitAll() // Si tienes endpoints de auth
                
                // ===== HEALTH CHECKS =====
                .requestMatchers(new AntPathRequestMatcher("/api/health")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/actuator/**")).permitAll()
                
                // ===== ENDPOINTS PARA PRUEBAS (con autenticación básica) =====
                .requestMatchers(new AntPathRequestMatcher("/api/suscripciones/**")).authenticated()
                .requestMatchers(new AntPathRequestMatcher("/api/pagos/**")).authenticated()
                .requestMatchers(new AntPathRequestMatcher("/api/admin/**")).hasRole("ADMIN")
                
                // ===== CUALQUIER OTRO ENDPOINT =====
                .anyRequest().authenticated()
            )
            
            // ========== AUTENTICACIÓN ==========
            .httpBasic(httpBasic -> {}) // Habilita Basic Auth para Postman
            
            // ========== CONFIGURACIÓN H2 CONSOLE ==========
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin()) // Permite H2 en iframe
            )
            
            .build();
    }

    @Bean
    @Profile("prod") // Configuración para producción
    public SecurityFilterChain prodSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable()) // O configura CSRF adecuadamente para tu frontend
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(new AntPathRequestMatcher("/api/suscripciones/planes")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/suscripciones/verificar/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/health")).permitAll()
                .anyRequest().authenticated()
            )
            .build();
    }
}