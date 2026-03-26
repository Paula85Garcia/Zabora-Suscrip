package com.zabora.subscription.repositorio;

import com.zabora.subscription.modelo.entidad.UsuarioSuscripcion;
import com.zabora.subscription.modelo.enumeracion.EstadoSuscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para gestión de suscripciones de usuarios
 */
@Repository
public interface UsuarioSuscripcionRepositorio extends JpaRepository<UsuarioSuscripcion, String> {
    
    Optional<UsuarioSuscripcion> findByUsuarioIdAndEstado(Integer usuarioId, EstadoSuscripcion estado);
    List<UsuarioSuscripcion> findByUsuarioIdOrderByFechaCreacionDesc(Integer usuarioId);
    long countByEstado(EstadoSuscripcion estado);
    
    /**
     * Cuenta el número total de usuarios distintos que tienen suscripciones
     * @return número de usuarios únicos
     */
    @Query("SELECT COUNT(DISTINCT s.usuarioId) FROM UsuarioSuscripcion s")
    long countDistinctUsuarioId();
    
    /**
     * Cuenta suscripciones creadas después de una fecha específica
     * @param fecha fecha límite
     * @return número de suscripciones creadas después de la fecha
     */
    long countByFechaCreacionAfter(LocalDateTime fecha);
    
    /**
     * Cuenta suscripciones canceladas después de una fecha
     * @param fecha fecha límite
     * @return número de cancelaciones después de la fecha
     */
    long countByFechaCancelacionAfter(LocalDateTime fecha);
    
    long countByFechaCreacionBetween(LocalDateTime inicio, LocalDateTime fin);
    List<UsuarioSuscripcion> findByEstado(EstadoSuscripcion estado);
    long deleteByFechaCancelacionBeforeAndEstado(LocalDateTime fecha, EstadoSuscripcion estado);
    List<UsuarioSuscripcion> findByFechaCreacionBetween(LocalDateTime inicio, LocalDateTime fin);
    
    /**
     * Encuentra los usuarios más activos basado en pagos realizados
     * @return lista de usuarios con su actividad
     */
    @Query("SELECT u.usuarioId, u.plan.nombre, COUNT(p.id) as actividad " +
           "FROM UsuarioSuscripcion u " +
           "JOIN u.pagos p " +
           "WHERE u.estado = 'ACTIVA' " +
           "GROUP BY u.usuarioId, u.plan.nombre " +
           "ORDER BY actividad DESC")
    List<Object[]> findUsuariosMasActivos();
}
