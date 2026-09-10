-- ============================================================================
--  ByteKids Academy - Guia del maestro para "IA para Ninos (Intermedio)"
--  Fecha: 10-sep-2026
--
--  QUE HACE
--    Agrega la guia para dar la clase a las 17 piezas del nivel intermedio,
--    dentro de content_body -> teacher_notes. Es el equivalente de lo que ya
--    existe en Principiante desde el 7-sep.
--
--  QUE **NO** HACE
--    No toca titulos, descripciones, tipos, XP ni enlaces. Solo escribe la
--    llave teacher_notes. Si una pieza necesita correccion de contenido, se
--    hace aparte y a proposito: mezclar las dos cosas fue lo que complico el
--    script de Principiante.
--
--  POR QUE AHI
--    content_body es JSONB: no hace falta migrar el esquema, y en este
--    proyecto eso importa porque no hay Flyway y cada cambio es SQL a mano.
--    El backend borra teacher_notes, expected_output y solution_check antes
--    de responderle a un alumno (ContentResponse.cuerpoVisible), asi que la
--    guia nunca llega al navegador del nino.
--
--  ORDEN
--    Corre el PASO A, revisa el resultado, y luego B y C.
--    Es idempotente: se puede volver a correr sin duplicar nada.
--
--  OJO - UNA INCONSISTENCIA QUE HAY QUE REVISAR
--    En el plan del temario, la pieza 8 se titula "Neuronas, pesos y capas"
--    pero su descripcion habla del curso de IA para Oceanos de Code.org, que
--    es justo el recurso de la pieza 1 de Principiante. Una de las dos esta
--    mal. La guia de abajo esta escrita para el TITULO --neuronas, pesos y
--    capas-- porque es lo que sostiene el bloque 3 y lo que premia el logro
--    "Arquitecto de neuronas". Si en la base la pieza 8 de verdad manda a
--    Code.org Oceanos, avisame y la corregimos aparte.
-- ============================================================================


-- ============================================================================
--  PASO A - INVENTARIO
--  Verifica que los order_index sean 1..17 y no se repitan. Los UPDATE de
--  abajo cazan por order_index: si hay una pieza de mas, un indice repetido
--  o un titulo que no corresponde, PARA aqui y avisa antes de seguir.
-- ============================================================================

SELECT c.order_index, c.type, c.title,
       (c.content_body ? 'teacher_notes') AS ya_tiene_guia
FROM content c JOIN subjects s ON s.id = c.subject_id
WHERE s.name ILIKE '%Intermedio%'
ORDER BY c.order_index;


-- ============================================================================
--  PASO B - GUIA DEL MAESTRO EN LAS 17 PIEZAS
--
--  Forma de cada guia:
--    objetivo  - la unica frase que el alumno se tiene que llevar.
--    duracion  - tiempo real de clase, no el estimado de la pieza.
--    explicar  - lo que el maestro prepara o dice ANTES de soltarlos.
--    preguntas - para abrir discusion cuando ya lo hicieron.
--    errores   - lo que se rompe en la practica, y como atajarlo.
--    cierre    - con que se termina la sesion.
-- ============================================================================

-- ── BLOQUE 1 · Como percibe una maquina ─────────────────────────────────────

-- 1. Una foto es un monton de numeros
UPDATE content c
SET content_body = jsonb_set(
      coalesce(c.content_body, '{}'::jsonb), '{teacher_notes}',
      $guia${
  "objetivo": "Que el alumno entienda que la máquina no ve imágenes: ve números, y que un conjunto de datos es un montón enorme de ejemplos que hicieron personas.",
  "duracion": "25 min",
  "explicar": [
    "Abre el explorador de Quick, Draw! y busca una categoría junto con el grupo: 'gato' funciona bien.",
    "Que vean la cantidad. Son 50 millones de dibujos y los hicieron personas jugando, no una máquina.",
    "El punto de hoy: cada trazo se guardó como una lista de coordenadas. Para el modelo, un gato es una tabla de números.",
    "Antes de soltarlos, pide que elijan DOS categorías y las comparen. Sin esa instrucción se quedan jugando 20 minutos."
  ],
  "preguntas": [
    "¿Quién dibujó todo esto?",
    "¿Se parecen entre sí todos los gatos? ¿Cuáles se salen del molde?",
    "Si casi toda la gente dibuja el gato de perfil y tú lo dibujas de frente, ¿qué crees que pasaría?"
  ],
  "errores": [
    "Quedarse jugando sin mirar los datos. La pieza no es el juego, es el explorador de datos.",
    "Creer que la máquina 've' como nosotros. Regresa siempre a los números.",
    "Pensar que el conjunto de datos es 'la verdad'. Son dibujos de personas, con sus manías y sus prisas."
  ],
  "cierre": "La frase que se llevan: para la máquina tu dibujo es una lista de números, nada más."
}$guia$::jsonb, true)
FROM subjects s
WHERE s.id = c.subject_id AND s.name ILIKE '%Intermedio%' AND c.order_index = 1;

