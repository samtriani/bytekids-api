-- ============================================================================
--  ByteKids Academy - Guia del maestro para "IA para Ninos (Principiante)"
--  Fecha: 7-sep-2026
--
--  QUE HACE
--    1. Convierte la pieza 1 de "material" a "tarea". Su descripcion pedia
--       "anota cual te sorprendio mas", pero un material NO tiene donde
--       escribir: la pantalla del alumno solo pinta el boton "Ya lo vi".
--       Ademas cambia la coleccion de experimentos por una actividad guiada.
--    2. Agrega una guia para dar la clase a las 12 piezas, dentro de
--       content_body -> teacher_notes.
--
--  POR QUE AHI
--    content_body es JSONB: no hace falta migrar el esquema, y en este
--    proyecto eso importa porque no hay Flyway y cada cambio es SQL a mano.
--    El backend borra teacher_notes, expected_output y solution_check antes
--    de responderle a un alumno (ContentResponse.cuerpoVisible).
--
--  ORDEN
--    Corre el PASO A, revisa el resultado, y luego B, C y D.
--    Es idempotente: se puede volver a correr sin duplicar nada.
-- ============================================================================


-- ============================================================================
--  PASO A - INVENTARIO
--  Verifica que los order_index sean 1..12 y no se repitan. Los UPDATE de
--  abajo cazan por order_index: si hay una pieza de mas o un indice repetido,
--  PARA aqui y avisa antes de seguir.
-- ============================================================================

SELECT c.order_index, c.type, c.title,
       (c.content_body ? 'teacher_notes') AS ya_tiene_guia
FROM content c JOIN subjects s ON s.id = c.subject_id
WHERE s.name ILIKE '%Ni%os%' AND s.name NOT ILIKE '%Intermedio%'
ORDER BY c.order_index;


-- ============================================================================
--  PASO B - ARREGLO DE LA PIEZA 1
--  De material a tarea, para que el alumno pueda escribir que entendio.
--  Deja de auto-aprobarse: el maestro tiene senal desde el dia uno.
-- ============================================================================

UPDATE content c
SET type              = 'tarea',
    xp_reward         = 30,
    estimated_minutes = 20,
    description       = 'La IA no es un robot que piensa como tú: es una máquina que aprende a reconocer patrones a partir de muchos ejemplos. Hoy vas a entrenar una tú mismo, etiquetando peces.',
    content_body      = coalesce(c.content_body, '{}'::jsonb) || jsonb_build_object(
        'url',           'https://studio.code.org/s/oceans',
        'resource_type', 'enlace',
        'instructions',  $inst$Entra al enlace y haz la actividad "IA para los océanos".
Si te aparece en inglés, cambia el idioma a Español hasta abajo de la página.

Vas a enseñarle a una máquina a separar peces de basura. Tú le pones las
etiquetas, y eso es justamente entrenarla.

Cuando termines, responde con tus propias palabras:
1. ¿Quién le enseñó a la máquina qué era un pez?
2. ¿Qué pasó cuando le mostraste algo raro que no había visto antes?
3. ¿La máquina entiende qué es un pez, o solo sabe a qué se parece lo que le enseñaste?$inst$)
FROM subjects s
WHERE s.id = c.subject_id
  AND s.name ILIKE '%Ni%os%' AND s.name NOT ILIKE '%Intermedio%'
  AND c.order_index = 1;


-- ============================================================================
--  PASO C - GUIA DEL MAESTRO EN LAS 12 PIEZAS
-- ============================================================================

