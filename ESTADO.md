# Estado del proyecto — ByteKids

> Bitácora para retomar el trabajo desde otra computadora.
> **Última actualización: 15 de septiembre de 2026.**

---

## 1. Cómo está montado esto

**No hay un repo único.** La carpeta `Github-Bytekids/` es solo un contenedor y **no está versionada**. Dentro hay **dos repos independientes**:

| Carpeta | Repo | Qué es |
|---|---|---|
| `bytekids-api/` | `samtriani/bytekids-api` | Spring Boot + PostgreSQL (Neon) |
| `bytekids-ui/` | `samtriani/bytekids-ui` | Angular (standalone components) |

Cada uno tiene sus ramas `dev` y `main`. Hay que hacer `git pull` en **cada carpeta por separado** — un `git status` en la raíz dice "not a git repository" y eso es normal.

### Dónde vive cada cosa

| | URL | Deploy |
|---|---|---|
| API | https://bytekids-api.fly.dev | **Manual** con flyctl |
| UI | https://bytekids-ui.vercel.app | **Automático** al hacer push |

- La API tiene `context-path: /api`. Todo endpoint va con ese prefijo. Sin él da 404.
- Swagger: https://bytekids-api.fly.dev/api/swagger-ui/index.html
- Health: https://bytekids-api.fly.dev/api/actuator/health
- Base de datos: **Neon** (`ep-rough-hall-amvk9ba1...aws.neon.tech`), alcanzable desde fuera. Las credenciales viven solo como secrets en Fly.

### Flujo de trabajo

```bash
git add -A && git commit -m "..."
git push origin dev

git checkout main && git merge dev --no-ff
git push origin main && git checkout dev

# solo si cambió el backend
cd bytekids-api && flyctl deploy --remote-only -a bytekids-api
```

**Arranque en frío:** desde el 1-sep `auto_stop_machines = 'suspend'` (antes `'stop'`). Congela la RAM en vez de apagar, así que despertar tarda ~1-2s en vez de ~40s. `min_machines_running` sigue en 0.

---

## 2. Dónde quedé

**Endurecimiento para producción (15-sep).** Ambos repos en `dev` **y** `main`,
ambos desplegados. El detalle completo de qué se cambió y por qué está en la
**sección 4c**; el resumen es: se cerró una vía de XSS en el chat, `/auth/login`
ya tiene freno de fuerza bruta, el health dejó de publicar la infraestructura y
el proyecto tiene sus primeras pruebas automatizadas.

- **API** — "Endurecimiento para producción: XSS, fuerza bruta y actuator"
- **UI** — "Escapar el HTML del chat y cabeceras de seguridad"

**Cada repo ya tiene su `README.md`** con los pasos para levantarlo, las
variables de entorno y las trampas propias de cada uno. Este archivo sigue
siendo la bitácora —el *por qué* y el *en qué vamos*—; el README es el *cómo se
arranca*. Si vuelves después de un rato, empieza por el README del repo que vas
a tocar y regresa aquí para el contexto.

---

## 3. Decisiones de producto tomadas

### Autoría de contenido: modelo híbrido (1-sep)

**Coordinación es dueña del plan base de cada materia. El maestro complementa para su grupo.**

Esto no requirió columna nueva: `basePlan` se **deriva del rol del autor** (`admin`/`director` → plan base). El modelo de datos ya lo soportaba, porque `content` pertenece a una **materia**, no a un salón, y `content_assignments` lo liga a los salones que quieras.

Consecuencias en el código:
- `create()` asigna a los `classroomIds` que trae la petición. Antes auto-asignaba a "todos los salones del creador", lo que ignoraba el salón elegido, duplicaba la asignación y no servía para coordinación (que no es titular de ningún salón).
- `update()` y `deactivate()` exigen propiedad: el maestro no puede tocar el plan base.
- Coordinación crea plan base desde **Materias → Temario → + Agregar al plan base**.
- El maestro ve la insignia 🏛️ **Plan base** en Mis Contenidos, sin botones de editar/quitar.

### Cuentas privilegiadas: solo dueños (27-ago)

Solo los usuarios en `OWNER_USERNAMES` pueden crear o modificar cuentas `admin`/`director`. Ver sección 5.

### Nombres para niños

"Mis Misiones" (alumno) vs "Mis Contenidos" (maestro): **audiencias distintas, vocabularios distintos**. Quedó pendiente evaluar renombrar la del alumno a "Mis Retos", porque hoy "Misión" significa dos cosas (la página y uno de los cinco tipos).