-- 2. Ensenale a escuchar
UPDATE content c
SET content_body = jsonb_set(
      coalesce(c.content_body, '{}'::jsonb), '{teacher_notes}',
      $guia${
  "objetivo": "Que entrenen un modelo de audio y descubran que es el MISMO método que usaron con imágenes en el básico: cambia el sentido, no la idea.",
  "duracion": "45 min",
  "explicar": [
    "Prueba el micrófono ANTES de la clase. Es lo que más tumba esta sesión.",
    "Teachable Machine pide primero 20 segundos de 'ruido de fondo'. No lo saltes ni dejes que lo salten: es la clase que se necesita para que el modelo sepa cuándo NO le están hablando.",
    "Tres comandos, unas ocho muestras cada uno. Que graben varios compañeros, no siempre la misma voz.",
    "Si el salón es ruidoso, es a favor: el modelo va a aprender con el ruido real donde se va a usar."
  ],
  "preguntas": [
    "¿Por qué crees que te pidió grabar el silencio del salón antes que nada?",
    "¿Qué pasa si lo dice tu compañero en vez de ti? ¿Y si lo dices más fuerte?",
    "¿El modelo entiende la palabra, o reconoce cómo suena?"
  ],
  "errores": [
    "Saltarse el ruido de fondo: el modelo contesta cualquier cosa todo el tiempo y no entienden por qué.",
    "Grabar las ocho muestras seguidas con la misma voz y el mismo tono: memoriza esa voz.",
    "Pegarse al micrófono al entrenar y alejarse al probar. Que graben a distancias distintas."
  ],
  "cierre": "Mismo método que las frutas del básico, otro sentido. Pregúntales qué otro sentido se podría entrenar igual."
}$guia$::jsonb, true)
FROM subjects s
WHERE s.id = c.subject_id AND s.name ILIKE '%Intermedio%' AND c.order_index = 2;

-- 3. El experimento del ruido
UPDATE content c
SET content_body = jsonb_set(
      coalesce(c.content_body, '{}'::jsonb), '{teacher_notes}',
      $guia${
  "objetivo": "Primera evaluación de verdad: probar el modelo en condiciones que NO vio en el entrenamiento y registrar dónde falla.",
  "duracion": "30 min",
  "explicar": [
    "Deja clarísimo que hoy NO se entrena. Hoy se prueba. Es la primera vez que hacen esa distinción y se les va a ir de las manos.",
    "Mínimo tres condiciones: con música de fondo, en otro lugar, y con la voz de alguien más.",
    "Que anoten en tabla: condición, y cuántas veces acertó de cinco intentos. Números, no impresiones.",
    "El modelo que van a probar es el suyo de la pieza 2, no uno nuevo."
  ],
  "preguntas": [
    "¿En cuál condición falló más? ¿Por qué crees?",
    "¿Qué le agregarías al ENTRENAMIENTO para que aguante esa condición?",
    "Si tu modelo funciona en tu casa pero no en el salón, ¿está bueno o malo?"
  ],
  "errores": [
    "Reentrenar en cuanto falla, en vez de anotar el fallo. El fallo es el dato de hoy.",
    "Escribir 'funcionó bien'. Pide el número: 3 de 5.",
    "Probar las tres condiciones con la misma persona y decir que ya cambió la voz."
  ],
  "cierre": "Un modelo no es bueno o malo: es bueno en unas condiciones y malo en otras. Que lo digan con sus datos en la mano."
}$guia$::jsonb, true)
FROM subjects s
WHERE s.id = c.subject_id AND s.name ILIKE '%Intermedio%' AND c.order_index = 3;


-- ── BLOQUE 2 · Entrenar de verdad ───────────────────────────────────────────

