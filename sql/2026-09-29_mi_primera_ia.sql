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

Soy ByteBot y voy a acompañarte en todo el curso. En 9 pasos vas a descubrir qué es la inteligencia artificial, vas a entrenar una con tus propias manos y al final ganas tu certificado de ByteKids Academy. 🎓

HOY VAS A PODER...
✔ Decir con tus palabras qué es la inteligencia artificial.
✔ Encontrar la IA que usas todos los días.

PASO 1 · JUEGA CONTRA UNA IA (5 min)
1. Abre el enlace con el botón 🚀 que está al final de este paso. Se llama Quick, Draw!
2. Dale a "¡Vamos a dibujar!" y dibuja lo que te pida.
3. Mientras dibujas, la computadora intenta ADIVINAR qué es. ¡Rápido!
4. 🏁 Juega UNA ronda: son 6 dibujos.

Se abre en otra pestaña. Esta de ByteKids se queda aquí esperándote.

🔙 REGRESA A BYTEKIDS
¿Ya viste tus 6 dibujos? Vuelve aquí: ahora vas a descubrir CÓMO los adivinó.

PASO 2 · EL TRUCO DE LA IA
¿Cómo supo que tu garabato era un gato? 🤔 Nadie estaba viendo tu dibujo...

Esa computadora vio MILLONES de dibujos de personas de todo el mundo. Vio tantos gatos que aprendió cómo se dibuja un gato.
Nadie le explicó "un gato tiene bigotes". Lo descubrió sola, VIENDO EJEMPLOS.

👉 Eso es la inteligencia artificial: un programa que APRENDE DE EJEMPLOS.

PASO 3 · ¿PROGRAMA NORMAL O IA?
📋 Programa normal: hace siempre lo mismo. Una calculadora nunca aprende nada nuevo.
🧠 Inteligencia artificial: aprende de ejemplos y encuentra el patrón ella sola.

Y la IA está escondida en tu día:
▸ YouTube te recomienda videos → aprendió de lo que ya viste.
▸ El teclado adivina tu siguiente palabra → aprendió de millones de mensajes.
▸ Un filtro te pone orejas de perrito → encontró dónde están tus ojos y tu nariz.

💬 ByteBot dice: "Yo también soy una IA: aprendí leyendo muchísimos textos. Pero aprender de ejemplos no es lo mismo que saberlo todo... eso lo vas a comprobar tú en la siguiente misión."

LO QUE TE LLEVAS HOY
La IA no es magia: es un programa que aprendió viendo muchos, muchos ejemplos.

⭐ RETO EXTRA (si quieres más)
▸ Juega otra ronda. ¿Qué dibujo adivinó más rápido? ¿Cuál le costó más? ¿Por qué crees?
▸ Busca en tu casa una cosa que creas que tiene IA. Más adelante en el curso la vas a necesitar.
No es obligatorio: es para los que se quedaron con ganas.

¿QUÉ SIGUE?
Vas a entrevistar a una inteligencia artificial de verdad. A mí. 😉$txt$,
    'teacher_notes', $txt$OBJETIVO
Que el niño distinga "programa que sigue instrucciones" de "programa que aprende de ejemplos". Todo el curso se para sobre esa idea.

TIEMPO: 15 a 20 minutos. ES MATERIAL: se consulta, no se califica.

