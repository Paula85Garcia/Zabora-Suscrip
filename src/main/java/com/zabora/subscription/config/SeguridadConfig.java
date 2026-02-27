package com.zabora.subscription.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SeguridadConfig {
    
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            // .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                
                
                // // Planes (lectura pública)
                 .requestMatchers("/api/suscripciones/planes").permitAll()
                
                // Webhooks de 
                .requestMatchers("/api/webhooks/**").permitAll()
                
                // Swagger/OpenAPI
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/api-docs/**",
                    "/swagger-resources/**",
                    "/webjars/**"
                ).permitAll()
                
                // H2 Console (solo desarrollo)
                .requestMatchers("/h2-console/**").permitAll()
                
                // Actuator Health
                .requestMatchers("/actuator/health").permitAll()
                
             
                // TODOS LOS DEMÁS ENDPOINTS REQUIEREN JWT
                
                .anyRequest().permitAll()
            );
            
        
        return http.build();
    }
    
    
}