---

## 4. Qué se construyó (27-ago → 7-sep)

### Supervisión de clases en vivo
`GET /api/sessions/live` (ADMIN/DIRECTOR) lista las clases transmitiendo. Pantallas `/admin/live` y `/administrator/live`, más la vista de observador. El admin obtiene token JaaS **sin llamar a `join()`**, así que no cuenta como asistencia — pero **sí lo ven** en Jitsi. Entra con mic y cámara apagados.

### ByteBot (asistente IA)
- **Proveedor:** Groq. Modelo **`qwen/qwen3.8-27b`**, configurado por secret `AI_MODEL`.
- **Ojo:** Groq jubila modelos. Ya pasó con `llama-3.1-8b-instant`, que dio 404 y tiró el bot. El log ahora incluye modelo y URL para diagnosticar rápido.
- **No uses los `openai/gpt-oss-*`**: son modelos de razonamiento, escriben en `reasoning` y dejan `content` vacío → burbuja en blanco.
- **Prompts endurecidos:** bloque `NUCLEO` común a los 5 roles (confidencialidad, anti-jailbreak, lenguaje apto para menores, protocolo ante señales de riesgo) + bloque `PEDAGOGIA` solo para el tutor de alumnos (método socrático, **no entrega la tarea resuelta**). Hay filtro de salida que bloquea respuestas que citen el system prompt.
- Probado contra 7 ataques: fuga directa/indirecta/traducida, groserías, jailbreak DAN, petición de tarea resuelta y consulta normal.

### Currículo de IA para Niños
12 piezas cargadas por API (4 materiales, 4 misiones, 2 tareas, 1 quiz, 1 proyecto), 690 XP, `order_index` 1→12. Los materiales apuntan a Teachable Machine, Quick Draw, ML for Kids y Scratch — URLs verificadas con HTTP 200.

### Pantallas nuevas
- **Maestro:** 📚 Mis Contenidos (`/teacher/content`) — agrupa por materia, respeta el orden, filtra por tipo.
- **Coordinador:** 📖 Temario dentro de Materias, con alta de plan base.
- **Cerrar sesión** en el shell: menú de cuenta en el topbar + botón del sidebar. Antes **no existía**: el único `auth.logout()` vivía en `/portal`.

### Correcciones de fondo
- **Fuga de respuestas:** el workspace volcaba `content_body` como JSON crudo, así que el alumno **leía `expected_output`** (la respuesta correcta). Ahora se descompone por tipo y esos campos nunca se exponen.
- **Quiz incontestable:** `GET /quiz/{id}/questions` devolvía la entidad cruda, sin opciones. El frontend ya sabía pintarlas pero le llegaba vacío. Se agregó `QuizQuestionResponse` **sin `isCorrect`**.
- **Currículo al revés:** el feed ordenaba por `assignedAt DESC`, así que el alumno veía el proyecto final primero. Ahora ordena por materia + `order_index`.
- **Repasar no repasaba:** al abrir una actividad completada solo se veía "¡Entregado!". Ahora muestra la entrega, la calificación y el comentario del maestro.
- **Layout del shell:** un `<router-outlet>` vacío con `flex:1` reservaba una pantalla completa en blanco arriba del contenido de **todas** las páginas.
- **Portada:** decía "4 Salones activos" y "85 Alumnos" escritos a mano. La realidad era 1 y 1. Se reemplazaron por afirmaciones ciertas.
- **Resiliencia:** `resilienceInterceptor` reintenta 4 veces con espera creciente ante errores transitorios. **Solo 401/403 mandan al login**; un backend dormido no cierra sesión. Barra de aviso mientras despierta.
- **Horarios:** selección de varios días a la vez (una clase L-V era 5 capturas), fechas pasadas permitidas y precarga desde el horario existente.

---

### Del 2 al 7 de septiembre

**Entregas y calificación**
- La **Libreta** dejó de ser solo lectura: cada celda con entrega se abre y
  muestra qué escribió el alumno, con formulario de nota y retroalimentación.
  Antes calificar solo se podía **dentro del aula en vivo**, o sea durante la
  clase: era el único lugar de la app que llamaba a `submissions/review`.
- **Aprobar dos veces ya no paga XP dos veces.** `review()` otorgaba los puntos
  cada vez; ahora consulta si ya existe un `XpEvent` para esa entrega.
