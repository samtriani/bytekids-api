-- ============================================================================
--  ByteKids Academy — Limpieza para arranque en produccion
--  Fecha: 7-sep-2026
--
--  QUE CONSERVA
--    * Los usuarios listados en `conservados` (por default: samuel.partida)
--    * Las materias cuyo nombre empieza con "IA para Ni~nos"
--    * Todo el contenido (misiones, tareas, quiz, proyectos, material)
--      de esas materias, con sus preguntas y opciones
--    * El catalogo achievement_definitions (es configuracion, no datos)
--
--  QUE BORRA
--    Todo lo demas: salones, inscripciones, horarios, sesiones de clase,
--    entregas, intentos de quiz, XP, progreso, logros ganados, mensajes,
--    notificaciones, conversaciones con la IA, vinculos padre-alumno,
--    asignaciones de contenido, y el resto de usuarios.
--
--  COMO USARLO (Neon SQL Editor)
--    1. Crea una branch en Neon como respaldo antes de nada.
--    2. Corre el PASO 1 solo. Revisa que los conteos cuadren.
--    3. Corre el PASO 2 completo (viene envuelto en una transaccion).
--    4. Corre el PASO 3 para confirmar como quedo.
-- ============================================================================


-- ============================================================================
--  PASO 0 — ¿YA CORRI EL PASO 2?
--  Corre esto si no estas seguro de si la limpieza ya se aplico.
--  Antes: users > 1 y classrooms > 0.  Despues: users = 1 y classrooms = 0.
-- ============================================================================

SELECT (SELECT count(*) FROM users)      AS usuarios,
       (SELECT count(*) FROM classrooms) AS salones,
       (SELECT count(*) FROM subjects)   AS materias,
       (SELECT count(*) FROM content)    AS contenido;


-- ============================================================================
--  PASO 1 — INVENTARIO (no borra nada, corre esto primero)
-- ============================================================================

-- 1a. Usuarios que SOBREVIVEN
SELECT username, display_name, role, is_active
FROM users
WHERE username IN ('samuel.partida')
ORDER BY role, username;

-- 1b. Usuarios que SE BORRAN
SELECT role, count(*) AS se_borran, string_agg(username, ', ' ORDER BY username) AS quienes
FROM users
WHERE username NOT IN ('samuel.partida')
GROUP BY role
ORDER BY role;

-- 1c. Materias que SOBREVIVEN (deben ser exactamente 2)
SELECT id, name, icon, is_active
FROM subjects
WHERE name ILIKE 'IA para Ni%os%'
ORDER BY name;

-- 1d. Materias que SE BORRAN
SELECT id, name FROM subjects
WHERE name NOT ILIKE 'IA para Ni%os%'
ORDER BY name;

-- 1e. Contenido que SOBREVIVE, por materia y tipo
SELECT s.name AS materia, c.type, count(*) AS piezas
FROM content c
JOIN subjects s ON s.id = c.subject_id
WHERE s.name ILIKE 'IA para Ni%os%'
GROUP BY s.name, c.type
ORDER BY s.name, c.type;

-- 1f. Contenido que SE BORRA (de otras materias o sin materia)
SELECT coalesce(s.name, '(sin materia)') AS materia, count(*) AS piezas
FROM content c
LEFT JOIN subjects s ON s.id = c.subject_id
WHERE s.id IS NULL OR s.name NOT ILIKE 'IA para Ni%os%'
GROUP BY s.name
ORDER BY 1;


-- ============================================================================
--  PASO 2 — LIMPIEZA (esto si borra; va todo en una transaccion)
-- ============================================================================

BEGIN;

-- Quien se queda. Si vas a conservar mas cuentas, agregalas aqui
-- Y TAMBIEN en el DELETE FROM users del final.
CREATE TEMP TABLE conservados AS
SELECT id FROM users WHERE username IN ('samuel.partida');

CREATE TEMP TABLE materias_base AS
SELECT id FROM subjects WHERE name ILIKE 'IA para Ni%os%';

