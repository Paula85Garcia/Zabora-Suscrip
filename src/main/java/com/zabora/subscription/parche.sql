-- ============================================================
-- PARCHE SQL — ejecutar UNA vez antes de reiniciar el servicio
-- ============================================================
USE zabora_subscriptions;

-- Hacer nullable id_intento_pago
-- (con Bricks el ID de MP llega en el mismo request; aun asi lo dejamos nullable
--  para no romper el trigger trg_log_pago_exitoso en casos edge)
ALTER TABLE pagos
  MODIFY COLUMN id_intento_pago VARCHAR(255) NULL;

-- Anadir columna motivo_fallo si no existe aun
ALTER TABLE pagos
  ADD COLUMN IF NOT EXISTS motivo_fallo VARCHAR(500) NULL AFTER metadatos;

-- Verificar
DESCRIBE pagos;
