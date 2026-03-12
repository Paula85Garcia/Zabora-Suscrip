package com.zabora.subscription.repositorio;

import com.zabora.subscription.modelo.entidad.UsuarioSuscripcion;
import com.zabora.subscription.modelo.enumeracion.EstadoSuscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para operaciones de la entidad UsuarioSuscripcion
 * Gestiona todas las operaciones de base de datos relacionadas con las suscripciones de usuarios
 */
@Repository
public interface UsuarioSuscripcionRepositorio extends JpaRepository<UsuarioSuscripcion, String> {
    
    /**
     * Buscar la suscripción activa de un usuario
     * Un usuario solo puede tener una suscripción activa a la vez
     */
    Optional<UsuarioSuscripcion> findByUsuarioIdAndEstado(Integer usuarioId, EstadoSuscripcion estado);
    
    /**
     * Buscar la suscripción más reciente de un usuario
     * Útil para consultar el historial de suscripciones
     */
    Optional<UsuarioSuscripcion> findTopByUsuarioIdOrderByFechaCreacionDesc(Integer usuarioId);
    
    /**
     * Buscar todas las suscripciones de un usuario
     * Ordenadas por fecha de creación descendente
     */
    List<UsuarioSuscripcion> findByUsuarioIdOrderByFechaCreacionDesc(Integer usuarioId);
    
    /**
     * Buscar suscripciones que están próximas a expirar
     * Utilizado para enviar recordatorios de renovación
     */
    @Query("SELECT s FROM UsuarioSuscripcion s WHERE s.estado = 'ACTIVA' " +
           "AND s.finPeriodoActual BETWEEN :now AND :expirationDate " +
           "AND s.cancelarAlFinalPeriodo = false")
    List<UsuarioSuscripcion> findExpiringSubscriptions(
        @Param("now") LocalDateTime now,
        @Param("expirationDate") LocalDateTime expirationDate
    );
    
    /**
     * Buscar suscripciones que deben cancelarse
     * Verifica suscripciones con cancelarAlFinalPeriodo = true y periodo finalizado
     */
    @Query("SELECT s FROM UsuarioSuscripcion s WHERE s.cancelarAlFinalPeriodo = true " +
           "AND s.finPeriodoActual <= :now AND s.estado = 'ACTIVA'")
    List<UsuarioSuscripcion> findSubscriptionsToCancel(@Param("now") LocalDateTime now);
    
    /**
     * Buscar suscripciones expiradas que necesitan actualización de estado
     */
    @Query("SELECT s FROM UsuarioSuscripcion s WHERE s.estado = 'ACTIVA' " +
           "AND s.finPeriodoActual < :now")
    List<UsuarioSuscripcion> findExpiredSubscriptions(@Param("now") LocalDateTime now);
    
    /**
     * Contar suscripciones activas por plan
     * Utilizado para estadísticas y reportes
     */
    @Query("SELECT s.plan.nombre, COUNT(s) FROM UsuarioSuscripcion s " +
           "WHERE s.estado = 'ACTIVA' GROUP BY s.plan.nombre")
    List<Object[]> countActiveSubscriptionsByPlan();
    
    /**
     * Verificar si el usuario tiene alguna suscripción activa
     */
    boolean existsByUsuarioIdAndEstado(Integer usuarioId, EstadoSuscripcion estado);
    
    /**
     * Buscar por ID de suscripción
     */
    Optional<UsuarioSuscripcion> findByIdSuscripcion(String idSuscripcion);
    
    /**
     * Buscar suscripciones creadas dentro de un rango de fechas
     */
    List<UsuarioSuscripcion> findByFechaCreacionBetween(
        LocalDateTime startDate, 
        LocalDateTime endDate
    );
}
