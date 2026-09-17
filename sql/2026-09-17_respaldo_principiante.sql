-- ============================================================================
--  ByteKids Academy - Respaldo de "IA para Ninos (Principiante)"
--  Fecha: 17-sep-2026
--
--  PARA QUE
--    Guardar como esta HOY el temario del principiante, antes de correr
--    sql/2026-09-15_frutas_en_ml4kids.sql y sql/2026-09-17_pulir_principiante.sql.
--    Si algo no gusta, el PASO 4 lo regresa tal cual estaba.
--
--  POR QUE UNA TABLA Y NO UN ARCHIVO
--    Un volcado a texto hay que guardarlo, encontrarlo y pegarlo de vuelta sin
--    equivocarse. Una tabla en la misma base se restaura con UNA sentencia, se
--    puede consultar para comparar antes y despues, y no depende de que nadie
--    conserve un archivo. Cuando ya no se necesite, se tira (PASO 5).
--
--  QUE GUARDA
--    Las 12 piezas COMPLETAS, no solo las cuatro que se van a tocar. Sale casi
--    gratis y ademas protege contra un error en los WHERE de los otros
--    scripts, que es justo el riesgo que un respaldo debe cubrir.
--
--    No hace falta respaldar el quiz (preguntas y opciones): ninguno de los
--    dos scripts los toca. Tampoco las entregas de los alumnos.
--
--  ES SEGURO REPETIRLO
--    Si la tabla ya existe, NO se sobrescribe. Es a proposito: la foto que
--    vale es la PRIMERA, la de antes del cambio. Correrlo otra vez despues de
--    mover el temario no debe pisarla con datos ya modificados.
-- ============================================================================


-- ============================================================================
--  PASO 1 - LA FOTO
--  El nombre lleva la fecha para que un respaldo futuro no choque con este.
-- ============================================================================

CREATE TABLE IF NOT EXISTS respaldo_principiante_20260917 AS
SELECT c.id,
       c.order_index,
       c.title,
       c.description,
       c.type,
       c.xp_reward,
       c.difficulty,
       c.estimated_minutes,
       c.content_body,
       c.is_published,
       c.is_active,
       now() AS respaldado_en
FROM content c
JOIN subjects s ON s.id = c.subject_id
WHERE s.name ILIKE '%Ni%os%' AND s.name NOT ILIKE '%Intermedio%';


-- ============================================================================
--  PASO 2 - COMPROBAR QUE LA FOTO SALIO BIEN
--
--  Deben ser 12 piezas, con guia del maestro en todas y los enlaces actuales
--  (los de Teachable Machine todavia deben aparecer: son los que se van a
--  mudar). Si aqui sale algo raro, PARA: no corras los otros scripts.
-- ============================================================================

SELECT count(*)                                            AS piezas,
       count(*) FILTER (WHERE content_body ? 'teacher_notes') AS con_guia,
       count(*) FILTER (WHERE content_body ->> 'url' ILIKE '%teachablemachine%')
                                                           AS en_teachable_machine,
       min(order_index)                                    AS primera,
       max(order_index)                                    AS ultima,
       min(respaldado_en)                                  AS cuando
FROM respaldo_principiante_20260917;

-- El detalle, pieza por pieza, para poder compararlo despues.
SELECT order_index AS pieza, type, title,
       content_body ->> 'url' AS url,
       length(content_body ->> 'instructions') AS largo_instrucciones
FROM respaldo_principiante_20260917
ORDER BY order_index;


-- ============================================================================
--  PASO 3 - AHORA SI, LOS OTROS SCRIPTS
--
--    1. sql/2026-09-15_frutas_en_ml4kids.sql     (pieza 2)
--    2. sql/2026-09-17_pulir_principiante.sql    (piezas 7, 8, 10 y 12)
--
--  Los dos necesitan que las cuentas de Machine Learning for Kids existan
--  antes -- la del maestro y las de los alumnos -- porque las instrucciones
--  le dicen al nino que entre con su usuario:
--    https://machinelearningforkids.co.uk/?lang=es
--
--  Para ver que cambio, una vez corridos:
--
--    SELECT r.order_index AS pieza, r.title,
--           r.content_body ->> 'url' AS antes,
--           c.content_body ->> 'url' AS ahora
--    FROM respaldo_principiante_20260917 r
--    JOIN content c ON c.id = r.id
--    WHERE r.content_body IS DISTINCT FROM c.content_body
--    ORDER BY r.order_index;
-- ============================================================================


-- ============================================================================
--  PASO 4 - DESHACER  (solo si algo no gusta)
--
--  Regresa las 12 piezas a como estaban. Casa por id, asi que no depende de
--  los titulos ni del orden.
--
--  Esta comentado a proposito: no debe correrse por accidente al pasar el
--  archivo completo. Quita los guiones de las lineas de abajo para usarlo.
-- ============================================================================

-- UPDATE content c
-- SET title             = r.title,
--     description       = r.description,
--     type              = r.type,
--     xp_reward         = r.xp_reward,
--     difficulty        = r.difficulty,
--     estimated_minutes = r.estimated_minutes,
--     content_body      = r.content_body,
--     is_published      = r.is_published,
--     is_active         = r.is_active
-- FROM respaldo_principiante_20260917 r
-- WHERE c.id = r.id;


-- ============================================================================
--  PASO 5 - TIRAR EL RESPALDO  (cuando ya estes conforme)
--
--  Tambien comentado. No hay prisa: una tabla de 12 filas no le pesa a nadie,
--  y mientras exista se puede volver atras.
-- ============================================================================

-- DROP TABLE respaldo_principiante_20260917;