PARA 8 A 12 AÑOS
El curso se escribió para que un niño de 8 lo termine solo: frases cortas, un paso a la vez y poco que escribir. Los de 10 a 12 encuentran al final un ⭐ Reto extra opcional. El reto nunca suma ni resta calificación.

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
       'facil'::difficulty_level, 20, '{}'::jsonb, 2, true, true
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
    estimated_minutes = 20,
    order_index       = 2,
    content_body      = jsonb_build_object(
    'instructions', $txt$Hoy eres PERIODISTA 🎤 y vas a entrevistar a una inteligencia artificial: a mí, ByteBot.

Tu misión: descubrir si ByteBot lo sabe TODO.

HOY VAS A PODER...
✔ Hacerle buenas preguntas a una IA.
✔ Descubrir qué hace una IA cuando no sabe algo.

PASO 1 · ADIVINA PRIMERO 🤔
¿Tú crees que ByteBot sabe todo? Piénsalo y escribe SÍ o NO en tu respuesta.
No hay respuesta mala: los científicos siempre adivinan antes de probar.

PASO 2 · ENTREVÍSTAME
Dale al botón 🤖 de ByteBot: el chat se abre aquí al lado. Pregúntame:
1. ¿Qué eres y cómo aprendiste?
2. ¿Qué cosas NO puedes hacer?
3. Una pregunta tuya, de lo que quieras: dinosaurios, el espacio, futbol...

PASO 3 · LA PREGUNTA TRAMPA 🕵️
Ahora ponme a prueba. Pregúntame algo que YO no puedo saber:
"¿Qué desayuné hoy?" o "¿De qué color es mi cuarto?"
Fíjate bien: ¿te dije que no sabía... o inventé algo?

PASO 4 · TU REPORTAJE (esto es lo que entregas)
Cortito y con tus palabras:
1. Antes de la entrevista, ¿creías que ByteBot sabía todo? Sí o No.
2. ¿Qué pasó con tu pregunta trampa?
3. Ahora: ¿ByteBot sabe todo? ¿Qué vas a hacer cuando una IA te diga algo importante?

💬 ByteBot dice: "Te cuento un secreto: yo aprendí de textos. No veo tu casa ni sé qué pasó hoy. Por eso a veces no sé... ¡y a veces invento aunque suene muy seguro! Revisa siempre."

REGLA DE ORO 🔒
No le digas a una IA tu nombre completo, dónde vives, tu escuela ni tu teléfono. Para esta entrevista no necesitas ninguno.

LO QUE TE LLEVAS HOY
Una IA puede sonar muy segura y aun así equivocarse. Por eso siempre revisas.

⭐ RETO EXTRA (para detectives avanzados)
▸ Pregúntame: "¿Cuántas patas hay entre tres gallinas y dos perros?" Haz tú la cuenta en una hoja. ¿Le atiné?
▸ Pregúntame quién ganó un partido de ayer. ¿Qué te contesté?
▸ Agrega a tu reportaje cuál fue mi mejor respuesta y por qué.
No es obligatorio y no cambia tu calificación.

¿QUÉ SIGUE?
Un quiz rápido para ver si ya tienes ojo de detective de IA.$txt$,
    'teacher_notes', $txt$OBJETIVO
Que el niño compruebe por sí mismo que una IA puede no saber, inventar o equivocarse, y que saque la conclusión de que hay que revisar. No se la decimos: la descubre.

TIEMPO: 15 a 20 minutos.

CÓMO CALIFICAR (sobre 10, se aprueba con 7)
- Respondió su predicción (Sí o No): 1
- Contó qué pasó con su pregunta trampa: 4
- Su conclusión menciona revisar, comprobar o preguntarle a un adulto: 5
El ⭐ Reto extra NO suma puntos. Si lo hizo, felicítalo en el comentario.

QUÉ ESPERAR SEGÚN LA EDAD
Un niño de 8 puede contestar con una oración por pregunta, y está perfecto. No le pidas más de lo que pide la actividad. Un niño de 12 probablemente escriba más y haga el reto: no lo compares con el de 8.

QUÉ HACER SI...
- Copió respuestas enteras de ByteBot: "Pedir correcciones" con: "¡Qué buena entrevista! Ahora cuéntamelo con TUS palabras, como se lo contarías a un amigo."
- Su conclusión es "ByteBot sabe todo": no lo repruebes. "Pedir correcciones" con: "¿Y qué pasó con tu pregunta trampa? Vuelve a leer lo que te contestó y cuéntame si cambias de opinión."
- ByteBot contestó bien la pregunta trampa (dijo "no puedo saberlo"): ¡también vale! La conclusión correcta es "una buena IA reconoce lo que no sabe, pero no todas lo hacen".

CUIDADO
Si en la entrega aparece un dato personal (dirección, escuela, nombre completo), recuérdaselo con cariño en el comentario. No es para regañar: es la regla de oro del curso.

PARA COMENTAR (ideas)
- "Hiciste una pregunta trampa buenísima. Eso es pensar como científico."
- "Me encantó tu conclusión: revisar es la súper habilidad de quien usa IA."$txt$)
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
    'instructions', $txt$¡Hora de probar tu ojo de detective! 🔍

La pista para todas las preguntas:
👉 ¿Aprende de ejemplos, o solo sigue instrucciones?

Son 8 preguntas. Pasas con 6. Y si no sale a la primera, ¡lo intentas otra vez! Equivocarse también es aprender.$txt$,
    'teacher_notes', $txt$OBJETIVO
Comprobar las dos ideas de las piezas 1 y 2: la IA aprende de ejemplos, y puede equivocarse.

SE CALIFICA SOLO. Aprueba con 70 (6 de 8). Se puede reintentar. Las opciones se revuelven solas, salvo las de verdadero/falso.

QUÉ IDEA EQUIVOCADA ATACA CADA DISTRACTOR
- P3, "tiene todos los dibujos guardados y buscó uno igual": confundir aprender con memorizar. Es el error más interesante del quiz: si varios niños caen, vale la pena mandarles una explicación por Mensajes.
- P5: "si suena seguro, es cierto". Conecta con la pregunta trampa de la misión 1.
- P7, "el semáforo que cambia cada 60 segundos": es un temporizador, sigue una instrucción fija.

SI UN NIÑO REPRUEBA DOS VECES
Escríbele antes del tercer intento: "Vuelve a leer el PASO 3 de la actividad 1, ¿programa normal o IA? Ahí está la llave de casi todas."$txt$)
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
    'instructions', $txt$Hoy descubres el secreto de cómo aprende una IA. Y además... ¡entrenas una! 🐟

HOY VAS A PODER...
✔ Explicar qué son los DATOS, las ETIQUETAS y el ENTRENAMIENTO.
✔ Entrenar una IA con tus propias manos.

PASO 1 · EL JUEGO DE LAS TARJETAS (sin computadora, 5 min)
1. Haz 6 tarjetitas y dibuja una cosa en cada una: 3 que se comen (manzana, pan, pizza) y 3 que no (zapato, lápiz, pelota).
2. Sepáralas en dos montones: "se come" y "no se come".

¡Listo! Acabas de hacer lo mismo que hace una persona cuando le enseña a una IA.

PASO 2 · LAS PALABRAS MÁGICAS
📦 DATOS: los ejemplos que le enseñas. Tus tarjetas son tus datos.
🏷️ ETIQUETAS: el nombre de cada grupo. "Se come" y "no se come" son tus etiquetas.
🏋️ ENTRENAR: la máquina mira los ejemplos y busca el patrón. Como estudiar para un examen.
🔮 PREDECIR: le enseñas algo NUEVO y ella adivina en qué grupo va.