-- 4. Entrenar y examinar no son lo mismo
UPDATE content c
SET content_body = jsonb_set(
      coalesce(c.content_body, '{}'::jsonb), '{teacher_notes}',
      $guia${
  "objetivo": "Que entiendan por qué los ejemplos de prueba se separan y nunca se usan para entrenar.",
  "duracion": "20 min",
  "explicar": [
    "Arranca con la analogía y no con la definición: si el examen trae exactamente las mismas preguntas de la guía de estudio, ¿tu 10 demuestra que aprendiste o que memorizaste?",
    "Traduce: entrenar es estudiar, probar es el examen. Si el modelo ya vio esas fotos, su calificación no vale.",
    "Enlázalo con la pieza 3: cuando probaron con música de fondo, estaban haciendo justo esto.",
    "Regla práctica que se van a llevar a la pieza 5: aparta unos ejemplos ANTES de entrenar y no los toques."
  ],
  "preguntas": [
    "Si te doy el examen con las respuestas, ¿qué mide tu calificación?",
    "¿Cómo sabrías si de verdad aprendiste algo?",
    "¿Por qué no sirve calificar al modelo con las mismas fotos que le enseñaste?"
  ],
  "errores": [
    "Confundir 'probar' con 'usar'. Probar es medir a propósito, anotando.",
    "Creer que más datos siempre mejora. Más datos IGUALES no mejoran nada.",
    "Quedarse con la definición sin la analogía: si no pueden explicarla con el examen, no la entendieron."
  ],
  "cierre": "Guardar ejemplos que el modelo nunca vio es la única forma honesta de calificarlo."
}$guia$::jsonb, true)
FROM subjects s
WHERE s.id = c.subject_id AND s.name ILIKE '%Intermedio%' AND c.order_index = 4;

-- 5. El modelo tramposo  <- la pieza mas importante del nivel
UPDATE content c
SET content_body = jsonb_set(
      coalesce(c.content_body, '{}'::jsonb), '{teacher_notes}',
      $guia${
  "objetivo": "Que construyan un modelo que funciona perfecto y descubran ellos solos que aprendió el fondo y no el objeto. Es la mejor lección de todo el temario.",
  "duracion": "50 min (35 de trabajo + 15 de discusión)",
  "explicar": [
    "NO les adelantes el final. El descubrimiento es la clase. Si les dices que va a fallar, se pierde.",
    "Instrucción literal: todas las fotos de la clase A contra la misma pared, y todas las de la clase B contra otra pared distinta. Que no muevan el fondo.",
    "Cuando el modelo salga con 100% de acierto, felicítalos en serio. Deja que se lo crean.",
    "Recién entonces: 'ahora enséñale el mismo objeto, pero párate en otro lado del salón'.",
    "Ten lista tu propia demo por si a algún equipo no le falla: necesitas el fracaso para dar la clase."
  ],
  "preguntas": [
    "¿Qué aprendió realmente tu modelo?",
    "¿Cómo podrías comprobar cuál de las dos cosas está mirando?",
    "¿Qué cambiarías del entrenamiento para arreglarlo? (respuesta: variar el fondo, no agregar más fotos iguales)",
    "¿Dónde crees que le pasa esto a una IA de verdad?"
  ],
  "errores": [
    "Adelantar el final. Es el único error grave de esta sesión.",
    "Que se muevan sin querer y el modelo NO falle: insiste en el rigor del fondo fijo.",
    "Frustración: hay niños que lo viven como que lo hicieron mal. Enmárcalo desde el principio como un experimento que buscamos que falle.",
    "Cerrar sin la discusión. La actividad sin la conversación no enseña nada."
  ],
  "cierre": "Acertar no es lo mismo que haber aprendido. Escríbelo en el pizarrón y déjalo ahí el resto del bloque."
}$guia$::jsonb, true)
FROM subjects s
WHERE s.id = c.subject_id AND s.name ILIKE '%Intermedio%' AND c.order_index = 5;

