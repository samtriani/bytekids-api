-- ============================================================================
--  ByteKids Academy - Resetear la clase muestra
--
--  Deja a los alumnos de muestra como recien creados: sin entregas, sin XP,
--  sin logros y sin intentos de quiz. Las cuentas, el salon, el horario y el
--  contenido asignado NO se tocan: el salon queda listo para la siguiente.
--
--  POR QUE HACE FALTA
--    El XP se acumula por alumno. A la tercera muestra tus "alumnos nuevos"
--    llegan con cientos de XP y logros desbloqueados, y se nota.
--
--  CONVENCION
--    Los alumnos de muestra se llaman alumno.muestra.01, .02, ... El patron
--    de abajo caza cualquier usuario cuyo username empiece con
--    "alumno.muestra". Si usas otro prefijo, cambialo en los tres lugares.
--
--  Corre PASO 1, revisa a quien va a afectar, y luego el PASO 2.
-- ============================================================================


-- ============================================================================
--  PASO 1 - A QUIEN AFECTA (no borra nada)
-- ============================================================================

SELECT u.username, u.display_name,
       (SELECT count(*) FROM submissions   s WHERE s.student_id = u.id) AS entregas,
       (SELECT count(*) FROM quiz_attempts q WHERE q.student_id = u.id) AS intentos,
       (SELECT coalesce(sum(x.amount), 0) FROM xp_events x WHERE x.student_id = u.id) AS xp,
       (SELECT count(*) FROM student_achievements a WHERE a.student_id = u.id) AS logros
FROM users u
WHERE u.username LIKE 'alumno.muestra%'
ORDER BY u.username;

-- Si esta consulta no devuelve nada, revisa el prefijo antes de seguir.


-- ============================================================================
--  PASO 2 - EL RESETEO
-- ============================================================================

BEGIN;

CREATE TEMP TABLE muestra AS
SELECT id FROM users WHERE username LIKE 'alumno.muestra%';

-- Cortafuegos: sin esto, un prefijo mal escrito no borraria nada... pero uno
-- DEMASIADO amplio se llevaria a los alumnos reales por delante.
DO $$
DECLARE n int;
BEGIN
  SELECT count(*) INTO n FROM muestra;
  IF n = 0 THEN
    RAISE EXCEPTION 'No hay usuarios con ese prefijo. Revisa el PASO 1.';
  END IF;
  IF n > 30 THEN
    RAISE EXCEPTION 'El patron caza % usuarios, demasiados para una muestra. Revisa el prefijo.', n;
  END IF;
END $$;

-- Respuestas de quiz antes que los intentos (FK hijo -> padre).
DELETE FROM quiz_attempt_answers
WHERE attempt_id IN (SELECT id FROM quiz_attempts WHERE student_id IN (SELECT id FROM muestra));

DELETE FROM quiz_attempts        WHERE student_id IN (SELECT id FROM muestra);
DELETE FROM submissions          WHERE student_id IN (SELECT id FROM muestra);
DELETE FROM xp_events            WHERE student_id IN (SELECT id FROM muestra);
DELETE FROM daily_activity       WHERE student_id IN (SELECT id FROM muestra);
DELETE FROM student_subject_progress WHERE student_id IN (SELECT id FROM muestra);
DELETE FROM student_achievements  WHERE student_id IN (SELECT id FROM muestra);
DELETE FROM notifications         WHERE recipient_id IN (SELECT id FROM muestra);

-- Conversaciones con ByteBot: la muestra siguiente arranca en blanco.
DELETE FROM ai_messages
WHERE conversation_id IN (SELECT id FROM ai_conversations WHERE student_id IN (SELECT id FROM muestra));
DELETE FROM ai_conversations WHERE student_id IN (SELECT id FROM muestra);

COMMIT;


-- ============================================================================
--  PASO 3 - VERIFICACION. Todo debe salir en cero.
-- ============================================================================

SELECT u.username,
       (SELECT count(*) FROM submissions   s WHERE s.student_id = u.id) AS entregas,
       (SELECT count(*) FROM quiz_attempts q WHERE q.student_id = u.id) AS intentos,
       (SELECT coalesce(sum(x.amount), 0) FROM xp_events x WHERE x.student_id = u.id) AS xp,
       (SELECT count(*) FROM student_achievements a WHERE a.student_id = u.id) AS logros
FROM users u
WHERE u.username LIKE 'alumno.muestra%'
ORDER BY u.username;
