-- ============================================================================
--  ByteKids Academy - Acentos de la pieza 1 del temario principiante
--  Fecha: 7-sep-2026
--
--  Escribi la descripcion y las instrucciones sin acentos para no arriesgar
--  la codificacion al generar el script, y al alumno le llegan con faltas de
--  ortografia. Este archivo repone solo eso.
--
--  Es lo mismo que el PASO B del script de la guia del maestro, pero suelto,
--  para no tener que volver a correr aquel. Es idempotente y no toca la guia
--  del maestro, ni el enlace, ni el tipo, ni los XP.
--
--  Corre PASO 1, luego PASO 2, luego PASO 3.
-- ============================================================================


-- ============================================================================
--  PASO 1 - COMO ESTA AHORA
-- ============================================================================

SELECT c.title, c.description, c.content_body ->> 'instructions' AS instrucciones
FROM content c JOIN subjects s ON s.id = c.subject_id
WHERE s.name ILIKE '%Ni%os%' AND s.name NOT ILIKE '%Intermedio%'
  AND c.order_index = 1;


-- ============================================================================
--  PASO 2 - LA CORRECCION
-- ============================================================================

UPDATE content c
SET description  = 'La IA no es un robot que piensa como tú: es una máquina que aprende a reconocer patrones a partir de muchos ejemplos. Hoy vas a entrenar una tú mismo, etiquetando peces.',
    content_body = jsonb_set(
        coalesce(c.content_body, '{}'::jsonb), '{instructions}',
        to_jsonb($inst$Entra al enlace y haz la actividad "IA para los océanos".
Si te aparece en inglés, cambia el idioma a Español hasta abajo de la página.

Vas a enseñarle a una máquina a separar peces de basura. Tú le pones las
etiquetas, y eso es justamente entrenarla.

Cuando termines, responde con tus propias palabras:
1. ¿Quién le enseñó a la máquina qué era un pez?
2. ¿Qué pasó cuando le mostraste algo raro que no había visto antes?
3. ¿La máquina entiende qué es un pez, o solo sabe a qué se parece lo que le enseñaste?$inst$::text),
        true)
FROM subjects s
WHERE s.id = c.subject_id
  AND s.name ILIKE '%Ni%os%' AND s.name NOT ILIKE '%Intermedio%'
  AND c.order_index = 1;


-- ============================================================================
--  PASO 3 - VERIFICACION
--  Las tres columnas deben decir "t". Si alguna dice "f", el acento no entro
--  y el problema esta en la codificacion del cliente, no en el texto.
-- ============================================================================

SELECT c.description LIKE '%como tú:%'                              AS desc_con_acento,
       c.content_body ->> 'instructions' LIKE '%enseñó%'            AS ensenio_con_enie,
       c.content_body ->> 'instructions' LIKE '%océanos%'           AS oceanos_con_acento,
       (c.content_body ? 'teacher_notes')                           AS guia_intacta
FROM content c JOIN subjects s ON s.id = c.subject_id
WHERE s.name ILIKE '%Ni%os%' AND s.name NOT ILIKE '%Intermedio%'
  AND c.order_index = 1;
