-- ============================================================================
--  ByteKids Academy - Reiniciar el avance de un alumno (o borrarlo del todo)
--  Fecha: 17-sep-2026
--
--  EL CASO
--    Un alumno de prueba ya entrego una actividad y al volver a abrirla solo
--    ve "Ya completaste esta actividad": no hay forma de rehacerla desde la
--    pantalla. Es a proposito -- un alumno de verdad no debe poder borrar su
--    entrega -- pero para probar estorba.
--
--  DOS SALIDAS, Y CASI SIEMPRE CONVIENE LA PRIMERA
--
--    OPCION A - Reiniciar el avance.  <-- la recomendada
--      Borra entregas, intentos de quiz, XP, logros y racha. El alumno se
--      queda con su usuario, su contrasena y su salon, y puede volver a hacer
--      todo desde cero. Es lo que se quiere el 95% de las veces.
--
--    OPCION B - Borrar al alumno entero.
--      Ademas de lo anterior, se lleva el usuario. Hay que volver a crearlo y
--      a inscribirlo en el salon. Solo tiene sentido si lo que se quiere
--      probar es el alta.
--
--  POR QUE LA B NO ES TAN SIMPLE COMO SE VE
--    Un DELETE FROM users se cae. Casi todo tiene borrado en cascada, pero DOS
--    tablas NO: class_sessions (la asistencia a clases en vivo) y
--    class_session_messages (el chat del aula). Sus llaves foraneas se
--    declararon sin ON DELETE, o sea RESTRICT.
--
--    Si el alumno entro alguna vez a una clase en vivo -- y este entro --
--    Postgres rechaza el borrado con un error de llave foranea. Por eso la
--    OPCION B limpia esas dos primero. Sin ese paso el DELETE no corre.
--
--  ANTES DE EMPEZAR
--    Corre el PASO 1 y confirma que el usuario es el que crees. Hay dos
--    alumnos con apellido Partida en la base: no te lleves al equivocado.
-- ============================================================================


-- ============================================================================
--  PASO 1 - QUIEN ES Y QUE TIENE
--
--  Debe devolver UNA fila. Si devuelve dos o ninguna, ajusta el nombre y
--  vuelve a correrlo antes de seguir.
-- ============================================================================

SELECT u.id,
       u.username,
       u.display_name,
       u.role,
       u.is_active,
       (SELECT count(*) FROM submissions          s WHERE s.student_id = u.id) AS entregas,
       (SELECT count(*) FROM quiz_attempts        q WHERE q.student_id = u.id) AS intentos_quiz,
       (SELECT count(*) FROM xp_events            x WHERE x.student_id = u.id) AS eventos_xp,
       (SELECT count(*) FROM student_achievements a WHERE a.student_id = u.id) AS logros,
       (SELECT count(*) FROM classroom_enrollments e WHERE e.student_id = u.id AND e.is_active) AS salones,
       (SELECT count(*) FROM class_sessions       cs WHERE cs.participant_id = u.id) AS asistencias,
       (SELECT count(*) FROM class_session_messages m WHERE m.sender_id = u.id) AS mensajes_de_aula
FROM users u
WHERE u.display_name ILIKE '%Diego%Herrera%'
   OR u.username     ILIKE '%diego%';


-- ============================================================================
--  OPCION A - REINICIAR EL AVANCE            <-- la recomendada
--
--  Deja intactos: el usuario, su contrasena, su inscripcion al salon, sus
--  mensajes y su asistencia a clases.
--
--  Borra: entregas, intentos de quiz, XP, logros, racha y avance por materia.
--
--  Va en una transaccion: o se hace todo o no se hace nada. Cambia el nombre
--  del WHERE si el alumno es otro.
-- ============================================================================

BEGIN;