-- 6. Quiz: entrenamiento, prueba y trampas
UPDATE content c
SET content_body = jsonb_set(
      coalesce(c.content_body, '{}'::jsonb), '{teacher_notes}',
      $guia${
  "objetivo": "Verificar que el bloque de método quedó, sobre todo la diferencia entre memorizar y aprender.",
  "duracion": "25 min (15 de quiz + 10 de repaso en grupo)",
  "explicar": [
    "Se contesta individual y sin ayuda. Es un diagnóstico tuyo, no una calificación definitiva.",
    "Antes de repartirlo, dales cinco minutos para releer sus notas de las piezas 4 y 5.",
    "Al terminar, revisen en grupo solo las preguntas que más se fallaron.",
    "La plataforma guarda cada intento y la calificación te llega sola a la libreta: no tienes que recogerlo."
  ],
  "preguntas": [
    "De las que fallaste, ¿cuál crees que era la trampa?",
    "¿Puedes explicar con tus palabras qué es un modelo tramposo?",
    "¿Cómo comprobarías si un modelo aprendió o memorizó?"
  ],
  "errores": [
    "Dejar que lo contesten en equipo: pierdes la señal de quién no entendió.",
    "Usarlo como castigo. Es un termómetro, dilo así.",
    "Seguir al bloque 3 con medio grupo reprobando la pregunta del modelo tramposo."
  ],
  "cierre": "Si más de la mitad falla la pregunta del modelo tramposo, regresa a la pieza 5 antes de avanzar. No se puede construir el bloque 3 sin eso."
}$guia$::jsonb, true)
FROM subjects s
WHERE s.id = c.subject_id AND s.name ILIKE '%Intermedio%' AND c.order_index = 6;

-- 7. Disena tu conjunto de datos
UPDATE content c
SET content_body = jsonb_set(
      coalesce(c.content_body, '{}'::jsonb), '{teacher_notes}',
      $guia${
  "objetivo": "Que planeen los datos ANTES de recolectarlos, y descubran que un conjunto de datos se diseña, no se junta.",
  "duracion": "35 min",
  "explicar": [
    "Sin computadora. Papel y lápiz, y se nota la diferencia: obliga a pensar en vez de dar clic.",
    "El encargo: qué fotos necesitaría una IA que distinga perros de gatos EN LA VIDA REAL, no en el salón.",
    "Cuatro preguntas que tienen que contestar: cuántas, de dónde, con qué variedad, y qué podría salir mal.",
    "Empuja la variedad concreta: razas, tamaños, de día y de noche, adentro y afuera, cachorros y adultos, de frente y de espaldas."
  ],
  "preguntas": [
    "Si solo tomas fotos de tu perro, ¿qué aprendió tu modelo?",
    "¿Qué animal se podría confundir con un gato? ¿Cómo lo cubres?",
    "¿De dónde saldrían las fotos de gatos negros de noche?",
    "¿Qué perro quedaría fuera de tu conjunto sin que te dieras cuenta?"
  ],
  "errores": [
    "Poner un número al azar sin justificarlo. Pide siempre el 'por qué ese número'.",
    "Olvidar la variedad y listar solo cantidad. La variedad es el punto de la pieza.",
    "Copiar la lista del compañero: aquí no hay una respuesta correcta, hay una defendible."
  ],
  "cierre": "Enlázalo con la pieza 5: el modelo tramposo falló por un conjunto de datos mal diseñado, no por mala suerte."
}$guia$::jsonb, true)
FROM subjects s
WHERE s.id = c.subject_id AND s.name ILIKE '%Intermedio%' AND c.order_index = 7;


-- ── BLOQUE 3 · Dentro del modelo ────────────────────────────────────────────

-- 8. Neuronas, pesos y capas
UPDATE content c
SET content_body = jsonb_set(
      coalesce(c.content_body, '{}'::jsonb), '{teacher_notes}',
      $guia${
  "objetivo": "Abrir la caja negra: que puedan nombrar las partes --neurona, peso, capa-- y decir que entrenar es ir ajustando los pesos.",
  "duracion": "30 min",
  "explicar": [
    "Empieza con una sola neurona en el pizarrón: entran números, cada uno con un peso, se suman, y sale una respuesta.",
    "El peso es qué tanto le importa cada dato. Ejemplo con el grupo: para decidir si sales a jugar, ¿pesa más la lluvia o la tarea?",
    "Entrenar es ir corrigiendo los pesos cada vez que se equivoca. No es magia: es ajustar números muchas veces.",
    "Capa: neuronas trabajando en paralelo, y la salida de unas es la entrada de otras. Las primeras ven bordes, las últimas ven objetos.",
    "Amárralo a lo que ya hicieron: cuando Teachable Machine mostraba la barra de entrenamiento, eso era exactamente esto."
  ],
  "preguntas": [
    "¿Qué es un peso, con tus palabras?",
    "¿Qué pasa dentro del modelo cuando se equivoca?",
    "¿Por qué hacen falta varias capas y no una sola neurona gigante?"
  ],
  "errores": [
    "Decir que las neuronas artificiales son como las del cerebro. Se parecen en el nombre y poco más; corrígelo.",
    "Meterse en matemáticas. A esta edad la suma con pesos es suficiente.",
    "Quedarse en la palabra sin el mecanismo: si no pueden explicar qué es un peso, no avances."
  ],
  "cierre": "Ya no es una caja negra: es una máquina de sumar con números que se van corrigiendo."
}$guia$::jsonb, true)
FROM subjects s
WHERE s.id = c.subject_id AND s.name ILIKE '%Intermedio%' AND c.order_index = 8;