-- 1. Que es la Inteligencia Artificial?
UPDATE content c
SET content_body = jsonb_set(
      coalesce(c.content_body, '{}'::jsonb), '{teacher_notes}',
      $guia${
  "objetivo": "Que el alumno pueda decir con sus palabras que la IA aprende de ejemplos, y que no 'piensa' como una persona.",
  "duracion": "25 min",
  "explicar": [
    "Arranca preguntando al grupo qué creen que es la IA, sin corregir todavía. Anota 3 respuestas en el pizarrón: al final las revisan.",
    "La idea única de hoy: nadie le escribió a la máquina una lista de reglas. Se le muestran muchos ejemplos y ella sola encuentra el patrón.",
    "En la actividad el alumno etiqueta peces como 'pez' o 'basura'. Eso ES entrenar: dilo con esas palabras mientras lo hacen.",
    "OJO: la página abre en inglés. Cambia a Español en el selector de idioma del pie de página ANTES de que empiecen."
  ],
  "preguntas": [
    "¿Quién le enseñó a la máquina qué es un pez?",
    "Si solo hubiéramos etiquetado peces azules, ¿qué pasaría con uno rojo?",
    "¿La computadora sabe qué es un pez, o solo sabe a qué se parece lo que le enseñamos?"
  ],
  "errores": [
    "Decir que la IA 'piensa' o 'entiende'. Corrígelo cada vez: es el error que arrastran todo el curso.",
    "Confundir IA con robot. Muéstrales que el buscador y el traductor son IA y no tienen cuerpo.",
    "Terminan en 5 min sin leer. Pide la respuesta escrita antes de dar la pieza por cerrada."
  ],
  "cierre": "Regresa a las 3 respuestas del pizarrón y que el grupo diga cuáles cambiarían ahora."
}$guia$::jsonb, true)
FROM subjects s
WHERE s.id = c.subject_id
  AND s.name ILIKE '%Ni%os%' AND s.name NOT ILIKE '%Intermedio%'
  AND c.order_index = 1;

-- 2. Ensenale a la computadora a ver frutas
UPDATE content c
SET content_body = jsonb_set(
      coalesce(c.content_body, '{}'::jsonb), '{teacher_notes}',
      $guia${
  "objetivo": "Que entrenen su primer modelo y descubran solos que la máquina únicamente puede elegir entre las clases que le enseñaron.",
  "duracion": "40 min (30 de trabajo + 10 de puesta en común)",
  "explicar": [
    "Necesitan cámara. Si no hay suficientes, que trabajen en parejas o usa la tuya proyectada.",
    "Antes de soltarlos, aclara qué es una 'clase': cada cajón donde meten ejemplos.",
    "Insiste en mover la fruta entre foto y foto, distinto ángulo y distinta luz. Con 30 fotos idénticas el modelo falla y no entienden por qué."
  ],
  "preguntas": [
    "Muéstrale tu mano a la cámara. ¿Qué contestó? ¿Por qué contestó algo en vez de decir 'no sé'?",
    "¿Qué tendríamos que hacer para que sí pudiera decir 'no sé'?",
    "¿Quién decidió cuáles eran las respuestas posibles?"
  ],
  "errores": [
    "30 fotos iguales sin mover la fruta: el modelo memoriza el fondo, no la fruta.",
    "Eligen dos frutas muy parecidas. Sugiere plátano contra manzana la primera vez.",
    "Esperan que el modelo diga 'no sé'. Ese es justo el descubrimiento: no se los adelantes."
  ],
  "cierre": "La frase que se tienen que llevar: 'solo puede elegir entre lo que le enseñamos'."
}$guia$::jsonb, true)
FROM subjects s
WHERE s.id = c.subject_id
  AND s.name ILIKE '%Ni%os%' AND s.name NOT ILIKE '%Intermedio%'
  AND c.order_index = 2;

-- 3. Caza de IA en tu casa
UPDATE content c
SET content_body = jsonb_set(
      coalesce(c.content_body, '{}'::jsonb), '{teacher_notes}',
      $guia${
  "objetivo": "Que reconozcan IA en su vida diaria y se acostumbren a preguntarse de dónde salieron los datos.",
  "duracion": "10 min en clase para explicarla; se entrega después",
  "explicar": [
    "Es tarea de casa y de observación: no necesitan computadora.",
    "Da tú un ejemplo completo antes, el teclado del teléfono que adivina la siguiente palabra: qué hace, qué tuvo que aprender, de dónde sacó los ejemplos.",
    "Avísales que no se vale contestar solo 'Alexa': las tres preguntas son obligatorias por cada objeto."
  ],
  "preguntas": [
    "¿La lavadora es IA? (No: sigue un programa fijo, no aprendió de ejemplos.)",
    "¿De dónde crees que salieron los ejemplos para que YouTube sepa qué recomendarte?"
  ],
  "errores": [
    "Listan aparatos electrónicos cualesquiera. El criterio es: ¿aprendió de ejemplos, o alguien le escribió las reglas?",
    "Copian de un hermano. Pide que digan en qué parte de su casa está cada cosa."
  ],
  "cierre": "Al revisar, junta en el pizarrón de dónde salieron los datos de cada caso. Casi siempre la respuesta es: de nosotros."
}$guia$::jsonb, true)
FROM subjects s
WHERE s.id = c.subject_id
  AND s.name ILIKE '%Ni%os%' AND s.name NOT ILIKE '%Intermedio%'
  AND c.order_index = 3;

