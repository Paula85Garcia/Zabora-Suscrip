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

@Repository
public interface UsuarioSuscripcionRepository extends JpaRepository<UsuarioSuscripcion, String> {
    
    // Buscar suscripción activa de un usuario
    Optional<UsuarioSuscripcion> findByUsuarioIdAndEstado(
        String usuarioId, 
        EstadoSuscripcion estado
    );
    
    // Todas las suscripciones de un usuario
    List<UsuarioSuscripcion> findByUsuarioId(String usuarioId);
    
    // Suscripciones que expiran pronto (para notificaciones)
    @Query("SELECT s FROM UsuarioSuscripcion s WHERE " +
           "s.estado = 'ACTIVA' AND " +
           "s.finPeriodoActual BETWEEN :fechaInicio AND :fechaFin")
    List<UsuarioSuscripcion> findSuscripcionesPorExpirar(
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin
    );
    
    // Contar suscripciones premium activas (para reportes)
    @Query("SELECT COUNT(s) FROM UsuarioSuscripcion s WHERE " +
           "s.estado = 'ACTIVA' AND s.plan.nombre = 'premium'")
    Long contarSuscripcionesPremiumActivas();
}