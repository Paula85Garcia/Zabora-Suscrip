/*Primero consultar el id de usuario a qe queremos consultar si tiene pagos o procesos*/
use zabora_subscriptions;
/*luego consultar estado del usuario segun id consultado antes y copiar el id de suscripcion de que tenga un estado activo*/
SELECT id, usuario_id, estado, inicio_periodo_actual, fin_periodo_actual
FROM suscripciones_usuarios
WHERE usuario_id = 3;
/*pegar el id de suscripcion y el id de usuario para hacer otro pago desde el front con el ismo usuario*/
CALL sp_cancelar_suscripcion(
    "sub_a1c0ad45-a32a-4186-8f55-c54327150f7a",
    3,
    TRUE  -- TRUE = inmediata
);