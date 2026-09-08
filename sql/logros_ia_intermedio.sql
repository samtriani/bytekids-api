-- ============================================================================
--  ByteKids Academy - Logros de "IA para Niños (Intermedio)"
--  Fecha: 8-sep-2026
--
--  EL PROBLEMA
--    Los 6 logros que existian eran todos de Principiante, y la condicion casa
--    por nombre EXACTO de materia. Un alumno que va en Intermedio puede
--    completar sus 17 piezas y no desbloquear absolutamente nada: sus entregas
--    no cuentan para ningun logro.
--
--  LA ESCALA
--    Intermedio tiene 17 piezas, no 12, asi que los cortes son otros. Y como
--    es el segundo nivel, arranca en "poco_comun": ya no es la primera vez que
--    el nino toca una IA.
--
--  NOMBRES
--    Cada titulo nombra algo que el alumno hizo en una pieza concreta del
--    temario, no una metrica. El corte esta puesto para caer justo despues de
--    esa pieza si va en orden.
--
--  Corre PASO 1, luego 2 y 3.
-- ============================================================================


-- ============================================================================
--  PASO 1 - EL NOMBRE DE LA MATERIA
--  Debe devolver exactamente una fila. Si no, PARA: los logros no servirian.
-- ============================================================================

SELECT id, '[' || name || ']' AS nombre_exacto,
       (SELECT count(*) FROM content c
        WHERE c.subject_id = s.id AND c.is_published AND c.is_active) AS piezas_publicadas
FROM subjects s
WHERE s.name = 'IA para Niños (Intermedio)';


-- ============================================================================
--  PASO 2 - LOS LOGROS
-- ============================================================================

INSERT INTO achievement_definitions
  (title, description, icon, xp_reward, category, rarity, condition_type, condition_value)
VALUES
  -- Cae tras la pieza 3, "El experimento del ruido".
  ('Oído de máquina',
   'Entrenaste una IA que reconoce sonidos y descubriste que el ruido de fondo también le enseña.',
   '🎧', 60, 'programacion', 'poco_comun', 'subject_missions',
   '{"subject": "IA para Niños (Intermedio)", "count": 3}'),

  -- Cae tras la pieza 6, el quiz de entrenamiento y trampas.
  ('Cazador de tramposos',
   'Descubriste que un modelo puede sacar 10 de 10 y aun así no haber aprendido nada.',
   '🕵️', 90, 'especial', 'poco_comun', 'subject_missions',
   '{"subject": "IA para Niños (Intermedio)", "count": 6}'),

  -- Cae tras la pieza 9, "Una IA que lee tu cuerpo".
  ('Arquitecto de neuronas',
   'Ya sabes qué hay dentro: neuronas, pesos y capas. Y construiste una IA que lee tu cuerpo.',
   '🧠', 120, 'programacion', 'raro', 'subject_missions',
   '{"subject": "IA para Niños (Intermedio)", "count": 9}'),

  -- Cae tras la pieza 11, "La tabla de errores".
  ('Medidor de errores',
   'Ya no dices "funciona bien": mides cuántas veces acierta y cuántas se equivoca, y en qué.',
   '📏', 140, 'especial', 'raro', 'subject_missions',
   '{"subject": "IA para Niños (Intermedio)", "count": 11}'),

  -- Cae tras la pieza 14, "Cazador de invenciones".
  ('Detector de inventos',
   'Cachaste a una IA inventando una respuesta con toda seguridad. Ya no le crees todo.',
   '🔎', 170, 'especial', 'raro', 'subject_missions',
   '{"subject": "IA para Niños (Intermedio)", "count": 14}'),

  -- Cae al terminar las 17.
  ('Ingeniero de IA',
   'Terminaste el nivel intermedio completo y construiste una IA para tu comunidad. Ya no solo la usas: la diseñas.',
   '⚙️', 300, 'especial', 'epico', 'subject_missions',
   '{"subject": "IA para Niños (Intermedio)", "count": 17}')

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
--  PASO 3 - APAGAR LOS QUE EL EVALUADOR NO SABE CALCULAR
--
--  AchievementCheckerService solo implementa seis reglas:
--    missions_count · streak_days · xp_total
--    subject_missions · subject_level · project_count
--  Cualquier otra cae en "default -> false", o sea que NUNCA se desbloquea.
--  Ahi caen "Team Player" (ayudar en la comunidad) y "Top Estudiante"
--  (ranking mensual): se ven bonitos y son inalcanzables.
--
--  "AI Explorer" NO se apaga aqui: su regla si se va a implementar, porque
--  premiar que el nino le pregunte al tutor es el habito que queremos.
-- ============================================================================

UPDATE achievement_definitions
SET is_active = false
WHERE is_active
  AND title <> 'AI Explorer'
  AND (condition_type IS NULL
       OR condition_type NOT IN ('missions_count', 'streak_days', 'xp_total',
                                 'subject_missions', 'subject_level', 'project_count'));


-- ============================================================================
--  PASO 4 - VERIFICACION
--  "alcanzable = f" es una medalla que nadie podra ganar.
-- ============================================================================

SELECT coalesce(condition_value ->> 'subject', '(transversal)') AS materia,
       (condition_value ->> 'count')::int                       AS requiere,
       title, rarity, xp_reward,
       condition_type IN ('missions_count','streak_days','xp_total',
                          'subject_missions','subject_level','project_count')
         AND (condition_value ->> 'subject' IS NULL
              OR EXISTS (SELECT 1 FROM subjects s
                         WHERE s.name = condition_value ->> 'subject'))    AS alcanzable
FROM achievement_definitions
WHERE is_active
ORDER BY materia, requiere NULLS FIRST, title;