-- 4. Como aprende una maquina: patrones y ejemplos
UPDATE content c
SET content_body = jsonb_set(
      coalesce(c.content_body, '{}'::jsonb), '{teacher_notes}',
      $guia${
  "objetivo": "Ver un modelo entrenado con millones de ejemplos y observar en qué falla.",
  "duracion": "20 min",
  "explicar": [
    "Quick Draw adivina porque vio millones de garabatos de personas reales. Di el número en voz alta: es lo que hace clic.",
    "Dales una consigna concreta o se quedan jugando: 'anota 2 dibujos que NO adivinó'."
  ],
  "preguntas": [
    "¿Por qué le costó tu dibujo? ¿Tú dibujas como la mayoría?",
    "Si todos los que dibujaron 'casa' hicieron una con techo de dos aguas, ¿qué pasa con quien vive en un edificio?"
  ],
  "errores": [
    "Se quedan jugando y no observan. Sin la consigna escrita, la pieza no enseña nada.",
    "Concluyen que 'la IA es tonta'. Redirige: no vio suficientes dibujos como el tuyo."
  ],
  "cierre": "Conecta con la sesión pasada: más ejemplos es mejor, pero solo si se parecen a lo que va a ver después."
}$guia$::jsonb, true)
FROM subjects s
WHERE s.id = c.subject_id
  AND s.name ILIKE '%Ni%os%' AND s.name NOT ILIKE '%Intermedio%'
  AND c.order_index = 4;

-- 5. El detective de patrones
UPDATE content c
SET content_body = jsonb_set(
      coalesce(c.content_body, '{}'::jsonb), '{teacher_notes}',
      $guia${
  "objetivo": "Que sientan en carne propia que con pocos ejemplos no se puede deducir la regla.",
  "duracion": "35 min",
  "explicar": [
    "Empieza tú en el pizarrón con 2, 4, 6 y pregunta la regla. Deja que digan 'de dos en dos'.",
    "Luego escribe 2, 4, 6, 10, 16 y muestra que esa serie también empezaba igual. Ese es el punto de toda la sesión: con 3 datos hay muchas reglas posibles."
  ],
  "preguntas": [
    "¿Cuántos números necesitas para estar seguro de la regla? ¿Estarías 100% seguro?",
    "Cuando la máquina se equivoca, ¿es tonta o le faltaron ejemplos?"
  ],
  "errores": [
    "En el inciso c) muchos ponen 10 en vez de 16. Está bien que se equivoquen: úsalo, no lo corrijas de inmediato.",
    "Inventan una secuencia sin regla real. Pídeles que la escriban aparte antes de compartirla."
  ],
  "cierre": "Cierra con: 'la máquina no adivina; encuentra la regla que mejor explica los ejemplos que le diste'."
}$guia$::jsonb, true)
FROM subjects s
WHERE s.id = c.subject_id
  AND s.name ILIKE '%Ni%os%' AND s.name NOT ILIKE '%Intermedio%'
  AND c.order_index = 5;

-- 6. Quiz: primeros pasos con la IA
UPDATE content c
SET content_body = jsonb_set(
      coalesce(c.content_body, '{}'::jsonb), '{teacher_notes}',
      $guia${
  "objetivo": "Verificar las 5 ideas base antes de subir de dificultad.",
  "duracion": "20 min",
  "explicar": [
    "Aplícalo DESPUÉS de la pieza 5, nunca antes: las preguntas 3 y 4 dependen de esas dos misiones.",
    "Se califica solo. Tú nada más necesitas leer el resultado del grupo."
  ],
  "preguntas": [
    "Al terminar, revisen en grupo la pregunta 3 (ejemplos desbalanceados): es la que prepara la pieza 8."
  ],
  "errores": [
    "La de 'entrenada solo con perros, ¿reconoce un gato?' se falla si no hicieron la misión 2. Si muchos fallan, repite la demo de la mano frente a la cámara."
  ],
  "cierre": "Si el grupo queda abajo de 60%, no avances: repite la misión 2 antes de la pieza 7."
}$guia$::jsonb, true)
FROM subjects s
WHERE s.id = c.subject_id
  AND s.name ILIKE '%Ni%os%' AND s.name NOT ILIKE '%Intermedio%'
  AND c.order_index = 6;

