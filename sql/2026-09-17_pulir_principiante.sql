-- ============================================================================
--  ByteKids Academy - Pulido de "IA para Ninos (Principiante)"
--  Fecha: 17-sep-2026
--
--  CONTINUA sql/2026-09-15_frutas_en_ml4kids.sql, que mudo la pieza 2.
--  Aqui va el resto de lo que quedaba del principiante:
--
--    Pieza 8  - se muda a ML4Kids, por lo mismo que la 2: Teachable Machine
--               bloquea las tablets a proposito.
--    Pieza 7  - el material de datos, que estaba en una linea.
--    Pieza 10 - el material de Scratch, idem.
--    Pieza 12 - el proyecto final, que era imposible de hacer sin el maestro
--               al lado todo el tiempo.
--
--  UNA CORRECCION IMPORTANTE SOBRE LA PIEZA 7
--    Se habia pensado arrancar con un proyecto de TEXTO en ML4Kids. No se
--    puede: los proyectos de texto son los unicos que piden una API key de
--    watsonx. Los de imagen, numeros y sonido no piden nada. Arrancar por
--    texto habria dejado al grupo atorado en un registro de IBM.
--    Se queda en imagenes, que ademas es lo que el nino ya uso en la pieza 2.
--
--  LO QUE NO SE TOCA
--    Tipo, XP, dificultad, order_index, asignaciones y la guia del maestro
--    (teacher_notes). Este script solo reescribe descripcion y content_body.
--
--  ANTES DE CORRER ESTO
--    Las cuentas de ML4Kids tienen que existir ya (maestro + alumnos). Es el
--    mismo requisito del script del 15-sep:
--      https://machinelearningforkids.co.uk/?lang=es
--
--  ORDEN
--    Corre el PASO 1, mira lo que hay, y si algo del texto actual vale la pena
--    incorporalo antes de seguir. Es idempotente: se puede repetir.
-- ============================================================================


-- ============================================================================
--  PASO 1 - QUE HAY HOY EN ESAS CUATRO PIEZAS
-- ============================================================================

SELECT c.order_index                     AS pieza,
       c.type,
       c.title,
       c.content_body ->> 'url'          AS url_actual,
       left(c.description, 80)           AS descripcion_actual,
       (c.content_body ? 'teacher_notes') AS tiene_guia
FROM content c
JOIN subjects s ON s.id = c.subject_id
WHERE s.name ILIKE '%Ni%os%' AND s.name NOT ILIKE '%Intermedio%'
  AND c.order_index IN (7, 8, 10, 12)
ORDER BY c.order_index;


-- ============================================================================
--  PASO 2 - PIEZA 8: EL CLASIFICADOR DE ANIMALES SE MUDA A ML4KIDS
--
--  Mismo ejercicio, otra herramienta. Y como el nino ya entreno frutas en la
--  pieza 2, aqui las instrucciones son mas cortas a proposito: lo nuevo no es
--  la herramienta, es que ELIGE los animales y por eso puede meter el sesgo
--  que la pieza 9 le va a pedir encontrar.
-- ============================================================================

UPDATE content c
SET description = 'Ya entrenaste una IA con frutas. Ahora tú decides qué animales aprende, y vas a descubrir que lo que elijas enseñarle cambia lo que la máquina puede ver.',
    content_body = coalesce(c.content_body, '{}'::jsonb) || jsonb_build_object(
    'url',           'https://machinelearningforkids.co.uk/?lang=es',
    'resource_type', 'enlace',
    'instructions',  $inst$Hoy entrenas tu segunda IA. Ya sabes cómo: es el mismo camino que con las frutas.

ARMA EL PROYECTO
1. Entra al enlace con tu usuario y dale a "Añadir un nuevo proyecto".
2. Ponle nombre, elige el tipo "imágenes" y ábrelo.
3. Entra a "Entrenar".

ELIGE TRES ANIMALES
4. Crea tres etiquetas, una por animal. Tú decides cuáles: perro, gato,
   pájaro, pez, lo que quieras.
5. Junta como 10 imágenes de cada uno. Puedes usar la cámara con tus
   peluches o tus mascotas, o buscar fotos.

   Acuérdate de lo de las frutas: si las 10 fotos son casi iguales, la
   computadora aprende el fondo y no el animal.

ENTRENA Y PRUEBA
6. Ve a "Aprender & Probar" y dale a "Entrenar nuevo modelo".
7. Pruébalo con animales que SÍ le enseñaste. ¿Le atina?

AHORA LAS PREGUNTAS QUE IMPORTAN
8. Enséñale un animal que NO esté en tus tres etiquetas. Un elefante, por
   ejemplo. Anota qué contestó.
9. Si todos tus perros eran del mismo color, enséñale un perro de otro color
   y mira qué pasa.

Contesta con tus palabras:
1. ¿Qué animales puede reconocer tu IA, y cuáles no? ¿Por qué?
2. ¿Qué contestó con el animal que nunca le enseñaste?
3. Si tuvieras que mejorarla, ¿qué fotos le agregarías?

Guarda tu proyecto: lo vas a necesitar en la siguiente actividad.$inst$)
FROM subjects s
WHERE s.id = c.subject_id
  AND s.name ILIKE '%Ni%os%' AND s.name NOT ILIKE '%Intermedio%'
  AND c.order_index = 8;


