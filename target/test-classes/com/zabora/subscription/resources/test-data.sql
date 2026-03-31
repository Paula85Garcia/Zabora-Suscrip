-- =====================================================
-- PLANES DE SUSCRIPCIÓN
-- =====================================================
INSERT INTO planes_suscripcion
(nombre, descripcion, precio, moneda, limite_condiciones_medicas, limite_alergias,
 limite_preferencias_alimentarias, ingredientes_por_busqueda, limite_recetas_favoritas, activo)
VALUES
('gratuito','Plan gratuito con caracteristicas basicas',0,'COP',2,2,2,7,4,TRUE),
('premium','Plan premium con todas las caracteristicas',29900,'COP',3,4,4,20,NULL,TRUE);

-- =====================================================
-- USUARIO DE PRUEBA
-- =====================================================
-- Si no tienes tabla de usuarios, se puede omitir.
-- INSERT INTO usuarios (id, nombre, email) VALUES (1,'Test User','test@example.com');

-- =====================================================
-- SUSCRIPCIÓN PENDIENTE
-- =====================================================
INSERT INTO suscripciones_usuarios
(id, usuario_id, plan_id, estado, inicio_periodo_actual, fin_periodo_actual, cancelar_al_final_periodo, fecha_creacion, fecha_actualizacion)
VALUES
('sub_test_001', 1, 2, 'PENDIENTE_PAGO', NULL, NULL, FALSE, NOW(), NOW());

-- =====================================================
-- PAGO PENDIENTE
-- =====================================================
INSERT INTO pagos
(id, suscripcion_id, usuario_id, monto, moneda, metodo_pago, estado, id_intento_pago, fecha_creacion)
VALUES
('pay_test_001', 'sub_test_001', 1, 29900, 'COP', 'TARJETA_CREDITO', 'PENDIENTE', 'pi_test_001', NOW());