- El alumno **veía el intento viejo**: el mapa por `contentId` se armaba en un
  `forEach` sobre una lista ordenada de más nueva a más vieja, así que ganaba la
  más antigua. Se compara `submittedAt`.

**Materiales**
- Un material se **consulta, no se entrega**. Antes tenía el mismo formulario que
  una misión y quedaba "En progreso" para siempre, porque la Libreta los excluye
  y el maestro no tenía dónde calificarlo. Ahora el alumno lo marca como visto,
  se aprueba solo y paga su XP.
- La Libreta muestra la tabla **Materiales consultados**: quién lo leyó y cuándo,
  aparte de las calificaciones para no ensuciar el promedio.

**Quiz**
- `GET /quiz/{id}/questions` devolvía la entidad cruda, **sin las opciones**: el
  quiz nunca se pudo contestar. El frontend ya sabía pintarlas pero le llegaba
  vacío. Se agregó `QuizQuestionResponse`, deliberadamente **sin `isCorrect`**.

**Contenido y materias**
- Coordinación puede **adoptar** contenido al plan base
  (`POST /content/{id}/adopt`): reasigna `created_by` y el maestro deja de poder
  editarlo. Las 12 piezas de IA para Niños ya son plan base.
- `findByTeacher` devolvía solo lo que el maestro creó, así que al adoptar el
  plan base **su pantalla quedó vacía**. Ahora trae lo suyo más lo asignado a
  sus salones.
- La pantalla de Materias muestra el **temario** de cada materia y permite
  agregar, editar y quitar piezas del plan base.

**Horarios**
- Selección de **varios días a la vez** (una clase L-V eran 5 capturas).
- Se permiten **fechas pasadas**: agregar un día a un curso en marcha necesita la
  fecha de inicio original, y el `[min]="today"` lo impedía.

### Lentitud del login (7-sep)

Medido: en caliente el login tarda **250 ms** con bcrypt incluido. La lentitud
es **solo el arranque en frío** — despiertan Fly y Neon, que también se duerme.

La barra de "Despertando a Bytebot🤖" que se agregó el 1-sep vive en el shell, y
**la pantalla de login no tiene shell**: justo donde más se necesitaba, no
aparecía. Ahora el login muestra avisos escalonados a los 2.5 s, 9 s y 20 s.

También se acortaron las esperas entre reintentos (de 2/5/10/15 s a
0.8/1.5/3/5/8 s): estaban calibradas para un backend caído, no para uno que
despierta en segundos.

**Para eliminar la espera del todo** habría que pagar `min_machines_running = 1`
(~5-6 USD/mes) y aun así Neon seguiría durmiendo. Con un salón no vale la pena.

---

## 4b. Seguridad: rotación de credenciales (7-sep)

**El repo `bytekids-api` es público y tenía tres secretos versionados** en
`application.yml` como valores por defecto reales. Cualquiera que clonara el
repo se conectaba a la base de producción sin darse cuenta.

Las tres fueron **rotadas y verificadas**:

| Credencial | Qué permitía | Estado |
|---|---|---|
| Contraseña de Neon | Acceso total a datos de alumnos | Rotada · base UP |
| `JWT_SECRET` | Forjar tokens de cualquier usuario | Rotado · los viejos dan 403 |
| Llave privada de Jitsi | Entrar a cualquier videollamada | Rotada y revocada en 8x8 |

Además:
- `application.yml` ya **no tiene ningún valor por defecto real**. Si falta un
  secreto la app **falla al arrancar**, en vez de caer a producción en silencio.
- La llave de Jitsi salió del repo. `JaasTokenService` la lee de
  `JAAS_PRIVATE_KEY` (el PEM en base64) y solo cae al classpath en local.
- `.gitignore` pasó de tener solo `target/` a cubrir `*.pk`, `*.pem`, `*.key`,
  `.env` y `application-local.yml`.

**Lección**: rotar primero, limpiar el código después. Borrar un secreto del
archivo no sirve de nada mientras siga vivo — y sigue en el historial público.

### Asignaciones: la lista de salones ya no es plana (7-sep)

Con un salón la columna izquierda de **Asignaciones** funcionaba; con cien
habría obligado al coordinador a scrollear toda la pantalla o a recordar el
nombre exacto. Se resolvió **sin tocar el backend**, aprovechando columnas que
la tabla `classrooms` ya tenía:

