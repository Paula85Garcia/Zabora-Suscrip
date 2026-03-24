package com.zabora.subscription.data;



import java.io.IOException;



import org.springframework.stereotype.Component;

import org.springframework.web.filter.OncePerRequestFilter;



import jakarta.servlet.FilterChain;

import jakarta.servlet.ServletException;

import jakarta.servlet.http.HttpServletRequest;

import jakarta.servlet.http.HttpServletResponse;



@Component

public class UserContextFilter extends OncePerRequestFilter {



    @Override

    protected void doFilterInternal(HttpServletRequest request,

            HttpServletResponse response,

            FilterChain filterChain)

            throws ServletException, IOException {



        Integer userId = request.getHeader("X-User-Id") != null ? Integer.valueOf(request.getHeader("X-User-Id")) : null;

        String email = request.getHeader("X-User-Email");

        String role = request.getHeader("X-User-Role");



        // Temporal: Para pruebas, usar usuario hardcodeado si no viene de headers
        if (userId == null) {
            userId = 1; // Usuario hardcodeado para pruebas
            email = "test@example.com";
            role = "USER";
        }

        if (userId != null) {

            UserContext.set(new UserData(userId, email, role));

        }



        try {

            filterChain.doFilter(request, response);

        } finally {

            UserContext.clear();

        }

    }

}

