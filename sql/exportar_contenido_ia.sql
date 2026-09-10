-- ============================================================================
--  ByteKids Academy - Exportador del contenido de "IA para Ninos"
--  Fecha: 10-sep-2026
--
--  PARA QUE SIRVE
--    Genera el script de siembra de las dos materias --Principiante e
--    Intermedio-- con su temario completo y sus quizzes, para poder volver a
--    cargarlo en otra base: una escuela nueva, un ambiente de pruebas, o para
--    recuperar el temario si se pierde.
--
--  POR QUE UN GENERADOR Y NO UN ARCHIVO YA ESCRITO
--    Un respaldo tiene que salir de la base real. Escribirlo a mano seria una
--    reconstruccion de lo que yo creo que hay, y bastaria con que alguien
--    hubiera editado una descripcion desde Materias para que dejara de
--    coincidir. Esto lee lo que HAY y te lo devuelve como SQL.
--
--  POR QUE NO SIRVE UN VOLCADO NORMAL
--    content.subject_id y content.created_by son llaves foraneas a UUID que no
--    existen en la otra base. Un dump con esos UUID no corre. El script que
--    sale de aqui resuelve las dos referencias EN LA BASE DESTINO: la materia
--    por nombre, y el autor por el primer usuario de coordinacion o direccion
--    que encuentre.
--
--  COMO SE USA
--    1. Corre este archivo completo en la base de origen.
--    2. El resultado son muchas filas con una sola columna, "linea".
--       En DBeaver: clic en el encabezado de la columna para seleccionarla,
--       y luego "Advanced copy" o exportar el resultado a archivo .sql.
--       Cuida que la exportacion NO agregue comillas ni encabezado.
--    3. Guarda el resultado como sql/siembra_contenido_ia.sql y ese es tu
--       respaldo. Correrlo en otra base recrea el temario.
--
--  EL SCRIPT QUE SALE ES IDEMPOTENTE
--    Cada pieza se inserta solo si esa materia no tiene ya una pieza con ese
--    order_index. Se puede volver a correr sin duplicar. NO borra nada: si en
--    la base destino ya hay contenido en ese indice, lo respeta y lo deja.
-- ============================================================================


-- ============================================================================
--  PASO 0 - QUE SE VA A EXPORTAR
--  Deben salir las dos materias. Si sale una sola, revisa el nombre antes de
--  seguir: todo el exportador casa por 'IA para Ni%os'.
-- ============================================================================

SELECT s.name AS materia,
       count(c.id)                                       AS piezas,
       count(*) FILTER (WHERE c.type = 'quiz')           AS quizzes,
       count(*) FILTER (WHERE c.content_body ? 'teacher_notes') AS con_guia,
       min(c.order_index) AS primer_indice,
       max(c.order_index) AS ultimo_indice
FROM subjects s
LEFT JOIN content c ON c.subject_id = s.id
WHERE s.name ILIKE '%IA para Ni%os%'
GROUP BY s.name
ORDER BY s.name;


-- ============================================================================
--  PASO 1 - EL GENERADOR
--  Devuelve el script de siembra, una linea por fila, en orden.
-- ============================================================================

WITH materias AS (
    SELECT s.* FROM subjects s WHERE s.name ILIKE '%IA para Ni%os%'
),

-- ── Encabezado del script generado ──────────────────────────────────────────
encabezado AS (
    SELECT 0 AS seccion, n AS orden, linea
    FROM (VALUES
      (1,  '-- ============================================================================'),
      (2,  '--  ByteKids Academy - Siembra del contenido de "IA para Ninos"'),
      (3,  '--  Generado por sql/exportar_contenido_ia.sql'),
      (4,  '--'),
      (5,  '--  QUE HACE'),
      (6,  '--    Recrea las materias de IA para Ninos con su temario completo y sus'),
      (7,  '--    quizzes. Sirve para montar el curso en una base nueva.'),
      (8,  '--'),
      (9,  '--  QUE NECESITA LA BASE DESTINO'),
      (10, '--    Ya tiene que existir al menos un usuario con rol admin o director:'),
      (11, '--    es quien queda como autor del contenido, y por eso nace como plan base'),
      (12, '--    (el maestro lo imparte pero no lo edita). Si no hay ninguno, no'),
      (13, '--    inserta nada y no falla con estruendo: revisa el conteo del final.'),
      (14, '--'),
      (15, '--  ES IDEMPOTENTE'),
      (16, '--    Cada pieza entra solo si esa materia no tiene ya algo en ese'),
      (17, '--    order_index. Se puede repetir. NO borra ni sobrescribe nada.'),
      (18, '-- ============================================================================'),
      (19, ''),
      (20, 'BEGIN;'),
      (21, '')
    ) AS v(n, linea)
),

