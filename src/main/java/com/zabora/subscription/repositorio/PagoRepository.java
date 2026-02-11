package com.zabora.subscription.repositorio;

import com.zabora.subscription.modelo.entidad.Pago;
import com.zabora.subscription.modelo.enumeracion.EstadoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para operaciones de la entidad Pago
 * Proporciona métodos CRUD y consultas personalizadas para la gestión de pagos
 */
@Repository
public interface PagoRepository extends JpaRepository<Pago, String> {
    
    /**
     * Buscar todos los pagos de un usuario específico
     * Ordenados por fecha de creación descendente (más recientes primero)
     */
    List<Pago> findByUsuarioIdOrderByFechaCreacionDesc(String usuarioId);
    
    /**
     * Buscar un pago por el ID del Payment Intent de Stripe
     * Utilizado para el procesamiento de webhooks y verificación de pagos
     */
    Optional<Pago> findByIdIntentoPagoStripe(String paymentIntentId);
    
    /**
     * Buscar todos los pagos por estado
     * Útil para reportes y monitoreo administrador 
     */
    List<Pago> findByEstado(EstadoPago estado);
    
    /**
     * Buscar todos los pagos de un usuario con un estado específico
     */
    List<Pago> findByUsuarioIdAndEstado(String usuarioId, EstadoPago estado);
    
    /**
     * Verificar si existe un pago para una suscripción específica con determinado estado
     */
    boolean existsBySuscripcionIdAndEstado(String suscripcionId, EstadoPago estado);
    
    /**
     * Calcular el ingreso total en un rango de fechas
     * Solo contabiliza pagos COMPLETADOS
     */
    @Query("SELECT SUM(p.monto) FROM Pago p WHERE p.estado = 'COMPLETADO' " +
           "AND p.fechaPago BETWEEN :startDate AND :endDate")
    Double calculateRevenueByDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Obtener estadísticas mensuales de ingresos
     */
    @Query("SELECT MONTH(p.fechaPago) as mes, YEAR(p.fechaPago) as anio, " +
           "SUM(p.monto) as total, COUNT(p) as cantidad " +
           "FROM Pago p WHERE p.estado = 'COMPLETADO' " +
           "AND p.fechaPago >= :startDate " +
           "GROUP BY YEAR(p.fechaPago), MONTH(p.fechaPago) " +
           "ORDER BY anio DESC, mes DESC")
    List<Object[]> getMonthlyRevenue(@Param("startDate") LocalDateTime startDate);
    
    /**
     * Contar pagos exitosos de un usuario
     */
    long countByUsuarioIdAndEstado(String usuarioId, EstadoPago estado);
    
    /**
     * Buscar pagos pendientes antiguos (más viejos que las horas especificadas)
     * Utilizado para identificar pagos atascados o abandonados
     */
    @Query("SELECT p FROM Pago p WHERE p.estado = 'PENDIENTE' " +
           "AND p.fechaCreacion < :cutoffDate")
    List<Pago> findStalePendingPayments(@Param("cutoffDate") LocalDateTime cutoffDate);
}
