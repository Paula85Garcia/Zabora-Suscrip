package com.zabora.subscription.repositorio;

import com.zabora.subscription.modelo.entidad.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, String> {
	/**
     * Busca una factura asociada a un pago específico.
     * @param pagoId ID del pago
     * @return Opcional con la factura si existe, vacío si no
     */
	Optional<Factura> findByPagoId(String pagoId);
	/**
     * Obtiene todas las facturas de un usuario, ordenadas de la más reciente a la más antigua.
     */
	List<Factura> findByUsuarioIdOrderByFechaEmisionDesc(String usuarioId);
	/**
     * Busca una factura por su número único.
     * @param numeroFactura Número de la factura
     * @return Opcional con la factura si existe, vacío si no
     */
	Optional<Factura> findByNumeroFactura(String numeroFactura);
}