WITH alumno AS (
    SELECT id FROM users
    WHERE display_name ILIKE '%Diego%Herrera%'
    LIMIT 1
)
, borra_entregas AS (
    -- quiz_attempt_answers se va en cascada al borrar el intento.
    DELETE FROM submissions   WHERE student_id IN (SELECT id FROM alumno) RETURNING 1
)
, borra_intentos AS (
    DELETE FROM quiz_attempts WHERE student_id IN (SELECT id FROM alumno) RETURNING 1
)
, borra_xp AS (
    DELETE FROM xp_events     WHERE student_id IN (SELECT id FROM alumno) RETURNING 1
)
, borra_logros AS (
    DELETE FROM student_achievements WHERE student_id IN (SELECT id FROM alumno) RETURNING 1
)
, borra_racha AS (
    DELETE FROM daily_activity WHERE student_id IN (SELECT id FROM alumno) RETURNING 1
)
, borra_avance AS (
    DELETE FROM student_subject_progress WHERE student_id IN (SELECT id FROM alumno) RETURNING 1
)
SELECT (SELECT count(*) FROM borra_entregas) AS entregas_borradas,
       (SELECT count(*) FROM borra_intentos) AS intentos_borrados,
       (SELECT count(*) FROM borra_xp)       AS xp_borrado,
       (SELECT count(*) FROM borra_logros)   AS logros_borrados,
       (SELECT count(*) FROM borra_racha)    AS dias_borrados,
       (SELECT count(*) FROM borra_avance)   AS avances_borrados;

-- Revisa los numeros de arriba. Si cuadran:
COMMIT;
-- Si NO cuadran, corre esto en su lugar y no se cambia nada:
-- ROLLBACK;


-- ============================================================================
--  PASO FINAL DE LA OPCION A - COMPROBAR
--
--  Todo debe quedar en cero. El alumno sigue en su salon y puede volver a
--  entrar con la misma contrasena.
-- ============================================================================

SELECT u.display_name,
       (SELECT count(*) FROM submissions          s WHERE s.student_id = u.id) AS entregas,
       (SELECT count(*) FROM xp_events            x WHERE x.student_id = u.id) AS eventos_xp,
       (SELECT count(*) FROM student_achievements a WHERE a.student_id = u.id) AS logros,
       (SELECT count(*) FROM classroom_enrollments e WHERE e.student_id = u.id AND e.is_active) AS salones_conserva
FROM users u
WHERE u.display_name ILIKE '%Diego%Herrera%';


-- ============================================================================
--  OPCION B - BORRAR AL ALUMNO ENTERO
--
--  Solo si de verdad se quiere volver a darlo de alta. Se pierde el usuario,
--  la contrasena y la inscripcion: hay que crearlo de nuevo y volver a
--  inscribirlo en el salon.
--
--  Esta comentado a proposito para que pasar el archivo completo no se lo
--  lleve por accidente. Quita los guiones para usarlo.
--
--  El orden importa: primero las dos tablas que NO tienen cascada, o el
--  DELETE final se cae con un error de llave foranea.
-- ============================================================================

-- BEGIN;
--
-- WITH alumno AS (
--     SELECT id FROM users
--     WHERE display_name ILIKE '%Diego%Herrera%'
--     LIMIT 1
-- )
-- DELETE FROM class_session_messages
-- WHERE sender_id IN (SELECT id FROM alumno);
--
-- WITH alumno AS (
--     SELECT id FROM users
--     WHERE display_name ILIKE '%Diego%Herrera%'
--     LIMIT 1
-- )
-- DELETE FROM class_sessions
-- WHERE participant_id IN (SELECT id FROM alumno);
--
-- -- Lo demas se va en cascada: entregas, quizzes, XP, logros, racha,
-- -- inscripciones, notificaciones y mensajes directos.
-- DELETE FROM users
-- WHERE display_name ILIKE '%Diego%Herrera%';
--
-- COMMIT;


-- ============================================================================
--  LO QUE ESTE SCRIPT NO ARREGLA
--
--  Que un alumno no pueda rehacer una actividad es correcto para un alumno de
--  verdad, pero deja sin salida a las pruebas. Si esto se va a repetir seguido
--  conviene una de dos:
--
--    - Un boton de "reiniciar avance" en la ficha del alumno, visible solo
--      para coordinacion.
--    - O el "deshacer aprobacion" que ya estaba pendiente: el maestro
--      devuelve una entrega y el alumno la puede volver a hacer, que es el
--      caso real de un nino que entrego por error.
--
--  Lo segundo sirve en produccion; lo primero solo para probar.
-- ============================================================================
