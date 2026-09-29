-- ============================================================================
--  ByteKids Academy - Curso gratuito "MI PRIMERA IA"
--  Fecha: 29-sep-2026
--
--  NO SE EDITA A MANO. Lo genera sql/mi_primera_ia/curso.py, que es la fuente
--  unica del contenido. Si hay que cambiar un texto, se cambia alla y se
--  vuelve a correr:  python sql/mi_primera_ia/curso.py
--
--  QUE HACE
--    Crea la materia "Mi Primera IA" con sus 9 piezas --materiales,
--    2 misiones, investigacion, 2 quizzes y proyecto final-- y las 16
--    preguntas de los quizzes. Cada pieza trae su guia del maestro.
--
--  SE PUEDE CORRER VARIAS VECES
--    - La materia y las piezas se crean si no existen, y si ya existen se
--      ACTUALIZAN con el texto de este archivo. Asi se corrige una errata
--      sin tocar nada a mano.
--    - Las preguntas de los quizzes solo se crean si faltan. NO se
--      reescriben: si un nino ya contesto, sus respuestas apuntan a esas
--      opciones. Cambiar un quiz ya lanzado se hace aparte y con cuidado.
--
--  NADIE LO VE TODAVIA
--    La materia nace sin salon. Ningun alumno la ve hasta que la asignes a
--    un salon desde Coordinacion (ver PASO 4).
-- ============================================================================


-- ============================================================================
--  PASO 0 - INVENTARIO: QUE HAY ANTES DE EMPEZAR
--  Si la materia ya existe, veras cuantas piezas tiene. Si es la primera vez,
--  sale vacio: es lo esperado.
-- ============================================================================

SELECT s.name, s.is_active, count(c.id) AS piezas
FROM subjects s LEFT JOIN content c ON c.subject_id = s.id
WHERE s.name = 'Mi Primera IA'
GROUP BY s.name, s.is_active;

-- Tiene que salir al menos UN usuario: queda como autor del curso.
SELECT username, role FROM users
WHERE role IN ('admin','director') AND is_active
ORDER BY created_at LIMIT 1;


BEGIN;

-- ============================================================================
--  PASO 1 - LA MATERIA
-- ============================================================================

INSERT INTO subjects (name, icon, color, description, is_active)
VALUES ('Mi Primera IA', '🤖', '#C4992A', 'Curso gratuito de ByteKids Academy. Descubre qué es la inteligencia artificial, entrena tu primera IA con tus propias manos y diseña una IA que ayude a alguien. Al terminar te llevas tu certificado.', true)
ON CONFLICT (name) DO UPDATE
  SET icon = EXCLUDED.icon, color = EXCLUDED.color,
      description = EXCLUDED.description, is_active = true;

-- ============================================================================
--  PASO 2 - LAS 9 PIEZAS
--  Cada una: se crea si falta (por titulo dentro de la materia) y despues se
--  actualiza con el texto de este archivo.
-- ============================================================================

-- ─── Pieza 1 · material · ¿Qué es la inteligencia artificial? ───
INSERT INTO content (title, description, type, subject_id, created_by, xp_reward,
                     difficulty, estimated_minutes, content_body, order_index,
                     is_published, is_active)
SELECT '¿Qué es la inteligencia artificial?', 'Tu celular, tus videos y hasta tus juegos esconden algo: inteligencia artificial. Hoy juegas contra una IA que adivina tus dibujos y descubres qué es, cómo funciona y dónde se esconde.', 'material'::content_type, mat.id, autor.id, 25,
       'facil'::difficulty_level, 20, '{}'::jsonb, 1, true, true
FROM       (SELECT id FROM subjects WHERE name = 'Mi Primera IA') mat
CROSS JOIN (SELECT id FROM users WHERE role IN ('admin','director') AND is_active
            ORDER BY created_at LIMIT 1) autor
WHERE NOT EXISTS (SELECT 1 FROM content x
                  WHERE x.subject_id = mat.id AND x.title = '¿Qué es la inteligencia artificial?');

UPDATE content c
SET description       = 'Tu celular, tus videos y hasta tus juegos esconden algo: inteligencia artificial. Hoy juegas contra una IA que adivina tus dibujos y descubres qué es, cómo funciona y dónde se esconde.',
    type              = 'material'::content_type,
    xp_reward         = 25,
    difficulty        = 'facil'::difficulty_level,
    estimated_minutes = 20,
    order_index       = 1,
    content_body      = jsonb_build_object(
    'instructions', $txt$¡Bienvenido a MI PRIMERA IA! 🤖

Soy ByteBot y voy a acompañarte en todo el curso. En 9 pasos vas a descubrir qué es la inteligencia artificial, le vas a enseñar algo a una máquina con tus propias manos y vas a inventar tu propia IA. Al final te ganas tu certificado de ByteKids Academy. 🎓

HOY VAS A PODER...
✔ Explicar con tus palabras qué es la inteligencia artificial.
✔ Encontrar la IA que se esconde en cosas que usas todos los días.

PRIMERO, UN RETO (5 minutos)
1. Abre el enlace de arriba. Se llama Quick, Draw!
2. Dale a "¡Vamos a dibujar!". Te va a pedir que dibujes algo en 20 segundos.
3. Mientras dibujas, la computadora intenta ADIVINAR qué es.
4. Juega una ronda completa: son 6 dibujos.

PAUSA PARA PENSAR 🤔
¿Cómo supo la computadora que tu garabato era un gato? Nadie estaba viendo tu dibujo...
Piénsalo un momento antes de seguir leyendo.

LO QUE ACABA DE PASAR
Esa computadora vio MILLONES de dibujos hechos por personas de todo el mundo. Vio tantos gatos —chuecos, gordos, con bigotes, sin bigotes— que aprendió cómo se ve un gato cuando alguien lo dibuja rápido.

Nadie le escribió "un gato tiene orejas puntiagudas y bigotes". Lo descubrió ella sola, VIENDO EJEMPLOS.

Eso es la inteligencia artificial:
👉 Programas que aprenden de ejemplos para hacer cosas que antes solo podíamos hacer las personas: reconocer, adivinar, recomendar, platicar.

UN PROGRAMA NORMAL vs. UNA IA
📋 Programa normal: sigue instrucciones. Una calculadora hace exactamente lo que alguien le escribió para cada botón. Siempre hace lo mismo y nunca aprende nada nuevo.
🧠 Inteligencia artificial: aprende de ejemplos. En vez de darle todas las reglas, le damos muchos ejemplos y ella encuentra el patrón.

LA IA ESTÁ ESCONDIDA EN TU DÍA
▸ YouTube te recomienda videos → aprendió de lo que ya viste.
▸ Un celular se desbloquea con la cara → aprendió cómo es esa cara.
▸ El teclado adivina tu siguiente palabra → aprendió de millones de mensajes.
▸ Un filtro te pone orejas de perrito → una IA encontró dónde están tus ojos y tu nariz.
▸ Le hablas a un asistente de voz → una IA convierte tu voz en palabras.

💬 ByteBot dice: "Yo también soy una IA. Aprendí leyendo muchísimos textos, por eso puedo platicar contigo. Pero aprender de ejemplos no es lo mismo que saberlo todo... eso lo vas a comprobar tú en la siguiente actividad."

PARA CERRAR (no se entrega, es para ti)
1. ¿Cuál de tus dibujos adivinó más rápido? ¿Cuál le costó más? ¿Por qué crees?
2. Piensa en una cosa que usas diario y que crees que tiene IA.

LO QUE TE LLEVAS HOY
La IA no es magia: es un programa que aprendió viendo muchos, muchos ejemplos.

¿QUÉ SIGUE?
Vas a entrevistar a una inteligencia artificial de verdad. A mí. 😉$txt$,
    'teacher_notes', $txt$OBJETIVO
Que el niño distinga "programa que sigue instrucciones" de "programa que aprende de ejemplos". Todo el curso se para sobre esa idea.

TIEMPO: 20 minutos. ES MATERIAL: se consulta, no se califica.

POR QUÉ QUICK, DRAW!
Corre en tablet, no pide cuenta y la IA "piensa en voz alta" mientras el niño dibuja: se ve el aprendizaje funcionando en tiempo real. Es el mejor gancho de 5 minutos que existe para este tema.

IDEAS EQUIVOCADAS QUE VAS A OÍR
- "La IA piensa como una persona." → Reconoce patrones; no entiende lo que ve. Si un niño lo pregunta por Mensajes: "Buena pregunta. ¿Tú crees que la computadora sabe qué es un gato, o solo sabe cómo se DIBUJA un gato?"
- "Todo lo que tiene computadora es IA." → La calculadora es el contraejemplo. El quiz de la pieza 3 lo evalúa.

CUIDADO
Quick, Draw! guarda los dibujos, sin nombre, en una colección pública de Google. No pide ningún dato personal. Si una familia pregunta, eso es lo que hay que decirle.$txt$,
    'url', 'https://quickdraw.withgoogle.com/?locale=es',
    'resource_type', 'enlace')
FROM subjects s
WHERE s.id = c.subject_id AND s.name = 'Mi Primera IA' AND c.title = '¿Qué es la inteligencia artificial?';

-- ─── Pieza 2 · mision · Misión 1: Entrevista a una IA ───
INSERT INTO content (title, description, type, subject_id, created_by, xp_reward,
                     difficulty, estimated_minutes, content_body, order_index,
                     is_published, is_active)
SELECT 'Misión 1: Entrevista a una IA', 'Hoy eres periodista y tu entrevistado es una inteligencia artificial: ByteBot. Tu misión es descubrir qué sabe, qué no sabe y qué hace cuando no tiene la respuesta.', 'mision'::content_type, mat.id, autor.id, 75,
       'facil'::difficulty_level, 30, '{}'::jsonb, 2, true, true