-- 7. Los datos son la comida de la IA
UPDATE content c
SET content_body = jsonb_set(
      coalesce(c.content_body, '{}'::jsonb), '{teacher_notes}',
      $guia${
  "objetivo": "Que vean la variedad de proyectos posibles y elijan uno, preparando la misión 8.",
  "duracion": "20 min",
  "explicar": [
    "El sitio está en inglés. Recórrelo tú proyectado y traduce en voz alta; no los sueltes solos.",
    "El objetivo NO es hacer un proyecto hoy: es elegir uno. No dejes que se pierdan registrándose."
  ],
  "preguntas": [
    "De los proyectos que viste, ¿cuál necesitaría más ejemplos para funcionar bien? ¿Por qué?"
  ],
  "errores": [
    "Se atoran creando cuenta. Usa el modo de prueba sin registro, o proyéctalo tú."
  ],
  "cierre": "Que cada uno diga en una frase qué proyecto eligió y qué le tendría que enseñar."
}$guia$::jsonb, true)
FROM subjects s
WHERE s.id = c.subject_id
  AND s.name ILIKE '%Ni%os%' AND s.name NOT ILIKE '%Intermedio%'
  AND c.order_index = 7;

-- 8. Entrena un clasificador de animales
UPDATE content c
SET content_body = jsonb_set(
      coalesce(c.content_body, '{}'::jsonb), '{teacher_notes}',
      $guia${
  "objetivo": "Que provoquen el sesgo con sus propias manos, antes de que tú se lo nombres.",
  "duracion": "50 min. Es la sesión más larga del curso; no la partas en dos.",
  "explicar": [
    "NO uses la palabra 'sesgo' hoy. Se nombra hasta la pieza 9, después de que lo vivieron.",
    "El paso 3 es el corazón: tienen que ANOTAR cuántas veces falla ANTES de arreglarlo. Sin ese dato no hay comparación y la misión se desperdicia.",
    "Si no hay cámara con mascotas a la mano, usa imágenes de internet proyectadas o impresas."
  ],
  "preguntas": [
    "¿Se equivocó al azar, o se equivocó SIEMPRE hacia el mismo lado?",
    "Si esto fuera una app que decide quién entra a un lugar, ¿a quién dejaría fuera?"
  ],
  "errores": [
    "Se saltan el paso 3 y entrenan balanceado desde el inicio. Supervisa ese paso: ahí está la lección.",
    "Confunden 'pocas fotos' con 'fotos malas'. Aquí el problema es el desbalance ENTRE clases, no la calidad."
  ],
  "cierre": "Deja la pregunta abierta para la próxima sesión: '¿y esto cómo se llamará?'"
}$guia$::jsonb, true)
FROM subjects s
WHERE s.id = c.subject_id
  AND s.name ILIKE '%Ni%os%' AND s.name NOT ILIKE '%Intermedio%'
  AND c.order_index = 8;

-- 9. Es justo? Encuentra el sesgo
UPDATE content c
SET content_body = jsonb_set(
      coalesce(c.content_body, '{}'::jsonb), '{teacher_notes}',
      $guia${
  "objetivo": "Ponerle nombre a lo que ya vivieron y llevarlo a consecuencias sobre personas reales.",
  "duracion": "35 min",
  "explicar": [
    "Ahora sí: escribe SESGO en el pizarrón. Definición para ellos: cuando los ejemplos están mal repartidos, la IA falla siempre contra los mismos.",
    "Los 3 casos son fáciles a propósito. Lo que de verdad vale es la última pregunta, que los regresa a su propio modelo de la pieza 8."
  ],
  "preguntas": [
    "En el caso 1, ¿quién programó la app para que fallara con niños? (Nadie. Y aun así pasó.)",
    "¿A quién le tocaría darse cuenta antes de que la app saliera?"
  ],
  "errores": [
    "Contestan 'la IA es mala'. Redirige: la IA no quiere nada; el problema está en los ejemplos que le dimos.",
    "Se saltan la última pregunta, que es la que vale. Insiste en que la amarren con su clasificador."
  ],
  "cierre": "Esta es la idea más importante del curso para su vida fuera del salón. Dedícale el cierre completo."
}$guia$::jsonb, true)
FROM subjects s
WHERE s.id = c.subject_id
  AND s.name ILIKE '%Ni%os%' AND s.name NOT ILIKE '%Intermedio%'
  AND c.order_index = 9;

