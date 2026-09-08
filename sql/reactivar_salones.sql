-- ============================================================================
--  ByteKids Academy - Reactivar salones dados de baja por error
--  Fecha: 8-sep-2026
--
--  QUE PASO
--    En Asignaciones habia una X diminuta junto a cada salon de la lista de
--    navegacion. No quitaba nada local: daba de baja el salon COMPLETO, y por
--    eso desaparecia tambien del menu de Salones. Ya se quito esa X; la baja
--    de un salon vive solo en su pantalla dedicada.
--
--  NO SE PERDIO NADA
--    Es baja logica (is_active = false). El salon, sus inscripciones, su
--    horario y su contenido asignado siguen intactos: solo dejo de listarse.
--    Reactivarlo lo devuelve tal como estaba.
--
--  Corre PASO 1, identifica cual quieres, y luego el 2 o el 3.
-- ============================================================================


-- ============================================================================
--  PASO 1 - QUE SALONES ESTAN DADOS DE BAJA
--  Con lo que cada uno conserva, para que veas que nada se borro.
-- ============================================================================

SELECT c.id,
       c.name,
       c.grade_level,
       c.section,
       c.school_year,
       u.display_name AS profesor,
       (SELECT count(*) FROM classroom_enrollments e
        WHERE e.classroom_id = c.id AND e.is_active)      AS alumnos,
       (SELECT count(*) FROM class_schedules s
        WHERE s.classroom_id = c.id)                      AS clases_en_horario,
       (SELECT count(*) FROM content_assignments a
        WHERE a.classroom_id = c.id AND a.is_active)      AS piezas_asignadas
FROM classrooms c
LEFT JOIN users u ON u.id = c.teacher_id
WHERE NOT c.is_active
ORDER BY c.name;


-- ============================================================================
--  PASO 2 - REACTIVAR TODOS LOS QUE ESTEN DE BAJA
--  Sirve ahora, que ninguna baja fue intencional.
-- ============================================================================

UPDATE classrooms SET is_active = true WHERE NOT is_active;


-- ============================================================================
--  PASO 3 - O REACTIVAR SOLO UNO, POR NOMBRE
--  Usalo en vez del PASO 2 si algun salon SI querias darlo de baja.
-- ============================================================================

-- UPDATE classrooms SET is_active = true
-- WHERE name = 'Salón IA para Niños (Principiante)';


-- ============================================================================
--  PASO 4 - VERIFICACION
-- ============================================================================

SELECT name, school_year, is_active
FROM classrooms
ORDER BY is_active DESC, name;

-- Y si algun salon quedo sin alumnos activos, aqui se ve:
SELECT c.name,
       (SELECT count(*) FROM classroom_enrollments e
        WHERE e.classroom_id = c.id AND e.is_active) AS alumnos_activos
FROM classrooms c
WHERE c.is_active
ORDER BY c.name;