FROM       (SELECT id FROM subjects WHERE name = 'Mi Primera IA') mat
CROSS JOIN (SELECT id FROM users WHERE role IN ('admin','director') AND is_active
            ORDER BY created_at LIMIT 1) autor
WHERE NOT EXISTS (SELECT 1 FROM content x
                  WHERE x.subject_id = mat.id AND x.title = 'Misión 1: Entrevista a una IA');

UPDATE content c
SET description       = 'Hoy eres periodista y tu entrevistado es una inteligencia artificial: ByteBot. Tu misión es descubrir qué sabe, qué no sabe y qué hace cuando no tiene la respuesta.',
    type              = 'mision'::content_type,
    xp_reward         = 75,
    difficulty        = 'facil'::difficulty_level,
    estimated_minutes = 30,
    order_index       = 2,
    content_body      = jsonb_build_object(
    'instructions', $txt$Hoy eres PERIODISTA 🎤 y vas a entrevistar a una inteligencia artificial: a mí, ByteBot.

Los buenos periodistas no se creen todo lo que les dicen: preguntan, comparan y anotan. Esa es tu misión.

HOY VAS A PODER...
✔ Platicar con una IA y hacerle buenas preguntas.
✔ Descubrir qué hace una IA cuando no sabe algo.

CÓMO ENTRAS A LA ENTREVISTA
Dale al botón "🤖 Pedir ayuda a ByteBot". Se abre el chat conmigo. Lo que escribes para entregar va aquí, en esta actividad.
Ten esta pantalla y el chat a la mano: vas a ir y venir.

ANTES DE EMPEZAR: PREDICE ✍️
Anota en tu entrega: ¿crees que ByteBot sabe TODO? Sí o no, y por qué.
No hay respuesta mala: es tu predicción. Los científicos siempre predicen antes de probar.

PARTE 1 · LAS 5 PREGUNTAS DEL PERIODISTA
Hazme estas preguntas, una por una. Puedes escribirlas igualito o con tus palabras:
1. ¿Qué eres y cómo aprendiste lo que sabes?
2. ¿Qué cosas NO puedes hacer?
3. ¿Me explicas qué es la inteligencia artificial como si tuviera 8 años?
4. ¿En qué se parece tu forma de aprender a la de un niño? ¿En qué es diferente?
5. Una pregunta tuya, de lo que quieras: dinosaurios, el espacio, futbol, animales...

PARTE 2 · LA PREGUNTA TRAMPA 🕵️
Los mejores periodistas ponen a prueba a su entrevistado. Escoge UNA:
A) Algo que yo no puedo saber: "¿Qué desayuné hoy?" o "¿De qué color es mi cuarto?"
B) Algo que pasó apenas: "¿Quién ganó el partido de ayer?"
C) Una cuenta con truco: "¿Cuántas patas hay entre tres gallinas y dos perros?" ... y revisa TÚ si la cuenta está bien. (Pista: hazla tú primero en una hoja.)

Observa con lupa: ¿dije que no sabía? ¿Inventé algo? ¿Me equivoqué? ¿Sonaba muy seguro?

PARTE 3 · TU REPORTAJE (esto es lo que entregas)
Escríbelo con tus palabras, no copies mis respuestas completas:
1. Tu predicción: ¿creías que ByteBot sabía todo?
2. Las 2 respuestas mías que más te gustaron, y por qué.
3. Qué pregunta trampa hiciste y qué pasó.
4. La gran conclusión: después de la entrevista, ¿ByteBot sabe todo? ¿Qué vas a hacer tú cuando una IA te conteste algo importante?

💬 ByteBot dice: "Te cuento un secreto: yo aprendí de textos. No vivo en tu casa, no veo por la ventana y no sé qué pasó hoy. Por eso hay cosas que no sé, y a veces me equivoco aunque suene muy seguro. Un buen usuario de IA siempre revisa."

REGLA DE ORO DE HOY 🔒
Nunca le des a una IA tu nombre completo, tu dirección, tu escuela, tu teléfono ni fotos tuyas. Para esta entrevista no necesitas ninguno.

LO QUE TE LLEVAS HOY
Una IA puede ser muy útil y muy simpática, y aun así equivocarse. Sonar seguro no es lo mismo que tener razón.

¿QUÉ SIGUE?
Un quiz rápido para ver si ya tienes ojo de detective de IA.$txt$,
    'teacher_notes', $txt$OBJETIVO
Que el niño compruebe por sí mismo que una IA puede no saber, inventar o equivocarse, y que saque la conclusión de que hay que revisar. No se la decimos: la descubre.

TIEMPO: 30 minutos.

CÓMO CALIFICAR (sobre 10, se aprueba con 7)
- Predicción escrita: 1
- Dos respuestas favoritas con un "por qué" propio: 2
- Pregunta trampa descrita + qué observó: 3
- Conclusión: menciona revisar, comprobar o preguntar a un adulto: 4

QUÉ HACER SI...
- Copió respuestas enteras de ByteBot: "Pedir correcciones" con: "¡Qué buena entrevista! Ahora cuéntamelo con TUS palabras, como se lo contarías a un amigo."
- Su conclusión es "ByteBot sabe todo": no lo repruebes. "Pedir correcciones" con: "¿Y qué pasó con tu pregunta trampa? Vuelve a leer lo que te contestó y cuéntame si cambias de opinión."
- ByteBot contestó bien la pregunta trampa (por ejemplo, dijo "no puedo saberlo"): ¡también vale! La conclusión correcta es "una buena IA reconoce lo que no sabe, pero no todas lo hacen".

CUIDADO
Si en la entrega aparece un dato personal (dirección, escuela, nombre completo), recuérdaselo con cariño en el comentario. No es para regañar: es la regla de oro del curso.

PARA COMENTAR (ideas)
- "Hiciste una pregunta trampa buenísima. Eso es pensar como científico."
- "Me encantó tu conclusión: revisar es la súper habilidad de quien usa IA."$txt$,
    'checklist', jsonb_build_array(
        'Escribí mi predicción antes de empezar',
        'Le hice a ByteBot las 5 preguntas del periodista',
        'Hice una pregunta trampa y observé qué pasó',
        'Escogí las 2 respuestas que más me gustaron y dije por qué',
        'Escribí mi conclusión con mis palabras',
        'No compartí ningún dato personal en el chat'))
FROM subjects s
WHERE s.id = c.subject_id AND s.name = 'Mi Primera IA' AND c.title = 'Misión 1: Entrevista a una IA';

-- ─── Pieza 3 · quiz · Quiz: ¿IA o no IA? ───
INSERT INTO content (title, description, type, subject_id, created_by, xp_reward,
                     difficulty, estimated_minutes, content_body, order_index,
                     is_published, is_active)
SELECT 'Quiz: ¿IA o no IA?', '8 preguntas rápidas para ver si ya tienes ojo de detective: ¿esto usa inteligencia artificial, o solo sigue instrucciones?', 'quiz'::content_type, mat.id, autor.id, 40,
       'facil'::difficulty_level, 10, '{}'::jsonb, 3, true, true
FROM       (SELECT id FROM subjects WHERE name = 'Mi Primera IA') mat
CROSS JOIN (SELECT id FROM users WHERE role IN ('admin','director') AND is_active
            ORDER BY created_at LIMIT 1) autor
WHERE NOT EXISTS (SELECT 1 FROM content x
                  WHERE x.subject_id = mat.id AND x.title = 'Quiz: ¿IA o no IA?');

UPDATE content c
SET description       = '8 preguntas rápidas para ver si ya tienes ojo de detective: ¿esto usa inteligencia artificial, o solo sigue instrucciones?',
    type              = 'quiz'::content_type,
    xp_reward         = 40,
    difficulty        = 'facil'::difficulty_level,
    estimated_minutes = 10,
    order_index       = 3,
    content_body      = jsonb_build_object(
    'instructions', $txt$¡Hora de poner a prueba tu ojo de detective! 🔍

La pista para todas las preguntas es la misma:
👉 ¿Aprende de ejemplos, o solo sigue instrucciones?

Son 8 preguntas. Lee con calma: algunas tienen trampa.
Se aprueba con 6 de 8, y si no te sale a la primera puedes volver a intentarlo. Equivocarse también es aprender.$txt$,
    'teacher_notes', $txt$OBJETIVO
Comprobar las dos ideas de las piezas 1 y 2: la IA aprende de ejemplos, y puede equivocarse.

SE CALIFICA SOLO. Aprueba con 70 (6 de 8). Se puede reintentar. Las opciones se revuelven solas, salvo las de verdadero/falso.

QUÉ IDEA EQUIVOCADA ATACA CADA DISTRACTOR
- P3, "tiene todos los dibujos guardados y buscó uno igual": confundir aprender con memorizar. Es el error más interesante del quiz: si varios niños caen, vale la pena mandarles una explicación por Mensajes.
- P5: "si suena seguro, es cierto". Conecta con la pregunta trampa de la misión 1.
- P7, "el semáforo que cambia cada 60 segundos": es un temporizador, sigue una instrucción fija.

SI UN NIÑO REPRUEBA DOS VECES
Escríbele antes del tercer intento: "Vuelve a leer la parte UN PROGRAMA NORMAL vs. UNA IA de la actividad 1. Ahí está la llave de casi todas."$txt$)
FROM subjects s
WHERE s.id = c.subject_id AND s.name = 'Mi Primera IA' AND c.title = 'Quiz: ¿IA o no IA?';

-- ─── Pieza 4 · material · Las máquinas aprenden con ejemplos ───
INSERT INTO content (title, description, type, subject_id, created_by, xp_reward,
                     difficulty, estimated_minutes, content_body, order_index,
                     is_published, is_active)