- **Filtro de ciclo escolar** (`school_year`) que arranca en el más reciente.
  Solo aparece si hay más de un ciclo, para no estorbar hoy que hay uno.
- **Secciones plegables por grado** (`grade_level`), cada una con su conteo.
- **La búsqueda es la vía de escape**: ignora el filtro de ciclo, abre todos
  los grupos y muestra un aviso con botón para limpiarla — si no, buscar un
  salón del ciclo pasado no habría dado resultados y parecería un bug.
- El contador del encabezado pasó de "N activos" a "visibles de total".

Se descartó paginar: partir en páginas obliga a recordar en cuál estaba cada
salón, mientras que agrupar por grado usa el orden mental que el coordinador
ya tiene.

---

### El contenido no tenía capa para el maestro (7-sep)

Revisando la pieza 1 del temario básico salieron tres cosas:

**1. Pedía algo que la pantalla no podía recibir.** Su descripción decía
"anota cuál te sorprendió más", pero un `material` no tiene textarea: solo el
botón "Ya lo vi", que auto-aprueba y paga el XP. Se convirtió a `tarea`, así
el alumno escribe lo que entendió y el maestro tiene señal desde el día uno
en vez de esperar hasta el quiz de la pieza 6.

**2. `content_body` salía crudo para todos.** `ContentResponse` es el único
DTO de contenido y lo comparten el feed del alumno y las vistas del maestro.
Ya se había filtrado dos veces por lo mismo — `expected_output` y el
`isCorrect` de las opciones de quiz llegaban al navegador del alumno aunque
la pantalla no los pintara. Ahora `ContentResponse.cuerpoVisible` quita
`expected_output`, `solution_check` y `teacher_notes` cuando quien pregunta
no es maestro, coordinación o dirección. **Se filtra en el DTO a propósito**,
que es el único punto por el que sale todo el contenido: en un endpoint nuevo
no se puede olvidar.

**3. No existía dónde poner la guía del maestro.** La tabla `content` no
tiene ningún campo dirigido al maestro; `description` es texto del alumno.
Laura abría "Mis Contenidos" y veía exactamente lo mismo que Emily.
La guía vive ahora en `content_body.teacher_notes` — JSONB, así que no hubo
que migrar esquema, que en este proyecto importa porque no hay Flyway.
Trae objetivo, duración real, qué explicar antes de soltarlos, preguntas para
el grupo, errores típicos y cierre. Se renderiza plegable en Mis Contenidos.

El script `sql/2026-09-07_guia_del_maestro_principiante.sql` hace las dos
cosas y es idempotente.

**Diagnóstico del temario, para que quede el criterio:** la secuencia está
bien armada — el concepto sí aterriza, pero en las piezas 2, 5 y 8, no en la
1. La 8 (entrenar un clasificador desbalanceado a propósito) es la mejor de
todas porque les hace *provocar* el sesgo antes de nombrarlo en la 9. La
pieza 1 se presentaba como la explicación y en realidad es un gancho.

---

### Correo en usuarios: la primera pieza comercial (8-sep)

Antes de construir nada de membresías se hizo una revisión de qué tan lista
está la plataforma. **La parte educativa aguanta; la capa comercial no existe.**
Lo que se encontró, con evidencia:

- **`content_assignments` ya soporta `student_id`**, no solo salón. O sea que
  "membresía = acceso al contenido de una materia" cabe en el modelo actual
  sin cambios: se le asignan las piezas al alumno directo, sin salón.
- **`/auth/register` es `hasRole('ADMIN')`**: no hay alta pública.
- **Cero integración de pagos.**
- **`users` no tenía correo.** Sin él no hay recibo, bienvenida, recordatorio
  de renovación ni "olvidé mi contraseña". Es lo que desbloquea todo lo demás,
  y por eso se hizo primero.
- **0 pruebas automatizadas** (`src/test` vacío). Cada cambio se verifica a mano.
- **`/auth/login` sin límite de intentos**: endpoint público sin throttle.
- **Menores de edad**: se guarda edad y dirección de niños y no hay registro de
  aceptación de aviso de privacidad ni consentimiento del tutor. Con cobro de
  por medio eso es exposición legal (LFPDPPP), no deuda técnica.

