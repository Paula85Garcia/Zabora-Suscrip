package com.zabora.subscription.repositorio;

import com.zabora.subscription.modelo.entidad.Pago;
import com.zabora.subscription.modelo.enumeracion.EstadoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PagoRepository extends JpaRepository<Pago, String> {
    
    // Historial de pagos de un usuario
    List<Pago> findByUsuarioIdOrderByFechaCreacionDesc(String usuarioId);
    
    // Pagos de una suscripción específica
    List<Pago> findBySuscripcionId(String suscripcionId);
    
    // Buscar por ID de Stripe
    Optional<Pago> findByIdIntentoPagoStripe(String stripePaymentIntentId);
    
    // Calcular ingresos totales en un periodo
    @Query("SELECT SUM(p.monto) FROM Pago p WHERE " +
           "p.estado = 'COMPLETADO' AND " +
           "p.fechaPago BETWEEN :fechaInicio AND :fechaFin")
    BigDecimal calcularIngresosPorPeriodo(
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin
    );
    
    // Contar pagos por estado en un periodo
    @Query("SELECT COUNT(p) FROM Pago p WHERE " +
           "p.estado = :estado AND " +
           "p.fechaCreacion BETWEEN :fechaInicio AND :fechaFin")
    Long contarPagosPorEstado(
        @Param("estado") EstadoPago estado,
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin
    );
}