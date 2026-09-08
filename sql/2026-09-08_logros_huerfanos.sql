-- ============================================================================
--  ByteKids Academy - Apagar logros que nadie puede ganar
--  Fecha: 8-sep-2026
--
--  EL PROBLEMA
--    Quedaron logros del catalogo original apuntando a materias que ya no
--    existen: "Web Wizard" pide completar las misiones de HTML/CSS, y no hay
--    ninguna materia con ese nombre. Son medallas imposibles: el nino las ve
--    bloqueadas para siempre y nunca sabe por que no avanza.
--
--    La condicion casa por nombre EXACTO de materia
--    (s.content.subject.name = :subjectName), asi que basta con que el nombre
--    no exista en `subjects` para que el logro sea inalcanzable.
--
--  NO SE BORRA NADA
--    Baja logica (is_active = false). Si algun dia abres el curso de
--    programacion o de robotica, los vuelves a encender. Y si alguien ya se
--    gano uno, su fila en student_achievements se conserva intacta.
--
--  Corre PASO 1, revisa la lista, y luego 2 y 3.
-- ============================================================================


-- ============================================================================
--  PASO 1 - RADIOGRAFIA DEL CATALOGO
--  "alcanzable = f" es una medalla que nadie podra ganar jamas.
-- ============================================================================

SELECT title,
       condition_type,
       condition_value ->> 'subject' AS materia_que_pide,
       condition_value ->> 'count'   AS requiere,
       CASE
         WHEN condition_type IS NULL                       THEN false
         WHEN condition_value ->> 'subject' IS NULL         THEN true   -- transversal
         WHEN EXISTS (SELECT 1 FROM subjects s
                      WHERE s.name = condition_value ->> 'subject')     THEN true
         ELSE false
       END AS alcanzable
FROM achievement_definitions
WHERE is_active
ORDER BY alcanzable, title;


-- ============================================================================
--  PASO 2 - APAGAR LOS INALCANZABLES
--  Regla automatica: pide una materia y esa materia no existe.
--  Los transversales (racha, XP total) NO tienen materia y no se tocan.
-- ============================================================================

UPDATE achievement_definitions
SET is_active = false
WHERE is_active
  AND condition_value ->> 'subject' IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM subjects s
                  WHERE s.name = condition_value ->> 'subject');


-- ============================================================================
--  PASO 3 - APAGAR LOS QUE NO TIENEN CONDICION
--  Sin condition_type el evaluador los descarta: se pintan bloqueados y no
--  hay forma de ganarlos. Revisa la lista del PASO 1 antes de correr esto.
-- ============================================================================

UPDATE achievement_definitions
SET is_active = false
WHERE is_active
  AND (condition_type IS NULL OR condition_value IS NULL);


-- ============================================================================
--  PASO 4 - COMO QUEDO
--  Deberian quedar los 6 de IA para Ninos y los transversales de racha.
-- ============================================================================

SELECT title, category, rarity, xp_reward,
       coalesce(condition_value ->> 'subject', '(transversal)') AS materia,
       condition_type
FROM achievement_definitions
WHERE is_active
ORDER BY materia, (condition_value ->> 'count')::int NULLS FIRST, title;

-- Y los que quedaron apagados, por si quieres reactivar alguno:
--   UPDATE achievement_definitions SET is_active = true WHERE title = '...';
SELECT title AS apagados FROM achievement_definitions
WHERE NOT is_active ORDER BY title;
