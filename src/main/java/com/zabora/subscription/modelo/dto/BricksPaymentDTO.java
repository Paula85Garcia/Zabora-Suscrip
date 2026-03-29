package com.zabora.subscription.modelo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO que recibe el frontend después de que el Brick captura los datos de tarjeta.
 *
 * externalReference = ID de la suscripcion en MySQL (UsuarioSuscripcion.id).
 * MercadoPago lo almacena en el pago y lo devuelve en el webhook, por eso es
 * el nexo entre el evento de pago y la suscripcion local.
 *
 * NO incluye back_urls ni preference_id (esos son del flujo viejo de Checkout Pro).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BricksPaymentDTO {

    /** Token de tarjeta generado por el SDK de MercadoPago en el frontend. */
    @NotBlank(message = "El token de tarjeta es obligatorio")
    private String token;

    /** Identificador del medio de pago. Ejemplos: "visa", "master", "amex" */
    @NotBlank(message = "El payment_method_id es obligatorio")
    private String paymentMethodId;

    /**
     * ID del banco emisor. Puede ser null para algunos metodos;
     * el Brick lo devuelve y hay que reenviarlo tal cual.
     */
    private String issuerId;

    /** Numero de cuotas seleccionadas por el usuario. */
    @NotNull(message = "Las cuotas son obligatorias")
    @Positive(message = "Las cuotas deben ser mayor a 0")
    private Integer installments;

    /** Email del pagador (requerido por MercadoPago). */
    @NotBlank(message = "El email del pagador es obligatorio")
    @Email(message = "El email no es valido")
    private String payerEmail;

    /**
     * ID de la suscripcion en MySQL.
     * Se envia a MP como external_reference y vuelve en el webhook.
     */
    @NotBlank(message = "La referencia externa (suscripcion ID) es obligatoria")
    private String externalReference;

    /** Monto a cobrar. Debe coincidir con el precio del plan. */
    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser positivo")
    private BigDecimal transactionAmount;

    /** Descripcion que aparece en el resumen del banco del comprador. */
    private String description;
}