SELECT 'Las máquinas aprenden con ejemplos', '¿Cómo aprende una máquina? Igual que tú aprendiste a distinguir un perro de un gato: viendo muchos ejemplos. Hoy lo pruebas con tarjetas y luego entrenas a una IA para limpiar el océano.', 'material'::content_type, mat.id, autor.id, 25,
       'facil'::difficulty_level, 25, '{}'::jsonb, 4, true, true
FROM       (SELECT id FROM subjects WHERE name = 'Mi Primera IA') mat
CROSS JOIN (SELECT id FROM users WHERE role IN ('admin','director') AND is_active
            ORDER BY created_at LIMIT 1) autor
WHERE NOT EXISTS (SELECT 1 FROM content x
                  WHERE x.subject_id = mat.id AND x.title = 'Las máquinas aprenden con ejemplos');

UPDATE content c
SET description       = '¿Cómo aprende una máquina? Igual que tú aprendiste a distinguir un perro de un gato: viendo muchos ejemplos. Hoy lo pruebas con tarjetas y luego entrenas a una IA para limpiar el océano.',
    type              = 'material'::content_type,
    xp_reward         = 25,
    difficulty        = 'facil'::difficulty_level,
    estimated_minutes = 25,
    order_index       = 4,
    content_body      = jsonb_build_object(
    'instructions', $txt$Hoy descubres el secreto de cómo aprende una IA. Y además... ¡entrenas una por primera vez! 🐟

HOY VAS A PODER...
✔ Explicar qué son los DATOS, las ETIQUETAS y el ENTRENAMIENTO.
✔ Entrenar una IA con tus propias manos.

PRIMERO, SIN COMPUTADORA: EL JUEGO DE LAS TARJETAS (10 min)
Necesitas una hoja, tijeras (o solo dobleces) y un lápiz.
1. Haz 8 tarjetitas y dibuja una cosa en cada una: 4 que se comen (manzana, pan, pizza, plátano) y 4 que no (zapato, lápiz, pelota, llave).
2. En otra hoja escribe dos títulos: SE COME y NO SE COME.
3. Acomoda cada tarjeta debajo de su título.

¡Listo! Acabas de hacer exactamente lo que hace una persona cuando le enseña a una IA.

LAS 3 PALABRAS MÁGICAS DE LA IA
📦 DATOS: los ejemplos que le enseñas. Tus 8 tarjetas son tus datos.
🏷️ ETIQUETAS: el nombre del grupo de cada ejemplo. "Se come" y "No se come" son tus etiquetas.
🏋️ ENTRENAR: cuando la máquina mira todos los ejemplos con su etiqueta y busca el patrón. Es como estudiar para un examen.

Y hay una cuarta palabra:
🔮 PREDECIR: cuando la IA ya aprendió, le enseñas algo NUEVO que nunca vio y ella dice a qué grupo cree que pertenece.

PAUSA PARA PENSAR 🤔
Si le enseñaras una tarjeta nueva con una galleta, ¿en qué columna la pondría una IA que aprendió de tus tarjetas? ¿Y si le enseñas una piedra que parece pan? 😏

AHORA, CON COMPUTADORA: IA PARA LOS OCÉANOS (15 min)
1. Abre el enlace de arriba. Si sale en inglés, busca el selector de idioma (casi siempre hasta abajo de la página) y escoge "Español".
2. Mira el video corto del principio.
3. Van a pasar peces y basura. Tu trabajo es decirle a la IA cuál es "pez" y cuál "no es pez". ¡Estás ETIQUETANDO DATOS!
4. Cuando hayas etiquetado bastantes, dale a continuar y mira cómo TU IA limpia el océano sola.
5. Haz por lo menos las dos primeras partes. Si te gusta, sigue: más adelante se pone más interesante.

PAUSA PARA PENSAR 🤔
▸ Cuando le diste pocos ejemplos, ¿se equivocaba más o menos?
▸ ¿Tu IA sacó del agua algún pez por error? ¿Por qué crees que pasó?

💬 ByteBot dice: "¿Quieres otra explicación? Dale al botón de ByteBot y escríbeme: ¿Qué son los datos de entrenamiento? Explícamelo como si tuviera 7 años. Luego compara: ¿me entendiste mejor a mí o a esta lectura?"

LO QUE TE LLEVAS HOY
Una IA es tan buena como sus ejemplos.
Pocos ejemplos → aprende poco.
Ejemplos variados → aprende mejor.

¿QUÉ SIGUE?
Vas a entrenar TU PROPIA IA desde cero, con dibujos tuyos. Ve preparando tus colores. 🖍️$txt$,
    'teacher_notes', $txt$OBJETIVO
Vocabulario base del curso: datos, etiquetas, entrenar, predecir. La misión 2 y el quiz 2 lo dan por sabido.

TIEMPO: 25 minutos. ES MATERIAL: no se califica.

POR QUÉ EMPEZAR SIN COMPUTADORA
La actividad de tarjetas hace que el niño SEA la máquina antes de usarla. Cuando después etiqueta peces, ya sabe qué está haciendo y por qué. Es la diferencia entre seguir pasos y entender.

IA PARA LOS OCÉANOS (Code.org)
Es gratis, no pide cuenta y tiene videos en español. Es el mismo recurso que citamos en la guía del Principiante. Si una familia pregunta: más adelante el juego pide clasificar peces por cosas de opinión, y ahí asoma el sesgo que vemos en la pieza 8. Es un buen puente.

PREGUNTAS QUE VAN A LLEGAR
- "¿La galleta sí se come?" → justo: depende de los ejemplos que le diste. Si nunca vio una galleta, adivina por lo que se le parece.
- "Mi IA sacó peces" → perfecto, eso es un error de entrenamiento. Pídele que piense qué ejemplo le faltó.$txt$,
    'url', 'https://studio.code.org/s/oceans',
    'resource_type', 'enlace')
FROM subjects s
WHERE s.id = c.subject_id AND s.name = 'Mi Primera IA' AND c.title = 'Las máquinas aprenden con ejemplos';

-- ─── Pieza 5 · mision · Misión 2: Entrena tu Detector de Caritas ───
INSERT INTO content (title, description, type, subject_id, created_by, xp_reward,
                     difficulty, estimated_minutes, content_body, order_index,
                     is_published, is_active)
SELECT 'Misión 2: Entrena tu Detector de Caritas', 'Hoy creas tu primera inteligencia artificial desde cero: una IA que reconoce si una carita dibujada está feliz o triste. Tú la entrenas, tú la pones a prueba y tú descubres su secreto.', 'mision'::content_type, mat.id, autor.id, 75,
       'medio'::difficulty_level, 45, '{}'::jsonb, 5, true, true
FROM       (SELECT id FROM subjects WHERE name = 'Mi Primera IA') mat
CROSS JOIN (SELECT id FROM users WHERE role IN ('admin','director') AND is_active
            ORDER BY created_at LIMIT 1) autor
WHERE NOT EXISTS (SELECT 1 FROM content x
                  WHERE x.subject_id = mat.id AND x.title = 'Misión 2: Entrena tu Detector de Caritas');

UPDATE content c
SET description       = 'Hoy creas tu primera inteligencia artificial desde cero: una IA que reconoce si una carita dibujada está feliz o triste. Tú la entrenas, tú la pones a prueba y tú descubres su secreto.',
    type              = 'mision'::content_type,
    xp_reward         = 75,
    difficulty        = 'medio'::difficulty_level,
    estimated_minutes = 45,
    order_index       = 5,
    content_body      = jsonb_build_object(
    'instructions', $txt$Esta es LA misión del curso. Hoy creas una inteligencia artificial de verdad, desde cero, con tus propias manos. 🧠✨

Tu IA va a mirar una carita dibujada y va a decir si está FELIZ 😊 o TRISTE 😢.

HOY VAS A PODER...
✔ Entrenar una IA con tus propios datos.
✔ Ponerla a prueba como un científico.
✔ Descubrir un secreto que tienen TODAS las IAs.

PARTE 1 · PREPARA TUS DATOS (15 min)
Necesitas: hojas blancas, un plumón o crayola AZUL y uno ROJO.
1. Corta o dobla hojas para hacer 20 tarjetas del tamaño de tu mano.
2. En 10 tarjetas dibuja caritas FELICES 😊, todas con el color AZUL.
3. En otras 10 dibuja caritas TRISTES 😢, todas con el color ROJO.
4. Dibújalas grandes, que llenen la tarjeta. Pueden ser distintas: redondas, cuadradas, con pelo, sin pelo...

¿Por qué azul y rojo? Es parte del experimento. Confía en mí. 😉
Y guarda más hojas y tus colores: los vas a necesitar al final.

⚠️ IMPORTANTE: tu IA solo va a ver DIBUJOS. Nunca le enseñes tu cara ni la de otra persona.

PARTE 2 · ARMA TU PROYECTO (5 min)
1. Abre el enlace de arriba (Machine Learning for Kids).
2. Escoge "Pruébalo ahora" para entrar sin registrarte. Si te pide usuario, pídeselo a tu maestra en Mensajes.
3. Dale a "Añadir un nuevo proyecto". Nombre: Detector de caritas. Tipo: reconocer IMÁGENES. Créalo y ábrelo.
4. Entra a "Entrenar".
5. Agrega dos etiquetas: feliz y triste.

PARTE 3 · ENSÉÑALE (10 min)
1. En la etiqueta "feliz", usa el botón de la cámara y enséñale tus 10 caritas AZULES, una por una. Una foto por tarjeta.
2. En "triste", enséñale tus 10 caritas ROJAS.
3. Ve a "Aprender & Probar" y dale a "Entrenar nuevo modelo". Espera a que termine: eso es ENTRENAR.

⚠️ Esta forma de entrar no guarda tu proyecto para siempre. Anota tus resultados mientras avanzas.

PARTE 4 · PREDICE Y PRUEBA, COMO CIENTÍFICO (10 min)
Dibuja 4 caritas NUEVAS. Antes de enseñarle cada una, escribe qué crees que va a contestar. Luego pruébala y anota qué contestó.

   Prueba                        | Yo predigo | La IA dijo
   A) Carita feliz en AZUL       |            |
   B) Carita triste en ROJO      |            |
   C) Carita FELIZ en ROJO   🤔  |            |
   D) Carita TRISTE en AZUL  🤔  |            |