PASO 3 · ENTRENA UNA IA DE VERDAD (15 min)
1. Abre el enlace con el botón 🚀 que está al final de este paso. Si sale en inglés, busca el idioma hasta abajo de la página y escoge "Español".
2. Van a pasar peces y basura. Tú le dices a la IA cuál es "pez" y cuál "no es pez". ¡Estás ETIQUETANDO DATOS!
3. Dale a continuar y mira cómo TU IA limpia el océano sola.
4. 🏁 TU META: cuando tu IA limpie el océano por primera vez, ¡lo lograste!

⏰ Son 15 minutos. Pon una alarma o pídele a alguien de tu casa que te avise.
Se abre en otra pestaña. Esta de ByteKids se queda aquí esperándote.

🔙 REGRESA A BYTEKIDS
Cuando tu IA limpie el océano, vuelve aquí. Te esperan una pregunta de detective, ByteBot y tus 25 XP: se ganan aquí, con el botón "Ya lo vi". En el océano no cuentan.

PAUSA PARA PENSAR 🤔 (ya de regreso)
¿Tu IA sacó algún pez del agua por error? ¿Por qué crees que pasó?

💬 ByteBot dice: "¡Ya volviste! Dale al botón de ByteBot y cuéntame qué hizo tu IA. Lo investigamos juntos."

LO QUE TE LLEVAS HOY
Una IA es tan buena como sus ejemplos.
Pocos ejemplos → aprende poco.
Ejemplos variados → aprende mejor.

⭐ RETO EXTRA (si quieres más)
▸ Ya que le diste "Ya lo vi", regresa a IA para los Océanos y sigue con las siguientes partes. Más adelante la IA tiene que aprender cosas que no son tan fáciles de decidir. 😏
▸ Si a la IA de tus tarjetas le enseñas una galleta, ¿en qué montón la pondría? ¿Y una piedra que parece pan?
No es obligatorio: es para los que se quedaron con ganas.

¿QUÉ SIGUE?
Vas a entrenar TU PROPIA IA desde cero, con dibujos tuyos. Ve preparando tus colores. 🖍️$txt$,
    'teacher_notes', $txt$OBJETIVO
Vocabulario base del curso: datos, etiquetas, entrenar, predecir. La misión 2 y el quiz 2 lo dan por sabido.

TIEMPO: 20 a 25 minutos. ES MATERIAL: no se califica.

POR QUÉ EMPEZAR SIN COMPUTADORA
La actividad de tarjetas hace que el niño SEA la máquina antes de usarla. Cuando después etiqueta peces, ya sabe qué está haciendo y por qué. Es la diferencia entre seguir pasos y entender.

IA PARA LOS OCÉANOS (Code.org)
Es gratis, no pide cuenta y tiene videos en español. Más adelante el juego pide clasificar peces por cosas de opinión, y ahí asoma el sesgo que vemos en la pieza 8. Por eso seguir jugando está en el ⭐ Reto extra.

