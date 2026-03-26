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

/**
 * Repositorio para operaciones de la entidad Pago
 * Gestiona todas las operaciones de base de datos relacionadas con los pagos
 */
@Repository
public interface PagoRepositorio extends JpaRepository<Pago, String> {
    
    List<Pago> findByUsuarioIdOrderByFechaCreacionDesc(Integer usuarioId);
    
    Optional<Pago> findByIdIntentoPago(String idIntentoPago);
    
    Optional<Pago> findBySuscripcionIdAndEstado(String suscripcionId, EstadoPago estado);
    
    boolean existsBySuscripcionIdAndEstado(String suscripcionId, EstadoPago estado);
    
    List<Pago> findByEstado(EstadoPago estado);
    
    List<Pago> findByUsuarioIdAndEstado(Integer usuarioId, EstadoPago estado);
    
    long countByUsuarioIdAndEstado(Integer usuarioId, EstadoPago estado);

    long countByEstado(EstadoPago estado);
    
    List<Pago> findBySuscripcionIdOrderByFechaCreacionDesc(String suscripcionId);
    
    /**
     * Sumar ingresos por estado
     */
    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p WHERE p.estado = :estado")
    BigDecimal sumarIngresosPorEstado(@Param("estado") EstadoPago estado);
    
    /**
     * Sumar ingresos desde una fecha
     */
    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p WHERE p.fechaCreacion >= :fecha")
    BigDecimal sumarIngresosDesde(@Param("fecha") LocalDateTime fecha);
    
    /**
     * Sumar ingresos en un rango de fechas
     */
    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p WHERE p.fechaCreacion BETWEEN :inicio AND :fin")
    BigDecimal sumarIngresosEntre(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
    
    /**
     * Contar pagos por método de pago
     */
    @Query("SELECT p.metodoPago, COUNT(p.id) FROM Pago p GROUP BY p.metodoPago")
    List<Object[]> countPagosPorMetodo();
    
    /**
     * Promedio de monto por estado
     */
    @Query("SELECT AVG(p.monto) FROM Pago p WHERE p.estado = :estado")
    Double averageMontoByEstado(@Param("estado") EstadoPago estado);
    
    /**
     * Contar pagos en un rango de fechas
     */
    long countByFechaCreacionBetween(LocalDateTime inicio, LocalDateTime fin);
    
    /**
     * Encontrar pagos recientes ordenados por fecha descendente
     */
    List<Pago> findTop50ByOrderByFechaCreacionDesc();
    
    /**
     * Eliminar pagos antiguos por estado
     */
    long deleteByFechaCreacionBeforeAndEstado(LocalDateTime fecha, EstadoPago estado);
}