PAUSA PARA PENSAR 🤔
Mira las pruebas C y D. ¿Qué pasó?
Si tu IA se equivocó, pregúntate: ¿de verdad aprendió a ver SONRISAS... o aprendió otra cosa más fácil?

EL SECRETO QUE DESCUBRISTE
Todas tus caritas felices eran azules y todas las tristes rojas. Para la IA, fijarse en el COLOR era mucho más fácil que fijarse en la boca. Y como nunca vio una carita feliz roja, aprendió una trampa: "rojo = triste".

¡Eso tiene nombre! Se llama SESGO: cuando una IA aprende algo equivocado porque sus ejemplos no eran variados. Le pasa a las IAs de verdad, y ahora ya sabes por qué.

(¿Tu IA le atinó a todo? ¡Qué bien! Entonces tu IA sí se fijó en la boca. Pero quédate con la pregunta: ¿qué pudo haber salido mal?)

PARTE 5 · ARRÉGLALA (5 min)
1. Dibuja 5 caritas felices en ROJO y 5 tristes en AZUL.
2. Agrégalas a sus etiquetas y vuelve a darle a "Entrenar nuevo modelo".
3. Repite las pruebas C y D. ¿Mejoró?

TU ENTREGA
Escribe aquí:
1. Tu tabla de 4 pruebas, con lo que predijiste y lo que contestó la IA.
2. ¿Qué pasó con las pruebas C y D?
3. ¿Qué crees que aprendió tu IA en realidad?
4. ¿Qué hiciste para arreglarla, y funcionó?
5. En una frase: ¿qué es el SESGO?

💬 ByteBot dice: "Si te atoras en cualquier paso, dale al botón de ByteBot y cuéntame en qué parte vas. Y si tu IA hizo algo rarísimo... ¡cuéntamelo también!"

LO QUE TE LLEVAS HOY
Tú entrenaste una IA. Y descubriste lo más importante: una IA aprende EXACTAMENTE lo que le enseñas, hasta lo que no querías enseñarle.$txt$,
    'teacher_notes', $txt$OBJETIVO
Que el niño entrene un modelo real y DESCUBRA el sesgo por sí mismo. Es la pieza más importante del curso: si la vive bien, lo demás se entiende solo.

TIEMPO: 45 minutos. Es la más larga; que no se haga en 10.

EL DISEÑO: UN ERROR PROVOCADO A PROPÓSITO
Felices en azul y tristes en rojo es una trampa: con 10 ejemplos por lado, el color es el patrón más fácil de aprender, y la IA casi siempre se agarra de él. Las pruebas C y D lo destapan. Es aprendizaje por error productivo: el niño se equivoca en su predicción, busca la causa y la arregla. Eso se queda mucho más que una explicación.

Si la IA de un niño le atinó a todo (puede pasar si dibujó bocas muy grandes y marcadas), NO es un fracaso: la entrega pide explicar "qué pudo haber salido mal". Evalúa el razonamiento, no el resultado.

CÓMO CALIFICAR (sobre 10, se aprueba con 7)
- Tabla con las 4 pruebas, con predicción Y resultado: 3
- Explica qué pasó en C y D, o qué pudo pasar: 2
- Dice qué aprendió "en realidad" la IA (color vs. boca): 2
- Describe cómo la arregló y si funcionó: 2
- Define sesgo con sus palabras: 1

PROBLEMAS TÉCNICOS PROBABLES
- "No me deja entrar": si "Pruébalo ahora" no aparece, créale cuenta de ML4Kids (como en el Principiante) y mándale usuario y contraseña por Mensajes.
- "La cámara no prende": la tablet debe dar permiso de cámara al navegador. En iPad: Ajustes → Safari → Cámara → Permitir.
- "Se borró mi proyecto": esa forma de entrar no guarda para siempre. Lo que se califica es la entrega escrita; no hace falta repetir el entrenamiento.

CUIDADO CON LA PRIVACIDAD
Si en una entrega se ve que el niño usó su cara en vez de dibujos, coméntaselo con cariño: la instrucción lo prohíbe a propósito.

PARA COMENTAR (ideas)
- "¡Descubriste el sesgo tú solo! Los científicos de IA pasan mucho tiempo buscando justo eso."
- "Tu predicción falló y tú encontraste por qué. Así se aprende de verdad."$txt$,
    'url', 'https://machinelearningforkids.co.uk/?lang=es',
    'resource_type', 'enlace',
    'checklist', jsonb_build_array(
        'Dibujé 10 caritas felices en azul y 10 tristes en rojo',
        'Creé mi proyecto de imágenes con las etiquetas feliz y triste',
        'Le enseñé mis 20 tarjetas con la cámara',
        'Le di a "Entrenar nuevo modelo" y esperé',
        'Hice las 4 pruebas escribiendo primero mi predicción',
        'Expliqué qué pasó con las caritas de color cambiado',
        'Agregué caritas de colores mezclados y volví a entrenar',
        'Escribí qué es el sesgo con mis palabras'))
FROM subjects s
WHERE s.id = c.subject_id AND s.name = 'Mi Primera IA' AND c.title = 'Misión 2: Entrena tu Detector de Caritas';

-- ─── Pieza 6 · tarea · Investigación: La IA en mi casa ───
INSERT INTO content (title, description, type, subject_id, created_by, xp_reward,
                     difficulty, estimated_minutes, content_body, order_index,
                     is_published, is_active)
SELECT 'Investigación: La IA en mi casa', 'Conviértete en detective: encuentra la inteligencia artificial escondida en tu casa, entrevista a un adulto de tu familia y descubre con ByteBot cómo funciona una de ellas.', 'tarea'::content_type, mat.id, autor.id, 60,
       'facil'::difficulty_level, 40, '{}'::jsonb, 6, true, true
FROM       (SELECT id FROM subjects WHERE name = 'Mi Primera IA') mat
CROSS JOIN (SELECT id FROM users WHERE role IN ('admin','director') AND is_active
            ORDER BY created_at LIMIT 1) autor
WHERE NOT EXISTS (SELECT 1 FROM content x
                  WHERE x.subject_id = mat.id AND x.title = 'Investigación: La IA en mi casa');

UPDATE content c
SET description       = 'Conviértete en detective: encuentra la inteligencia artificial escondida en tu casa, entrevista a un adulto de tu familia y descubre con ByteBot cómo funciona una de ellas.',
    type              = 'tarea'::content_type,
    xp_reward         = 60,
    difficulty        = 'facil'::difficulty_level,
    estimated_minutes = 40,
    order_index       = 6,
    content_body      = jsonb_build_object(
    'instructions', $txt$Hoy eres DETECTIVE DE IA 🔍 y tu escena del crimen es... ¡tu casa!

HOY VAS A PODER...
✔ Reconocer la IA en aparatos y apps de verdad.
✔ Investigar como detective: buscar, preguntar y comprobar.

PARTE 1 · LA BÚSQUEDA (15 min)
Recorre tu casa con un adulto y encuentra 3 aparatos o apps que usen IA.

Pistas de detective. Tiene IA si...
▸ Aprende de ti o de lo que te gusta.
▸ Reconoce tu voz, tu cara o tu huella.
▸ Te recomienda cosas.
▸ Adivina lo que vas a hacer o escribir.

Lugares donde casi siempre hay:
📺 La tele o la app de videos (lo que te recomienda)
📱 El celular (desbloqueo con cara, fotos que se agrupan solas por persona, el teclado)
🗣️ Un asistente de voz
🗺️ El mapa que avisa del tráfico
🌐 El traductor
📧 El correo que separa el spam solito

Para cada una anota:
   Qué es  |  Qué hace con IA  |  ¿De qué ejemplos crees que aprendió?

PARTE 2 · LA ENTREVISTA (10 min)
Entrevista a un adulto de tu familia. Pregúntale:
1. ¿Qué inteligencia artificial usas en tu trabajo o en tu día?
2. ¿En qué te ayuda?
3. ¿Alguna vez se equivocó o hizo algo raro?
⭐ Pregunta bonus: ¿Cómo crees que va a ser la IA cuando yo sea grande?
Anota sus respuestas.

PARTE 3 · PREGÚNTALE A BYTEBOT (10 min)
Escoge UNA de las 3 cosas que encontraste y pregúntame, con el botón de ByteBot:
"¿Cómo aprendió [lo que encontraste] a [lo que hace]?"
Ejemplo: "¿Cómo aprendió el mapa a saber dónde hay tráfico?"

Luego escríbelo con TUS palabras, en 3 renglones máximo. Y contesta: ¿lo que te dije se parece a lo que tú habías pensado?

TU ENTREGA
1. Tu tabla con las 3 cosas que encontraste.
2. Las respuestas de tu entrevista.
3. Lo que aprendiste de ByteBot, con tus palabras, y si coincide con lo que pensabas.

🔒 REGLA DE ORO: no mandes fotos de tu casa ni escribas tu dirección. No se necesitan.

LO QUE TE LLEVAS HOY
La IA no es cosa del futuro: ya vive en tu casa. Y ahora sabes reconocerla.

¿QUÉ SIGUE?
Un quiz para comprobar que ya sabes cómo aprende una máquina.$txt$,
    'teacher_notes', $txt$OBJETIVO
Llevar la IA del curso a la vida real del niño, y meter a la familia en el curso.

ESTO ES MARKETING TAMBIÉN
La entrevista hace que un papá o una mamá vea el curso funcionando y platique de IA con su hijo. Es el momento en que la familia decide si ByteKids vale la pena. Comenta estas entregas con especial cariño.

TIEMPO: 40 minutos, que pueden repartirse en dos días.

CÓMO CALIFICAR (sobre 10, se aprueba con 7)
- Tres hallazgos válidos, cada uno con "de qué ejemplos aprendió": 4
- Entrevista con las 3 respuestas: 3
- ByteBot explicado con sus palabras, más la comparación: 3

QUÉ HACER SI...
- Pone algo que no es IA (el microondas, el foco): no lo cuentes, pero explícale por qué con la pista "¿aprende o sigue instrucciones?". Si le quedan 2 válidos, pide correcciones para que busque el tercero.
- Copió a ByteBot tal cual: pide correcciones: "Cuéntamelo como se lo explicarías a tu abuelita."
- No tuvo con quién hacer la entrevista: acepta a un maestro, un vecino o un familiar por videollamada.$txt$,
    'checklist', jsonb_build_array(
        'Encontré 3 cosas de mi casa que usan IA',
        'Para cada una escribí qué hace y de qué ejemplos creo que aprendió',
        'Entrevisté a un adulto de mi familia y anoté sus respuestas',
        'Le pregunté a ByteBot cómo aprendió una de mis 3 cosas',
        'Escribí la respuesta con mis palabras, sin copiar',
        'No compartí fotos de mi casa ni mi dirección'))
