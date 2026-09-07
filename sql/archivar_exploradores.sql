-- ============================================================================
--  ByteKids Academy - Archivar exploradores que no se inscribieron
--
--  MODELO
--    Cada nino de clase muestra tiene su PROPIA cuenta, con su nombre real:
--        username      explorador.antonio
--        display_name  Antonio Ruiz
--    Asi el XP empieza en cero sin resetear nada, y si la familia se inscribe
--    el nino conserva lo que hizo en la muestra: su primera mision, sus
--    primeros XP, su proyecto.
--
--  QUE HACE ESTE SCRIPT
--    Da de baja (is_active = false) las cuentas de exploradores que NO se
--    inscribieron, para que no se acumulen en el listado de Alumnos.
--    NO borra nada: la cuenta y su historia se conservan y se puede reactivar.
--
--  LA REGLA
--    Se archiva a un explorador solo si esta inscrito UNICAMENTE en salones
--    de muestra. En cuanto lo inscribes a un salon real, este script deja de
--    tocarlo para siempre.
--
--  CONFIGURACION
--    Los salones de muestra se reconocen por su nombre. Ajusta el patron de
--    abajo si les pones otro. Y ANTIGUEDAD son los dias que deben pasar antes
--    de archivar, para no dar de baja a alguien que todavia esta decidiendo.
--
--  Corre PASO 1, revisa la lista, y luego el PASO 2.
-- ============================================================================


-- ============================================================================
--  PASO 1 - QUIENES SE ARCHIVARIAN (no cambia nada)
--  Revisa la columna "salones": si ves un salon real ahi, algo esta mal en
--  el patron y NO debes seguir.
-- ============================================================================

SELECT u.username, u.display_name,
       u.created_at::date                                    AS creado,
       (now()::date - u.created_at::date)                    AS dias,
       coalesce(string_agg(c.name, ', ' ORDER BY c.name), '(sin salon)') AS salones,
       (SELECT coalesce(sum(x.amount), 0) FROM xp_events x WHERE x.student_id = u.id) AS xp
FROM users u
LEFT JOIN classroom_enrollments ce ON ce.student_id = u.id AND ce.is_active
LEFT JOIN classrooms            c  ON c.id = ce.classroom_id
WHERE u.role = 'student'
  AND u.is_active
  AND u.username ~ '^explorador\.'
GROUP BY u.id, u.username, u.display_name, u.created_at
HAVING count(*) FILTER (WHERE c.id IS NOT NULL AND c.name NOT ILIKE '%muestra%') = 0
   AND (now()::date - u.created_at::date) >= 14
ORDER BY u.created_at;


-- ============================================================================
--  PASO 2 - ARCHIVAR
--  Misma condicion exacta que el PASO 1. Baja logica: is_active = false.
-- ============================================================================

BEGIN;

CREATE TEMP TABLE a_archivar AS
SELECT u.id
FROM users u
LEFT JOIN classroom_enrollments ce ON ce.student_id = u.id AND ce.is_active
LEFT JOIN classrooms            c  ON c.id = ce.classroom_id
WHERE u.role = 'student'
  AND u.is_active
  AND u.username ~ '^explorador\.'
GROUP BY u.id, u.created_at
HAVING count(*) FILTER (WHERE c.id IS NOT NULL AND c.name NOT ILIKE '%muestra%') = 0
   AND (now()::date - u.created_at::date) >= 14;

-- Cortafuegos: este script solo debe tocar exploradores. Si por un cambio de
-- patron cazara a alguien mas, mejor que reviente aqui.
DO $$
DECLARE intrusos int;
BEGIN
  SELECT count(*) INTO intrusos
  FROM users u JOIN a_archivar a ON a.id = u.id
  WHERE u.username !~ '^explorador\.';
  IF intrusos > 0 THEN
    RAISE EXCEPTION 'El filtro cazo % cuentas que no son exploradores. Abortando.', intrusos;
  END IF;
END $$;

UPDATE users SET is_active = false WHERE id IN (SELECT id FROM a_archivar);

COMMIT;


-- ============================================================================
--  PASO 3 - VERIFICACION
-- ============================================================================

SELECT u.is_active, count(*) AS cuentas
FROM users u
WHERE u.username ~ '^explorador\.'
GROUP BY u.is_active;

-- Para reactivar a uno que si se inscribio despues:
--   UPDATE users SET is_active = true WHERE username = 'explorador.antonio';
