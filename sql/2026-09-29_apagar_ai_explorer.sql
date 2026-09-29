-- ============================================================================
--  ByteKids Academy - Apagar el logro "AI Explorer" mientras no se pueda ganar
--  Fecha: 29-sep-2026
--
--  POR QUE
--    "AI Explorer" pide 20 conversaciones con ByteBot (condition_type
--    ai_conversations). Pero ByteBot NO guarda las conversaciones en ningun
--    lado, y el evaluador de logros no implementa esa condicion: cae en el
--    "default -> false". Nadie lo puede ganar.
--
--    Mientras estuviera activo, cada nino lo veia como meta, y el dashboard
--    le decia "de 3 posibles" contando uno imposible. Un logro que no se
--    puede ganar ensena que los logros no son de fiar.
--
--  QUE HACE
--    Lo marca inactivo. No lo borra: si algun dia se implementa el conteo de
--    conversaciones, se vuelve a prender con el PASO 3.
--
--  SE PUEDE CORRER VARIAS VECES.
-- ============================================================================


-- PASO 1 - Como esta hoy. Debe salir una fila, con is_active = true.
SELECT title, condition_type, condition_value, is_active,
       (SELECT count(*) FROM student_achievements sa WHERE sa.achievement_id = a.id) AS quienes_lo_tienen
FROM achievement_definitions a
WHERE title = 'AI Explorer';
-- "quienes_lo_tienen" debe ser 0: nadie pudo ganarlo. Si NO es 0, para y
-- avisa: alguien lo gano por otra via y hay que entender cual.


-- PASO 2 - Apagarlo.
UPDATE achievement_definitions
SET is_active = false
WHERE title = 'AI Explorer'
  AND condition_type = 'ai_conversations';


-- PASO 3 - (Solo si algun dia se implementa el conteo) volver a prenderlo.
--   UPDATE achievement_definitions SET is_active = true WHERE title = 'AI Explorer';


-- VERIFICACION: ningun logro activo puede tener una condicion que el
-- evaluador no sepa revisar. Debe salir VACIO.
SELECT title, condition_type
FROM achievement_definitions
WHERE is_active
  AND condition_type NOT IN ('missions_count', 'streak_days', 'xp_total',
                             'subject_missions', 'subject_level',
                             'project_count', 'subject_content');
