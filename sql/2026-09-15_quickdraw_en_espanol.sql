-- ============================================================================
--  ByteKids Academy - Quick, Draw! en espanol
--  Fecha: 15-sep-2026
--
--  EL PROBLEMA
--    El enlace a Quick, Draw! esta guardado sin parametro de idioma, asi que
--    Google decide el idioma por el navegador. En un celular configurado en
--    ingles --o con el idioma del sistema en ingles, que es comun en telefonos
--    de gama alta-- al nino le abre "Can a neural net learn to recognize
--    doodling?" en vez de "¿Puede una red neuronal reconocer tus dibujos?".
--
--  LA SOLUCION, Y POR QUE ESE PARAMETRO
--    Es ?locale=es, NO ?lang=es. No es una suposicion: el propio sitio lo
--    declara en su cabecera, que es la version que Google considera canonica
--    para espanol:
--
--      <link rel="alternate" hreflang="es"
--            href="https://quickdraw.withgoogle.com/?locale=es" />
--
--    Verificado el 15-sep-2026: sin el parametro la pagina responde "Can a
--    neural net...", y con el responde "¿Puede una red neuronal...".
--
--  POR QUE SE ARREGLA EL ENLACE Y NO SE LE AVISA AL NINO
--    La pieza 1 del temario resuelve esto pidiendole al alumno "si te aparece
--    en ingles, cambia el idioma hasta abajo de la pagina". Funciona, pero le
--    carga a un nino de nueve anos una tarea que no es la de la clase, y en
--    celular ese selector queda al final de un scroll largo. Si el enlace
--    puede llegar ya en espanol, que llegue.
--
--  BUSCA POR URL, NO POR order_index
--    A proposito. No se cuantas piezas apuntan a Quick, Draw! ni en cual de
--    las dos materias estan, y un indice escrito a mano se rompe el dia que
--    alguien reordene el temario. Buscar por dominio encuentra todas, en
--    Principiante y en Intermedio.
--
--  ES IDEMPOTENTE
--    El PASO 2 ignora las filas que ya traen locale. Se puede volver a correr.
--
--  Corre PASO 1, luego PASO 2, luego PASO 3.
-- ============================================================================


-- ============================================================================
--  PASO 1 - COMO ESTA AHORA
--  Si no devuelve filas, el enlace no esta en content_body.url: revisa si
--  quedo escrito dentro de 'instructions' (ahi tambien lo encuentra el LIKE
--  de abajo) o si el dominio se guardo de otra forma.
-- ============================================================================

SELECT s.name                               AS materia,
       c.order_index                        AS pieza,
       c.title,
       c.content_body ->> 'url'             AS url_actual,
       c.content_body ->> 'instructions'    AS instrucciones
FROM content c
JOIN subjects s ON s.id = c.subject_id
WHERE c.content_body ->> 'url' ILIKE '%quickdraw.withgoogle.com%'
   OR c.content_body ->> 'instructions' ILIKE '%quickdraw.withgoogle.com%'
ORDER BY s.name, c.order_index;


-- ============================================================================
--  PASO 2 - LA CORRECCION
--  Respeta una query string que ya exista (usa & en vez de ?), aunque hoy no
--  haya ninguna: el dia que alguien agregue un parametro, esto no la parte.
-- ============================================================================

UPDATE content c
SET content_body = jsonb_set(
        coalesce(c.content_body, '{}'::jsonb),
        '{url}',
        to_jsonb(
            CASE
                WHEN position('?' in (c.content_body ->> 'url')) > 0
                    THEN (c.content_body ->> 'url') || '&locale=es'
                ELSE (c.content_body ->> 'url') || '?locale=es'
            END
        ),
        true)
WHERE c.content_body ->> 'url' ILIKE '%quickdraw.withgoogle.com%'
  -- El candado que lo hace idempotente: si ya trae idioma, no lo toca.
  AND c.content_body ->> 'url' NOT ILIKE '%locale=%';


-- ============================================================================
--  PASO 3 - VERIFICACION
--  "listo" debe decir t en todas las filas. Si alguna dice f, esa pieza tiene
--  el enlace guardado de una forma que el PASO 2 no alcanzo: revisala a mano.
-- ============================================================================

SELECT s.name                       AS materia,
       c.order_index                AS pieza,
       c.title,
       c.content_body ->> 'url'     AS url_final,
       (c.content_body ->> 'url') ILIKE '%locale=es%' AS listo
FROM content c
JOIN subjects s ON s.id = c.subject_id
WHERE c.content_body ->> 'url' ILIKE '%quickdraw.withgoogle.com%'
ORDER BY s.name, c.order_index;


-- ============================================================================
--  DESPUES DE CORRER ESTO
--
--  1. Abre la pieza como alumno desde un celular y comprueba que llega en
--     espanol. El navegador cachea la pagina de Google, asi que si la abriste
--     antes en ingles puede que necesites recargar.
--
--  2. Revisa las instrucciones que devolvio el PASO 1. Si esa pieza le dice
--     al alumno "si te aparece en ingles, cambia el idioma abajo", esa linea
--     ya sobra y ahora confunde mas de lo que ayuda.
--
--  3. NO se tocaron los otros enlaces del temario --Teachable Machine,
--     code.org, ML for Kids, Scratch-- porque no pude confirmar cual es su
--     parametro de idioma: son aplicaciones que arman la pagina con
--     JavaScript y, a diferencia de Quick, Draw!, ninguna declara una version
--     en espanol en su cabecera. Inventarles un ?lang=es podria dejar un
--     enlace roto, que es peor que uno en ingles. Para saberlo hay que abrir
--     cada uno en un navegador con el idioma en ingles y ver que pasa.
-- ============================================================================