-- ============================================================================
--  PASO 3 - PIEZA 7: LOS DATOS SON LA COMIDA DE LA IA
--
--  Era un enlace suelto. Y como la pieza 2 ya se mudo a ML4Kids, el nino LLEGA
--  AQUI CONOCIENDO la herramienta: esta pieza ya no tiene que presentarsela,
--  puede dedicarse a lo suyo, que son los datos.
--
--  Es material: se consulta, no se entrega. Por eso lleva una exploracion
--  guiada y no un formulario.
-- ============================================================================

UPDATE content c
SET description = 'Una IA no nace sabiendo: aprende de los ejemplos que alguien le da. Hoy vas a ver de cerca de qué está hecha esa comida, y qué pasa cuando está mal servida.',
    content_body = coalesce(c.content_body, '{}'::jsonb) || jsonb_build_object(
    'url',           'https://machinelearningforkids.co.uk/?lang=es',
    'resource_type', 'enlace',
    'instructions',  $inst$Ya entrenaste una IA con frutas, así que conoces la herramienta. Hoy la vas a usar para mirar otra cosa: LOS DATOS.

Abre tu proyecto de las frutas y entra a "Entrenar".

MÍRALO CON OTROS OJOS
1. Cuenta cuántas fotos tiene cada etiqueta. ¿Están parejas, o una tiene
   muchas más que la otra?
2. Mira las fotos de un cajón, una por una. ¿Se parecen demasiado entre sí?
   ¿Todas con la misma luz, el mismo fondo, el mismo ángulo?

HAZ UN EXPERIMENTO
3. Borra casi todas las fotos de UNA de las dos frutas: déjale solo dos o
   tres.
4. Entrena el modelo otra vez y pruébalo con esa fruta.
5. ¿Empeoró? Vuelve a subir las fotos que quitaste y entrena de nuevo.

LO QUE TE VAS A LLEVAR
Una IA no es más lista que su comida. Si le das pocos ejemplos, aprende poco.
Si le das ejemplos todos iguales, aprende lo que no querías. Y si le das más
de una cosa que de otra, se inclina hacia la que vio más.

Eso último tiene nombre y lo vas a ver en dos actividades más: se llama
SESGO.$inst$)
FROM subjects s
WHERE s.id = c.subject_id
  AND s.name ILIKE '%Ni%os%' AND s.name NOT ILIKE '%Intermedio%'
  AND c.order_index = 7;


-- ============================================================================
--  PASO 4 - PIEZA 10: CHATBOTS, MAQUINAS QUE CONVERSAN
--
--  Decia "explora la plataforma antes de la siguiente mision", que para un
--  nino de nueve anos no es una instruccion: es un encargo sin puerta de
--  entrada. Scratch es enorme y se pierde.
--
--  Ahora entra por un camino concreto y sale sabiendo los CUATRO bloques que
--  la pieza 11 le va a pedir. Sin eso, la 11 se convierte en clase de Scratch
--  en vez de clase de chatbots.
-- ============================================================================

