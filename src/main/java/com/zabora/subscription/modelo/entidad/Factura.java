package com.zabora.subscription.modelo.entidad;

import com.zabora.subscription.modelo.enumeracion.EstadoFactura;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
/**
 * Entidad que representa una factura en el sistema.
 * 
 * Contiene información sobre el pago asociado, montos, fechas, estado y documentos relacionados.
 */
@Entity
@Table(name = "facturas")
@Data
public class Factura {
    //Identificador único de la factura (UUID).
    @Id
    @Column(name = "id", length = 36)
    private String id;
    /**
     * Pago asociado a esta factura.
     * Relación Many-to-One: muchas facturas pueden estar asociadas a un pago.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pago_id", nullable = false)
    private Pago pago;
    
    //ID del usuario dueño de la factura.
    @Column(name = "usuario_id", nullable = false, length = 36)
    private String usuarioId;
    
    
    //Prefijo de la factura, usado en la numeración.
    @Column(name = "prefijo", length = 10, nullable = false)
    private String prefijo = "FZ";
    
    
    //Número consecutivo de la factura dentro del prefijo.
    @Column(name = "consecutivo", nullable = false)
    private Long consecutivo;
    
    
    //Número completo de la factura (prefijo + consecutivo), generado automáticamente en BD.
    @Column(name = "numero_factura", length = 50, unique = true, insertable = false, updatable = false)
    private String numeroFactura; // GENERATED column
    
    //Fecha en la que se emite la factura.
    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;
    
    
    //Fecha de vencimiento de la factura
    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;
    
    
    //Subtotal de la factura (sin IVA)
    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;
    
    
    //Valor del IVA incluido en la factura.
    @Column(name = "iva", nullable = false, precision = 10, scale = 2)
    private BigDecimal iva = BigDecimal.ZERO;
    
    
    //Total de la factura (subtotal + IVA).
    @Column(name = "total", nullable = false, precision = 10, scale = 2)
    private BigDecimal total;
    
    
    //Estado actual de la factura (BORRADOR, EMITIDA, PAGADA, ANULADA).
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoFactura estado = EstadoFactura.BORRADOR;
    
    
    //Código Único de Factura Electrónica (CUFE).
    @Column(name = "cufe", length = 200)
    private String cufe;
    
    //Respuesta de la DIAN en formato JSON.
    @Column(name = "respuesta_dian", columnDefinition = "JSON")
    private String respuestaDian;
    
    
    //URL del PDF de la factura.Opcional segun tiempo de desarrollo
    @Column(name = "pdf_url", length = 500)
    private String pdfUrl;
    
    
    //URL del XML de la factura.Opcional segun tiempo de desarrollo
    @Column(name = "xml_url", length = 500)
    private String xmlUrl;
    
    //Fecha y hora de creación de la factura
    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion = LocalDateTime.now();
    
    
    //Fecha y hora de última actualización de la factura.
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion = LocalDateTime.now();
    
  //Acción ejecutada antes de insertar la factura en la base de datos.
    //Se asegura de asignar un ID si no existe y actualizar las fechas.
    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
    }
    //Acción ejecutada antes de actualizar la factura en la base de datos.
    //Actualiza la fecha de actualización.
    @PreUpdate
    public void preUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}