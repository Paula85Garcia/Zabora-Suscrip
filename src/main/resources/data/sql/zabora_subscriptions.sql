-- ========================================
-- ZABORA SUBSCRIPTION SERVICE - DATABASE
-- Script completo SIN índices
-- ========================================

-- Eliminar base de datos si existe (CUIDADO en producción)
DROP DATABASE IF EXISTS zabora_subscriptions;

-- Crear base de datos
CREATE DATABASE zabora_subscriptions 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE zabora_subscriptions;

-- ========================================
-- TABLA: PLANES DE SUSCRIPCIÓN
-- ========================================
CREATE TABLE IF NOT EXISTS planes_suscripcion (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    descripcion TEXT,
    precio DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    moneda VARCHAR(3) DEFAULT 'COP',
    
    -- Límites del plan
    limite_condiciones_medicas INT NOT NULL DEFAULT 0,
    limite_alergias INT NOT NULL DEFAULT 0,
    limite_preferencias_alimentarias INT NOT NULL DEFAULT 0,
    ingredientes_por_busqueda INT NOT NULL DEFAULT 0,
    limite_recetas_favoritas INT NULL,
    
    -- Control
    activo BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- TABLA: SUSCRIPCIONES DE USUARIOS
-- ========================================
CREATE TABLE IF NOT EXISTS suscripciones_usuarios (
    id VARCHAR(100) PRIMARY KEY,
    usuario_id VARCHAR(100) NOT NULL,
    plan_id BIGINT NOT NULL,
    
    estado ENUM('ACTIVA', 'CANCELADA', 'EXPIRADA', 'PENDIENTE_PAGO', 'SIN_SUSCRIPCION')
        DEFAULT 'PENDIENTE_PAGO',
    
    inicio_periodo_actual DATETIME NULL,
    fin_periodo_actual DATETIME NULL,
    cancelar_al_final_periodo BOOLEAN DEFAULT FALSE,
    fecha_cancelacion DATETIME NULL,
    
    -- Integración con Stripe
    id_cliente_stripe VARCHAR(255) NULL,
    id_suscripcion_stripe VARCHAR(255) NULL,
    
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (plan_id)
        REFERENCES planes_suscripcion(id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- TABLA: MÉTODOS DE PAGO
-- ========================================
CREATE TABLE IF NOT EXISTS metodos_pago (
    id VARCHAR(100) PRIMARY KEY,
    usuario_id VARCHAR(100) NOT NULL,
    
    tipo ENUM('TARJETA_CREDITO', 'PSE') NOT NULL,
    
    -- Para tarjetas
    ultimos_cuatro VARCHAR(4) NULL,
    marca VARCHAR(50) NULL,
    expira_mes INT NULL,
    expira_anio INT NULL,
    
    -- Para PSE
    banco VARCHAR(100) NULL,
    tipo_cuenta ENUM('ahorros', 'corriente') NULL,
    referencia_pse VARCHAR(100) NULL,
    
    id_metodo_pago_stripe VARCHAR(255) NOT NULL UNIQUE,
    
    predeterminado BOOLEAN DEFAULT FALSE,
    activo BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- TABLA: PAGOS
-- ========================================
CREATE TABLE IF NOT EXISTS pagos (
    id VARCHAR(100) PRIMARY KEY,
    suscripcion_id VARCHAR(100) NOT NULL,
    usuario_id VARCHAR(100) NOT NULL,
    
    monto DECIMAL(10,2) NOT NULL,
    moneda VARCHAR(3) DEFAULT 'COP',
    
    metodo_pago ENUM('TARJETA_CREDITO', 'PSE') NOT NULL,
    
    estado ENUM('PENDIENTE', 'COMPLETADO', 'FALLIDO', 'REEMBOLSADO', 'CANCELADO')
        DEFAULT 'PENDIENTE',
    
    id_intento_pago_stripe VARCHAR(255) NOT NULL UNIQUE,
    
    fecha_pago DATETIME NULL,
    url_comprobante VARCHAR(500) NULL,
    codigo_autorizacion VARCHAR(50) NULL,
    
    estado_pse VARCHAR(50) NULL,
    referencia_pse VARCHAR(100) NULL,
    
    metadatos JSON NULL,
    
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (suscripcion_id)
        REFERENCES suscripciones_usuarios(id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- TABLA: SECUENCIA DE FACTURAS
-- ========================================
CREATE TABLE IF NOT EXISTS secuencia_facturas (
    id INT PRIMARY KEY DEFAULT 1,
    consecutivo BIGINT NOT NULL DEFAULT 1000
);

INSERT IGNORE INTO secuencia_facturas (id, consecutivo)
VALUES (1, 1000);

-- ========================================
-- TABLA: FACTURAS DIAN
-- ========================================
CREATE TABLE IF NOT EXISTS facturas (
    id VARCHAR(100) PRIMARY KEY,
    pago_id VARCHAR(100) NOT NULL,
    usuario_id VARCHAR(100) NOT NULL,
    
    prefijo VARCHAR(10) NOT NULL DEFAULT 'FZ',
    consecutivo BIGINT NOT NULL,
    
    numero_factura VARCHAR(50)
        GENERATED ALWAYS AS (CONCAT(prefijo, '-', consecutivo)) STORED,
    
    fecha_emision DATE NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    iva DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total DECIMAL(10,2) NOT NULL,
    
    estado ENUM('BORRADOR', 'EMITIDA', 'PAGADA', 'ANULADA') DEFAULT 'BORRADOR',
    cufe VARCHAR(200) NULL,
    respuesta_dian JSON NULL,
    
    pdf_url VARCHAR(500) NULL,
    xml_url VARCHAR(500) NULL,
    
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE (numero_factura),
    
    FOREIGN KEY (pago_id)
        REFERENCES pagos(id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- TABLA: LOGS DE SUSCRIPCIONES
-- ========================================
CREATE TABLE IF NOT EXISTS logs_suscripciones (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    suscripcion_id VARCHAR(100) NOT NULL,
    usuario_id VARCHAR(100) NOT NULL,
    
    accion ENUM(
        'CREACION',
        'ACTIVACION',
        'CANCELACION',
        'RENOVACION',
        'PAGO_EXITOSO',
        'PAGO_FALLIDO',
        'CAMBIO_PLAN',
        'REEMBOLSO',
        'CAMBIO_ESTADO'
    ) NOT NULL,
    
    estado_anterior VARCHAR(50) NULL,
    estado_nuevo VARCHAR(50) NULL,
    
    descripcion TEXT NULL,
    realizado_por VARCHAR(100) NOT NULL,
    
    ip_address VARCHAR(45) NULL,
    user_agent TEXT NULL,
    
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (suscripcion_id)
        REFERENCES suscripciones_usuarios(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- TABLA: REPORTES DE INGRESOS
-- ========================================
CREATE TABLE IF NOT EXISTS reportes_ingresos (
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
    
    datos_reportes JSON NOT NULL,
    pdf_url VARCHAR(500) NULL,
    
    generado_por VARCHAR(100) NULL,
    fecha_generacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- VISTAS
-- ========================================

-- Vista: Suscripciones Activas
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
WHERE su.estado = 'ACTIVA'
  AND (su.fin_periodo_actual IS NULL OR su.fin_periodo_actual > NOW());

-- Vista: Ingresos Mensuales
CREATE OR REPLACE VIEW vista_ingresos_mensuales AS
SELECT 
    DATE_FORMAT(p.fecha_pago, '%Y-%m') AS mes,
    COUNT(p.id) AS total_pagos,
    SUM(CASE WHEN p.estado = 'COMPLETADO' THEN p.monto ELSE 0 END) AS ingresos_totales,
    SUM(CASE WHEN p.metodo_pago = 'TARJETA_CREDITO' THEN 1 ELSE 0 END) AS pagos_tarjeta,
    SUM(CASE WHEN p.metodo_pago = 'PSE' THEN 1 ELSE 0 END) AS pagos_pse
FROM pagos p
WHERE p.estado = 'COMPLETADO'
  AND p.fecha_pago IS NOT NULL
GROUP BY DATE_FORMAT(p.fecha_pago, '%Y-%m')
ORDER BY mes DESC;

-- ========================================
-- PROCEDIMIENTOS ALMACENADOS
-- ========================================

-- Procedimiento: Cancelar Suscripción
DELIMITER $$
CREATE PROCEDURE sp_cancelar_suscripcion(
    IN p_suscripcion_id VARCHAR(100),
    IN p_usuario_id VARCHAR(100),
    IN p_motivo TEXT
)
BEGIN
    DECLARE v_estado_actual VARCHAR(50);
    DECLARE v_fecha_creacion DATETIME;
    DECLARE v_plan_nombre VARCHAR(50);

    SELECT estado, fecha_creacion, ps.nombre
    INTO v_estado_actual, v_fecha_creacion, v_plan_nombre
    FROM suscripciones_usuarios su
    JOIN planes_suscripcion ps ON su.plan_id = ps.id
    WHERE su.id = p_suscripcion_id
      AND su.usuario_id = p_usuario_id;

    IF v_estado_actual IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Suscripción no encontrada';
    END IF;

    IF v_estado_actual = 'CANCELADA' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La suscripción ya está cancelada';
    END IF;

    UPDATE suscripciones_usuarios
    SET estado = 'CANCELADA',
        fecha_cancelacion = NOW(),
        fecha_actualizacion = NOW(),
        cancelar_al_final_periodo = TRUE
    WHERE id = p_suscripcion_id
      AND usuario_id = p_usuario_id;

    INSERT INTO logs_suscripciones
        (suscripcion_id, usuario_id, accion, estado_anterior, estado_nuevo, descripcion, realizado_por)
    VALUES
        (p_suscripcion_id, p_usuario_id, 'CANCELACION', v_estado_actual, 'CANCELADA', p_motivo, p_usuario_id);

    IF TIMESTAMPDIFF(HOUR, v_fecha_creacion, NOW()) <= 24
       AND v_plan_nombre = 'premium' THEN
        INSERT INTO logs_suscripciones
            (suscripcion_id, usuario_id, accion, descripcion, realizado_por)
        VALUES
            (p_suscripcion_id, p_usuario_id, 'REEMBOLSO',
             'Reembolso automático por cancelación en 24 horas', 'sistema');
    END IF;

    SELECT 'Suscripción cancelada exitosamente' AS mensaje;
END$$
DELIMITER ;

-- Procedimiento: Generar Factura
DELIMITER $$
CREATE PROCEDURE sp_generar_factura(
    IN p_pago_id VARCHAR(100)
)
BEGIN
    DECLARE v_consecutivo BIGINT;
    DECLARE v_pago_monto DECIMAL(10,2);
    DECLARE v_usuario_id VARCHAR(100);
    DECLARE v_iva DECIMAL(10,2);

    UPDATE secuencia_facturas
        SET consecutivo = consecutivo + 1
    WHERE id = 1;

    SELECT consecutivo INTO v_consecutivo
    FROM secuencia_facturas
    WHERE id = 1;

    SELECT monto, usuario_id, monto * 0.19
    INTO v_pago_monto, v_usuario_id, v_iva
    FROM pagos
    WHERE id = p_pago_id AND estado = 'COMPLETADO';

    IF v_pago_monto IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Pago no encontrado o no completado';
    END IF;

    INSERT INTO facturas
        (id, pago_id, usuario_id, prefijo, consecutivo,
         fecha_emision, fecha_vencimiento,
         subtotal, iva, total, estado)
    VALUES (
        UUID(),
        p_pago_id,
        v_usuario_id,
        'FZ',
        v_consecutivo,
        CURDATE(),
        DATE_ADD(CURDATE(), INTERVAL 30 DAY),
        v_pago_monto - v_iva,
        v_iva,
        v_pago_monto,
        'EMITIDA'
    );

    SELECT 'Factura generada exitosamente' AS mensaje;
END$$
DELIMITER ;

-- ========================================
-- TRIGGERS
-- ========================================

-- Trigger: Log de cambios de estado
DELIMITER $$
CREATE TRIGGER trg_log_cambio_suscripcion
AFTER UPDATE ON suscripciones_usuarios
FOR EACH ROW
BEGIN
    IF OLD.estado <> NEW.estado THEN
        INSERT INTO logs_suscripciones
            (suscripcion_id, usuario_id, accion, estado_anterior, estado_nuevo, realizado_por)
        VALUES
            (NEW.id, NEW.usuario_id, 'CAMBIO_ESTADO', OLD.estado, NEW.estado, 'sistema');
    END IF;
END$$
DELIMITER ;

-- Trigger: Actualizar fecha de pago
DELIMITER $$
CREATE TRIGGER trg_actualizar_fecha_pago
BEFORE UPDATE ON pagos
FOR EACH ROW
BEGIN
    IF OLD.estado <> NEW.estado AND NEW.estado = 'COMPLETADO' THEN
        SET NEW.fecha_pago = NOW();
    END IF;
END$$
DELIMITER ;

-- ========================================
-- DATOS INICIALES
-- ========================================

-- Insertar planes
INSERT INTO planes_suscripcion 
    (nombre, descripcion, precio, moneda,
     limite_condiciones_medicas, limite_alergias,
     limite_preferencias_alimentarias, ingredientes_por_busqueda,
     limite_recetas_favoritas, activo)
VALUES
    ('gratuito', 'Plan gratuito con características básicas', 0.00, 'COP',
     2, 2, 1, 7, 4, TRUE),

    ('premium', 'Plan premium con todas las características', 29900.00, 'COP',
     3, 4, 1, 20, NULL, TRUE);

-- ========================================
-- VERIFICACIÓN
-- ========================================

SELECT 'Base de datos creada exitosamente' AS Estado;

SELECT 
    'Tablas creadas:' AS Info,
    COUNT(*) AS Total
FROM information_schema.tables 
WHERE table_schema = 'zabora_subscriptions';

SELECT 
    'Planes insertados:' AS Info,
    COUNT(*) AS Total
FROM planes_suscripcion;

SELECT 
    id,
    nombre,
    precio,
    activo
FROM planes_suscripcion;