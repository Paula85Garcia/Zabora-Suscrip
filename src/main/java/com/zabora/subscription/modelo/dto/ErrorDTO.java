package com.zabora.subscription.modelo.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * DTO (Data Transfer Object) utilizado para representar la información de un error en la API.
 * Se utiliza para enviar al cliente detalles del error ocurrido en una petición.
 */
@Data
@Builder
public class ErrorDTO {
	//Fecha y hora en que ocurrió el error.
    private LocalDateTime timestamp;
    //Mensaje principal del error.
    private String mensaje;
    //Detalle adicional del error, como la causa o la excepción.
    private String detalle;
    //Ruta de la petición HTTP que generó el error.
    private String ruta;
    //Código de error interno o de negocio para identificar el tipo de error.
    private String codigoError;
}