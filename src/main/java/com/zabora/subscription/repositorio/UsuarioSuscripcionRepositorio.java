package com.zabora.subscription.repositorio;

import com.zabora.subscription.modelo.entidad.UsuarioSuscripcion;
import com.zabora.subscription.modelo.enumeracion.EstadoSuscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioSuscripcionRepositorio extends JpaRepository<UsuarioSuscripcion, String> {

    Optional<UsuarioSuscripcion> findByUsuarioIdAndEstado(Integer usuarioId, EstadoSuscripcion estado);

    List<UsuarioSuscripcion> findByUsuarioIdOrderByFechaCreacionDesc(Integer usuarioId);

    long countByEstado(EstadoSuscripcion estado);

    @Query("SELECT COUNT(DISTINCT s.usuarioId) FROM UsuarioSuscripcion s")
    long countDistinctUsuarioId();

    long countByFechaCreacionAfter(LocalDateTime fecha);

    long countByFechaCancelacionAfter(LocalDateTime fecha);

    long countByFechaCreacionBetween(LocalDateTime inicio, LocalDateTime fin);

    List<UsuarioSuscripcion> findByEstado(EstadoSuscripcion estado);

    long deleteByFechaCancelacionBeforeAndEstado(LocalDateTime fecha, EstadoSuscripcion estado);

    List<UsuarioSuscripcion> findByFechaCreacionBetween(LocalDateTime inicio, LocalDateTime fin);

    // FEAT-1: Para el job de expiracion automatica
    List<UsuarioSuscripcion> findByEstadoAndFinPeriodoActualBefore(
            EstadoSuscripcion estado, LocalDateTime fecha);

    // FEAT-2: Para cancelaciones diferidas al final del periodo
    List<UsuarioSuscripcion> findByCancelarAlFinalPeriodoTrueAndEstadoAndFinPeriodoActualBefore(
            EstadoSuscripcion estado, LocalDateTime fecha);

    @Query("SELECT u.usuarioId, u.plan.nombre, COUNT(p.id) as actividad " +
           "FROM UsuarioSuscripcion u " +
           "JOIN u.pagos p " +
           "WHERE u.estado = 'ACTIVA' " +
           "GROUP BY u.usuarioId, u.plan.nombre " +
           "ORDER BY actividad DESC")
    List<Object[]> findUsuariosMasActivos();
}
