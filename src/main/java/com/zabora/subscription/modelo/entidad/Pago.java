package com.zabora.subscription.modelo.entidad;

import com.zabora.subscription.modelo.enumeracion.EstadoPago;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad que representa un pago realizado por un usuario
 */
@Entity
@Table(name = "pagos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pago {

    @Id
    @Column(name = "id", length = 100)
    private String id;

    @Column(name = "suscripcion_id", length = 100, nullable = false)
    private String suscripcionId;

    @Column(name = "usuario_id", length = 100, nullable = false)
    private Integer usuarioId;

    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "moneda", length = 3, nullable = false)
    private String moneda = "COP";

    @Column(name = "metodo_pago", nullable = false)
    private String metodoPago;  // "TARJETA_CREDITO" o "PSE"

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoPago estado = EstadoPago.PENDIENTE;

    @Column(name = "id_intento_pago", unique = true, length = 255)
    private String idIntentoPago;  // Preference ID de MercadoPago

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @Column(name = "url_comprobante", length = 500)
    private String urlComprobante;

    @Column(name = "codigo_autorizacion", length = 50)
    private String codigoAutorizacion;

    @Column(name = "estado_pse", length = 50)
    private String estadoPse;

    @Column(name = "referencia_pse", length = 100)
    private String referenciaPse;

    @Column(name = "metadatos", columnDefinition = "LONGTEXT")
    private String metadatos;

    @Column(name = "motivo_fallo", length = 500)
    private String motivoFallo;

    @Column(name = "tipo_documento", length = 20)
    private String tipoDocumento;

    @Column(name = "numero_documento", length = 50)
    private String numeroDocumento;

    @Column(name = "banco", length = 50)
    private String banco;

    @Column(name = "tipo_persona", length = 20)
    private String tipoPersona;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    // Getters y Setters manuales
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSuscripcionId() { return suscripcionId; }
    public void setSuscripcionId(String suscripcionId) { this.suscripcionId = suscripcionId; }

    public Integer getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Integer usuarioId) { this.usuarioId = usuarioId; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public EstadoPago getEstado() { return estado; }
    public void setEstado(EstadoPago estado) { this.estado = estado; }

    public String getIdIntentoPago() { return idIntentoPago; }
    public void setIdIntentoPago(String idIntentoPago) { this.idIntentoPago = idIntentoPago; }

    public LocalDateTime getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDateTime fechaPago) { this.fechaPago = fechaPago; }

    public String getUrlComprobante() { return urlComprobante; }
    public void setUrlComprobante(String urlComprobante) { this.urlComprobante = urlComprobante; }

    public String getCodigoAutorizacion() { return codigoAutorizacion; }
    public void setCodigoAutorizacion(String codigoAutorizacion) { this.codigoAutorizacion = codigoAutorizacion; }

    public String getEstadoPse() { return estadoPse; }
    public void setEstadoPse(String estadoPse) { this.estadoPse = estadoPse; }

    public String getReferenciaPse() { return referenciaPse; }
    public void setReferenciaPse(String referenciaPse) { this.referenciaPse = referenciaPse; }

    public String getMetadatos() { return metadatos; }
    public void setMetadatos(String metadatos) { this.metadatos = metadatos; }

    public String getMotivoFallo() { return motivoFallo; }
    public void setMotivoFallo(String motivoFallo) { this.motivoFallo = motivoFallo; }

    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }

    public String getNumeroDocumento() { return numeroDocumento; }
    public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }

    public String getBanco() { return banco; }
    public void setBanco(String banco) { this.banco = banco; }

    public String getTipoPersona() { return tipoPersona; }
    public void setTipoPersona(String tipoPersona) { this.tipoPersona = tipoPersona; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}