FROM subjects s
WHERE s.id = c.subject_id AND s.name = 'Mi Primera IA' AND c.title = 'Investigación: La IA en mi casa';

-- ─── Pieza 7 · quiz · Quiz: ¿Cómo aprende una máquina? ───
INSERT INTO content (title, description, type, subject_id, created_by, xp_reward,
                     difficulty, estimated_minutes, content_body, order_index,
                     is_published, is_active)
SELECT 'Quiz: ¿Cómo aprende una máquina?', '8 preguntas sobre datos, etiquetas, entrenamiento y lo que descubriste con tu Detector de Caritas.', 'quiz'::content_type, mat.id, autor.id, 40,
       'facil'::difficulty_level, 10, '{}'::jsonb, 7, true, true
FROM       (SELECT id FROM subjects WHERE name = 'Mi Primera IA') mat
CROSS JOIN (SELECT id FROM users WHERE role IN ('admin','director') AND is_active
            ORDER BY created_at LIMIT 1) autor
WHERE NOT EXISTS (SELECT 1 FROM content x
                  WHERE x.subject_id = mat.id AND x.title = 'Quiz: ¿Cómo aprende una máquina?');

UPDATE content c
SET description       = '8 preguntas sobre datos, etiquetas, entrenamiento y lo que descubriste con tu Detector de Caritas.',
    type              = 'quiz'::content_type,
    xp_reward         = 40,
    difficulty        = 'facil'::difficulty_level,
    estimated_minutes = 10,
    order_index       = 7,
    content_body      = jsonb_build_object(
    'instructions', $txt$Ya entrenaste tu propia IA. Veamos cuánto aprendiste TÚ. 🧠

Acuérdate de las palabras mágicas: DATOS, ETIQUETAS, ENTRENAR y PREDECIR. Y de lo que pasó con tus caritas de colores...

Son 8 preguntas. Se aprueba con 6 de 8 y puedes volver a intentarlo.$txt$,
    'teacher_notes', $txt$OBJETIVO
Comprobar el vocabulario de la pieza 4 y la lección de sesgo de la misión 2.

SE CALIFICA SOLO. Aprueba con 70 (6 de 8). Se puede reintentar.

LAS QUE MÁS DICEN
- P5 (la carita feliz roja): si un niño la falla, probablemente no llegó a las pruebas C y D de la misión 2. Revisa su entrega.
- P6 (ejemplos variados): es la idea que necesita para el proyecto final. Si la falla, recuérdasela cuando comentes el proyecto.$txt$)
FROM subjects s
WHERE s.id = c.subject_id AND s.name = 'Mi Primera IA' AND c.title = 'Quiz: ¿Cómo aprende una máquina?';

-- ─── Pieza 8 · material · Cuando la IA se equivoca ───
INSERT INTO content (title, description, type, subject_id, created_by, xp_reward,
                     difficulty, estimated_minutes, content_body, order_index,
                     is_published, is_active)
SELECT 'Cuando la IA se equivoca', 'Tu Detector de Caritas se equivocó por algo que tiene nombre: sesgo. Hoy descubres por qué importa en el mundo real y armas tus reglas de oro para usar la IA con cuidado.', 'material'::content_type, mat.id, autor.id, 25,
       'facil'::difficulty_level, 20, '{}'::jsonb, 8, true, true
FROM       (SELECT id FROM subjects WHERE name = 'Mi Primera IA') mat
CROSS JOIN (SELECT id FROM users WHERE role IN ('admin','director') AND is_active
            ORDER BY created_at LIMIT 1) autor
WHERE NOT EXISTS (SELECT 1 FROM content x
                  WHERE x.subject_id = mat.id AND x.title = 'Cuando la IA se equivoca');

UPDATE content c
SET description       = 'Tu Detector de Caritas se equivocó por algo que tiene nombre: sesgo. Hoy descubres por qué importa en el mundo real y armas tus reglas de oro para usar la IA con cuidado.',
    type              = 'material'::content_type,
    xp_reward         = 25,
    difficulty        = 'facil'::difficulty_level,
    estimated_minutes = 20,
    order_index       = 8,
    content_body      = jsonb_build_object(
    'instructions', $txt$¿Te acuerdas de tu carita feliz ROJA? 😊🔴
Tu IA pensó que estaba triste. No fue por mala: fue porque solo había visto caritas felices azules.

HOY VAS A PODER...
✔ Explicar qué es el sesgo y por qué pasa.
✔ Usar la IA con tus 5 reglas de oro.

EL SESGO, EN FÁCIL
Imagina que le enseñas a una IA a reconocer gatos, pero todas las fotos son de gatos NARANJAS. 🐈
Un día le enseñas un gato negro... y dice "no es un gato".
La IA no es tonta: aprendió exactamente lo que le enseñaron. El problema estaba en los EJEMPLOS.

Eso es el sesgo: cuando una IA aprende algo chueco porque sus ejemplos no eran variados o no eran justos.

¿Y ESTO PASA DE VERDAD? SÍ.
▸ Algunas IAs que reconocen caras funcionaban peor con ciertas personas, porque casi no las vieron en sus ejemplos.
▸ Algunos traductores escribían "el doctor" y "la enfermera" aunque nadie dijera si era hombre o mujer, porque así venía en la mayoría de sus textos.
▸ Una IA que recomienda juguetes puede decidir que ciertos juguetes son "de niña" o "de niño" solo porque así se compraban antes.

Por eso las personas que crean IAs tienen que revisar muy bien sus ejemplos. Justo lo que tú hiciste al arreglar tu Detector.

PAUSA PARA PENSAR 🤔
Si entrenaras una IA para reconocer PERROS solo con fotos de chihuahuas, ¿qué pasaría cuando vea un gran danés?

LA IA TAMBIÉN PUEDE INVENTAR
¿Te acuerdas de tu pregunta trampa en la entrevista? A veces una IA que platica, como yo, contesta algo que suena muy seguro pero no es cierto. No lo hace para engañarte: arma la respuesta que le parece más probable.

MIS 5 REGLAS DE ORO CON LA IA ⭐
1. 🔒 MIS DATOS SON MÍOS. No comparto mi nombre completo, dirección, escuela, teléfono, contraseñas ni fotos.
2. 🔍 REVISO. Si algo es importante, lo compruebo en otro lugar o con un adulto.
3. 🧠 PIENSO YO PRIMERO. La IA me ayuda a aprender; no piensa ni hace la tarea por mí.
4. ❤️ SOY AMABLE. No uso la IA para molestar, asustar ni burlarme de nadie.
5. 🗣️ SI ALGO ME INCOMODA, LE DIGO A UN ADULTO. Siempre.

💬 ByteBot dice: "Pregúntame con el botón de ByteBot: ¿Qué información nunca debería compartir con una IA? Compara mi respuesta con tus 5 reglas. Si te digo algo que no está en tu lista, ¡agrégalo como regla número 6!"

PARA CERRAR, PIENSA
Una IA que decide qué niños reciben un premio de la escuela, ¿dónde podría tener sesgo? ¿Qué ejemplos le darías para que fuera justa?

LO QUE TE LLEVAS HOY
Una IA es tan justa como sus ejemplos. Y quien la usa bien, revisa, cuida sus datos y piensa por sí mismo.

¿QUÉ SIGUE?
¡Tu proyecto final! Vas a inventar una IA que ayude a alguien. Y ya sabes lo más importante: pensar qué podría salir mal.$txt$,
    'teacher_notes', $txt$OBJETIVO
Ponerle nombre a lo que el niño vivió en la misión 2 (sesgo), llevarlo al mundo real y cerrar el curso con hábitos de uso seguro.

TIEMPO: 20 minutos. ES MATERIAL: no se califica, pero el proyecto final pide aplicarlo.

LOS EJEMPLOS DEL MUNDO REAL
Están suavizados a propósito para niños de 8 a 12. Si una familia pide más, los casos reales son el reconocimiento facial con peor precisión en ciertos grupos y los traductores con sesgo de género. Los dos están bien documentados.

LAS REGLAS DE ORO
Son el hilo de seguridad del curso: ya aparecieron en las misiones 1 y 2 y en la investigación. Si un niño comparte datos personales en cualquier entrega, lo correcto es recordarle la regla 1 con cariño, no reprobarlo.

LA ACTIVIDAD CON BYTEBOT
ByteBot tiene reglas propias de protección para niños, así que su respuesta sobre qué no compartir suele coincidir con la lista y agregar algo (contraseñas, ubicación). Que el niño agregue una "regla 6" le da sentido de dueño sobre sus reglas.$txt$)
FROM subjects s
WHERE s.id = c.subject_id AND s.name = 'Mi Primera IA' AND c.title = 'Cuando la IA se equivoca';

