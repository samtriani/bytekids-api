-- ============================================================================
--  ByteKids Academy - Logros de "IA para Ninos (Principiante)"
--  Fecha: 7-sep-2026
--
--  EL PROBLEMA
--    El catalogo de logros era de un curso de programacion: "Primer Codigo",
--    "Loop Master", "Bug Hunter". Un nino que lleva IA veia seis candados que
--    no tienen nada que ver con lo que esta haciendo.
--
--  LO QUE NO HIZO FALTA
--    Ni migrar el esquema ni tocar Java. AchievementCheckerService ya evalua
--    condition_type = 'subject_missions', que cuenta las entregas APROBADAS
--    del alumno en una materia. Esto es puro dato.
--
--  OJO CON EL NOMBRE DE LA MATERIA
--    La condicion casa por nombre EXACTO:
--        s.content.subject.name = :subjectName
--    Si algun dia renombras la materia, estos logros dejan de desbloquearse
--    en silencio. El PASO 1 verifica que el nombre exista antes de insertar,
--    y el PASO 4 te deja revisarlo cuando quieras.
--
--  Corre PASO 1, luego 2 y 3.
-- ============================================================================


-- ============================================================================
--  PASO 1 - EL NOMBRE DE LA MATERIA TIENE QUE EXISTIR TAL CUAL
--  Debe devolver exactamente una fila. Si no, PARA: los logros no funcionarian.
-- ============================================================================

SELECT id, '[' || name || ']' AS nombre_exacto
FROM subjects
WHERE name = 'IA para Niños (Principiante)';


-- ============================================================================
--  PASO 2 - LOS LOGROS
--  Escalonados sobre las 12 piezas del temario, y cada titulo nombra algo que
--  el nino de verdad hizo, no una metrica.
-- ============================================================================

INSERT INTO achievement_definitions
  (title, description, icon, xp_reward, category, rarity, condition_type, condition_value)
VALUES
  ('Primer contacto con la IA',
   'Completaste tu primera actividad de Inteligencia Artificial. Ya sabes que una máquina aprende de ejemplos.',
   '🤖', 30, 'programacion', 'comun', 'subject_missions',
   '{"subject": "IA para Niños (Principiante)", "count": 1}'),

  ('Entrenador de máquinas',
   'Le enseñaste a una computadora a reconocer cosas por sí sola. Tú pusiste los ejemplos.',
   '🍎', 50, 'programacion', 'comun', 'subject_missions',
   '{"subject": "IA para Niños (Principiante)", "count": 3}'),

  ('Detective de patrones',
   'Descubriste que con pocos ejemplos no alcanza para adivinar la regla.',
   '🔍', 70, 'programacion', 'poco_comun', 'subject_missions',
   '{"subject": "IA para Niños (Principiante)", "count": 5}'),

  ('Cazador de sesgos',
   'Encontraste por qué una IA se equivoca siempre contra los mismos, y supiste cómo arreglarlo.',
   '⚖️', 100, 'especial', 'raro', 'subject_missions',
   '{"subject": "IA para Niños (Principiante)", "count": 8}'),

  ('Domador de chatbots',
   'Construiste un asistente que responde. Y entendiste que sigue TUS reglas, no las suyas.',
   '💬', 120, 'programacion', 'raro', 'subject_missions',
   '{"subject": "IA para Niños (Principiante)", "count": 10}'),

  ('Constructor de IA',
   'Terminaste el curso completo de IA para Niños. Ya puedes explicarle a un adulto qué es la Inteligencia Artificial.',
   '🏅', 200, 'especial', 'epico', 'subject_missions',
   '{"subject": "IA para Niños (Principiante)", "count": 12}')

ON CONFLICT (title) DO UPDATE
  SET description     = EXCLUDED.description,
      icon            = EXCLUDED.icon,
      xp_reward       = EXCLUDED.xp_reward,
      category        = EXCLUDED.category,
      rarity          = EXCLUDED.rarity,
      condition_type  = EXCLUDED.condition_type,
      condition_value = EXCLUDED.condition_value,
      is_active       = true;


-- ============================================================================
--  PASO 3 - APAGAR LOS LOGROS DE PROGRAMACION QUE NO APLICAN
--  Baja logica: no se borran, y si algun dia abres un curso de codigo los
--  vuelves a encender con is_active = true.
--  Si alguien YA se gano uno, su fila en student_achievements se conserva.
-- ============================================================================

UPDATE achievement_definitions
SET is_active = false
WHERE title IN ('Primer Código', 'Loop Master', 'Bug Hunter')
   OR (condition_value ->> 'subject') IN ('python', 'Python', 'Scratch', 'Roblox');


-- ============================================================================
--  PASO 4 - VERIFICACION
-- ============================================================================

SELECT title, icon, rarity, xp_reward,
       condition_value ->> 'count'   AS requiere,
       condition_value ->> 'subject' AS materia,
       is_active,
       -- "t" significa que el nombre de la materia SI existe en subjects.
       EXISTS (SELECT 1 FROM subjects s
               WHERE s.name = condition_value ->> 'subject') AS materia_existe
FROM achievement_definitions
WHERE is_active
ORDER BY (condition_value ->> 'count')::int NULLS FIRST, title;

-- Si "materia_existe" sale en "f", el logro nunca se va a desbloquear:
-- el nombre en condition_value no coincide con ninguna materia real.
