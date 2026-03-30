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
 * Datos para crear un pago PSE vía API de Mercado Pago (sin token de tarjeta).
 * externalReference = id de {@code UsuarioSuscripcion} en MySQL.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BricksPsePaymentDTO {

    @NotBlank(message = "La referencia externa (suscripcion ID) es obligatoria")
    private String externalReference;

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser positivo")
    private BigDecimal transactionAmount;

    @NotBlank(message = "El email del pagador es obligatorio")
    @Email(message = "El email no es valido")
    private String payerEmail;

    @NotBlank(message = "La institucion financiera es obligatoria")
    private String financialInstitution;

    @NotBlank(message = "El tipo de persona (entity_type) es obligatorio")
    private String entityType;

    @NotBlank(message = "El tipo de documento es obligatorio")
    private String identificationType;

    @NotBlank(message = "El numero de documento es obligatorio")
    private String identificationNumber;

    @NotBlank(message = "El nombre del pagador es obligatorio")
    private String firstName;

    @NotBlank(message = "El apellido del pagador es obligatorio")
    private String lastName;

    private String zipCode;
    private String streetName;
    private String streetNumber;
    private String neighborhood;
    private String city;
    private String federalUnit;
    private String phoneAreaCode;
    private String phoneNumber;

    private String description;

    /** Si el usuario solicita recibir comprobante/factura por correo (persistido en el pago). */
    private Boolean recibirFactura;
}