UPDATE content c
SET description = 'Un chatbot no entiende lo que le dices: sigue reglas que alguien escribió. Hoy conoces los cuatro bloques de Scratch con los que vas a construir el tuyo en la próxima actividad.',
    content_body = coalesce(c.content_body, '{}'::jsonb) || jsonb_build_object(
    'url',           'https://scratch.mit.edu/projects/editor/?tutorial=getStarted',
    'resource_type', 'enlace',
    'instructions',  $inst$Hoy no construyes nada todavía: hoy reconoces tus herramientas.

PRIMERO, EL RECORRIDO
1. Entra al enlace. Se abre Scratch con el tutorial "Primeros pasos" a un
   lado. Síguelo completo, son unos 10 minutos.
2. Si se te cierra, lo vuelves a abrir en el botón "Tutoriales", arriba.

AHORA BUSCA ESTOS CUATRO BLOQUES
Son los que vas a usar la próxima clase. Encuéntralos y arrástralos al
área de trabajo para verlos de cerca:

- En SENSORES (azul claro): "preguntar [ ] y esperar"
  Es el bloque que le hace una pregunta al que está del otro lado.

- En SENSORES: "respuesta"
  Es el óvalo que guarda lo que la persona escribió.

- En CONTROL (amarillo): "si [ ] entonces / si no"
  Con este el chatbot decide qué contestar.

- En OPERADORES (verde): "[ ] contiene [ ]?"
  Sirve para preguntar si la respuesta traía cierta palabra.

PRUÉBALOS
3. Arma esto y dale al banderín verde:
   preguntar "¿Cómo te llamas?" y esperar
   decir (respuesta)
4. El gato te devuelve tu nombre. Eso ya es un chatbot pequeñito.

PARA PENSAR
Cuando le contestaste al gato, ¿él entendió tu nombre, o solo lo repitió?
Guárdate esa pregunta: es de lo que trata la próxima actividad.$inst$)
FROM subjects s
WHERE s.id = c.subject_id
  AND s.name ILIKE '%Ni%os%' AND s.name NOT ILIKE '%Intermedio%'
  AND c.order_index = 10;


-- ============================================================================
--  PASO 5 - PIEZA 12: EL PROYECTO FINAL
--
--  El problema no era el nivel, era el enunciado. Decia "identifica un
--  problema real de tu salon" y la lista pedia cosas como "entrene el modelo"
--  y "revise si tiene sesgo". Eso a un nino de nueve anos no le dice que
--  hacer: le dice que ya deberia saberlo.
--
--  Dos cambios de fondo:
--    1. Se le dan TRES proyectos concretos para escoger. Disenar desde cero
--       es la parte mas dificil del trabajo, no la mas facil, y no es lo que
--       esta materia enseno.
--    2. Cada punto de la lista dice la accion, no el concepto. "Entrene el
--       modelo" se convierte en "le di a Entrenar nuevo modelo y espere".
--
--  La herramienta es ML4Kids, que es la que corre en tablet y la que ya uso
--  en las piezas 2 y 8. Scratch queda como anadido opcional, no como
--  requisito: juntar las dos cosas es lo que hacia el proyecto inalcanzable.
-- ============================================================================

UPDATE content c
SET description = 'Tu proyecto final: una IA que resuelva algo de tu salón. Escoge uno de los tres proyectos, entrénalo, pruébalo de verdad y preséntalo. No tiene que ser perfecto: tiene que ser tuyo y tienes que saber en qué falla.',
    content_body = coalesce(c.content_body, '{}'::jsonb) || jsonb_build_object(
    'url',           'https://machinelearningforkids.co.uk/?lang=es',
    'resource_type', 'enlace',
    'instructions',  $inst$Es tu último proyecto y lo vas a presentar al grupo. Tienes tres sesiones.

PASO 1 - ESCOGE UNO
No tienes que inventar nada. Escoge el que más te lata:

A) EL SEPARADOR DE BASURA
   Enseña a la IA a distinguir papel, plástico y basura orgánica, para que
   ayude a separar el bote del salón.
   Tus etiquetas: "papel", "plástico", "orgánico".

B) ¿ESTÁ LIMPIO MI LUGAR?
   Enséñale cómo se ve una mesa recogida y cómo se ve una desordenada.
   Tus etiquetas: "limpio", "desordenado".

C) EL GUARDIÁN DE LOS ÚTILES
   Enséñale a reconocer tres cosas que se pierden seguido en el salón.
   Tus etiquetas: los tres objetos que elijas.

   (Si de verdad se te ocurre otro, enséñaselo a tu maestro antes de
   empezar. Tiene que poder resolverse con fotos.)

PASO 2 - JÚNTALE LOS EJEMPLOS
Mínimo 10 fotos de cada etiqueta, tomadas en tu salón de verdad.
Y acuérdate de lo de siempre: cámbialas de lugar, de luz y de ángulo.
Anota cuántas fotos le pusiste a cada una.

PASO 3 - ENTRÉNALA
Ve a "Aprender & Probar" y dale a "Entrenar nuevo modelo". Espera a que
termine. Eso es entrenar: no hay más.

