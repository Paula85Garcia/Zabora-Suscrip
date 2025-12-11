package com.zabora.subscription.repositorio;

import com.zabora.subscription.modelo.entidad.LogSuscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LogSuscripcionRepository extends JpaRepository<LogSuscripcion, Long> {
	/**
     * Obtiene todos los registros de un log asociados a una suscripción específica,
     * ordenados de más reciente a más antiguo según la fecha de creación.
     * 
     * @param suscripcionId ID de la suscripción
     * @return Lista de logs de la suscripción ordenada por fecha descendente
     */
	List<LogSuscripcion> findBySuscripcionIdOrderByFechaCreacionDesc(String suscripcionId);
	/**
     * Obtiene todos los registros de un log asociados a un usuario específico,
     * ordenados de más reciente a más antiguo según la fecha de creación.
     * 
     * @param usuarioId ID del usuario
     * @return Lista de logs del usuario ordenada por fecha descendente
     */
	List<LogSuscripcion> findByUsuarioIdOrderByFechaCreacionDesc(String usuarioId);
}