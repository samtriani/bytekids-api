-- ============================================================================
--  ByteKids Academy - Tope diario de ByteBot y certificados
--  Fecha: 30-sep-2026
--
--  DOS TABLAS NUEVAS. No se toca ninguna existente.
--
--  1. ai_uso_diario
--     Cuantos mensajes le ha mandado cada alumno a ByteBot cada dia. El tope
--     es 25 por dia (app.ai.limite-diario-alumno); maestros, familias y
--     coordinacion no tienen tope. Se recarga a medianoche, hora del centro de
--     Mexico.
--     Mientras esta tabla NO exista, ByteBot funciona sin tope: la API lo
--     tolera a proposito para no tumbar el chat.
--
--  2. certificados
--     El certificado de una materia terminada. Nace cuando el alumno lo pide y
--     vale cuando un maestro o coordinacion lo entrega. Uno por alumno y
--     materia, con un folio unico (BK-2026-7KQ4M).
--     Mientras esta tabla NO exista, las pantallas de certificado no cargan;
--     el resto de la plataforma no se entera.
--
--  SE PUEDE CORRER VARIAS VECES (IF NOT EXISTS).
-- ============================================================================


-- PASO 1 - Que hay hoy. Lo normal la primera vez: las dos en "false".
SELECT to_regclass('public.ai_uso_diario') IS NOT NULL AS existe_ai_uso_diario,
       to_regclass('public.certificados')  IS NOT NULL AS existe_certificados;


BEGIN;

-- PASO 2 - El tope de ByteBot
CREATE TABLE IF NOT EXISTS ai_uso_diario (
    user_id  uuid    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    fecha    date    NOT NULL,
    mensajes integer NOT NULL DEFAULT 0 CHECK (mensajes >= 0),
    PRIMARY KEY (user_id, fecha)
);
COMMENT ON TABLE ai_uso_diario IS
  'Mensajes a ByteBot por alumno por dia (hora de Mexico). Solo cuenta respuestas que el modelo si dio.';


-- PASO 3 - Los certificados
CREATE TABLE IF NOT EXISTS certificados (
    id            uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id    uuid        NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
    subject_id    uuid        NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    folio         varchar(20) NOT NULL UNIQUE,
    solicitado_en timestamptz NOT NULL DEFAULT now(),
    entregado_en  timestamptz,
    entregado_por uuid        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT certificados_un_alumno_una_materia UNIQUE (student_id, subject_id)
);
CREATE INDEX IF NOT EXISTS certificados_pendientes
    ON certificados (solicitado_en) WHERE entregado_en IS NULL;
COMMENT ON TABLE certificados IS
  'Certificado por materia terminada. entregado_en NULL = pedido pero todavia no valido.';

COMMIT;


-- PASO 4 - Verificacion: las dos en "true".
SELECT to_regclass('public.ai_uso_diario') IS NOT NULL AS existe_ai_uso_diario,
       to_regclass('public.certificados')  IS NOT NULL AS existe_certificados;


-- ============================================================================
--  CONSULTAS UTILES (no hace falta correrlas)
--
--  Quien uso mas a ByteBot hoy:
--    SELECT u.display_name, a.mensajes
--    FROM ai_uso_diario a JOIN users u ON u.id = a.user_id
--    WHERE a.fecha = (now() AT TIME ZONE 'America/Mexico_City')::date
--    ORDER BY a.mensajes DESC;
--
--  Certificados pedidos y todavia sin entregar:
--    SELECT c.folio, u.display_name, s.name, c.solicitado_en
--    FROM certificados c JOIN users u ON u.id = c.student_id JOIN subjects s ON s.id = c.subject_id
--    WHERE c.entregado_en IS NULL ORDER BY c.solicitado_en;
-- ============================================================================