LA META DE REGRESO
Un alumno se quedó jugando el océano una y otra vez sin volver. Por eso la actividad le da una meta clara ("cuando tu IA limpie el océano por primera vez") y una razón para regresar. Si en clase ves a alguien que no vuelve, recuérdale que su XP se gana en ByteKids.

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
       'medio'::difficulty_level, 35, '{}'::jsonb, 5, true, true
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
    estimated_minutes = 35,
    order_index       = 5,
    content_body      = jsonb_build_object(
    'instructions', $txt$Esta es LA misión del curso. Hoy creas una inteligencia artificial de verdad, con tus propias manos. 🧠✨

Tu IA va a mirar una carita dibujada y va a decir si está FELIZ 😊 o TRISTE 😢.

HOY VAS A PODER...
✔ Entrenar una IA con tus propios dibujos.
✔ Descubrir un secreto que tienen TODAS las IAs.

PASO 1 · DIBUJA TUS DATOS (10 min)
Necesitas: hojas, un color AZUL y uno ROJO.
1. Haz 12 tarjetas del tamaño de tu mano.
2. En 6 tarjetas dibuja caritas FELICES 😊, todas en AZUL.
3. En las otras 6 dibuja caritas TRISTES 😢, todas en ROJO.

¿Por qué azul y rojo? Es parte del experimento. Confía en mí. 😉

⚠️ Tu IA solo va a ver DIBUJOS. Nunca le enseñes tu cara ni la de otra persona.

PASO 2 · ARMA TU PROYECTO (5 min)
Vas a ir y venir: aquí lees el paso, allá lo haces. Esta pestaña de ByteKids no la cierres.
1. Abre el enlace con el botón 🚀 que está al final de este paso.
2. Escoge "Pruébalo ahora". Si te pide usuario, pídeselo a tu maestra en Mensajes.
3. Dale a "Añadir un nuevo proyecto". Nombre: Detector de caritas. Tipo: IMÁGENES. Ábrelo.
4. Entra a "Entrenar" y agrega dos etiquetas: feliz y triste.

PASO 3 · ENSÉÑALE (5 min)
1. En "feliz", usa el botón de la cámara y enséñale tus 6 caritas AZULES, una por una.
2. En "triste", enséñale tus 6 caritas ROJAS.
3. Ve a "Aprender & Probar" y dale a "Entrenar nuevo modelo". Espera: ¡eso es ENTRENAR!

PASO 4 · LA PRUEBA SECRETA 🕵️ (5 min)
Dibuja 2 caritas NUEVAS, con los colores al revés:
🔴 Una carita FELIZ en ROJO.
🔵 Una carita TRISTE en AZUL.
Antes de enseñárselas, adivina: ¿qué va a contestar tu IA? Luego pruébalas y anota qué dijo.

⚠️ Esta forma de entrar no guarda tu proyecto para siempre. Anota lo que contesta tu IA.

🔙 REGRESA A BYTEKIDS
Ya hiciste tu prueba secreta. Vuelve a esta pestaña: aquí escribes tu entrega y ganas tus 75 XP.

PASO 5 · TU ENTREGA
Cortito y con tus palabras:
1. ¿Qué contestó tu IA con la carita FELIZ ROJA? ¿Y con la TRISTE AZUL?
2. ¿Por qué crees que pasó eso? ¿Se fijó en la boca... o en otra cosa?

EL SECRETO QUE DESCUBRISTE
Léelo cuando ya hayas escrito tu entrega. 🤫
Tus caritas felices eran todas azules y las tristes, rojas. Para la IA era más fácil fijarse en el COLOR que en la boca. Por eso pensó: "rojo = triste".

¡Eso tiene nombre! Se llama SESGO: cuando una IA aprende algo chueco porque sus ejemplos no eran variados. Le pasa a las IAs de verdad, y ahora ya sabes por qué.

(¿Tu IA le atinó a todo? ¡Qué bien! Entonces sí se fijó en la boca. Pero ya sabes qué pudo haber salido mal.)

💬 ByteBot dice: "Si te atoras en algún paso, dale al botón de ByteBot y cuéntame en qué parte vas. Y si tu IA hizo algo rarísimo... ¡cuéntamelo también!"

LO QUE TE LLEVAS HOY
Una IA aprende EXACTAMENTE lo que le enseñas... hasta lo que no querías enseñarle.

⭐ RETO EXTRA (para científicos valientes)
▸ ARRÉGLALA: dibuja 3 caritas felices en ROJO y 3 tristes en AZUL, agrégalas a sus etiquetas y vuelve a darle a "Entrenar nuevo modelo". Repite tu prueba secreta. ¿Mejoró?
▸ Agrega a tu entrega qué hiciste para arreglarla y si funcionó.
▸ Explica en una frase qué es el SESGO.
No es obligatorio y no cambia tu calificación.

¿QUÉ SIGUE?
Te conviertes en detective de IA... ¡en tu propia casa! 🔍$txt$,
    'teacher_notes', $txt$OBJETIVO
Que el niño entrene un modelo real y DESCUBRA el sesgo por sí mismo. Es la pieza más importante del curso: si la vive bien, lo demás se entiende solo.

TIEMPO: 30 a 35 minutos. Dibujar las 12 tarjetas es lo que más tarda.

EL DISEÑO: UN ERROR PROVOCADO A PROPÓSITO
Felices en azul y tristes en rojo es una trampa: con pocos ejemplos por lado, el color es el patrón más fácil de aprender, y la IA casi siempre se agarra de él. La prueba secreta (feliz roja, triste azul) lo destapa. El niño se equivoca en su predicción, busca la causa, y eso se queda mucho más que una explicación.

Son 6 tarjetas por etiqueta y no 10: para un niño de 8, dibujar 20 caritas antes de empezar se come la misión. Con 6 el sesgo de color aparece igual o más.

Si la IA de un niño le atinó a todo (puede pasar si dibujó bocas muy grandes y marcadas), NO es un fracaso: evalúa el razonamiento, no el resultado.

CÓMO CALIFICAR (sobre 10, se aprueba con 7)
- Dice qué contestó la IA con las dos caritas de la prueba secreta: 4
- Explica por qué cree que pasó (el color contra la boca), o qué pudo pasar: 6
El ⭐ Reto extra (arreglarla y definir sesgo) NO suma puntos. Si lo hizo, felicítalo: es justo lo que hacen los científicos de IA.

PROBLEMAS TÉCNICOS PROBABLES
- "No me deja entrar": si "Pruébalo ahora" no aparece, créale cuenta de ML4Kids (como en el Principiante) y mándale usuario y contraseña por Mensajes.
- "La cámara no prende": la tablet debe dar permiso de cámara al navegador. En iPad: Ajustes → Safari → Cámara → Permitir.
- "Se borró mi proyecto": esa forma de entrar no guarda para siempre. Lo que se califica es la entrega escrita; no hace falta repetir el entrenamiento.
- "No me deja entrenar": ML4Kids pide un mínimo de ejemplos por etiqueta. Si marca que faltan, que agregue una o dos caritas más del mismo color.

CUIDADO CON LA PRIVACIDAD
Si en una entrega se ve que el niño usó su cara en vez de dibujos, coméntaselo con cariño: la instrucción lo prohíbe a propósito.

PARA COMENTAR (ideas)
- "¡Descubriste el sesgo tú solo! Los científicos de IA pasan mucho tiempo buscando justo eso."
- "Tu predicción falló y tú encontraste por qué. Así se aprende de verdad."$txt$,
    'url', 'https://machinelearningforkids.co.uk/?lang=es',
    'resource_type', 'enlace')
FROM subjects s
WHERE s.id = c.subject_id AND s.name = 'Mi Primera IA' AND c.title = 'Misión 2: Entrena tu Detector de Caritas';