-- 9. Una IA que lee tu cuerpo
UPDATE content c
SET content_body = jsonb_set(
      coalesce(c.content_body, '{}'::jsonb), '{teacher_notes}',
      $guia${
  "objetivo": "Que descubran que el modelo no ve su cuerpo: ve unos cuantos puntos y las distancias entre ellos.",
  "duracion": "50 min",
  "explicar": [
    "Necesitas espacio físico. Despeja un área donde quepa un niño completo frente a la cámara.",
    "Tres poses BIEN distintas. Si eligen dos parecidas, la sesión se vuelve frustrante.",
    "Que se muevan un poco entre captura y captura: mismo gesto, distinta posición en el cuadro.",
    "Enséñales el esqueleto de puntos que dibuja la herramienta. Ahí está la clase entera: eso es lo que el modelo mira."
  ],
  "preguntas": [
    "Si te pones un suéter de otro color, ¿cambia algo? Pruébalo.",
    "¿Y si te haces para atrás, o te pones de lado?",
    "¿Qué está mirando realmente el modelo: tu cara, tu ropa, o la posición de tus brazos?",
    "¿Funcionaría con un compañero más alto que tú?"
  ],
  "errores": [
    "Poses demasiado parecidas entre sí.",
    "Cámara mal encuadrada: si corta los brazos, los puntos que importan no existen.",
    "Entrenar las tres desde el mismo lugar exacto y luego moverse. Es el mismo error de la pieza 5, y está bien que lo relacionen."
  ],
  "cierre": "No te ve a ti: ve puntos y distancias. Por eso le da igual el color de tu suéter."
}$guia$::jsonb, true)
FROM subjects s
WHERE s.id = c.subject_id AND s.name ILIKE '%Intermedio%' AND c.order_index = 9;

-- 10. Que tan buena es? Aciertos y errores
UPDATE content c
SET content_body = jsonb_set(
      coalesce(c.content_body, '{}'::jsonb), '{teacher_notes}',
      $guia${
  "objetivo": "Que dejen de preguntar 'cuánto acierta' y empiecen a preguntar 'en qué se equivoca'. Los dos tipos de error no cuestan lo mismo.",
  "duracion": "25 min",
  "explicar": [
    "Arranca con el detector de incendios que SIEMPRE dice 'no hay incendio'. Acierta el 99% de las veces. Pregúntales si lo comprarían.",
    "Ahí se rompe la idea de que un porcentaje alto es buena señal. Deja que lo discutan antes de explicar.",
    "Los dos errores, con nombres de la vida real: la alarma que suena sin incendio, y el incendio que nadie detectó.",
    "El punto fino: cuál de los dos duele más DEPENDE de para qué sirve el modelo. No hay respuesta universal."
  ],
  "preguntas": [
    "En un detector de incendios, ¿cuál error te da más miedo?",
    "¿Y en el filtro de correo basura? ¿Cambia la respuesta? ¿Por qué?",
    "¿Y si una IA decide quién entra a una escuela?",
    "¿Se puede tener cero de los dos errores?"
  ],
  "errores": [
    "Quedarse con el porcentaje. Todo el trabajo de hoy es sacarlos de ahí.",
    "Buscar la respuesta 'correcta' sobre cuál error es peor. Lo valioso es que justifiquen.",
    "Pieza difícil: si el grupo se pierde, quédate solo con el detector de incendios y dalo por bueno."
  ],
  "cierre": "La pregunta de un ingeniero no es 'cuánto acierta', es 'en qué se equivoca y a quién le cuesta'."
}$guia$::jsonb, true)
FROM subjects s
WHERE s.id = c.subject_id AND s.name ILIKE '%Intermedio%' AND c.order_index = 10;