-- ─── Pieza 9 · proyecto · Proyecto final: Mi IA para ayudar ───
INSERT INTO content (title, description, type, subject_id, created_by, xp_reward,
                     difficulty, estimated_minutes, content_body, order_index,
                     is_published, is_active)
SELECT 'Proyecto final: Mi IA para ayudar', 'Tu gran proyecto: inventa una inteligencia artificial que ayude a alguien de tu casa, tu escuela o tu comunidad. La diseñas, la presentas y, si te animas, la construyes. Al aprobarlo te ganas tu certificado ByteKids.', 'proyecto'::content_type, mat.id, autor.id, 135,
       'medio'::difficulty_level, 90, '{}'::jsonb, 9, true, true
FROM       (SELECT id FROM subjects WHERE name = 'Mi Primera IA') mat
CROSS JOIN (SELECT id FROM users WHERE role IN ('admin','director') AND is_active
            ORDER BY created_at LIMIT 1) autor
WHERE NOT EXISTS (SELECT 1 FROM content x
                  WHERE x.subject_id = mat.id AND x.title = 'Proyecto final: Mi IA para ayudar');

UPDATE content c
SET description       = 'Tu gran proyecto: inventa una inteligencia artificial que ayude a alguien de tu casa, tu escuela o tu comunidad. La diseñas, la presentas y, si te animas, la construyes. Al aprobarlo te ganas tu certificado ByteKids.',
    type              = 'proyecto'::content_type,
    xp_reward         = 135,
    difficulty        = 'medio'::difficulty_level,
    estimated_minutes = 90,
    order_index       = 9,
    content_body      = jsonb_build_object(
    'instructions', $txt$¡Llegaste al final! 🎉 Ahora tú eres el inventor.

Vas a diseñar una inteligencia artificial que AYUDE a alguien. Al aprobar este proyecto te ganas tu certificado de ByteKids Academy. 🎓

HOY VAS A PODER...
✔ Usar todo lo que aprendiste para inventar tu propia IA.
✔ Pensar como los verdaderos creadores de IA: qué necesita, qué puede salir mal y cómo mejorarla.

PASO 1 · ENCUENTRA UN PROBLEMA (el paso más importante)
Piensa en alguien de tu casa, tu escuela o tu colonia. ¿Qué le cuesta trabajo? ¿Qué le ayudaría?
Tu IA tiene que poder aprender con IMÁGENES, como tu Detector de Caritas.

¿No se te ocurre nada? Escoge una de estas o úsalas de inspiración:
🗑️ La Separadora de Basura: ¿es papel, plástico u orgánico?
🌱 La Cuidadora de Plantas: ¿esta hoja está sana o necesita agua?
🐾 La Guardiana del Plato: ¿el plato de mi mascota está lleno o vacío?
🎨 La Jueza de Dibujos: ¿qué animal dibujó mi hermanito?
💡 ¡La tuya! La que tú inventes.

PASO 2 · DALE VIDA
▸ Ponle un NOMBRE a tu IA.
▸ Haz un DIBUJO de cómo se vería funcionando. Puede ser una app, un robot o una cámara. ¡Tú decides!
▸ Escribe en una frase: "Mi IA ayuda a ____ a ____".

PASO 3 · ¿QUÉ TIENE QUE APRENDER?
▸ Escribe sus ETIQUETAS: 2 o 3 grupos. Ejemplo: "sana" y "seca".
▸ Para cada etiqueta, describe por lo menos 3 EJEMPLOS con los que la entrenarías.
▸ ¡Acuérdate del Detector de Caritas! Tus ejemplos tienen que ser VARIADOS: distintos colores, tamaños, luces y lugares. Explica por qué escogiste esos.

PASO 4 · ¿QUÉ PODRÍA SALIR MAL?
Piensa como los científicos de IA:
▸ ¿Dónde se podría equivocar tu IA? (¿Con poca luz? ¿Con algo que nunca vio?)
▸ ¿Podría tener SESGO? ¿Por qué?
▸ ¿Qué pasaría si se equivoca? ¿Es grave o no tanto?
▸ ¿Cómo lo evitarías?

PASO 5 · PÍDELE CONSEJO A BYTEBOT 💬
Dale al botón de ByteBot y preséntame tu idea así:
"Mi IA se llama ____ y sirve para ____. Sus etiquetas son ____. ¿Cómo la puedo mejorar?"
Escoge UNO de mis consejos y cuenta si lo vas a usar o no, y POR QUÉ.
(Está perfecto no hacerme caso si tienes una buena razón. ¡Tú eres el inventor!)

PASO 6 · ENTREGA TU FICHA
Escribe aquí tu ficha, con estos títulos:
   NOMBRE DE MI IA:
   A QUIÉN AYUDA Y CÓMO:
   MIS ETIQUETAS Y MIS EJEMPLOS:
   QUÉ PODRÍA SALIR MAL Y CÓMO LO EVITO:
   EL CONSEJO DE BYTEBOT Y QUÉ DECIDÍ:
   MI DIBUJO: descríbelo en 2 o 3 renglones.

DOS NIVELES: LOS DOS DAN CERTIFICADO
🟢 EXPLORADOR: tu ficha completa. Con eso basta para tu certificado.
🔵 CONSTRUCTOR (opcional, ¡para los valientes!): además, construye tu IA en Machine Learning for Kids (el enlace de arriba), igual que tu Detector de Caritas. Mínimo 10 ejemplos por etiqueta, hechos con dibujos o fotos de OBJETOS, nunca de personas. Haz 5 pruebas y agrega a tu ficha:
   MIS 5 PRUEBAS:  qué le enseñé | qué contestó | ¿le atinó?

ASÍ SE CALIFICA (sobre 10, se aprueba con 7)
⭐ Un problema claro y real, y a quién ayuda: 2
⭐ Etiquetas y ejemplos variados que sí le enseñan: 3
⭐ Qué podría salir mal y cómo lo evitas: 2
⭐ El consejo de ByteBot y tu decisión con un porqué: 2
⭐ Nombre, dibujo y creatividad: 1

No se califica que tu IA sea perfecta. Se califica que la pienses bien. Una IA que "a veces falla con poca luz, y así lo arreglaría" vale más que una que "funciona perfecto".

💬 ByteBot dice: "Estoy muy orgulloso de ti. Empezaste sin saber qué era la IA y hoy estás inventando una. Eso es lo que hacen los creadores de tecnología."

¡A INVENTAR! 🚀$txt$,
    'teacher_notes', $txt$OBJETIVO
Que el niño aplique TODO el curso: problema real → etiquetas → ejemplos variados → riesgos y sesgo → mejora con ayuda de una IA → presentación.

TIEMPO: 60 a 90 minutos. Puede tomar varios días; no hay prisa.

ESTE PROYECTO DESTRABA EL CERTIFICADO
Aprobarlo (7 o más) + los dos quizzes aprobados + las 9 piezas completas = certificado. Por eso: si un proyecto no llega, NO lo repruebes. Usa "Pedir correcciones" y dile exactamente qué falta. Queremos que TODOS lleguen al certificado; el estándar se sostiene con correcciones, no con rechazos.

RÚBRICA DETALLADA (sobre 10)
Problema y a quién ayuda (2)
  2 = claro, real, dice quién se beneficia · 1 = vago ("ayuda a la gente") · 0 = no hay
Etiquetas y ejemplos (3)
  3 = 2 o 3 etiquetas, 3+ ejemplos cada una, VARIADOS y lo justifica · 2 = ejemplos sin variedad · 1 = faltan etiquetas o ejemplos
Qué puede salir mal (2)
  2 = un riesgo concreto + cómo evitarlo · 1 = riesgo sin solución · 0 = "nada puede salir mal"
ByteBot (2)
  2 = consejo + decisión + porqué · 1 = solo copió el consejo · 0 = no consultó
Creatividad (1)
  1 = nombre y dibujo descritos con cariño

NIVEL CONSTRUCTOR
No da puntos extra en la rúbrica, porque no queremos castigar a quien no tiene dispositivo o tiempo. Reconócelo en el comentario: "¡Además lo construiste! Eso ya es de nivel Principiante."

IDEAS DELICADAS
Si un niño propone una IA que decide cosas importantes sobre personas (quién es buen alumno, quién miente) o que usa fotos de personas: no la rechaces. Úsala para enseñar: pídele en el comentario que piense en el sesgo y la privacidad, y que ajuste la idea para usar objetos o dibujos.

PARA COMENTAR AL APROBAR
"¡Felicidades, inventor/inventora! Tu IA ____ es una gran idea porque ____. Ya te ganaste tu certificado de Mi Primera IA."

LA INVITACIÓN AL CURSO COMPLETO VA A LA FAMILIA, NO AL NIÑO
Si ByteKids manda la invitación a "IA para Niños (Principiante)", que sea al papá o la mamá: por su cuenta de familia o por el canal que ellos dieron al inscribirse. Venderle directo a un niño no va con nosotros.$txt$,
    'url', 'https://machinelearningforkids.co.uk/?lang=es',
    'resource_type', 'enlace',
    'checklist', jsonb_build_array(
        'Encontré un problema real y escribí a quién ayuda mi IA',
        'Le puse nombre y describí mi dibujo',
        'Escribí mis etiquetas (2 o 3)',
        'Describí al menos 3 ejemplos variados por etiqueta',
        'Expliqué qué podría salir mal y cómo lo evitaría',
        'Le presenté mi idea a ByteBot y conté qué consejo usé o no, y por qué',
        'Entregué mi ficha con todos sus títulos',
        '(Constructor) Construí mi IA en ML4Kids e hice 5 pruebas'))