-- ─── Pieza 6 · tarea · Investigación: La IA en mi casa ───
INSERT INTO content (title, description, type, subject_id, created_by, xp_reward,
                     difficulty, estimated_minutes, content_body, order_index,
                     is_published, is_active)
SELECT 'Investigación: La IA en mi casa', 'Conviértete en detective: encuentra la inteligencia artificial escondida en tu casa, entrevista a un adulto de tu familia y descubre con ByteBot cómo funciona una de ellas.', 'tarea'::content_type, mat.id, autor.id, 60,
       'facil'::difficulty_level, 25, '{}'::jsonb, 6, true, true
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
    estimated_minutes = 25,
    order_index       = 6,
    content_body      = jsonb_build_object(
    'instructions', $txt$Hoy eres DETECTIVE DE IA 🔍 y tu escena del crimen es... ¡tu casa!

HOY VAS A PODER...
✔ Reconocer la IA en aparatos y apps de verdad.
✔ Investigar como detective: buscar y preguntar.

PASO 1 · LA BÚSQUEDA (10 min)
Recorre tu casa con un adulto y encuentra 2 cosas que usen IA.

Pistas de detective. Tiene IA si...
▸ Te recomienda cosas.
▸ Reconoce tu voz o tu cara.
▸ Adivina lo que vas a escribir.

Busca aquí:
📺 La tele o la app de videos
📱 El celular
🗣️ Un asistente de voz
🗺️ El mapa que avisa del tráfico

PASO 2 · LA ENTREVISTA (10 min)
Pregúntale a un adulto de tu familia:
1. ¿Qué inteligencia artificial usas en tu trabajo o en tu día?
2. ¿Alguna vez se equivocó o hizo algo raro?
Anota lo que te diga.

PASO 3 · TU ENTREGA
Cortito y con tus palabras:
1. Las 2 cosas con IA que encontraste, y qué hace cada una.
2. Lo que te contestó el adulto.

🔒 REGLA DE ORO: no mandes fotos de tu casa ni escribas tu dirección. No se necesitan.

LO QUE TE LLEVAS HOY
La IA no es cosa del futuro: ya vive en tu casa. Y ahora sabes reconocerla.

⭐ RETO EXTRA (para detectives avanzados)
▸ Encuentra una tercera cosa con IA. ¿De qué ejemplos crees que aprendió?
▸ Pregúntame con el botón de ByteBot: "¿Cómo aprendió [lo que encontraste] a [lo que hace]?" Escríbelo con tus palabras.
▸ Pregunta bonus para el adulto: "¿Cómo crees que va a ser la IA cuando yo sea grande?"
No es obligatorio y no cambia tu calificación.

¿QUÉ SIGUE?
Un quiz para comprobar que ya sabes cómo aprende una máquina.$txt$,
    'teacher_notes', $txt$OBJETIVO
Llevar la IA del curso a la vida real del niño, y meter a la familia en el curso.

ESTO ES MARKETING TAMBIÉN
La entrevista hace que un papá o una mamá vea el curso funcionando y platique de IA con su hijo. Es el momento en que la familia decide si ByteKids vale la pena. Comenta estas entregas con especial cariño.

TIEMPO: 20 a 25 minutos, que pueden repartirse en dos días.

CÓMO CALIFICAR (sobre 10, se aprueba con 7)
- Dos hallazgos válidos, cada uno con lo que hace: 5
- La entrevista, con lo que contestó el adulto: 5
El ⭐ Reto extra NO suma puntos. Si lo hizo, felicítalo.

QUÉ HACER SI...
- Pone algo que no es IA (el microondas, el foco): no lo cuentes, pero explícale por qué con la pista "¿aprende o sigue instrucciones?". Si le queda 1 válido, pide correcciones para que busque otro.
- Copió a ByteBot tal cual en el reto: no pasa nada, no se califica. Puedes comentarle: "Cuéntamelo como se lo explicarías a tu abuelita."
- No tuvo con quién hacer la entrevista: acepta a un maestro, un vecino o un familiar por videollamada.$txt$)
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
    'instructions', $txt$Ya entrenaste tu propia IA. ¡Ahora veamos cuánto aprendiste TÚ! 🧠

Acuérdate de las palabras mágicas: DATOS, ETIQUETAS, ENTRENAR y PREDECIR. Y de lo que pasó con tus caritas de colores...

Son 8 preguntas. Pasas con 6, y puedes volver a intentarlo.$txt$,
    'teacher_notes', $txt$OBJETIVO
Comprobar el vocabulario de la pieza 4 y la lección de sesgo de la misión 2.

SE CALIFICA SOLO. Aprueba con 70 (6 de 8). Se puede reintentar.

LAS QUE MÁS DICEN
- P5 (la carita feliz roja): si un niño la falla, probablemente no llegó a la prueba secreta de la misión 2. Revisa su entrega.
- P6 (ejemplos variados): es la idea que necesita para el proyecto final. Si la falla, recuérdasela cuando comentes el proyecto.$txt$)
FROM subjects s
WHERE s.id = c.subject_id AND s.name = 'Mi Primera IA' AND c.title = 'Quiz: ¿Cómo aprende una máquina?';

-- ─── Pieza 8 · material · Cuando la IA se equivoca ───
INSERT INTO content (title, description, type, subject_id, created_by, xp_reward,
                     difficulty, estimated_minutes, content_body, order_index,
                     is_published, is_active)