-- 11. La tabla de errores
UPDATE content c
SET content_body = jsonb_set(
      coalesce(c.content_body, '{}'::jsonb), '{teacher_notes}',
      $guia${
  "objetivo": "Su primera matriz de confusión, hecha a mano: no solo cuántas veces falló, sino qué confundió con qué.",
  "duracion": "40 min",
  "explicar": [
    "Usan su propio modelo de poses de la pieza 9. No entrenan nada nuevo.",
    "Veinte pruebas, repartidas entre las tres poses. No veinte veces la que ya saben que funciona.",
    "La tabla lleva dos columnas que importan: qué pose hice, y qué pose dijo el modelo.",
    "Al final cuentan: aciertos, confusiones (y con cuál), y veces que no reconoció nada.",
    "Dibuja tú la tabla en el pizarrón antes de que empiecen. Sin el formato, anotan cualquier cosa."
  ],
  "preguntas": [
    "¿Qué dos poses se confunden entre sí? ¿Por qué justo esas?",
    "¿Cuál es tu porcentaje de acierto? Dilo en 'x de 20'.",
    "¿Qué le faltó al entrenamiento para que no confundiera esas dos?"
  ],
  "errores": [
    "Hacer las 20 pruebas iguales. Sale un 20 de 20 que no significa nada.",
    "No anotar sobre la marcha y reconstruir de memoria al final.",
    "Repetir la prueba cuando falla, hasta que salga bien, y anotar solo esa. Diles que el fallo es el dato."
  ],
  "cierre": "Ahora pueden decir 'acierta 17 de 20 y confunde la pose A con la B'. Esa frase es la diferencia entre opinar y medir."
}$guia$::jsonb, true)
FROM subjects s
WHERE s.id = c.subject_id AND s.name ILIKE '%Intermedio%' AND c.order_index = 11;


-- ── BLOQUE 4 · IA que crea, y cuando no creerle ─────────────────────────────

-- 12. Cuando la IA inventa
UPDATE content c
SET content_body = jsonb_set(
      coalesce(c.content_body, '{}'::jsonb), '{teacher_notes}',
      $guia${
  "objetivo": "Distinguir clasificar de generar. Hasta hoy sus modelos elegían entre opciones; estos producen algo que no existía.",
  "duracion": "25 min",
  "explicar": [
    "Recuérdales lo que llevan haciendo todo el curso: el modelo elegía entre las clases que le enseñaron. Eso es clasificar.",
    "AutoDraw hace otra cosa: completa el garabato con un dibujo que tú no hiciste.",
    "El mecanismo, en una frase: vio muchísimos ejemplos y predice lo que sigue. No copia uno, ni inventa de la nada.",
    "Si hay tiempo, Blob Opera para que oigan lo mismo con música."
  ],
  "preguntas": [
    "¿De dónde salió ese dibujo? ¿Lo copió de algún lado?",
    "¿Qué le tuvieron que enseñar para que pudiera hacerlo?",
    "¿En qué se parece y en qué se diferencia de tu clasificador de frutas?"
  ],
  "errores": [
    "Creer que 'crea' como un artista, con intención. No hay intención, hay predicción.",
    "Creer que copia y pega de una biblioteca. Tampoco.",
    "Quedarse jugando. Diez minutos de juego y luego la conversación."
  ],
  "cierre": "Generar es predecir lo que sigue, a partir de millones de ejemplos. Guarda la frase: la vas a necesitar en la pieza 14."
}$guia$::jsonb, true)
FROM subjects s
WHERE s.id = c.subject_id AND s.name ILIKE '%Intermedio%' AND c.order_index = 12;

-- 13. El arte de preguntar
UPDATE content c
SET content_body = jsonb_set(
      coalesce(c.content_body, '{}'::jsonb), '{teacher_notes}',
      $guia${
  "objetivo": "Que comprueben que el resultado depende de CÓMO preguntan, no solo de qué preguntan.",
  "duracion": "40 min",
  "explicar": [
    "Usan ByteBot, dentro de la plataforma. No tienen que salirse.",
    "La misma petición, cuatro formas: (1) vaga, (2) con contexto, (3) pidiendo un formato concreto, (4) con un ejemplo de lo que quieren.",
    "MISMO tema en las cuatro. Si cambian de tema, dejó de ser una comparación y no aprenden nada.",
    "Que peguen las cuatro respuestas una debajo de otra antes de opinar."
  ],
  "preguntas": [
    "¿Cuál respuesta sirvió más? ¿Qué tenía de distinto la pregunta?",
    "¿Qué le faltaba a la primera?",
    "Si tuvieras que enseñarle a un compañero a preguntar bien, ¿qué tres consejos le darías?"
  ],
  "errores": [
    "Cambiar el tema entre pruebas.",
    "Usar la sesión para que la IA les haga otra tarea. Hoy el objeto de estudio es la pregunta, no la respuesta.",
    "Conformarse con 'la 4 estuvo mejor'. Pide que digan POR QUÉ, señalando la parte de la pregunta que lo causó."
  ],
  "cierre": "Contexto, formato y ejemplo. Esas tres cosas cambian el resultado más que cualquier palabra mágica."
}$guia$::jsonb, true)
FROM subjects s
WHERE s.id = c.subject_id AND s.name ILIKE '%Intermedio%' AND c.order_index = 13;