CREATE TEMP TABLE contenido_base AS
SELECT id FROM content WHERE subject_id IN (SELECT id FROM materias_base);

-- Cortafuegos: si el patron no encontro las dos materias, aborta.
-- Sin esto, un nombre distinto en la base borraria todo el curriculo.
DO $$
DECLARE n int;
BEGIN
  SELECT count(*) INTO n FROM materias_base;
  IF n <> 2 THEN
    RAISE EXCEPTION 'Se esperaban 2 materias "IA para Ninos" y se encontraron %. Revisa el PASO 1c antes de continuar.', n;
  END IF;
END $$;

-- ── Clases en vivo (FK sin ON DELETE: hay que ir de hijo a padre) ──
DELETE FROM class_session_messages;
DELETE FROM class_session_missions;
DELETE FROM class_sessions;
DELETE FROM class_schedules;

-- ── Actividad del alumno ──
DELETE FROM quiz_attempt_answers;
DELETE FROM quiz_attempts;
DELETE FROM submissions;
DELETE FROM xp_events;
DELETE FROM daily_activity;
DELETE FROM student_subject_progress;
DELETE FROM student_achievements;

-- ── Comunicacion ──
DELETE FROM ai_messages;
DELETE FROM ai_conversations;
DELETE FROM messages;
DELETE FROM notifications;

-- ── Asignaciones de contenido (se reasignan cuando existan los salones) ──
DELETE FROM content_assignments;

-- ── Contenido ajeno al curriculo base ──
-- quiz_questions, quiz_options y mission_prerequisites caen por CASCADE.
DELETE FROM content WHERE id NOT IN (SELECT id FROM contenido_base);

-- El curriculo base pasa a ser propiedad de coordinacion. Ademas de ser
-- la regla de autoria acordada, content.created_by es ON DELETE RESTRICT:
-- sin esto, borrar al maestro que lo creo fallaria.
UPDATE content
SET created_by = (SELECT id FROM conservados LIMIT 1)
WHERE id IN (SELECT id FROM contenido_base);

-- ── Estructura escolar ──
DELETE FROM parent_student;
DELETE FROM classroom_enrollments;
DELETE FROM classroom_subjects;
DELETE FROM classrooms;

-- ── Materias ajenas ──
DELETE FROM subjects WHERE id NOT IN (SELECT id FROM materias_base);

-- ── Usuarios ──
DELETE FROM users WHERE username NOT IN ('samuel.partida');

COMMIT;


-- ============================================================================
--  PASO 3 — VERIFICACION
-- ============================================================================

SELECT 'users'                 AS tabla, count(*) FROM users
UNION ALL SELECT 'subjects',              count(*) FROM subjects
UNION ALL SELECT 'content',               count(*) FROM content
UNION ALL SELECT 'quiz_questions',        count(*) FROM quiz_questions
UNION ALL SELECT 'quiz_options',          count(*) FROM quiz_options
UNION ALL SELECT 'classrooms',            count(*) FROM classrooms
UNION ALL SELECT 'classroom_enrollments', count(*) FROM classroom_enrollments
UNION ALL SELECT 'classroom_subjects',    count(*) FROM classroom_subjects
UNION ALL SELECT 'class_schedules',       count(*) FROM class_schedules
UNION ALL SELECT 'content_assignments',   count(*) FROM content_assignments
UNION ALL SELECT 'submissions',           count(*) FROM submissions
UNION ALL SELECT 'xp_events',             count(*) FROM xp_events
UNION ALL SELECT 'notifications',         count(*) FROM notifications
UNION ALL SELECT 'achievement_definitions', count(*) FROM achievement_definitions
ORDER BY 1;

-- Esperado: users = 1, subjects = 2, classrooms = 0, content_assignments = 0,
-- content = las piezas de los dos temarios, achievement_definitions intacto.

SELECT s.name AS materia, c.type, count(*) AS piezas
FROM content c JOIN subjects s ON s.id = c.subject_id
GROUP BY s.name, c.type
ORDER BY s.name, c.type;