-- 10. Chatbots: maquinas que conversan
UPDATE content c
SET content_body = jsonb_set(
      coalesce(c.content_body, '{}'::jsonb), '{teacher_notes}',
      $guia${
  "objetivo": "Distinguir 'seguir reglas' de 'aprender de ejemplos', y familiarizarse con Scratch.",
  "duracion": "20 min",
  "explicar": [
    "Cuidado con esta distinción: el chatbot que van a hacer en Scratch SIGUE REGLAS que ellos escriben. NO aprende. Un asistente como ChatGPT sí predice a partir de lo que leyó.",
    "Si el grupo ya usa Scratch, sáltate la exploración y pasa directo a la misión 11."
  ],
  "preguntas": [
    "¿Alguna vez han hablado con un bot? ¿Cómo se dieron cuenta de que no era una persona?"
  ],
  "errores": [
    "Salen creyendo que su chatbot de Scratch es IA. Es la confusión más común de esta unidad: anticípala hoy, no después."
  ],
  "cierre": "Ancla la diferencia: 'reglas que yo escribo' contra 'patrones que la máquina encontró sola'."
}$guia$::jsonb, true)
FROM subjects s
WHERE s.id = c.subject_id
  AND s.name ILIKE '%Ni%os%' AND s.name NOT ILIKE '%Intermedio%'
  AND c.order_index = 10;

-- 11. Construye tu primer chatbot
UPDATE content c
SET content_body = jsonb_set(
      coalesce(c.content_body, '{}'::jsonb), '{teacher_notes}',
      $guia${
  "objetivo": "Programar por reglas y darse cuenta, por contraste, de qué significa aprender.",
  "duracion": "50 min",
  "explicar": [
    "La pieza trae bloques de arranque de Scratch. Proyéctalos para quienes se atoren empezando.",
    "El punto 3, qué dice cuando no entiende, es el que más enseña: obliga a pensar en el caso no previsto."
  ],
  "preguntas": [
    "¿Tu bot podría contestar algo que tú no escribiste?",
    "¿Cuántas reglas necesitarías para que contestara cualquier cosa?"
  ],
  "errores": [
    "Hacen 3 respuestas y se detienen. Empújalos al reto extra de recordar el nombre.",
    "Se pierden decorando el personaje. Da 10 min para eso al final, no al principio."
  ],
  "cierre": "La respuesta a la pregunta de la pieza: sigue una regla que ellos escribieron. No piensa."
}$guia$::jsonb, true)
FROM subjects s
WHERE s.id = c.subject_id
  AND s.name ILIKE '%Ni%os%' AND s.name NOT ILIKE '%Intermedio%'
  AND c.order_index = 11;

-- 12. Mi asistente de IA para el salon
UPDATE content c
SET content_body = jsonb_set(
      coalesce(c.content_body, '{}'::jsonb), '{teacher_notes}',
      $guia${
  "objetivo": "Integrar todo el curso: problema, datos, entrenamiento, prueba, sesgo y comunicación.",
  "duracion": "2 sesiones de trabajo + 1 de presentaciones",
  "explicar": [
    "No dejes que empiecen a entrenar el primer día. La sesión 1 es solo elegir el problema y decidir qué datos necesitan.",
    "Los 7 puntos del checklist SON la rúbrica. Enséñaselos desde el inicio, no hasta que califiques.",
    "El punto 5, revisar el sesgo, es lo que separa un proyecto bueno de uno excelente."
  ],
  "preguntas": [
    "¿Tu asistente resuelve un problema real del salón o uno que te imaginaste?",
    "¿A quién podría dejar fuera tu asistente?"
  ],
  "errores": [
    "Eligen problemas gigantes ('que haga la tarea'). Aterrízalos a algo con 2 o 3 clases.",
    "Presentan sin haberlo probado con alguien más del salón."
  ],
  "cierre": "Las presentaciones de 3 minutos son parte del proyecto, no un extra opcional."
}$guia$::jsonb, true)
FROM subjects s
WHERE s.id = c.subject_id
  AND s.name ILIKE '%Ni%os%' AND s.name NOT ILIKE '%Intermedio%'
  AND c.order_index = 12;


-- ============================================================================
--  PASO D - VERIFICACION
--  Las 12 piezas deben decir "t" en tiene_guia, y la 1 debe ser tipo "tarea".
-- ============================================================================

SELECT c.order_index, c.type, c.title,
       (c.content_body ? 'teacher_notes') AS tiene_guia,
       length(c.content_body -> 'teacher_notes' ->> 'objetivo') AS largo_objetivo
FROM content c JOIN subjects s ON s.id = c.subject_id
WHERE s.name ILIKE '%Ni%os%' AND s.name NOT ILIKE '%Intermedio%'
ORDER BY c.order_index;
