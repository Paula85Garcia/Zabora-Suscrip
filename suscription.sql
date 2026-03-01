DROP DATABASE IF EXISTS zabora_subscriptions;
CREATE DATABASE zabora_subscriptions CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE zabora_subscriptions;


-- TABLA: PLANES DE SUSCRIPCIÓN
CREATE TABLE planes_suscripcion (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    descripcion TEXT,
    precio DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    moneda VARCHAR(3) DEFAULT 'COP',
    limite_condiciones_medicas INT NOT NULL DEFAULT 0,
    limite_alergias INT NOT NULL DEFAULT 0,
    limite_preferencias_alimentarias INT NOT NULL DEFAULT 0,
    ingredientes_por_busqueda INT NOT NULL DEFAULT 0,
    limite_recetas_favoritas INT NULL,
    activo BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- TABLA: SUSCRIPCIONES DE USUARIOS 
CREATE TABLE suscripciones_usuarios (
    id VARCHAR(100) PRIMARY KEY,
    usuario_id VARCHAR(100) NOT NULL,
    plan_id INT NOT NULL,
    estado ENUM('ACTIVA', 'CANCELADA', 'EXPIRADA', 'PENDIENTE_PAGO', 'SIN_SUSCRIPCION') DEFAULT 'PENDIENTE_PAGO',
    inicio_periodo_actual DATETIME NULL,
    fin_periodo_actual DATETIME NULL,
    cancelar_al_final_periodo BOOLEAN DEFAULT FALSE,
    fecha_cancelacion DATETIME NULL,
    id_cliente_mercadopago VARCHAR(255) NULL,
    id_suscripcion_mercadopago VARCHAR(255) NULL,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (plan_id) REFERENCES planes_suscripcion(id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- TABLA: MÉTODOS DE PAGO
CREATE TABLE metodos_pago (
    id VARCHAR(100) PRIMARY KEY,
    usuario_id VARCHAR(100) NOT NULL,
    tipo ENUM('TARJETA_CREDITO', 'PSE', 'EFECTIVO', 'TRANSFERENCIA') NOT NULL,
    ultimos_cuatro VARCHAR(4) NULL,
    marca VARCHAR(50) NULL,
    expira_mes INT NULL,
    expira_anio INT NULL,
    banco VARCHAR(100) NULL,
    tipo_cuenta ENUM('ahorros', 'corriente') NULL,
    referencia_pse VARCHAR(100) NULL,
    id_metodo_pago_mercadopago VARCHAR(255) NULL UNIQUE,
    predeterminado BOOLEAN DEFAULT FALSE,
    activo BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- TABLA: PAGOS 
CREATE TABLE pagos (
    id VARCHAR(100) PRIMARY KEY,
    suscripcion_id VARCHAR(100) NOT NULL,
    usuario_id VARCHAR(100) NOT NULL,
    monto DECIMAL(10,2) NOT NULL,
    moneda VARCHAR(3) DEFAULT 'COP',
    metodo_pago VARCHAR(50) NOT NULL,
    estado ENUM('PENDIENTE', 'COMPLETADO', 'FALLIDO', 'REEMBOLSADO', 'CANCELADO') DEFAULT 'PENDIENTE',
    id_preference_mercadopago VARCHAR(255) NULL UNIQUE,
    id_pago_mercadopago VARCHAR(255) NULL,
    fecha_pago DATETIME NULL,
    url_comprobante VARCHAR(500) NULL,
    codigo_autorizacion VARCHAR(50) NULL,
    estado_pse VARCHAR(50) NULL,
    referencia_pse VARCHAR(100) NULL,
    metadatos LONGTEXT NULL,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (suscripcion_id) REFERENCES suscripciones_usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- TABLA: SECUENCIA DE FACTURAS
CREATE TABLE secuencia_facturas (
    id INT PRIMARY KEY DEFAULT 1,
    consecutivo BIGINT NOT NULL DEFAULT 1000
);

INSERT INTO secuencia_facturas (id, consecutivo) VALUES (1, 1000);


-- TABLA: FACTURAS
CREATE TABLE facturas (
    id VARCHAR(100) PRIMARY KEY,
    pago_id VARCHAR(100) NOT NULL,
    usuario_id VARCHAR(100) NOT NULL,
    prefijo VARCHAR(10) NOT NULL DEFAULT 'FZ',
    consecutivo BIGINT NOT NULL,
    numero_factura VARCHAR(50) AS (CONCAT(prefijo, '-', consecutivo)) STORED,
    fecha_emision DATE NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    iva DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total DECIMAL(10,2) NOT NULL,
    estado ENUM('BORRADOR', 'EMITIDA', 'PAGADA', 'ANULADA') DEFAULT 'BORRADOR',
    cufe VARCHAR(200) NULL,
    respuesta_dian LONGTEXT NULL,
    pdf_url VARCHAR(500) NULL,
    xml_url VARCHAR(500) NULL,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE (numero_factura),
    FOREIGN KEY (pago_id) REFERENCES pagos(id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- TABLA: LOGS DE SUSCRIPCIONES
CREATE TABLE logs_suscripciones (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    suscripcion_id VARCHAR(100) NOT NULL,
    usuario_id VARCHAR(100) NOT NULL,
    accion ENUM('CREACION', 'ACTIVACION', 'CANCELACION', 'RENOVACION', 'PAGO_EXITOSO', 'PAGO_FALLIDO', 'CAMBIO_PLAN', 'REEMBOLSO', 'CAMBIO_ESTADO') NOT NULL,
    estado_anterior VARCHAR(50) NULL,
    estado_nuevo VARCHAR(50) NULL,
    descripcion TEXT NULL,
    realizado_por VARCHAR(100) NOT NULL,
    ip_address VARCHAR(45) NULL,
    user_agent TEXT NULL,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (suscripcion_id) REFERENCES suscripciones_usuarios(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- TABLA: REPORTES DE INGRESOS
CREATE TABLE reportes_ingresos (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    tipo_reporte ENUM('DIARIO', 'SEMANAL', 'MENSUAL', 'ANUAL', 'PERSONALIZADO') NOT NULL,
    total_ingresos DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    total_pagos INT NOT NULL DEFAULT 0,
    pagos_exitosos INT NOT NULL DEFAULT 0,
    pagos_fallidos INT NOT NULL DEFAULT 0,
    suscripciones_gratuitas INT NOT NULL DEFAULT 0,
    suscripciones_premium INT NOT NULL DEFAULT 0,
    conversion_rate DECIMAL(5,2) NULL,
    pagos_tarjeta INT NOT NULL DEFAULT 0,
    pagos_pse INT NOT NULL DEFAULT 0,
    datos_reportes LONGTEXT NOT NULL,
    pdf_url VARCHAR(500) NULL,
    generado_por VARCHAR(100) NULL,
    fecha_generacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- DATOS INICIALES
INSERT INTO planes_suscripcion (nombre, descripcion, precio, moneda, limite_condiciones_medicas, limite_alergias, limite_preferencias_alimentarias, ingredientes_por_busqueda, limite_recetas_favoritas, activo)
VALUES 
('gratuito', 'Plan gratuito con caracteristicas basicas', 0.00, 'COP', 2, 2, 2, 7, 4, TRUE),
('premium', 'Plan premium con todas las caracteristicas', 29900.00, 'COP', 3, 4, 4, 20, NULL, TRUE);


-- VISTAS
CREATE OR REPLACE VIEW vista_suscripciones_activas AS
SELECT 
    su.id AS suscripcion_id,
    su.usuario_id,
    ps.nombre AS plan_nombre,
    ps.precio AS plan_precio,
    su.estado AS estado_suscripcion,
    su.inicio_periodo_actual,
    su.fin_periodo_actual,
    DATEDIFF(su.fin_periodo_actual, CURDATE()) AS dias_restantes,
    ps.limite_condiciones_medicas,
    ps.limite_alergias,
    ps.limite_preferencias_alimentarias,
    ps.ingredientes_por_busqueda,
    ps.limite_recetas_favoritas
FROM suscripciones_usuarios su
JOIN planes_suscripcion ps ON su.plan_id = ps.id
WHERE su.estado = 'ACTIVA' AND (su.fin_periodo_actual IS NULL OR su.fin_periodo_actual > NOW());

CREATE OR REPLACE VIEW vista_ingresos_mensuales AS
SELECT 
    DATE_FORMAT(p.fecha_pago, '%Y-%m') AS mes,
    COUNT(p.id) AS total_pagos,
    SUM(CASE WHEN p.estado = 'COMPLETADO' THEN p.monto ELSE 0 END) AS ingresos_totales,
    SUM(CASE WHEN p.metodo_pago LIKE '%TARJETA%' THEN 1 ELSE 0 END) AS pagos_tarjeta,
    SUM(CASE WHEN p.metodo_pago = 'PSE' THEN 1 ELSE 0 END) AS pagos_pse
FROM pagos p
WHERE p.estado = 'COMPLETADO' AND p.fecha_pago IS NOT NULL
GROUP BY DATE_FORMAT(p.fecha_pago, '%Y-%m')
ORDER BY mes DESC;


-- TRIGGERS

DELIMITER $$

CREATE TRIGGER trg_log_cambio_suscripcion
AFTER UPDATE ON suscripciones_usuarios
FOR EACH ROW
BEGIN
    IF OLD.estado != NEW.estado THEN
        INSERT INTO logs_suscripciones (suscripcion_id, usuario_id, accion, estado_anterior, estado_nuevo, realizado_por)
        VALUES (NEW.id, NEW.usuario_id, 'CAMBIO_ESTADO', OLD.estado, NEW.estado, 'sistema');
    END IF;
END$$

CREATE TRIGGER trg_log_creacion_suscripcion
AFTER INSERT ON suscripciones_usuarios
FOR EACH ROW
BEGIN
    INSERT INTO logs_suscripciones (suscripcion_id, usuario_id, accion, estado_nuevo, realizado_por)
    VALUES (NEW.id, NEW.usuario_id, 'CREACION', NEW.estado, NEW.usuario_id);
END$$

CREATE TRIGGER trg_actualizar_fecha_pago
BEFORE UPDATE ON pagos
FOR EACH ROW
BEGIN
    IF OLD.estado != NEW.estado AND NEW.estado = 'COMPLETADO' THEN
        SET NEW.fecha_pago = NOW();
    END IF;
END$$

CREATE TRIGGER trg_log_pago_exitoso
AFTER UPDATE ON pagos
FOR EACH ROW
BEGIN
    -- Cuando el pago se completa
    IF OLD.estado != NEW.estado AND NEW.estado = 'COMPLETADO' THEN
        
        -- Log del pago exitoso
        INSERT INTO logs_suscripciones (
            suscripcion_id, 
            usuario_id, 
            accion, 
            descripcion, 
            realizado_por
        )
        VALUES (
            NEW.suscripcion_id, 
            NEW.usuario_id, 
            'PAGO_EXITOSO', 
            CONCAT('Pago exitoso por ', NEW.monto, ' ', NEW.moneda, ' via MercadoPago'), 
            'sistema'
        );
        
        -- Actualizar suscripción a ACTIVA
        UPDATE suscripciones_usuarios
        SET 
            estado = 'ACTIVA',
            inicio_periodo_actual = NOW(),
            fin_periodo_actual = DATE_ADD(NOW(), INTERVAL 30 DAY),
            fecha_actualizacion = NOW()
        WHERE id = NEW.suscripcion_id;
        
    END IF;
    
    -- Cuando el pago falla
    IF OLD.estado != NEW.estado AND NEW.estado = 'FALLIDO' THEN
        INSERT INTO logs_suscripciones (
            suscripcion_id, 
            usuario_id, 
            accion, 
            descripcion, 
            realizado_por
        )
        VALUES (
            NEW.suscripcion_id, 
            NEW.usuario_id, 
            'PAGO_FALLIDO', 
            'Intento de pago fallido en MercadoPago', 
            'sistema'
        );
    END IF;
END$$

DELIMITER ;


-- PROCEDIMIENTOS ALMACENADOS

DELIMITER $$

CREATE PROCEDURE sp_cancelar_suscripcion(
    IN p_suscripcion_id VARCHAR(100),
    IN p_usuario_id VARCHAR(100),
    IN p_inmediata BOOLEAN
)
BEGIN
    DECLARE v_estado_actual VARCHAR(50);
    SELECT estado INTO v_estado_actual
    FROM suscripciones_usuarios
    WHERE id = p_suscripcion_id AND usuario_id = p_usuario_id;

    IF v_estado_actual IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Suscripcion no encontrada';
    END IF;
    IF v_estado_actual = 'CANCELADA' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La suscripcion ya esta cancelada';
    END IF;

    IF p_inmediata THEN
        UPDATE suscripciones_usuarios
        SET estado = 'CANCELADA', fecha_cancelacion = NOW(), cancelar_al_final_periodo = FALSE
        WHERE id = p_suscripcion_id;
    ELSE
        UPDATE suscripciones_usuarios
        SET cancelar_al_final_periodo = TRUE, fecha_cancelacion = NOW()
        WHERE id = p_suscripcion_id;
    END IF;

    SELECT 'Suscripcion cancelada exitosamente' AS mensaje;
END$$

CREATE PROCEDURE sp_generar_factura(
    IN p_pago_id VARCHAR(100)
)
BEGIN
    DECLARE v_consecutivo BIGINT;
    DECLARE v_pago_monto DECIMAL(10,2);
    DECLARE v_usuario_id VARCHAR(100);
    DECLARE v_iva DECIMAL(10,2);
    DECLARE v_suscripcion_id VARCHAR(100);

    UPDATE secuencia_facturas SET consecutivo = consecutivo + 1 WHERE id = 1;
    SELECT consecutivo INTO v_consecutivo FROM secuencia_facturas WHERE id = 1;

    SELECT monto, usuario_id, suscripcion_id, monto * 0.19
    INTO v_pago_monto, v_usuario_id, v_suscripcion_id, v_iva
    FROM pagos
    WHERE id = p_pago_id AND estado = 'COMPLETADO';

    IF v_pago_monto IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Pago no encontrado o no completado';
    END IF;

    INSERT INTO facturas (id, pago_id, usuario_id, consecutivo, fecha_emision, fecha_vencimiento, subtotal, iva, total, estado)
    VALUES (UUID(), p_pago_id, v_usuario_id, v_consecutivo, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 30 DAY), v_pago_monto - v_iva, v_iva, v_pago_monto, 'EMITIDA');

    SELECT 'Factura generada exitosamente' AS mensaje, v_consecutivo AS numero_consecutivo;
END$$

CREATE PROCEDURE sp_expirar_suscripciones()
BEGIN
    UPDATE suscripciones_usuarios
    SET estado = 'EXPIRADA'
    WHERE estado = 'ACTIVA' AND fin_periodo_actual < NOW() AND cancelar_al_final_periodo = FALSE;

    UPDATE suscripciones_usuarios
    SET estado = 'CANCELADA'
    WHERE estado = 'ACTIVA' AND fin_periodo_actual < NOW() AND cancelar_al_final_periodo = TRUE;

    SELECT ROW_COUNT() AS suscripciones_actualizadas;
END$$

CREATE PROCEDURE sp_estadisticas_suscripciones(
    IN p_fecha_inicio DATE,
    IN p_fecha_fin DATE
)
BEGIN
    SELECT 
        COUNT(*) AS total_suscripciones,
        SUM(CASE WHEN su.estado = 'ACTIVA' THEN 1 ELSE 0 END) AS activas,
        SUM(CASE WHEN su.estado = 'CANCELADA' THEN 1 ELSE 0 END) AS canceladas,
        SUM(CASE WHEN su.estado = 'EXPIRADA' THEN 1 ELSE 0 END) AS expiradas,
        SUM(CASE WHEN ps.nombre = 'gratuito' THEN 1 ELSE 0 END) AS gratuitas,
        SUM(CASE WHEN ps.nombre = 'premium' THEN 1 ELSE 0 END) AS premium,
        COALESCE(SUM(CASE WHEN ps.nombre = 'premium' AND su.estado = 'ACTIVA' THEN ps.precio ELSE 0 END), 0) AS ingresos_recurrentes
    FROM suscripciones_usuarios su
    JOIN planes_suscripcion ps ON su.plan_id = ps.id
    WHERE su.fecha_creacion BETWEEN p_fecha_inicio AND p_fecha_fin;
END$$

DELIMITER ;

-- Mensaje de éxito
SELECT 'Base de datos creada exitosamente con soporte para MercadoPago' AS resultado;	