FROM subjects s
WHERE s.id = c.subject_id AND s.name = 'Mi Primera IA' AND c.title = 'Proyecto final: Mi IA para ayudar';

-- ============================================================================
--  PASO 3 - LAS PREGUNTAS DE LOS QUIZZES
--  Solo se crean las que falten. Ver el encabezado: no se reescriben.
-- ============================================================================

-- Quiz: ¿IA o no IA? · pregunta 1
WITH nueva AS (
  INSERT INTO quiz_questions (content_id, question_text, question_type, points, order_index)
  SELECT c.id, '¿Cuál de estos USA inteligencia artificial?', 'opcion_multiple'::question_type, 1, 1
  FROM content c JOIN subjects s ON s.id = c.subject_id
  WHERE s.name = 'Mi Primera IA' AND c.title = 'Quiz: ¿IA o no IA?'
    AND NOT EXISTS (SELECT 1 FROM quiz_questions x
                    WHERE x.content_id = c.id AND x.order_index = 1)
  RETURNING id
)
INSERT INTO quiz_options (question_id, option_text, is_correct, order_index)
SELECT nueva.id, v.texto, v.correcta, v.pos
FROM nueva, (VALUES
       ('Una calculadora', false, 1),
       ('Un foco que prendes con el apagador', false, 2),
       ('Una app que reconoce qué planta es con una foto', true, 3),
       ('Un reloj despertador', false, 4)) AS v(texto, correcta, pos);

-- Quiz: ¿IA o no IA? · pregunta 2
WITH nueva AS (
  INSERT INTO quiz_questions (content_id, question_text, question_type, points, order_index)
  SELECT c.id, 'La inteligencia artificial aprende viendo muchos ejemplos.', 'verdadero_falso'::question_type, 1, 2
  FROM content c JOIN subjects s ON s.id = c.subject_id
  WHERE s.name = 'Mi Primera IA' AND c.title = 'Quiz: ¿IA o no IA?'
    AND NOT EXISTS (SELECT 1 FROM quiz_questions x
                    WHERE x.content_id = c.id AND x.order_index = 2)
  RETURNING id
)
INSERT INTO quiz_options (question_id, option_text, is_correct, order_index)
SELECT nueva.id, v.texto, v.correcta, v.pos
FROM nueva, (VALUES
       ('Verdadero', true, 1),
       ('Falso', false, 2)) AS v(texto, correcta, pos);

-- Quiz: ¿IA o no IA? · pregunta 3
WITH nueva AS (
  INSERT INTO quiz_questions (content_id, question_text, question_type, points, order_index)
  SELECT c.id, 'Quick, Draw! adivinó tu dibujo porque...', 'opcion_multiple'::question_type, 1, 3
  FROM content c JOIN subjects s ON s.id = c.subject_id
  WHERE s.name = 'Mi Primera IA' AND c.title = 'Quiz: ¿IA o no IA?'
    AND NOT EXISTS (SELECT 1 FROM quiz_questions x
                    WHERE x.content_id = c.id AND x.order_index = 3)
  RETURNING id
)
INSERT INTO quiz_options (question_id, option_text, is_correct, order_index)
SELECT nueva.id, v.texto, v.correcta, v.pos
FROM nueva, (VALUES
       ('Una persona escondida estaba viendo tu dibujo', false, 1),
       ('Vio millones de dibujos de otras personas y aprendió cómo se ve cada cosa', true, 2),
       ('Tiene guardados todos los dibujos del mundo y buscó uno igualito al tuyo', false, 3),
       ('Adivinó por pura suerte', false, 4)) AS v(texto, correcta, pos);

-- Quiz: ¿IA o no IA? · pregunta 4
WITH nueva AS (
  INSERT INTO quiz_questions (content_id, question_text, question_type, points, order_index)
  SELECT c.id, '¿Cuál es la diferencia entre un programa normal y una IA?', 'opcion_multiple'::question_type, 1, 4
  FROM content c JOIN subjects s ON s.id = c.subject_id
  WHERE s.name = 'Mi Primera IA' AND c.title = 'Quiz: ¿IA o no IA?'
    AND NOT EXISTS (SELECT 1 FROM quiz_questions x
                    WHERE x.content_id = c.id AND x.order_index = 4)
  RETURNING id
)
INSERT INTO quiz_options (question_id, option_text, is_correct, order_index)
SELECT nueva.id, v.texto, v.correcta, v.pos
FROM nueva, (VALUES
       ('La IA es más cara', false, 1),
       ('El programa normal sigue instrucciones fijas; la IA aprende de ejemplos', true, 2),
       ('La IA siempre tiene la razón', false, 3),
       ('No hay ninguna diferencia', false, 4)) AS v(texto, correcta, pos);

-- Quiz: ¿IA o no IA? · pregunta 5
WITH nueva AS (
  INSERT INTO quiz_questions (content_id, question_text, question_type, points, order_index)
  SELECT c.id, 'Si una IA contesta algo muy segura, seguro es cierto.', 'verdadero_falso'::question_type, 1, 5
  FROM content c JOIN subjects s ON s.id = c.subject_id
  WHERE s.name = 'Mi Primera IA' AND c.title = 'Quiz: ¿IA o no IA?'
    AND NOT EXISTS (SELECT 1 FROM quiz_questions x
                    WHERE x.content_id = c.id AND x.order_index = 5)
  RETURNING id
)
INSERT INTO quiz_options (question_id, option_text, is_correct, order_index)
SELECT nueva.id, v.texto, v.correcta, v.pos
FROM nueva, (VALUES
       ('Verdadero', false, 1),
       ('Falso', true, 2)) AS v(texto, correcta, pos);

-- Quiz: ¿IA o no IA? · pregunta 6
WITH nueva AS (
  INSERT INTO quiz_questions (content_id, question_text, question_type, points, order_index)
  SELECT c.id, 'YouTube te recomienda videos de dinosaurios porque...', 'opcion_multiple'::question_type, 1, 6
  FROM content c JOIN subjects s ON s.id = c.subject_id
  WHERE s.name = 'Mi Primera IA' AND c.title = 'Quiz: ¿IA o no IA?'
    AND NOT EXISTS (SELECT 1 FROM quiz_questions x
                    WHERE x.content_id = c.id AND x.order_index = 6)
  RETURNING id
)
INSERT INTO quiz_options (question_id, option_text, is_correct, order_index)
SELECT nueva.id, v.texto, v.correcta, v.pos
FROM nueva, (VALUES
       ('Adivinó por suerte', false, 1),
       ('Aprendió de los videos que ya viste', true, 2),
       ('Una persona de YouTube los escoge para ti', false, 3),
       ('Todos los niños ven los mismos videos', false, 4)) AS v(texto, correcta, pos);

-- Quiz: ¿IA o no IA? · pregunta 7
WITH nueva AS (
  INSERT INTO quiz_questions (content_id, question_text, question_type, points, order_index)
  SELECT c.id, '¿Cuál de estos NO es inteligencia artificial?', 'opcion_multiple'::question_type, 1, 7
  FROM content c JOIN subjects s ON s.id = c.subject_id
  WHERE s.name = 'Mi Primera IA' AND c.title = 'Quiz: ¿IA o no IA?'
    AND NOT EXISTS (SELECT 1 FROM quiz_questions x
                    WHERE x.content_id = c.id AND x.order_index = 7)
  RETURNING id
)
INSERT INTO quiz_options (question_id, option_text, is_correct, order_index)
SELECT nueva.id, v.texto, v.correcta, v.pos
FROM nueva, (VALUES
       ('Un filtro que te pone orejas de perrito', false, 1),
       ('Un teclado que adivina tu siguiente palabra', false, 2),
       ('Un semáforo que cambia de color cada 60 segundos', true, 3),
       ('Un asistente al que le hablas con tu voz', false, 4)) AS v(texto, correcta, pos);

-- Quiz: ¿IA o no IA? · pregunta 8
WITH nueva AS (
  INSERT INTO quiz_questions (content_id, question_text, question_type, points, order_index)
  SELECT c.id, 'Una IA te contesta algo importante. ¿Qué es lo MÁS inteligente que puedes hacer?', 'opcion_multiple'::question_type, 1, 8
  FROM content c JOIN subjects s ON s.id = c.subject_id
  WHERE s.name = 'Mi Primera IA' AND c.title = 'Quiz: ¿IA o no IA?'
    AND NOT EXISTS (SELECT 1 FROM quiz_questions x
                    WHERE x.content_id = c.id AND x.order_index = 8)
  RETURNING id
)
INSERT INTO quiz_options (question_id, option_text, is_correct, order_index)
SELECT nueva.id, v.texto, v.correcta, v.pos
FROM nueva, (VALUES
       ('Creerle todo', false, 1),
       ('No volver a usar nunca una IA', false, 2),
       ('Revisarlo en otro lugar o preguntarle a un adulto', true, 3),
       ('Preguntarle lo mismo hasta que te conteste lo que quieres', false, 4)) AS v(texto, correcta, pos);

-- Quiz: ¿Cómo aprende una máquina? · pregunta 1
WITH nueva AS (
  INSERT INTO quiz_questions (content_id, question_text, question_type, points, order_index)
  SELECT c.id, 'Los ejemplos que le das a una IA para que aprenda se llaman...', 'opcion_multiple'::question_type, 1, 1
  FROM content c JOIN subjects s ON s.id = c.subject_id
  WHERE s.name = 'Mi Primera IA' AND c.title = 'Quiz: ¿Cómo aprende una máquina?'
    AND NOT EXISTS (SELECT 1 FROM quiz_questions x
                    WHERE x.content_id = c.id AND x.order_index = 1)
  RETURNING id
)
INSERT INTO quiz_options (question_id, option_text, is_correct, order_index)
SELECT nueva.id, v.texto, v.correcta, v.pos
FROM nueva, (VALUES
       ('Etiquetas', false, 1),
       ('Datos', true, 2),
       ('Botones', false, 3),
       ('Reglas', false, 4)) AS v(texto, correcta, pos);