-- 14. Cazador de invenciones
UPDATE content c
SET content_body = jsonb_set(
      coalesce(c.content_body, '{}'::jsonb), '{teacher_notes}',
      $guia${
  "objetivo": "Que comprueben con sus propias manos que una IA puede equivocarse con total seguridad. Probablemente lo más útil de todo el curso.",
  "duracion": "40 min",
  "explicar": [
    "Piden cinco datos VERIFICABLES: fechas, cifras, nombres. Nada de opiniones ni de '¿qué te parece?'.",
    "Truco que hace que la sesión funcione: temas locales y específicos. Cuántos habitantes tiene su municipio, en qué año se fundó su escuela, quién gobernaba tal año. Ahí sí falla. Con datos famosísimos casi nunca se equivoca y la clase se cae.",
    "Cada dato se comprueba en OTRA fuente, no volviéndole a preguntar a la IA.",
    "Registro de tres estados: correcto, incorrecto, y no se pudo comprobar. El tercero también es un resultado.",
    "Recuérdales no escribir datos personales suyos ni de su familia en la conversación."
  ],
  "preguntas": [
    "Cuando se equivocó, ¿se notó? ¿Dudó, o sonaba igual de segura que cuando acertó?",
    "¿En qué tipo de dato falló más?",
    "¿Cómo vas a usar esto la próxima vez que hagas una tarea con IA?"
  ],
  "errores": [
    "Verificar con la misma IA. Es el error más común y anula la actividad.",
    "Elegir datos demasiado conocidos y concluir que nunca se equivoca.",
    "Pasar de la confianza ciega a 'la IA siempre miente'. Ni una ni otra: se verifica lo verificable."
  ],
  "cierre": "Que suene seguro no significa que sea cierto. Esa es la frase del nivel entero."
}$guia$::jsonb, true)
FROM subjects s
WHERE s.id = c.subject_id AND s.name ILIKE '%Intermedio%' AND c.order_index = 14;

-- 15. Tus datos valen
UPDATE content c
SET content_body = jsonb_set(
      coalesce(c.content_body, '{}'::jsonb), '{teacher_notes}',
      $guia${
  "objetivo": "Que sepan qué pasa con la foto que suben, la voz que graban y lo que le cuentan a un chatbot, y que se lleven una regla práctica para decidir.",
  "duracion": "25 min",
  "explicar": [
    "Enlázalo con lo que ya hicieron: ellos entrenaron modelos con sus propias fotos y su propia voz. Ya saben que los datos alimentan modelos.",
    "Por qué es gratis: porque muchas veces el producto son los datos. Dilo sin dramatizar.",
    "Lo que nunca se comparte: domicilio, nombre de la escuela, teléfono, fotos de otras personas sin permiso, cosas de la familia.",
    "CUIDADO: no pidas ejemplos personales en voz alta frente al grupo. Que lo escriban para sí mismos o lo comenten en parejas.",
    "El tono importa: ni pánico ni indiferencia. Se usa, con criterio."
  ],
  "preguntas": [
    "¿Qué le contaste a un chatbot esta semana?",
    "¿Se lo dirías a un desconocido en la calle?",
    "Si una aplicación es gratis, ¿quién la está pagando?"
  ],
  "errores": [
    "Convertirlo en una charla de miedo. Salen creyendo que nada se puede usar, y ese no es el mensaje.",
    "Pedir confesiones públicas. Puede exponer a un niño.",
    "Quedarse en lo abstracto sin una regla que se puedan llevar."
  ],
  "cierre": "La regla: si no lo pondrías en el pizarrón del salón, no se lo des a una IA."
}$guia$::jsonb, true)
FROM subjects s
WHERE s.id = c.subject_id AND s.name ILIKE '%Intermedio%' AND c.order_index = 15;


-- ── BLOQUE 5 · Construir algo real ──────────────────────────────────────────

