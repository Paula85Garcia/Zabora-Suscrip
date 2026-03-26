package com.zabora.subscription.config;

import com.zabora.subscription.data.UserContextFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuracion de seguridad del servicio de suscripciones.
 *
 * Este microservicio no valida JWT por si mismo.
 * El API Gateway ya valido el token y agrego los headers:
 *   X-User-Id, X-User-Email, X-User-Role
 *
 * UserContextFilter extrae esos headers y los pone en UserContext (ThreadLocal).
 * La autorizacion por rol (ADMIN vs USER) se hace manualmente en cada endpoint.
 *
 * Spring Security esta configurado en modo pass-through: deja pasar todo
 * y deja que la logica de negocio decida quien puede hacer que.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SeguridadConfig {

    private final UserContextFilter userContextFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .anyRequest().permitAll()
            )
            .addFilterBefore(userContextFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}