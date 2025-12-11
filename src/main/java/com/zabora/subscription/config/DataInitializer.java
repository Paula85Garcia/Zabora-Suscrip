package com.zabora.subscription.config;

import com.zabora.subscription.modelo.entidad.*;
import com.zabora.subscription.modelo.enumeracion.*;
import com.zabora.subscription.repositorio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Clase de inicialización de datos de prueba.
 * Se ejecuta al iniciar la aplicación y carga:
 * - Planes de suscripción
 * - Usuarios de ejemplo
 * - Métodos de pago
 * - Pagos de prueba
 * - Logs de auditoría
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    /**
     * Inicializa la base de datos con datos de prueba si no existen.
     * Solo se ejecuta en perfiles distintos a "test".
     */
    @Bean
    @Profile("!test")
    CommandLineRunner initDatabase(
            PlanSuscripcionRepository planRepo,
            UsuarioSuscripcionRepository suscripcionRepo,
            PagoRepository pagoRepo,
            MetodoPagoRepository metodoPagoRepo,
            LogSuscripcionRepository logRepo) {

        return args -> {
            log.info("Iniciando carga de datos de prueba...");

            // Evitar duplicar datos
            if (planRepo.count() > 0) {
                log.info("Los datos ya existen, omitiendo inicialización");
                return;
            }

            // ========== 1. CREAR PLANES ==========
            log.info("Creando planes de suscripción...");

            PlanSuscripcion planGratuito = new PlanSuscripcion();
            planGratuito.setNombre("gratuito");
            planGratuito.setDescripcion("Plan gratuito con características básicas");
            planGratuito.setPrecio(BigDecimal.ZERO);
            planGratuito.setMoneda("COP");
            planGratuito.setLimiteCondicionesMedicas(2);
            planGratuito.setLimiteAlergias(2);
            planGratuito.setLimitePreferenciasAlimentarias(1);
            planGratuito.setIngredientesPorBusqueda(7);
            planGratuito.setLimiteRecetasFavoritas(4);
            planGratuito.setActivo(true);
            planRepo.save(planGratuito);

            PlanSuscripcion planPremium = new PlanSuscripcion();
            planPremium.setNombre("premium");
            planPremium.setDescripcion("Plan premium con todas las características");
            planPremium.setPrecio(new BigDecimal("29900.00"));
            planPremium.setMoneda("COP");
            planPremium.setLimiteCondicionesMedicas(3);
            planPremium.setLimiteAlergias(4);
            planPremium.setLimitePreferenciasAlimentarias(1);
            planPremium.setIngredientesPorBusqueda(20);
            planPremium.setLimiteRecetasFavoritas(null); // Ilimitado
            planPremium.setActivo(true);
            planRepo.save(planPremium);

            log.info("Planes creados: {} y {}", planGratuito.getNombre(), planPremium.getNombre());

            // ========== 2. CREAR USUARIOS DE PRUEBA ==========
            log.info("Creando usuarios de prueba...");

            // Usuario 1: Premium Activo
            String usuario1Id = "user_premium_001";
            UsuarioSuscripcion sus1 = new UsuarioSuscripcion();
            sus1.setId(UUID.randomUUID().toString());
            sus1.setUsuarioId(usuario1Id);
            sus1.setPlan(planPremium);
            sus1.setEstado(EstadoSuscripcion.ACTIVA);
            sus1.setInicioPeriodoActual(LocalDateTime.now().minusDays(10));
            sus1.setFinPeriodoActual(LocalDateTime.now().plusDays(20));
            sus1.setIdClienteStripe("cus_test_001");
            sus1.setIdSuscripcionStripe("sub_test_001");
            suscripcionRepo.save(sus1);

            // Usuario 2: Gratuito
            String usuario2Id = "user_free_002";
            UsuarioSuscripcion sus2 = new UsuarioSuscripcion();
            sus2.setId(UUID.randomUUID().toString());
            sus2.setUsuarioId(usuario2Id);
            sus2.setPlan(planGratuito);
            sus2.setEstado(EstadoSuscripcion.ACTIVA);
            sus2.setInicioPeriodoActual(LocalDateTime.now().minusDays(5));
            suscripcionRepo.save(sus2);

            // Usuario 3: Premium Pendiente de Pago
            String usuario3Id = "user_pending_003";
            UsuarioSuscripcion sus3 = new UsuarioSuscripcion();
            sus3.setId(UUID.randomUUID().toString());
            sus3.setUsuarioId(usuario3Id);
            sus3.setPlan(planPremium);
            sus3.setEstado(EstadoSuscripcion.PENDIENTE_PAGO);
            suscripcionRepo.save(sus3);

            log.info("Usuarios creados: {}, {}, {}", usuario1Id, usuario2Id, usuario3Id);

            // ========== 3. CREAR MÉTODOS DE PAGO ==========
            log.info("Creando métodos de pago de prueba...");

            // Tarjeta para usuario 1
            MetodoPago tarjeta1 = new MetodoPago();
            tarjeta1.setId(UUID.randomUUID().toString());
            tarjeta1.setUsuarioId(usuario1Id);
            tarjeta1.setTipo(TipoMetodoPago.TARJETA_CREDITO);
            tarjeta1.setUltimosCuatro("4242");
            tarjeta1.setMarca("Visa");
            tarjeta1.setExpiraMes(12);
            tarjeta1.setExpiraAnio(2025);
            tarjeta1.setIdMetodoPagoStripe("pm_test_001");
            tarjeta1.setPredeterminado(true);
            tarjeta1.setActivo(true);
            metodoPagoRepo.save(tarjeta1);

            // PSE para usuario 1
            MetodoPago pse1 = new MetodoPago();
            pse1.setId(UUID.randomUUID().toString());
            pse1.setUsuarioId(usuario1Id);
            pse1.setTipo(TipoMetodoPago.PSE);
            pse1.setBanco("Bancolombia");
            pse1.setTipoCuenta(TipoCuentaBanco.AHORROS);
            pse1.setIdMetodoPagoStripe("pm_test_002");
            pse1.setPredeterminado(false);
            pse1.setActivo(true);
            metodoPagoRepo.save(pse1);

            log.info("Métodos de pago creados");

            // ========== 4. CREAR PAGOS DE EJEMPLO ==========
            log.info("Creando pagos de prueba...");

            // Pago exitoso para usuario 1
            Pago pago1 = new Pago();
            pago1.setId(UUID.randomUUID().toString());
            pago1.setSuscripcion(sus1);
            pago1.setUsuarioId(usuario1Id);
            pago1.setMonto(new BigDecimal("29900.00"));
            pago1.setMoneda("COP");
            pago1.setMetodoPago(TipoMetodoPago.TARJETA_CREDITO);
            pago1.setEstado(EstadoPago.COMPLETADO);
            pago1.setIdIntentoPagoStripe("pi_test_001");
            pago1.setFechaPago(LocalDateTime.now().minusDays(10));
            pagoRepo.save(pago1);

            // Pago fallido para usuario 3
            Pago pago2 = new Pago();
            pago2.setId(UUID.randomUUID().toString());
            pago2.setSuscripcion(sus3);
            pago2.setUsuarioId(usuario3Id);
            pago2.setMonto(new BigDecimal("29900.00"));
            pago2.setMoneda("COP");
            pago2.setMetodoPago(TipoMetodoPago.TARJETA_CREDITO);
            pago2.setEstado(EstadoPago.FALLIDO);
            pago2.setIdIntentoPagoStripe("pi_test_002");
            pagoRepo.save(pago2);

            log.info("Pagos creados");

            // ========== 5. CREAR LOGS ==========
            log.info("Creando logs de auditoría...");

            LogSuscripcion log1 = new LogSuscripcion();
            log1.setSuscripcionId(sus1.getId());
            log1.setUsuarioId(usuario1Id);
            log1.setAccion(AccionLog.CREACION);
            log1.setEstadoNuevo(EstadoSuscripcion.PENDIENTE_PAGO.name());
            log1.setDescripcion("Suscripción premium creada");
            log1.setRealizadoPor(usuario1Id);
            logRepo.save(log1);

            LogSuscripcion log2 = new LogSuscripcion();
            log2.setSuscripcionId(sus1.getId());
            log2.setUsuarioId(usuario1Id);
            log2.setAccion(AccionLog.PAGO_EXITOSO);
            log2.setEstadoAnterior(EstadoSuscripcion.PENDIENTE_PAGO.name());
            log2.setEstadoNuevo(EstadoSuscripcion.ACTIVA.name());
            log2.setDescripcion("Pago completado, suscripción activada");
            log2.setRealizadoPor("sistema");
            logRepo.save(log2);

            log.info("Logs creados");

            // ========== RESUMEN ==========
            log.info("DATOS DE PRUEBA CARGADOS EXITOSAMENTE");
            log.info("Planes: 2 (gratuito, premium)");
            log.info("Usuarios: 3 (user_premium_001, user_free_002, user_pending_003)");
            log.info("Métodos de Pago: 2");
            log.info("Pagos: 2 (1 exitoso, 1 fallido)");
            log.info("Logs: 2");
            log.info("Usa estos datos para probar en Postman:");
            log.info("Header: X-Usuario-Id: user_premium_001");
            log.info("Auth: usuario/usuario123 o admin/admin123");
        };
    }
}
