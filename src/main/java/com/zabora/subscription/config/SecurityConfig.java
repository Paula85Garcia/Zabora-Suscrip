package com.zabora.subscription.config;

import com.zabora.subscription.data.BearerJwtUserContextFallback;
import com.zabora.subscription.data.UserContextFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuracion de seguridad.
 *
 * UserContextFilter va en la cadena de seguridad (no solo como @Component) para que
 * el contexto exista antes del DispatcherServlet y no dependa del orden de filtros servlet.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public UserContextFilter userContextFilter(BearerJwtUserContextFallback bearerJwtUserContextFallback) {
        return new UserContextFilter(bearerJwtUserContextFallback);
    }

    /**
     * Evita doble ejecución: el filtro solo entra por SecurityFilterChain.
     */
    @Bean
    public FilterRegistrationBean<UserContextFilter> userContextFilterServletRegistration(UserContextFilter filter) {
        FilterRegistrationBean<UserContextFilter> bean = new FilterRegistrationBean<>(filter);
        bean.setEnabled(false);
        return bean;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, UserContextFilter userContextFilter)
            throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(userContextFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/webhooks/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/api-docs/**", "/v3/api-docs/**").permitAll()
                .anyRequest().permitAll()
            );

        return http.build();
    }
}
