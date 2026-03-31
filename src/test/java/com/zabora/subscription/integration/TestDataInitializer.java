package com.zabora.subscription.integration;

import com.zabora.subscription.modelo.entidad.PlanSuscripcion;
import com.zabora.subscription.repositorio.PlanSuscripcionRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;

@Configuration
@Profile("test")
@RequiredArgsConstructor
@Slf4j
public class TestDataInitializer {

    private final PlanSuscripcionRepositorio planRepositorio;

    @Bean
    CommandLineRunner initTestData() {
        return args -> {
            log.info("Initializing test data...");
            
            // Crear planes si no existen
            if (planRepositorio.count() == 0) {
                PlanSuscripcion planGratuito = PlanSuscripcion.builder()
                    .nombre("gratuito")
                    .descripcion("Plan gratuito con caracteristicas basicas")
                    .precio(BigDecimal.ZERO)
                    .moneda("COP")
                    .activo(true)
                    .build();
                
                PlanSuscripcion planPremium = PlanSuscripcion.builder()
                    .nombre("premium")
                    .descripcion("Plan premium con todas las caracteristicas")
                    .precio(new BigDecimal("29900.00"))
                    .moneda("COP")
                    .activo(true)
                    .build();
                
                planRepositorio.save(planGratuito);
                planRepositorio.save(planPremium);
                
                log.info("Test data initialized: gratuito and premium plans created");
            } else {
                log.info("Test data already exists");
            }
        };
    }
}