-- ── Las materias ────────────────────────────────────────────────────────────
sql_materias AS (
    SELECT 1 AS seccion,
           row_number() OVER (ORDER BY m.name) AS orden,
           format(
E'-- ---------------------------------------------------------------------------\n' ||
E'--  MATERIA: %s\n' ||
E'-- ---------------------------------------------------------------------------\n' ||
E'INSERT INTO subjects (name, icon, color, description, is_active)\n' ||
E'VALUES (%s, %s, %s, %s, %s)\n' ||
E'ON CONFLICT (name) DO UPDATE\n' ||
E'  SET icon = EXCLUDED.icon, color = EXCLUDED.color,\n' ||
E'      description = EXCLUDED.description, is_active = EXCLUDED.is_active;\n',
             m.name,
             quote_literal(m.name), quote_nullable(m.icon), quote_nullable(m.color),
             quote_nullable(m.description), m.is_active
           ) AS linea
    FROM materias m
),

-- ── El temario ──────────────────────────────────────────────────────────────
--  Las dos referencias que no son portables se resuelven en el destino:
--  la materia por nombre, y el autor por el primer admin o director.
sql_contenido AS (
    SELECT 2 AS seccion,
           row_number() OVER (ORDER BY m.name, c.order_index, c.title) AS orden,
           format(
E'-- %s · pieza %s · %s\n' ||
E'INSERT INTO content (title, description, type, subject_id, created_by, xp_reward,\n' ||
E'                     difficulty, estimated_minutes, content_body, order_index,\n' ||
E'                     is_published, is_active)\n' ||
E'SELECT %s, %s, %s::content_type, mat.id, autor.id, %s, %s::difficulty_level,\n' ||
E'       %s, %s, %s, %s, %s\n' ||
E'FROM      (SELECT id FROM subjects WHERE name = %s) mat\n' ||
E'CROSS JOIN (SELECT id FROM users\n' ||
E'            WHERE role IN (''admin'',''director'') AND is_active\n' ||
E'            ORDER BY created_at LIMIT 1) autor\n' ||
E'WHERE NOT EXISTS (\n' ||
E'  SELECT 1 FROM content x JOIN subjects xs ON xs.id = x.subject_id\n' ||
E'  WHERE xs.name = %s AND x.title = %s);\n',
             m.name, coalesce(c.order_index::text, 's/n'), c.title,
             quote_literal(c.title),
             quote_nullable(c.description),
             quote_literal(c.type::text),
             c.xp_reward,
             quote_literal(c.difficulty::text),
             coalesce(c.estimated_minutes::text, 'NULL'),
             CASE WHEN c.content_body IS NULL THEN 'NULL'
                  ELSE quote_literal(c.content_body::text) || '::jsonb' END,
             coalesce(c.order_index::text, 'NULL'),
             c.is_published, c.is_active,
             quote_literal(m.name),
             -- La guarda casa por TITULO y no por order_index: order_index
             -- admite NULL, y "x.order_index = NULL" nunca es cierto, asi que
             -- una pieza sin indice se reinsertaria en cada corrida. El titulo
             -- es NOT NULL en el esquema.
             quote_literal(m.name), quote_literal(c.title)
           ) AS linea
    FROM content c JOIN materias m ON m.id = c.subject_id
),