-- Quiz: ¿Cómo aprende una máquina? · pregunta 2
WITH nueva AS (
  INSERT INTO quiz_questions (content_id, question_text, question_type, points, order_index)
  SELECT c.id, 'En tu Detector de Caritas, "feliz" y "triste" eran...', 'opcion_multiple'::question_type, 1, 2
  FROM content c JOIN subjects s ON s.id = c.subject_id
  WHERE s.name = 'Mi Primera IA' AND c.title = 'Quiz: ¿Cómo aprende una máquina?'
    AND NOT EXISTS (SELECT 1 FROM quiz_questions x
                    WHERE x.content_id = c.id AND x.order_index = 2)
  RETURNING id
)
INSERT INTO quiz_options (question_id, option_text, is_correct, order_index)
SELECT nueva.id, v.texto, v.correcta, v.pos
FROM nueva, (VALUES
       ('Los datos', false, 1),
       ('Las etiquetas', true, 2),
       ('Los colores', false, 3),
       ('Los errores', false, 4)) AS v(texto, correcta, pos);

-- Quiz: ¿Cómo aprende una máquina? · pregunta 3
WITH nueva AS (
  INSERT INTO quiz_questions (content_id, question_text, question_type, points, order_index)
  SELECT c.id, 'Cuando le das a "Entrenar nuevo modelo", la IA...', 'opcion_multiple'::question_type, 1, 3
  FROM content c JOIN subjects s ON s.id = c.subject_id
  WHERE s.name = 'Mi Primera IA' AND c.title = 'Quiz: ¿Cómo aprende una máquina?'
    AND NOT EXISTS (SELECT 1 FROM quiz_questions x
                    WHERE x.content_id = c.id AND x.order_index = 3)
  RETURNING id
)
INSERT INTO quiz_options (question_id, option_text, is_correct, order_index)
SELECT nueva.id, v.texto, v.correcta, v.pos
FROM nueva, (VALUES
       ('Se conecta a internet para copiar la respuesta', false, 1),
       ('Busca el patrón en todos tus ejemplos', true, 2),
       ('Borra tus ejemplos', false, 3),
       ('Se apaga para descansar', false, 4)) AS v(texto, correcta, pos);

-- Quiz: ¿Cómo aprende una máquina? · pregunta 4
WITH nueva AS (
  INSERT INTO quiz_questions (content_id, question_text, question_type, points, order_index)
  SELECT c.id, 'Con 2 ejemplos, una IA aprende igual de bien que con 50.', 'verdadero_falso'::question_type, 1, 4
  FROM content c JOIN subjects s ON s.id = c.subject_id
  WHERE s.name = 'Mi Primera IA' AND c.title = 'Quiz: ¿Cómo aprende una máquina?'
    AND NOT EXISTS (SELECT 1 FROM quiz_questions x
                    WHERE x.content_id = c.id AND x.order_index = 4)
  RETURNING id
)
INSERT INTO quiz_options (question_id, option_text, is_correct, order_index)
SELECT nueva.id, v.texto, v.correcta, v.pos
FROM nueva, (VALUES
       ('Verdadero', false, 1),
       ('Falso', true, 2)) AS v(texto, correcta, pos);

-- Quiz: ¿Cómo aprende una máquina? · pregunta 5
WITH nueva AS (
  INSERT INTO quiz_questions (content_id, question_text, question_type, points, order_index)
  SELECT c.id, 'Entrenaste tu IA solo con caritas felices AZULES. Si le enseñas una carita feliz ROJA...', 'opcion_multiple'::question_type, 1, 5
  FROM content c JOIN subjects s ON s.id = c.subject_id
  WHERE s.name = 'Mi Primera IA' AND c.title = 'Quiz: ¿Cómo aprende una máquina?'
    AND NOT EXISTS (SELECT 1 FROM quiz_questions x
                    WHERE x.content_id = c.id AND x.order_index = 5)
  RETURNING id
)
INSERT INTO quiz_options (question_id, option_text, is_correct, order_index)
SELECT nueva.id, v.texto, v.correcta, v.pos
FROM nueva, (VALUES
       ('Siempre le atina', false, 1),
       ('Se puede equivocar, porque tal vez aprendió el color y no la sonrisa', true, 2),
       ('Se descompone', false, 3),
       ('Cambia la carita a azul', false, 4)) AS v(texto, correcta, pos);

-- Quiz: ¿Cómo aprende una máquina? · pregunta 6
WITH nueva AS (
  INSERT INTO quiz_questions (content_id, question_text, question_type, points, order_index)
  SELECT c.id, '¿Qué ejemplos ayudan MÁS a que una IA aprenda bien?', 'opcion_multiple'::question_type, 1, 6
  FROM content c JOIN subjects s ON s.id = c.subject_id
  WHERE s.name = 'Mi Primera IA' AND c.title = 'Quiz: ¿Cómo aprende una máquina?'
    AND NOT EXISTS (SELECT 1 FROM quiz_questions x
                    WHERE x.content_id = c.id AND x.order_index = 6)
  RETURNING id
)
INSERT INTO quiz_options (question_id, option_text, is_correct, order_index)
SELECT nueva.id, v.texto, v.correcta, v.pos
FROM nueva, (VALUES
       ('Muchos ejemplos casi idénticos', false, 1),
       ('Pocos ejemplos, pero muy bonitos', false, 2),
       ('Muchos ejemplos variados: distintos colores, tamaños y formas', true, 3),
       ('Un solo ejemplo perfecto', false, 4)) AS v(texto, correcta, pos);

-- Quiz: ¿Cómo aprende una máquina? · pregunta 7
WITH nueva AS (
  INSERT INTO quiz_questions (content_id, question_text, question_type, points, order_index)
  SELECT c.id, 'La IA ya aprendió y le enseñas algo nuevo para ver qué contesta. Eso se llama...', 'opcion_multiple'::question_type, 1, 7
  FROM content c JOIN subjects s ON s.id = c.subject_id
  WHERE s.name = 'Mi Primera IA' AND c.title = 'Quiz: ¿Cómo aprende una máquina?'
    AND NOT EXISTS (SELECT 1 FROM quiz_questions x
                    WHERE x.content_id = c.id AND x.order_index = 7)
  RETURNING id
)
INSERT INTO quiz_options (question_id, option_text, is_correct, order_index)
SELECT nueva.id, v.texto, v.correcta, v.pos
FROM nueva, (VALUES
       ('Etiquetar', false, 1),
       ('Predecir', true, 2),
       ('Dibujar', false, 3),
       ('Copiar', false, 4)) AS v(texto, correcta, pos);

-- Quiz: ¿Cómo aprende una máquina? · pregunta 8
WITH nueva AS (
  INSERT INTO quiz_questions (content_id, question_text, question_type, points, order_index)
  SELECT c.id, 'Una IA reconoce mejor las cosas que se parecen a los ejemplos con los que la entrenaron.', 'verdadero_falso'::question_type, 1, 8
  FROM content c JOIN subjects s ON s.id = c.subject_id
  WHERE s.name = 'Mi Primera IA' AND c.title = 'Quiz: ¿Cómo aprende una máquina?'
    AND NOT EXISTS (SELECT 1 FROM quiz_questions x
                    WHERE x.content_id = c.id AND x.order_index = 8)
  RETURNING id
)
INSERT INTO quiz_options (question_id, option_text, is_correct, order_index)
SELECT nueva.id, v.texto, v.correcta, v.pos
FROM nueva, (VALUES
       ('Verdadero', true, 1),
       ('Falso', false, 2)) AS v(texto, correcta, pos);

COMMIT;


-- ============================================================================
--  PASO 4 - VERIFICACION
--  Deben salir 9 piezas en orden, 500 XP en total, las 9 con guia del maestro,
--  y 8 preguntas en cada uno de los 2 quizzes.
-- ============================================================================

SELECT c.order_index AS pieza, c.type, c.title, c.xp_reward AS xp,
       c.estimated_minutes AS min,
       (c.content_body ? 'teacher_notes') AS con_guia,
       c.content_body ->> 'url' AS enlace,
       (SELECT count(*) FROM quiz_questions q WHERE q.content_id = c.id) AS preguntas
FROM content c JOIN subjects s ON s.id = c.subject_id
WHERE s.name = 'Mi Primera IA'
ORDER BY c.order_index;

SELECT sum(c.xp_reward) AS xp_total_debe_ser_500
FROM content c JOIN subjects s ON s.id = c.subject_id
WHERE s.name = 'Mi Primera IA';

-- Cada pregunta debe tener exactamente UNA opcion correcta.
SELECT c.title, q.order_index, count(*) FILTER (WHERE o.is_correct) AS correctas
FROM quiz_questions q
JOIN content c  ON c.id = q.content_id
JOIN subjects s ON s.id = c.subject_id
JOIN quiz_options o ON o.question_id = q.id
WHERE s.name = 'Mi Primera IA'
GROUP BY c.title, q.order_index
HAVING count(*) FILTER (WHERE o.is_correct) <> 1;
-- (Esta ultima debe salir VACIA.)

-- ============================================================================
--  PASO 5 - PARA QUE LOS NINOS LO VEAN
--  Desde la plataforma, no desde aqui:
--    1. Coordinacion > Salones > crea "Mi Primera IA - Grupo 1".
--    2. Asignale la materia "Mi Primera IA" y a su maestro (o a ti).
--    3. Crea las cuentas de los ninos e inscribelos en ese salon.
-- ============================================================================
