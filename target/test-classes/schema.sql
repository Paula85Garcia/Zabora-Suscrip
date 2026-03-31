-- Schema para tests de integración

-- Tabla de planes de suscripción
CREATE TABLE IF NOT EXISTS planes_suscripcion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    descripcion TEXT,
    precio DECIMAL(10,2) NOT NULL,
    moneda VARCHAR(3) DEFAULT 'COP',
    limite_condiciones_medicas INT DEFAULT 0,
    limite_alergias INT DEFAULT 0,
    limite_preferencias_alimentarias INT DEFAULT 0,
    ingredientes_por_busqueda INT DEFAULT 0,
    limite_recetas_favoritas INT,
    activo BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Tabla de suscripciones
CREATE TABLE IF NOT EXISTS suscripciones (
    id VARCHAR(36) PRIMARY KEY,
    usuario_id INT NOT NULL,
    plan_id BIGINT NOT NULL,
    estado VARCHAR(20) DEFAULT 'PENDIENTE_PAGO',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_inicio TIMESTAMP,
    fecha_expiracion TIMESTAMP,
    cancelar_al_final_periodo BOOLEAN DEFAULT FALSE,
    fecha_efecto_cancelacion TIMESTAMP,
    fecha_cancelacion TIMESTAMP,
    FOREIGN KEY (plan_id) REFERENCES planes_suscripcion(id)
);

-- Tabla de pagos
CREATE TABLE IF NOT EXISTS pagos (
    id VARCHAR(36) PRIMARY KEY,
    suscripcion_id VARCHAR(36) NOT NULL,
    monto DECIMAL(10,2) NOT NULL,
    moneda VARCHAR(3) DEFAULT 'COP',
    estado VARCHAR(20) DEFAULT 'PENDIENTE',
    tipo_pago VARCHAR(20),
    mp_payment_id VARCHAR(100),
    fecha_pago TIMESTAMP,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (suscripcion_id) REFERENCES suscripciones(id)
);