-- ── Las preguntas de los quizzes, con sus opciones ──────────────────────────
--  Cada pregunta se ancla a su pieza por materia + order_index, que es lo
--  unico estable entre bases. Las opciones entran en el mismo statement con
--  un CTE que devuelve el id recien insertado.
opciones AS (
    SELECT o.question_id,
           string_agg(
             format('(%s, %s, %s)', quote_literal(o.option_text), o.is_correct, o.order_index),
             E',\n              ' ORDER BY o.order_index
           ) AS lista
    FROM quiz_options o
    GROUP BY o.question_id
),
sql_quizzes AS (
    SELECT 3 AS seccion,
           row_number() OVER (ORDER BY m.name, c.order_index, q.order_index) AS orden,
           format(
E'-- %s · pieza %s · pregunta %s\n' ||
E'WITH nueva AS (\n' ||
E'  INSERT INTO quiz_questions (content_id, question_text, question_type, points, order_index)\n' ||
E'  SELECT c.id, %s, %s::question_type, %s, %s\n' ||
E'  FROM content c JOIN subjects s ON s.id = c.subject_id\n' ||
E'  WHERE s.name = %s AND c.title = %s\n' ||
E'    AND NOT EXISTS (SELECT 1 FROM quiz_questions x\n' ||
E'                    WHERE x.content_id = c.id AND x.order_index = %s)\n' ||
E'  RETURNING id\n' ||
E')\n' ||
E'INSERT INTO quiz_options (question_id, option_text, is_correct, order_index)\n' ||
E'SELECT nueva.id, v.texto, v.correcta, v.pos\n' ||
E'FROM nueva, (VALUES %s) AS v(texto, correcta, pos);\n',
             m.name, coalesce(c.order_index::text, 's/n'), q.order_index,
             quote_literal(q.question_text),
             quote_literal(q.question_type::text),
             q.points, q.order_index,
             -- Ancla por titulo, igual que el temario y por la misma razon.
             quote_literal(m.name), quote_literal(c.title),
             q.order_index,
             coalesce(op.lista, '(''(sin opciones)'', false, 0)')
           ) AS linea
    FROM quiz_questions q
    JOIN content  c ON c.id = q.content_id
    JOIN materias m ON m.id = c.subject_id
    LEFT JOIN opciones op ON op.question_id = q.id
),

-- ── Cierre y verificacion ───────────────────────────────────────────────────
pie AS (
    SELECT 4 AS seccion, n AS orden, linea
    FROM (VALUES
      (1, ''),
      (2, 'COMMIT;'),
      (3, ''),
      (4, '-- ============================================================================'),
      (5, '--  VERIFICACION'),
      (6, '--  Compara estos numeros con los de la base de origen. Si una materia sale'),
      (7, '--  con cero piezas, lo mas probable es que falte el usuario admin/director'),
      (8, '--  que queda como autor.'),
      (9, '-- ============================================================================'),
      (10, ''),
      (11, 'SELECT s.name AS materia,'),
      (12, '       count(c.id)                             AS piezas,'),
      (13, '       count(*) FILTER (WHERE c.type = ''quiz'') AS quizzes,'),
      (14, '       count(*) FILTER (WHERE c.content_body ? ''teacher_notes'') AS con_guia'),
      (15, 'FROM subjects s'),
      (16, 'LEFT JOIN content c ON c.subject_id = s.id'),
      (17, 'WHERE s.name ILIKE ''%IA para Ni%os%'''),
      (18, 'GROUP BY s.name'),
      (19, 'ORDER BY s.name;')
    ) AS v(n, linea)
),

todo AS (
    SELECT * FROM encabezado
    UNION ALL SELECT * FROM sql_materias
    UNION ALL SELECT * FROM sql_contenido
    UNION ALL SELECT * FROM sql_quizzes
    UNION ALL SELECT * FROM pie
)

-- Una sola celda con TODO el script. En DBeaver: clic en la celda y
-- Shift+Enter abre el visor de valor, de donde se copia completo.
-- Si tu cliente te la trunca, usa el PASO 1-B de abajo.
SELECT string_agg(linea, E'\n' ORDER BY seccion, orden) AS script_de_siembra
FROM todo;


-- ============================================================================
--  PASO 1-B - LA MISMA SALIDA, UNA FILA POR BLOQUE
--
--  Solo si tu cliente trunca la celda del PASO 1. Es el mismo contenido
--  repartido en filas: exporta la columna a un .sql cuidando que la
--  exportacion NO agregue comillas, encabezado ni separadores, porque cada
--  bloque ya trae saltos de linea adentro.
--
--  Para usarlo, copia desde "WITH materias AS (" del PASO 1 hasta el
--  "todo AS (...)" y termina con este SELECT en vez del de arriba:
--
--    SELECT linea FROM todo ORDER BY seccion, orden;
-- ============================================================================