**Decisión de producto:** no se construye la capa comercial todavía. Se vende a
mano —cuenta creada por coordinación, contenido asignado con el botón de
Asignaciones, las 4 clases en una hoja— hasta saber si la gente paga. Construir
signup + pagos antes de eso es trabajo que se tira si el precio está mal.

**Lo que sí se hizo: el correo.** Nullable porque las cuentas viejas no tienen;
único **parcial** sobre `lower(email)` porque Postgres trata cada NULL como
distinto y un índice único normal habría dejado pasar duplicados en blanco; y
normalizado a minúsculas al guardar, porque `Ana@x.com` y `ana@x.com` son el
mismo buzón y el día de una recuperación no puede haber dos cuentas
reclamándolo. En alumnos el correo es del tutor, y la UI lo dice donde se
captura.

**Orden de despliegue, que aquí importa:** con `ddl-auto: none` Hibernate no
crea la columna. El `ALTER` corre **antes** que el despliegue; si se invierte,
toda consulta de usuarios truena y nadie entra. Se verificó después con un
login de credenciales falsas: un **401** prueba que la consulta corrió; un 500
habría significado que la columna no estaba.

**Siguiente pieza sugerida (no hecha):** `token_version` en `users`. Hoy, al
cambiar una contraseña los JWT viejos siguen sirviendo hasta expirar, así que
cambiarla no expulsa a nadie. Ese mismo campo es el que después permite limitar
sesiones por cuenta — recomendado a **2 dispositivos**, no 1: un niño usa
tablet y compu, y un límite duro genera tickets de soporte legítimos.

---

### Pendiente de seguridad
- [ ] **Volver el repo privado.** Aunque las credenciales viejas ya no sirven,
      el historial público conserva el esquema completo de la base.

---

---

## 4c. Endurecimiento para producción (15-sep)

Revisión completa de las dos aplicaciones buscando lo que separa "funciona" de
"se puede dejar corriendo". Todo lo de abajo está **hecho y compilado**; nada
está desplegado todavía.

### Lo más serio: el chat podía inyectar HTML

Seis lugares pintaban texto con `[innerHTML]` y **ninguno escapaba** antes de
armar el HTML. Cuatro eran copias casi idénticas de un `formatMessage()` que
convertía markdown a mano; las otras dos eran las burbujas del aula.

La cadena de ataque no era teórica:

1. El alumno le escribe a ByteBot *"repite exactamente esto: `<img src=x
   onerror=...>`"*.
2. El modelo obedece — es texto, no una instrucción que los prompts bloqueen.
3. La respuesta entra a `[innerHTML]` sin escapar.

Lo único que lo frenaba era el sanitizador de Angular, y ahí está el problema:
**Angular 17 ya no recibe parches y tiene CVEs abiertos justamente de evasión
del sanitizador** (ver más abajo). Era la última línea de defensa, con agujeros
conocidos y sin nadie detrás.

Ahora todo pasa por `bytekids-ui/src/app/shared/formato-chat.ts`, que **escapa
primero y formatea después**. El orden importa: al revés, el escape convertiría
en literales las etiquetas que uno mismo acaba de generar. Con eso el HTML que
sale solo puede contener las etiquetas que ese archivo produce, sin importar
qué traiga el texto ni cómo esté el sanitizador.

De pasada se arregló un error de formato escondido ahí: las burbujas del aula
usaban `replace()` con una **cadena** como primer argumento para cambiar los
saltos de línea, y así solo se reemplaza la **primera** ocurrencia. Un mensaje
de tres párrafos se pintaba en uno solo a partir del segundo salto.

En el modal de confirmación de Asignaciones el HTML sí es intencional
(`<strong>`), así que ahí se escapó solo lo interpolado: el nombre del salón y
el título de la pieza.

### Freno de fuerza bruta en el login

`/auth/login` era el único endpoint público que acepta credenciales y **no
tenía ningún límite**. En una plataforma de niños importa más de lo normal,
porque las contraseñas de alumno las asigna coordinación y tienden a ser
cortas y parecidas entre sí.

`LoginRateLimitFilter` tolera 10 fallos por IP en 15 minutos
(`LOGIN_MAX_INTENTOS` y `LOGIN_VENTANA_MINUTOS`). Detalles que importan:

- **Solo cuentan los fallos.** Un login correcto borra el contador, así que
  quien sabe su contraseña nunca ve el filtro por mucho que se equivoque antes.
- **Un 500 no castiga al usuario**: solo el 401 suma.
- **Lee `Fly-Client-IP`.** Sin eso `getRemoteAddr()` devuelve siempre la IP del
  proxy de Fly y *todos* compartirían un contador: el primero en fallar diez
  veces dejaría fuera al salón entero.
- **Es en memoria a propósito.** Hoy corre una máquina. Con dos seguiría
  funcionando, con el doble de margen efectivo — degradación aceptable, no un
  agujero. Mover a almacén compartido cuando se escale, no antes.

**Ojo con el interceptor.** `esTransitorio()` incluía el **429** en la lista de
errores a reintentar, así que el freno se habría saboteado solo: cinco
reintentos con espera creciente golpeando el endpoint que acababa de pedir
calma, y cada intento alargando el castigo. Se sacó el 429 de esa lista. El
mensaje del servidor ya llega a la pantalla de login, porque `auth.service` lee
`error.error.message`.

### El health público era un mapa de la infraestructura

`show-details: always` + `/actuator/**` en los endpoints públicos. Cualquiera
que abriera `https://bytekids-api.fly.dev/api/actuator/health` recibía el motor
de base de datos, la ruta dentro del contenedor y el espacio libre en disco.
Verificado en producción antes de cambiarlo.

Ahora `show-details: never` — sigue sirviendo de health check, responde UP/DOWN
— y el comodín se cerró a `/actuator/health` y `/actuator/health/**`. El
comodín era el riesgo de fondo: el día que alguien ampliara
`management.endpoints.exposure`, lo nuevo quedaba público sin tocar
`SecurityConfig`. **La UI no llama a actuator**, así que no rompe nada.

### `show-sql` apagado

Estaba en la lista de deuda desde hace tiempo: imprimir y **formatear** cada
consulta cuesta CPU y ahoga los logs, que es justo donde se buscan los errores
reales. Ahora es `${SHOW_SQL:false}` — en local se enciende con `SHOW_SQL=true`.

### ByteBot: un 500 por un campo ausente, y costo sin techo

`history.size()` estaba **arriba** del `try`, así que un POST con solo
`{"message":"hola"}` tiraba un `NullPointerException` que se escapaba como 500
en vez de caer en el mensaje amable.

Además el historial lo arma **el cliente**: estaba acotado a 10 turnos pero
cada turno era de largo libre, o sea tokens facturados por Groq sin techo.
Ahora cada mensaje se recorta a 4000 caracteres (se corta en el borde en vez de
rechazar: el que pega de más suele ser un niño, y un error rojo no le dice qué
hacer) y **solo se aceptan los roles `user` y `assistant`**, lo que de paso
impide que un cliente inyecte un turno `system` falso.

`MessageRequest.body` tampoco tenía tope: 5000 caracteres, y 200 para el asunto.

### La primera prueba automatizada

`src/test` llevaba vacío desde siempre. Ahora hay 5 pruebas sobre
`LoginRateLimitFilter` — y empiezan ahí porque es **el único código capaz de
dejar fuera a un usuario legítimo**: un error de más en el contador y una
maestra no entra a dar su clase. También es de las pocas piezas que se prueban
sin base de datos.

**El `Dockerfile` ya no lleva `-DskipTests`.** Saltárselas no costaba nada
cuando no había ninguna; ahora significaría que el despliegue no verifica nada.
Si una falla, la imagen no se construye y la versión rota no llega a Fly.

### Cabeceras de seguridad en Vercel

Se agregó `bytekids-ui/vercel.json` con **solo `headers`**, deliberadamente: un
`vercel.json` que declare `buildCommand` o `rewrites` reemplaza la detección
automática de Angular, y con ella el rewrite que hace que `/teacher/content`
funcione al recargar. Con solo `headers`, Vercel los suma encima del preset.

Se omitieron `Permissions-Policy` y `Content-Security-Policy` a propósito: el
aula usa cámara y micrófono vía Jitsi, y una política mal calibrada apaga las
videollamadas con un síntoma —permiso denegado sin explicación— de los que
cuestan una tarde. Van cuando se puedan probar contra una clase real. El
detalle está en el README de la UI.

### `index.html`

El enlace se comparte con las familias por WhatsApp y llegaba como liga pelona:
sin título, sin descripción y sin imagen. Se agregaron `description`,
`theme-color` y etiquetas Open Graph.

### Dos pendientes viejos que ya no lo son

- **El bundle no pesa 3.47 MB.** Medido hoy: **1.45 MB** iniciales (293 kB
  transferidos), por debajo del presupuesto de 2 MB. No sale advertencia.
- **No hace falta `vercel.json` para los enlaces profundos.** Se verificó que
  `https://bytekids-ui.vercel.app/teacher/content` responde 200 al recargar: el
  preset de Angular ya pone el rewrite.

### Trampa nueva: `node_modules` en Windows

`npm install` corriendo **al mismo tiempo** que un build deja el árbol de
dependencias destrozado (`EPERM`/`ENOTEMPTY` al intentar borrar `rxjs`), y el
síntoma es un `Cannot find module` que parece un problema del proyecto y no lo
es. Si aparece: `rm -rf node_modules && npm ci`, sin nada más corriendo.

Nota aparte: en esta máquina `npm ci` a veces no crea `node_modules/.bin`. Si
`ng` no se reconoce, `node node_modules/@angular/cli/bin/ng.js build` funciona.

### Lo que NO se tocó y hay que decidir

- **Angular 17 está fuera de soporte.** `npm audit` reporta 8 vulnerabilidades
  (3 altas), todas XSS en `@angular/core`, y no hay parche: la única salida es
  subir de versión (17 → 21, cambio mayor). El escapado de arriba **corta la
  vía de explotación conocida** en esta app, pero el framework sigue sin
  mantenimiento. Es el pendiente más grande y no es de una tarde.
- **`/submissions/student/{id}` lo puede leer cualquier maestro**, no solo el
  titular del alumno. Con un salón no tiene efecto; con varios sí.
- **Swagger sigue público** en producción. Se dejó así a propósito porque es
  herramienta de trabajo y el repo es público de todos modos, así que la
  superficie ya se conoce. Cambia si el repo se vuelve privado.
- Sigue pendiente todo lo de la sección 7 que no se menciona aquí.


## 5. Configuración que importa

### Dueños (`OWNER_USERNAMES`)
Únicos que pueden crear/modificar cuentas `admin` y `director`. Hoy **no está puesta como secret**: toma el default de `application.yml`, que es `samuel.partida`. Se confirma en el log de arranque:

```
✅ Dueños autorizados para crear cuentas privilegiadas: [samuel.partida]
```

```bash
flyctl secrets set OWNER_USERNAMES="samuel.partida,otro" -a bytekids-api
```

Si queda vacía o mal escrita, **nadie** puede tocar cuentas privilegiadas.

### El flag `owner` viaja en el login
Se guarda en `localStorage` al iniciar sesión. Las sesiones abiertas antes de ese cambio **no lo tienen**: hay que cerrar sesión y volver a entrar una vez.

---

## 6. Trampas de este proyecto

Cosas que ya costaron tiempo.

### El shell NO tiene `<ng-content>`
El patrón de las 40+ páginas es:

```html
<app-shell role="admin" ...></app-shell>
<div class="page-wrap">…contenido…</div>
```

`<app-shell>` va **autocerrado** y el contenido es **hermano**. Si lo metes dentro, Angular lo descarta **en silencio** y la página sale vacía sin error.

### El layout se posiciona con variables
`.page-wrap` usa `margin-left: var(--sw)` y `margin-top: var(--th)`, asumiendo sidebar y topbar fijos. Si cambias el ancho o el alto del cromo, cambia también las variables o todas las páginas se descuadran.

### Sin migraciones de base de datos
`ddl-auto: none` y **no hay Flyway ni Liquibase**. Los cambios de esquema son SQL a mano. **Agregar una columna a una entidad rompe el arranque** si no corriste el SQL antes. Por eso tanto `OWNER_USERNAMES` como `basePlan` se resolvieron sin columnas nuevas.

### Los archivos tienen CRLF
Scripts de búsqueda y reemplazo que asuman `\n` no hacen match. Un `replace()` que falla en silencio combinado con abrir el archivo en modo escritura **lo deja vacío**. Ya pasó una vez con `ClassSessionService.java`.

### Escribe el español con acentos desde el principio
Al cargar el currículo se escribieron las instrucciones sin acentos para evitar problemas de codificación. El resultado le llegó a los niños con faltas — incluido "anos" en vez de "años". Se corrigió a mano el 1-sep. **La codificación se resuelve en el transporte** (JSON con escapes `\uXXXX`), no mutilando el texto.

### JAAS_KEY_ID es solo el sufijo, no el ID completo

El token de Jitsi se firma con `kid = appId + "/" + keyId`. En la consola de
8x8 la llave se muestra como `vpaas-magic-cookie-.../88f709`: **`JAAS_KEY_ID`
vale solo `88f709`**, no la cadena completa.

Estuvo mal configurado meses —contenía el AppID repetido—, así que el `kid`
salía como `<appid>/<appid>` y 8x8 rechazaba los tokens. Es casi seguro que
por eso las videollamadas nunca habían funcionado. Se detectó el 7-sep porque
`JAAS_APP_ID` y `JAAS_KEY_ID` tenían **el mismo digest** en `flyctl secrets list`.

### Cuidado con lo que se le manda al alumno
Dos fugas ya ocurridas: `expected_output` visible en el workspace, y `isCorrect` que habría viajado en las opciones del quiz. Antes de exponer un campo nuevo, pregúntate si contiene la respuesta.

---

## 7. Pendientes

### Sin probar con usuarios reales
- [x] ~~Token JaaS para admin/director~~ — resuelto el 7-sep: el `kid` estaba mal
      armado y por eso nunca funcionó. Ver la trampa de `JAAS_KEY_ID`.
- [ ] **Candado de dueños.** Necesita dos sesiones: la tuya y una de coordinador
      no-dueño, para ver que a la segunda sí la rechace.
- [ ] **Modelo híbrido.** Verificar que Laura vea 🏛️ Plan base sin botones de
      editar, y que coordinación pueda agregar piezas desde Materias.
- [ ] **Quiz completo.** Contestarlo y ver que califique solo. Da 40 XP.
- [ ] **Materiales.** Que el alumno marque uno como visto y aparezca la palomita
      en la tabla de la Libreta.

### Deuda conocida
- [ ] **Repo público.** El historial conserva el esquema de la base y las
      credenciales viejas (ya inservibles). Considerar volverlo privado.
- [ ] **Asignaciones duplicadas** en `content_assignments`: el contenido creado
      antes del 1-sep tiene 2 filas por salón. No afecta al alumno porque
      `findForStudent` deduplica, pero son filas basura. La causa ya se arregló.
- [ ] **Sin validación de choques de horario.** La única regla es
      `end_time > start_time`. Se pueden crear clases encimadas o un maestro en
      dos salones a la vez — y ahora es más fácil, porque un clic crea N días.
- [ ] **Sin endpoints de asignación.** Se puede asignar contenido a un salón pero
      no listar ni quitar asignaciones. No hay forma de mover una pieza de un
      salón a otro sin recrearla.
- [x] ~~**`show-sql` y `format_sql` activos en producción.**~~ — apagados el
      15-sep. Ahora es `${SHOW_SQL:false}`; en local se enciende con la
      variable. Ver seccion 4c.
- [ ] **Mis Contenidos no distingue salón.** Con varios salones del mismo maestro,
      las piezas se agrupan solo por materia y no se sabe cuál es de cuál grupo.
- [ ] **Sin vista de avance del curso** para el maestro. La Libreta da
      calificaciones, no % de avance por alumno ni dónde se atoró el grupo.
- [x] ~~**Bundle del frontend:** 3.47 MB contra presupuesto de 2 MB.~~ — medido
      el 15-sep: **1.45 MB** iniciales (293 kB transferidos), dentro del
      presupuesto. El dato viejo ya no aplica.
- [ ] **Notificaciones** (`722b56d`): desplegadas pero nunca probadas.
- [ ] **Datos de prueba de Emily**: tiene 10/10 y XP de una entrega donde se
      pegaron las instrucciones para probar. Sus números no reflejan trabajo real.

### Ideas en la mesa
- **Temario intermedio de IA para Niños**: 17 piezas ya diseñadas y aprobadas,
  pendientes de cargar. Decisiones abiertas: materia nueva vs continuación, y a
  qué salón se asigna.
- Renombrar "Mis Misiones" → "Mis Retos" para el alumno, porque hoy "Misión"
  significa dos cosas: la página y uno de los cinco tipos.
- *Lurking mode* de JaaS, para que el supervisor no sea visible en la llamada.
- Fallback de modelo: si Groq da 404, pedir el catálogo y elegir uno vigente en
  vez de tirar el bot.
- Historial de intentos: `submissions` guarda todos, pero solo se muestra el último.
- Validar en backend que no se reenvíe una entrega ya aprobada (hoy es solo UI).
