package com.zabora.subscription.servicio;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.zabora.subscription.modelo.dto.RespuestaPagoDTO;
import com.zabora.subscription.modelo.dto.SolicitudPagoDTO;
import com.zabora.subscription.modelo.entidad.Pago;
import com.zabora.subscription.modelo.entidad.UsuarioSuscripcion;
import com.zabora.subscription.modelo.enumeracion.EstadoPago;
import com.zabora.subscription.modelo.enumeracion.TipoMetodoPago;
import com.zabora.subscription.repositorio.AuthClient;
import com.zabora.subscription.repositorio.PagoRepository;
import com.zabora.subscription.repositorio.UsuarioSuscripcionRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio de Pagos con integracion completa de Stripe Maneja creacion de
 * Payment Intents, procesamiento de webhooks y estados de pago
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PagoServicioReal {

	@Value("${stripe.clave.secreta}")
	private String stripeSecretKey;

	@Value("${stripe.clave.publica}")
	private String stripePublicKey;

	private final PagoRepository pagoRepository;
	private final UsuarioSuscripcionRepository suscripcionRepository;
	private final EmailService emailService;
	private final AuthClient authClient;

	@PostConstruct
	public void init() {
		Stripe.apiKey = stripeSecretKey;
		log.info("Stripe inicializado correctamente");
		log.info("Clave publica de Stripe configurada");
	}

	/**
	 * Crear un Payment Intent real en Stripe Retorna el client secret para
	 * integracion con frontend
	 */
	@Transactional
	public RespuestaPagoDTO crearIntentoPago(String usuarioId, SolicitudPagoDTO solicitud) {
		log.info("Creando Payment Intent para usuario: {}", usuarioId);

		// Buscar suscripcion
		Optional<UsuarioSuscripcion> suscripcionOpt = suscripcionRepository.findById(solicitud.getIdSuscripcion());

		if (suscripcionOpt.isEmpty()) {
			throw new RuntimeException("Suscripcion no encontrada: " + solicitud.getIdSuscripcion());
		}

		UsuarioSuscripcion suscripcion = suscripcionOpt.get();

		try {
			// Convertir monto a centavos
			long amountInCents = solicitud.getMonto().multiply(new BigDecimal("100")).longValue();

			// Construir parametros del Payment Intent
			PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
					.setAmount(amountInCents).setCurrency("cop").setDescription("Suscripcion Premium - Zabora")
					.putMetadata("suscripcion_id", solicitud.getIdSuscripcion()).putMetadata("usuario_id", usuarioId)
					.setAutomaticPaymentMethods(
							PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build());

			// Agregar metadata de factura si fue solicitada
			if (Boolean.TRUE.equals(solicitud.getRecibirFactura())) {
				paramsBuilder.putMetadata("enviar_factura", "true");
			}

			// Crear Payment Intent en Stripe
			PaymentIntentCreateParams params = paramsBuilder.build();
			PaymentIntent intent = PaymentIntent.create(params);

			log.info("Payment Intent creado: {}", intent.getId());

			// Guardar registro de pago en base de datos
			Pago pago = new Pago();
			pago.setId(UUID.randomUUID().toString());
			pago.setSuscripcion(suscripcion);
			pago.setUsuarioId(usuarioId);
			pago.setMonto(solicitud.getMonto());
			pago.setMoneda("COP");
			pago.setMetodoPago(mapearTipoPago(solicitud.getTipoPago()));
			pago.setEstado(EstadoPago.PENDIENTE);
			pago.setIdIntentoPagoStripe(intent.getId());
			pago.setFechaCreacion(LocalDateTime.now());

			pagoRepository.save(pago);

			// Preparar respuesta con client secret
			Map<String, Object> detalles = new HashMap<>();
			detalles.put("payment_intent_id", intent.getId());
			detalles.put("client_secret", intent.getClientSecret());
			detalles.put("status", intent.getStatus());
			detalles.put("public_key", stripePublicKey);

			return RespuestaPagoDTO.builder().exito(true)
					.mensaje("Payment Intent creado exitosamente - proceder con Stripe Elements").idPago(pago.getId())
					.estado("PENDIENTE").monto(solicitud.getMonto()).moneda("COP").fechaPago(LocalDateTime.now())
					.requiereConfirmacion(true).recibirFactura(solicitud.getRecibirFactura()).detalles(detalles)
					.build();

		} catch (StripeException e) {
			log.error("Error de Stripe: {} - {}", e.getCode(), e.getMessage());
			throw new RuntimeException("Error creando pago: " + e.getUserMessage());
		}
	}

	/**
	 * Manejar webhook de pago exitoso desde Stripe
	 */
	@Transactional
	public void handlePaymentSucceeded(PaymentIntent paymentIntent) {
		String intentId = paymentIntent.getId();
		log.info("Procesando pago exitoso - Intent ID: {}", intentId);

		// Buscar registro de pago
		Optional<Pago> pagoOpt = pagoRepository.findByIdIntentoPagoStripe(intentId);

		if (pagoOpt.isEmpty()) {
			log.error("Registro de pago no encontrado para intent: {}", intentId);
			return;
		}

		Pago pago = pagoOpt.get();

		// Verificacion de idempotencia - evitar procesamiento duplicado
		if (pago.getEstado() == EstadoPago.COMPLETADO) {
			log.warn("Pago ya procesado (idempotente): {}", intentId);
			return;
		}

		try {
			// Actualizar estado del pago
			pago.setEstado(EstadoPago.COMPLETADO);
			pago.setFechaPago(LocalDateTime.now());

			// Por ahora, guardaremos el ID del PaymentIntent como comprobante
			pago.setUrlComprobante("https://dashboard.stripe.com/payments/" + intentId);

			pagoRepository.save(pago);

			// Activar suscripcion
			UsuarioSuscripcion suscripcion = pago.getSuscripcion();
			activarSuscripcion(suscripcion);
			authClient.actualizarRolPremium(pago.getUsuarioId());
			// Enviar email de confirmacion
			emailService.enviarConfirmacionPago(pago);

			// Enviar factura si fue solicitada
			String enviarFactura = paymentIntent.getMetadata().get("enviar_factura");
			if ("true".equals(enviarFactura)) {
				emailService.enviarFacturaPago(pago);
			}

			log.info("Pago procesado exitosamente - Suscripcion activada: {}", suscripcion.getId());

		} catch (Exception e) {
			log.error("Error procesando pago exitoso: {}", e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Manejar webhook de pago fallido desde Stripe
	 */
	@Transactional
	public void handlePaymentFailed(PaymentIntent paymentIntent) {
		String intentId = paymentIntent.getId();
		log.warn("Procesando pago fallido - Intent ID: {}", intentId);

		Optional<Pago> pagoOpt = pagoRepository.findByIdIntentoPagoStripe(intentId);

		if (pagoOpt.isEmpty()) {
			log.error("Registro de pago no encontrado para intent: {}", intentId);
			return;
		}

		Pago pago = pagoOpt.get();
		pago.setEstado(EstadoPago.FALLIDO);
		pagoRepository.save(pago);

		// Enviar notificacion de fallo
		String failureMessage = paymentIntent.getLastPaymentError() != null
				? paymentIntent.getLastPaymentError().getMessage()
				: "Error desconocido";

		emailService.enviarNotificacionPagoFallido(pago.getUsuarioId(), failureMessage);

		log.info("Pago fallido procesado - Usuario notificado");
	}

	/**
	 * Obtener estado de pago desde Stripe
	 */
	public Map<String, Object> obtenerEstadoPago(String paymentIntentId) {
		try {
			PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);

			Map<String, Object> resultado = new HashMap<>();
			resultado.put("id", intent.getId());
			resultado.put("status", intent.getStatus());
			resultado.put("amount", intent.getAmount());
			resultado.put("currency", intent.getCurrency());
			resultado.put("created", intent.getCreated());

			// Agregar registro local de pago si existe
			pagoRepository.findByIdIntentoPagoStripe(paymentIntentId).ifPresent(pago -> {
				resultado.put("local_status", pago.getEstado().name());
				resultado.put("local_id", pago.getId());
			});

			return resultado;

		} catch (StripeException e) {
			log.error("Error obteniendo estado de pago: {}", e.getMessage());
			throw new RuntimeException("Error consultando pago: " + e.getUserMessage());
		}
	}

	/**
	 * Obtener historial de pagos de un usuario
	 */
	public List<Map<String, Object>> obtenerHistorialPagos(String usuarioId) {
		return pagoRepository.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId).stream().map(pago -> {
			Map<String, Object> pagoMap = new HashMap<>();
			pagoMap.put("id", pago.getId());
			pagoMap.put("monto", pago.getMonto());
			pagoMap.put("estado", pago.getEstado().name());
			pagoMap.put("fecha", pago.getFechaPago() != null ? pago.getFechaPago() : pago.getFechaCreacion());
			pagoMap.put("metodo_pago", pago.getMetodoPago().name());
			pagoMap.put("url_comprobante", pago.getUrlComprobante() != null ? pago.getUrlComprobante() : "");
			return pagoMap;
		}).collect(Collectors.toList());
	}

	/**
	 * Activar suscripcion despues de pago exitoso
	 */
	private void activarSuscripcion(UsuarioSuscripcion suscripcion) {
		LocalDateTime now = LocalDateTime.now();
		suscripcion.setEstado(com.zabora.subscription.modelo.enumeracion.EstadoSuscripcion.ACTIVA);
		suscripcion.setInicioPeriodoActual(now);
		suscripcion.setFinPeriodoActual(now.plusDays(30)); // Suscripcion de 30 dias
		suscripcion.setFechaActualizacion(now);
		suscripcionRepository.save(suscripcion);
	}

	/**
	 * Mapear tipo de pago string a enum
	 */
	private TipoMetodoPago mapearTipoPago(String tipo) {
		return switch (tipo.toLowerCase()) {
		case "tarjeta_credito", "card", "tarjeta" -> TipoMetodoPago.TARJETA_CREDITO;
		case "pse" -> TipoMetodoPago.PSE;
		default -> throw new IllegalArgumentException("Tipo de pago no soportado: " + tipo);
		};
	}
}