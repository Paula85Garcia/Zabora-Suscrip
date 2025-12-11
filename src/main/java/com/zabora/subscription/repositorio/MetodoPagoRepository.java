package com.zabora.subscription.repositorio;

import com.zabora.subscription.modelo.entidad.MetodoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MetodoPagoRepository extends JpaRepository<MetodoPago, String> {
	/**
     * Obtiene todos los métodos de pago activos de un usuario específico.
     * 
     * @param usuarioId ID del usuario
     * @return Lista de métodos de pago activos del usuario
     */
	List<MetodoPago> findByUsuarioIdAndActivoTrue(String usuarioId);
	 /**
     * Busca un método de pago usando su ID proporcionado por Stripe.
     * 
     * @param stripePaymentMethodId ID del método de pago en Stripe
     * @return Opcional con el método de pago si existe
     */
	Optional<MetodoPago> findByIdMetodoPagoStripe(String stripePaymentMethodId);
	/**
     * Obtiene el método de pago predeterminado de un usuario, si existe.
     * 
     * @param usuarioId ID del usuario
     * @return Opcional con el método de pago predeterminado del usuario
     */
	Optional<MetodoPago> findByUsuarioIdAndPredeterminadoTrue(String usuarioId);
}