SELECT 'Cuando la IA se equivoca', 'Tu Detector de Caritas se equivocó por algo que tiene nombre: sesgo. Hoy descubres por qué importa en el mundo real y armas tus reglas de oro para usar la IA con cuidado.', 'material'::content_type, mat.id, autor.id, 25,
       'facil'::difficulty_level, 15, '{}'::jsonb, 8, true, true
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
    estimated_minutes = 15,
    order_index       = 8,
    content_body      = jsonb_build_object(
    'instructions', $txt$¿Te acuerdas de tu carita feliz ROJA? 😊🔴
Tu IA pensó que estaba triste. No fue por mala: solo había visto caritas felices azules.

HOY VAS A PODER...
✔ Explicar qué es el sesgo y por qué pasa.
✔ Usar la IA con tus 5 reglas de oro.

PASO 1 · EL SESGO, EN FÁCIL
Imagina que le enseñas a una IA a reconocer gatos, pero todas las fotos son de gatos NARANJAS. 🐈
Un día le enseñas un gato negro... y dice "no es un gato".
La IA no es tonta: aprendió exactamente lo que le enseñaron. El problema estaba en los EJEMPLOS.

Eso es el SESGO: cuando una IA aprende algo chueco porque sus ejemplos no eran variados o no eran justos.

PASO 2 · ¿Y ESTO PASA DE VERDAD? ¡SÍ!
▸ Algunas IAs que reconocen caras funcionaban peor con ciertas personas, porque casi no las vieron en sus ejemplos.
▸ Una IA que recomienda juguetes puede decidir que ciertos juguetes son "de niña" o "de niño" solo porque así se compraban antes.

Por eso quienes crean IAs tienen que revisar muy bien sus ejemplos. Justo lo que tú hiciste con tu Detector.

Y ojo: una IA que platica, como yo, también puede INVENTAR. ¿Te acuerdas de tu pregunta trampa? No lo hago para engañarte: armo la respuesta que me parece más probable.

PASO 3 · TUS 5 REGLAS DE ORO ⭐
1. 🔒 MIS DATOS SON MÍOS. No comparto mi nombre completo, dirección, escuela, teléfono, contraseñas ni fotos.
2. 🔍 REVISO. Si algo es importante, lo compruebo con un adulto o en otro lugar.
3. 🧠 PIENSO YO PRIMERO. La IA me ayuda a aprender; no hace la tarea por mí.
4. ❤️ SOY AMABLE. No uso la IA para molestar ni burlarme de nadie.
5. 🗣️ SI ALGO ME INCOMODA, LE DIGO A UN ADULTO. Siempre.

💬 ByteBot dice: "Pregúntame con el botón de ByteBot: ¿Qué información nunca debería compartir con una IA? Si te digo algo que no está en tus 5 reglas, ¡agrégalo como regla número 6!"

LO QUE TE LLEVAS HOY
Una IA es tan justa como sus ejemplos. Y quien la usa bien, revisa, cuida sus datos y piensa por sí mismo.

⭐ RETO EXTRA (si quieres más)
▸ Si entrenaras una IA para reconocer PERROS solo con fotos de chihuahuas, ¿qué pasaría cuando vea un gran danés?
▸ Una IA que decide qué niños reciben un premio de la escuela, ¿dónde podría tener sesgo? ¿Qué ejemplos le darías para que fuera justa?
No es obligatorio: es para los que se quedaron con ganas.

¿QUÉ SIGUE?
¡Tu proyecto final! Vas a inventar una IA que ayude a alguien. Y ya sabes lo más importante: pensar qué podría salir mal.$txt$,
    'teacher_notes', $txt$OBJETIVO
Ponerle nombre a lo que el niño vivió en la misión 2 (sesgo), llevarlo al mundo real y cerrar el curso con hábitos de uso seguro.

TIEMPO: 15 minutos. ES MATERIAL: no se califica, pero el proyecto final pide aplicarlo.

LOS EJEMPLOS DEL MUNDO REAL
Están suavizados a propósito para niños de 8 a 12. Si una familia pide más, los casos reales son el reconocimiento facial con peor precisión en ciertos grupos y los traductores con sesgo de género. Los dos están bien documentados. El de los traductores se quitó de la base para que la pieza fuera más corta.

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
       'medio'::difficulty_level, 45, '{}'::jsonb, 9, true, true
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
    estimated_minutes = 45,
    order_index       = 9,
    content_body      = jsonb_build_object(
    'instructions', $txt$¡Llegaste al final! 🎉 Ahora tú eres el inventor.

Vas a inventar una inteligencia artificial que AYUDE a alguien. Al aprobar este proyecto te ganas tu certificado de ByteKids Academy. 🎓

HOY VAS A PODER...
✔ Usar todo lo que aprendiste para inventar tu propia IA.
✔ Pensar como los creadores de IA: qué necesita y qué puede salir mal.

PASO 1 · ESCOGE UN PROBLEMA
Piensa en alguien de tu casa, tu escuela o tu colonia. ¿Qué le ayudaría?
Tu IA tiene que aprender con IMÁGENES, como tu Detector de Caritas.

¿No se te ocurre nada? Escoge una de estas:
🗑️ La Separadora de Basura: ¿es papel, plástico u orgánico?
🌱 La Cuidadora de Plantas: ¿esta hoja está sana o seca?
🐾 La Guardiana del Plato: ¿el plato de mi mascota está lleno o vacío?
💡 ¡La tuya! La que tú inventes.

PASO 2 · DALE VIDA
▸ Ponle un NOMBRE a tu IA.
▸ Dibújala en una hoja: puede ser una app, un robot o una cámara. ¡Tú decides!

PASO 3 · ¿QUÉ TIENE QUE APRENDER?
▸ Escribe sus ETIQUETAS: 2 grupos. Ejemplo: "sana" y "seca".
▸ Para cada etiqueta, escribe 2 EJEMPLOS con los que la entrenarías.
▸ ¡Acuérdate del Detector de Caritas! Tus ejemplos tienen que ser VARIADOS: distintos colores, tamaños y lugares.

PASO 4 · ¿QUÉ PODRÍA SALIR MAL?
Piensa como científico: ¿dónde se podría equivocar tu IA? ¿Con poca luz? ¿Con algo que nunca vio?

PASO 5 · ENTREGA TU FICHA
Escribe aquí tu ficha, con estos títulos. Una o dos líneas en cada uno:
   NOMBRE DE MI IA:
   A QUIÉN AYUDA Y CÓMO:
   MIS ETIQUETAS Y 2 EJEMPLOS DE CADA UNA:
   QUÉ PODRÍA SALIR MAL:

ASÍ SE CALIFICA (sobre 10, se aprueba con 7)
⭐ Un problema claro y a quién ayuda: 3
⭐ Etiquetas con ejemplos variados: 3
⭐ Qué podría salir mal: 3
⭐ Nombre y creatividad: 1

No se califica que tu IA sea perfecta. Se califica que la pienses bien.

💬 ByteBot dice: "Estoy muy orgulloso de ti. Empezaste sin saber qué era la IA y hoy estás inventando una. Eso es lo que hacen los creadores de tecnología."

⭐ RETO EXTRA (para inventores valientes)
▸ PÍDEME CONSEJO: dale al botón de ByteBot y escríbeme: "Mi IA se llama ____ y sirve para ____. ¿Cómo la puedo mejorar?" Agrega a tu ficha qué consejo usaste, o por qué no.
▸ ¿CÓMO LO EVITARÍAS? Agrega a tu ficha cómo harías para que tu IA no se equivoque en lo que puede salir mal.
▸ CONSTRUCTOR: construye tu IA en Machine Learning for Kids (abre el enlace con el botón 🚀 de abajo), igual que tu Detector de Caritas. Usa dibujos o fotos de OBJETOS, nunca de personas. Haz 5 pruebas y agrégalas a tu ficha.
🔙 Cuando termines tus pruebas, regresa a esta pestaña de ByteKids y agrégalas a tu ficha.
Nada del reto es obligatorio ni cambia tu calificación. Con tu ficha basta para tu certificado.

¡A INVENTAR! 🚀$txt$,
    'teacher_notes', $txt$OBJETIVO
Que el niño aplique TODO el curso: problema real → etiquetas → ejemplos variados → qué puede salir mal. El consejo de ByteBot y la construcción quedan en el ⭐ Reto extra.

TIEMPO: 30 a 45 minutos. Puede tomar varios días; no hay prisa. Con el reto extra, hasta 90.

ESTE PROYECTO DESTRABA EL CERTIFICADO
Aprobarlo (7 o más) + los dos quizzes aprobados + las 9 piezas completas = certificado. Por eso: si un proyecto no llega, NO lo repruebes. Usa "Pedir correcciones" y dile exactamente qué falta. Queremos que TODOS lleguen al certificado; el estándar se sostiene con correcciones, no con rechazos.

RÚBRICA DETALLADA (sobre 10)
Problema y a quién ayuda (3)
  3 = claro, real, dice quién se beneficia · 2 = escogió uno de la lista sin decir a quién ayuda · 1 = vago ("ayuda a la gente") · 0 = no hay
Etiquetas y ejemplos (3)
  3 = 2 etiquetas, 2 ejemplos cada una y VARIADOS · 2 = ejemplos sin variedad · 1 = faltan etiquetas o ejemplos
Qué puede salir mal (3)
  3 = un riesgo concreto (poca luz, algo que nunca vio) · 1 = riesgo vago · 0 = "nada puede salir mal"
Creatividad (1)
  1 = nombre y una idea propia

QUÉ ESPERAR SEGÚN LA EDAD
Un niño de 8 puede llenar cada título con una línea: eso es un 10 si las ideas están. No le bajes puntos por escribir poco.

EL RETO EXTRA
No da puntos, porque no queremos castigar a quien no tiene dispositivo, tiempo o 12 años. Reconócelo en el comentario: "¡Además lo construiste! Eso ya es de nivel Principiante."

IDEAS DELICADAS
Si un niño propone una IA que decide cosas importantes sobre personas (quién es buen alumno, quién miente) o que usa fotos de personas: no la rechaces. Úsala para enseñar: pídele en el comentario que piense en el sesgo y la privacidad, y que ajuste la idea para usar objetos o dibujos.

PARA COMENTAR AL APROBAR
"¡Felicidades, inventor/inventora! Tu IA ____ es una gran idea porque ____. Ya te ganaste tu certificado de Mi Primera IA."

LA INVITACIÓN AL CURSO COMPLETO VA A LA FAMILIA, NO AL NIÑO
Si ByteKids manda la invitación a "IA para Niños (Principiante)", que sea al papá o la mamá: por su cuenta de familia o por el canal que ellos dieron al inscribirse. Venderle directo a un niño no va con nosotros.$txt$,
    'url', 'https://machinelearningforkids.co.uk/?lang=es',
    'resource_type', 'enlace')
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

-- ============================================================================
--  PASO 3-A - EL ORDEN: CADA PIEZA PIDE LA ANTERIOR
--  Usa mission_prerequisites (la lee DesbloqueoService desde el 30-sep).
--  Una mision se desbloquea al ENTREGAR la anterior; un quiz, al aprobarlo.
--  El id va explicito con gen_random_uuid(): la entidad lo genera en Java y
--  no hay que suponer que la columna tenga default.
-- ============================================================================

INSERT INTO mission_prerequisites (id, mission_id, prerequisite_id)
SELECT gen_random_uuid(), actual.id, anterior.id
FROM content actual
JOIN content anterior ON anterior.subject_id = actual.subject_id
                     AND anterior.order_index = actual.order_index - 1
JOIN subjects s ON s.id = actual.subject_id
WHERE s.name = 'Mi Primera IA'
  AND actual.is_active AND anterior.is_active
ON CONFLICT (mission_id, prerequisite_id) DO NOTHING;

-- ============================================================================
--  PASO 3-B - LOS LOGROS DEL CURSO
--  Se crean o se actualizan por titulo. Los de tipo subject_content los
--  evalua el backend desde el 29-sep; con un backend anterior simplemente
--  no se otorgan, no truenan.
-- ============================================================================

INSERT INTO achievement_definitions
  (title, description, icon, xp_reward, category, rarity, condition_type, condition_value)
VALUES
  ('¡Hola, IA!',
   'Diste tu primer paso en Mi Primera IA. Ya sabes que una IA aprende de ejemplos.',
   '👋', 10, 'especial', 'comun', 'subject_missions',
   '{"subject": "Mi Primera IA", "count": 1}'),
  ('Entrevistador de IA',
   'Entrevistaste a una inteligencia artificial y descubriste que no lo sabe todo.',
   '🎤', 20, 'social', 'comun', 'subject_content',
   '{"subject": "Mi Primera IA", "title": "Misión 1: Entrevista a una IA"}'),
  ('Entrenador de IA',
   'Entrenaste tu propia IA desde cero y descubriste su secreto: el sesgo.',
   '🧠', 30, 'programacion', 'poco_comun', 'subject_content',
   '{"subject": "Mi Primera IA", "title": "Misión 2: Entrena tu Detector de Caritas"}'),
  ('Detective en casa',
   'Encontraste la IA escondida en tu casa y entrevistaste a tu familia.',
   '🔍', 20, 'social', 'poco_comun', 'subject_content',
   '{"subject": "Mi Primera IA", "title": "Investigación: La IA en mi casa"}'),
  ('Inventor de IA',
   'Inventaste una inteligencia artificial para ayudar a alguien. ¡Eres creador de tecnología!',
   '🚀', 40, 'proyectos', 'raro', 'subject_content',
   '{"subject": "Mi Primera IA", "title": "Proyecto final: Mi IA para ayudar"}'),
  ('Graduado de Mi Primera IA',
   'Terminaste las 9 actividades de Mi Primera IA. ¡Te ganaste tu certificado de ByteKids!',
   '🎓', 50, 'especial', 'epico', 'subject_missions',
   '{"subject": "Mi Primera IA", "count": 9}')
ON CONFLICT (title) DO UPDATE
  SET description     = EXCLUDED.description,
      icon            = EXCLUDED.icon,
      xp_reward       = EXCLUDED.xp_reward,
      category        = EXCLUDED.category,
      rarity          = EXCLUDED.rarity,
      condition_type  = EXCLUDED.condition_type,
      condition_value = EXCLUDED.condition_value,
      is_active       = true;

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

-- El orden: deben salir 8 filas, de la 2 pidiendo la 1 hasta la 9 pidiendo la 8.
SELECT m.order_index AS pieza, p.order_index AS pide_la, m.title
FROM mission_prerequisites mp
JOIN content m ON m.id = mp.mission_id
JOIN content p ON p.id = mp.prerequisite_id
JOIN subjects s ON s.id = m.subject_id
WHERE s.name = 'Mi Primera IA'
ORDER BY m.order_index;

-- Los 6 logros. En la columna "pieza_existe", los de subject_content deben
-- decir true: si alguno dice false, el titulo no casa y nadie lo va a ganar.
SELECT a.title, a.icon, a.condition_type,
       a.condition_value ->> 'title' AS pieza,
       a.condition_value ->> 'count' AS cuantas,
       CASE WHEN a.condition_type = 'subject_content'
            THEN EXISTS (SELECT 1 FROM content c JOIN subjects s ON s.id = c.subject_id
                         WHERE s.name = a.condition_value ->> 'subject'
                           AND c.title = a.condition_value ->> 'title')
       END AS pieza_existe
FROM achievement_definitions a
WHERE a.condition_value ->> 'subject' = 'Mi Primera IA' AND a.is_active
ORDER BY a.xp_reward;

-- Los ninos que YA hicieron piezas antes de correr esto no reciben los
-- logros al instante: la revision corre la proxima vez que entregan algo o
-- les aprueban algo. Como el curso aun no se lanza, no hace falta mas.

-- ============================================================================
--  PASO 5 - PARA QUE LOS NINOS LO VEAN
--  Desde la plataforma, no desde aqui:
--    1. Coordinacion > Salones > crea "Mi Primera IA - Grupo 1".
--    2. Asignale la materia "Mi Primera IA" y a su maestro (o a ti).
--    3. Crea las cuentas de los ninos e inscribelos en ese salon.
-- ============================================================================
