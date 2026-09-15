-- ============================================================================
--  ByteKids Academy - La pieza de las frutas se muda a Machine Learning for Kids
--  Fecha: 15-sep-2026
--
--  POR QUE
--    Teachable Machine NO funciona en tablet. No es un fallo de carga: Google
--    bloquea los dispositivos moviles a proposito y muestra "Sorry...
--    Teachable Machine isn't supported here :(". El reporte oficial lleva
--    abierto desde 2021 sin respuesta, asi que no hay ajuste de navegador ni
--    version que lo arregle:
--      https://github.com/googlecreativelab/teachablemachine-community/issues/172
--
--    Machine Learning for Kids si funciona en la tablet. Probado el 15-sep.
--
--  POR QUE ESTA HERRAMIENTA Y NO OTRA
--    Se descartaron varias. Lo que la deja parada:
--      - Los proyectos de IMAGENES no piden ninguna API key. El propio sitio
--        lo dice: "Images, numbers and sound projects don't require API Keys".
--        Solo los de TEXTO necesitan una llave de watsonx. Ojo con eso si
--        algun dia se agrega una pieza de texto.
--      - La cuenta de maestro crea los usuarios de los alumnos en bloque y
--        NO pide correo de los ninos. Con LFPDPPP de por medio, eso no es un
--        detalle menor: es la razon principal para preferirla.
--      - Es gratuita y la mantiene Dale Lane (IBM/taxinomitis), no es un
--        producto que pueda desaparecer de un dia para otro.
--
--    Se descarto la app de Teachable Machine de la App Store: NO es de Google,
--    es de un tercero (Robocar Ltd). Una app no oficial que procesa video de
--    ninos no entra aqui, por mas que ahorre trabajo.
--
--  LA PEDAGOGIA NO CAMBIA
--    Es el mismo ejercicio con otra herramienta. Los tres momentos que la
--    pieza tiene que producir siguen intactos:
--      1. El alumno decide cuales son las clases (los cajones).
--      2. El alumno pone los ejemplos, y mover la fruta entre foto y foto
--         importa: con 30 fotos identicas el modelo memoriza el fondo.
--      3. EL REMATE: le muestra la mano y el modelo igual contesta "platano".
--         Descubre solo que la maquina no puede decir "no se".
--    Si al adaptarlo se pierde el punto 3, se perdio la clase.
--
--  ANTES DE CORRER ESTO
--    Crea tu cuenta de maestro y las de los alumnos. Sin eso, el grupo llega a
--    la pagina y se atora en el registro:
--      https://machinelearningforkids.co.uk/?lang=es
--
--  OJO CON EL ESPANOL
--    La traduccion al espanol de esa herramienta esta incompleta y en partes
--    desactualizada: quedan textos en ingles, alguno con erratas ("Haz una
--    foto par probar tu modelo"), y hay pantallas de ayuda que todavia hablan
--    de API keys de IBM Watson, un servicio que IBM dio de baja en 2021. No
--    afecta al ejercicio, pero avisale al maestro para que no lo desconcierte.
--
--  Corre PASO 1, LEE lo que devuelve, y solo entonces PASO 2 y PASO 3.
-- ============================================================================


-- ============================================================================
--  PASO 1 - QUE DICE HOY  (no lo saltes)
--
--  Este script reescribe 'url' e 'instructions'. Lee primero lo que hay: si el
--  texto actual tiene algo que valga la pena conservar, incorporalo al PASO 2
--  antes de correrlo. La guia del maestro (teacher_notes) NO se toca.
-- ============================================================================

SELECT c.order_index                     AS pieza,
       c.title,
       c.type,
       c.xp_reward,
       c.content_body ->> 'url'          AS url_actual,
       c.content_body ->> 'instructions' AS instrucciones_actuales
FROM content c
JOIN subjects s ON s.id = c.subject_id
WHERE s.name ILIKE '%Ni%os%' AND s.name NOT ILIKE '%Intermedio%'
  AND c.order_index = 2;


-- ============================================================================
--  PASO 2 - LA MUDANZA
--
--  Solo cambia el enlace y las instrucciones. No toca el tipo, ni los XP, ni
--  la guia del maestro, ni las asignaciones a salones.
-- ============================================================================

UPDATE content c
SET content_body = coalesce(c.content_body, '{}'::jsonb) || jsonb_build_object(
    'url',           'https://machinelearningforkids.co.uk/?lang=es',
    'resource_type', 'enlace',
    'instructions',  $inst$Hoy vas a enseñarle a la computadora a distinguir dos frutas.

PREPARA TU PROYECTO
1. Entra al enlace e inicia sesión con el usuario que te dio tu maestro.
2. Dale a "Añadir un nuevo proyecto". Ponle el nombre que quieras y en el tipo
   elige "imágenes".
3. Ábrelo y entra a "Entrenar".

ENSEÑALE LAS DOS FRUTAS
4. Dale a "Añadir etiqueta" y escribe el nombre de tu primera fruta, por
   ejemplo "plátano". Haz lo mismo con la segunda, por ejemplo "manzana".
   Cada etiqueta es un cajón: ahí vas a meter los ejemplos.
5. Toma como 10 fotos de cada fruta con la cámara y guárdalas en su cajón.

   Importante: MUEVE la fruta entre foto y foto. Cámbiala de lugar, gírala,
   acércala, aléjala. Si tomas 10 fotos idénticas, la computadora aprende el
   fondo en lugar de la fruta, y después falla sin que sepas por qué.

ENTRÉNALA Y PRUÉBALA
6. Ve a "Aprender & Probar" y dale a "Entrenar nuevo modelo". Espera a que
   termine.
7. Enséñale tus frutas a la cámara. ¿Le atina?

AHORA LO IMPORTANTE
8. Enséñale TU MANO. O un lápiz. O tu zapato. Algo que nunca le enseñaste.

Cuando termines, contesta con tus propias palabras:
1. ¿Qué contestó cuando le enseñaste tu mano?
2. ¿Por qué crees que contestó algo, en vez de decir "no sé"?
3. ¿Quién decidió cuáles eran las respuestas posibles?$inst$)
FROM subjects s
WHERE s.id = c.subject_id
  AND s.name ILIKE '%Ni%os%' AND s.name NOT ILIKE '%Intermedio%'
  AND c.order_index = 2;


-- ============================================================================
--  PASO 3 - VERIFICACION
--  Las cuatro columnas deben decir "t".
-- ============================================================================

SELECT (c.content_body ->> 'url') ILIKE '%machinelearningforkids%'      AS enlace_nuevo,
       (c.content_body ->> 'instructions') LIKE '%Añadir etiqueta%'     AS instrucciones_con_acentos,
       (c.content_body ->> 'instructions') LIKE '%TU MANO%'             AS remate_intacto,
       (c.content_body ? 'teacher_notes')                               AS guia_del_maestro_intacta
FROM content c
JOIN subjects s ON s.id = c.subject_id
WHERE s.name ILIKE '%Ni%os%' AND s.name NOT ILIKE '%Intermedio%'
  AND c.order_index = 2;


-- ============================================================================
--  LO QUE FALTA
--
--  Teachable Machine bloquea la tablet en CINCO piezas, no en una. Esta es la
--  primera. Las otras cuatro siguen igual:
--
--    Principiante  8. Entrena un clasificador de animales   (imagen)
--    Intermedio    2. Enseñale a escuchar                   (audio)
--    Intermedio    3. El experimento del ruido              (audio)
--    Intermedio    9. Una IA que lee tu cuerpo              (pose)
--
--  Las tres primeras se pueden mudar igual que esta: ML4Kids tiene proyectos
--  de imagenes y de sonido, y ninguno pide API key.
--
--  La 9 NO. ML4Kids no tiene proyectos de "pose", asi que esa pieza necesita
--  otra solucion --o correrla en computadora-- y no se puede resolver
--  copiando este script. Decidirlo antes de llegar a esa sesion.
--
--  Para la 8 hay un detalle a favor y uno en contra: ML4Kids deja arrastrar
--  imagenes desde otra ventana del navegador, que le viene mejor que la camara
--  porque nadie va a fotografiar un leon; pero arrastrar entre ventanas en una
--  tablet es incomodo. Probarlo antes de decidir.
-- ============================================================================