-- 16. IA dentro de tu codigo
UPDATE content c
SET content_body = jsonb_set(
      coalesce(c.content_body, '{}'::jsonb), '{teacher_notes}',
      $guia${
  "objetivo": "Meter un modelo entrenado por ellos dentro de un programa que hace algo. La IA deja de ser una demostración aparte.",
  "duracion": "60 min",
  "explicar": [
    "PRUEBA EL FLUJO COMPLETO TÚ MISMO ANTES DE LA CLASE. Es la sesión más frágil del nivel: si el enlace entre ML for Kids y Scratch no jala, se pierde la hora.",
    "Revisa cómo está hoy el acceso a ML for Kids --si pide cuenta, si hay modo sin registro-- y resuélvelo antes, no con 20 niños esperando.",
    "Dos clases, no más. El objetivo es la conexión, no un modelo sofisticado.",
    "Que guarden el proyecto de Scratch en cuanto funcione lo mínimo. Antes de adornar.",
    "Vigila el reloj: se les va la hora diseñando el juego y no conectando la IA. Primero conectar, luego adornar."
  ],
  "preguntas": [
    "¿Qué pasa en tu juego cuando el modelo se equivoca? ¿Se rompe, o responde mal?",
    "¿Cómo se enteraría un jugador de que la IA falló?",
    "¿Qué le pondrías para que el juego aguante un error del modelo?"
  ],
  "errores": [
    "Modelos con demasiadas clases: más puntos de falla y menos tiempo.",
    "Perder el proyecto por no guardar.",
    "Confundir 'no funciona' con 'el modelo se equivocó'. Son dos problemas distintos y conviene que aprendan a separarlos."
  ],
  "cierre": "Ya no es una demostración: es una pieza dentro de algo que ellos construyeron. Con eso arrancan el proyecto final."
}$guia$::jsonb, true)
FROM subjects s
WHERE s.id = c.subject_id AND s.name ILIKE '%Intermedio%' AND c.order_index = 16;

-- 17. IA para mi comunidad
UPDATE content c
SET content_body = jsonb_set(
      coalesce(c.content_body, '{}'::jsonb), '{teacher_notes}',
      $guia${
  "objetivo": "Integrar todo el nivel con evidencia: un problema real, un modelo entrenado, una medición honesta y su sesgo documentado.",
  "duracion": "3 h repartidas en varias sesiones (no cabe en una)",
  "explicar": [
    "Cuatro entregables, y los cuatro cuentan: el problema, el modelo, la medición y el sesgo. Un modelo sin medición no es proyecto.",
    "Empuja problemas ACOTADOS. Funcionan: separar los residuos del salón, reconocer las señas que usan en clase, clasificar las plantas del patio. No funciona: 'una IA que ayude al medio ambiente'.",
    "La medición es la tabla de errores de la pieza 11. No es un formato nuevo, ya saben hacerla.",
    "El sesgo documentado sale de la pieza 5 y de la 7: con qué datos lo entrenaron y a quién deja fuera.",
    "Reserva una sesión completa para las presentaciones. Presentar es parte del proyecto, no un extra."
  ],
  "preguntas": [
    "¿En qué se equivoca tu modelo? Enséñame tu tabla.",
    "¿A quién podría perjudicar si se usara de verdad?",
    "¿Qué datos te faltaron y no pudiste conseguir?",
    "¿Qué harías distinto si empezaras otra vez?"
  ],
  "errores": [
    "Problema demasiado grande: se quedan sin modelo.",
    "Saltarse la medición porque 'ya se ve que funciona'.",
    "Presentar solo los casos en que acierta. Pide explícitamente un caso donde falle."
  ],
  "cierre": "Se califica la honestidad sobre los límites, no que el modelo sea perfecto. Díselos ANTES de que empiecen: un proyecto que dice 'falla con poca luz' vale más que uno que dice 'funciona'."
}$guia$::jsonb, true)
FROM subjects s
WHERE s.id = c.subject_id AND s.name ILIKE '%Intermedio%' AND c.order_index = 17;


-- ============================================================================
--  PASO C - VERIFICACION
--  Deben salir 17 filas, todas con guia = t y es_objeto = object. Si alguna
--  sale en f, su order_index no coincide con el del PASO A.
-- ============================================================================

SELECT c.order_index, c.type, c.title,
       (c.content_body ? 'teacher_notes')                AS guia,
       length(c.content_body ->> 'teacher_notes')        AS tamano,
       jsonb_typeof(c.content_body -> 'teacher_notes')   AS es_objeto
FROM content c JOIN subjects s ON s.id = c.subject_id
WHERE s.name ILIKE '%Intermedio%'
ORDER BY c.order_index;