PASO 4 - PRUÉBALA DE VERDAD, 10 VECES
Haz diez pruebas y anota cada una en una tabla así:

   Qué le enseñé  |  Qué contestó  |  ¿Le atinó?

Que no sean diez veces lo mismo. Prueba también con algo que no le
enseñaste, y anota qué contestó.

PASO 5 - BÚSCALE LA TRAMPA
Mira tu tabla y contesta:
   - ¿En qué se equivocó más?
   - ¿Todas tus fotos se parecían mucho entre sí? ¿En qué?
   - ¿Qué le faltó ver para no equivocarse ahí?
Eso que encontraste es el sesgo de tu IA. Escríbelo.

PASO 6 - PREPARA TU PRESENTACIÓN (3 minutos)
Cuenta cuatro cosas, en este orden:
   1. Qué problema escogiste y por qué.
   2. Cuántas fotos le pusiste a cada etiqueta.
   3. De tus 10 pruebas, cuántas le atinó. Enseña tu tabla.
   4. En qué falla tu IA, y qué le agregarías si tuvieras más tiempo.

Se califica que sepas dónde falla, no que funcione perfecto. Un proyecto que
dice "falla con poca luz" vale más que uno que dice "funciona bien".

¿QUIERES IR MÁS LEJOS? (no es obligatorio)
Si te sobra tiempo, conecta tu modelo a Scratch para que haga algo cuando
reconozca cada cosa: un sonido, un mensaje, un personaje que se mueva.
En ML for Kids, el botón "Hacer" te lleva a Scratch con tu modelo listo.$inst$,
    'checklist',     jsonb_build_array(
        'Escogí uno de los tres proyectos y lo escribí en una frase',
        'Decidí mis etiquetas y anoté cuáles son',
        'Junté mínimo 10 fotos de cada etiqueta, cambiándoles luz y lugar',
        'Anoté cuántas fotos le puse a cada etiqueta',
        'Le di a "Entrenar nuevo modelo" y esperé a que terminara',
        'Hice 10 pruebas y las anoté en mi tabla',
        'Probé con algo que NO le enseñé y anoté qué contestó',
        'Escribí en qué se equivoca mi IA y por qué creo que pasa',
        'Preparé mi presentación de 3 minutos con las cuatro cosas',
        'Escribí qué le agregaría si tuviera más tiempo'
    ))
FROM subjects s
WHERE s.id = c.subject_id
  AND s.name ILIKE '%Ni%os%' AND s.name NOT ILIKE '%Intermedio%'
  AND c.order_index = 12;


-- ============================================================================
--  PASO 6 - VERIFICACION
--
--  Deben salir cuatro filas. Revisa sobre todo que ninguna siga apuntando a
--  teachablemachine: si queda alguna, el grupo con tablet se atora ahi.
-- ============================================================================

SELECT c.order_index                       AS pieza,
       c.type,
       c.title,
       c.content_body ->> 'url'            AS url,
       (c.content_body ->> 'url') ILIKE '%teachablemachine%' AS sigue_bloqueada,
       length(c.content_body ->> 'instructions') AS largo_instrucciones,
       jsonb_array_length(coalesce(c.content_body -> 'checklist', '[]'::jsonb)) AS items_checklist,
       (c.content_body ? 'teacher_notes')  AS conserva_guia
FROM content c
JOIN subjects s ON s.id = c.subject_id
WHERE s.name ILIKE '%Ni%os%' AND s.name NOT ILIKE '%Intermedio%'
  AND c.order_index IN (7, 8, 10, 12)
ORDER BY c.order_index;


-- ============================================================================
--  PASO 7 - LO QUE SIGUE BLOQUEADO EN TODO EL TEMARIO
--
--  Esta consulta es el pendiente vivo: mientras devuelva filas, hay piezas que
--  no corren en tablet. Al terminar este script deben quedar SOLO las tres del
--  intermedio (2, 3 y 9). La 9 no se puede mudar a ML4Kids porque no tiene
--  proyectos de pose: esa necesita otra solucion o correrse en computadora.
-- ============================================================================

SELECT s.name AS materia, c.order_index AS pieza, c.title
FROM content c
JOIN subjects s ON s.id = c.subject_id
WHERE c.content_body ->> 'url' ILIKE '%teachablemachine%'
   OR c.content_body ->> 'instructions' ILIKE '%teachable%'
   OR c.content_body ->> 'teacher_notes' ILIKE '%Teachable Machine%'
ORDER BY s.name, c.order_index;
