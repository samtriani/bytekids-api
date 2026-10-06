# ByteKids API — notas para trabajar aquí

Spring Boot 4 · Java 21 · Maven · Postgres (Neon) · desplegada en Fly.io.

Es el backend de ByteKids Academy, una plataforma de clases de IA para niños.
Los roles son `admin` (coordinación), `director`, `teacher`, `student` y
`parent`, y casi toda regla de negocio depende de cuál pregunta.

---

## Lo que muerde si no lo sabes

### `mvn clean compile`, nunca el incremental

El incremental reportó `BUILD SUCCESS` sobre código que no compilaba: reusó
clases viejas. Perdí media hora persiguiendo un error que no existía en el
código que creía estar viendo. **Siempre `clean`.**

### No metas un método justo debajo de un `@Transactional`

La anotación se queda con el método nuevo y el de abajo pierde la suya.
Compila perfecto y no se nota hasta que algo falla a la mitad. Pasó dos veces
de verdad:

- `QuizService.submitAttempt` — escribe intento, respuestas, entrega y XP.
- `UserService.create` — estuvo días en producción sin transacción.

Si insertas algo cerca de una anotación, revisa después a qué método quedó
pegada.

### No hay Flyway, y no hay acceso a la base desde aquí

`ddl-auto: none`. Todo cambio de esquema o de datos es un script en `sql/`
que **el dueño corre a mano en Neon**. Claude no tiene credenciales ni
`flyctl ssh` (lo bloquea el clasificador de permisos), así que un cambio de
datos se entrega como SQL, no se aplica.

Los scripts de `sql/` son idempotentes a propósito y traen un PASO de
inventario al inicio: primero se mira, luego se escribe.

### Las máquinas de Fly se suspenden: ningún cron corre

`auto_stop_machines = 'suspend'` con `min_machines_running = 0`. Un
`@Scheduled` de las 3 de la mañana **no se ejecuta nunca**, y con dos
máquinas podría ejecutarse dos veces.

Por eso la purga de notificaciones cuelga de la consulta del panel
(`NotificationService.findByRecipient`) y no de una tarea programada.

### Neon y Fly duermen: la base puede no responder

Neon cierra las conexiones al dormirse y Fly congela la máquina. El pool
(`spring.datasource.hikari` en `application.yml`) usa conexiones de vida corta,
se rinde en 15 s y el fallo de conexión sale como **503**, que el front
reintenta solo (`GlobalExceptionHandler.esBaseDespertando`). Las variables son
`HIKARI_*`: en Fly hay secretos `POOL_*` viejos que ya no se leen.

**Un fallo de la base nunca debe convertirse en 401.** Hasta el 6-oct,
`JwtAuthFilter` se tragaba cualquier error al leer al usuario y la petición
seguía sin sesión: 401 y el front sacaba al niño al login al enviar su quiz.
`JwtAuthFilterTest` lo cuida.

---

## Decisiones que conviene respetar

### `ContentResponse.from()` es el único filtro de contenido

`content_body` mezcla lo que ve el alumno con lo que es del maestro
(`expected_output`, `solution_check`, `teacher_notes`). `cuerpoVisible()` las
quita según el rol, y es el **único** punto por el que sale todo el contenido.

Si necesitas el cuerpo en otra respuesta, pásalo por `ContentResponse.from()`
en vez de copiar `getContentBody()`. Así se hace en
`ClassSessionService.buildMissionResponse`.

### Quién le puede escribir a quién vive en un solo lugar

`MessageService.contactosPermitidos()`. Esa misma lista alimenta el selector
(`GET /messages/contactos`) **y** valida el envío. Calcularlas por separado
terminaría con una dejando pasar lo que la otra no muestra, y el agujero
estaría del lado que no se ve.

Alumno → alumno no existe como regla. Es la forma de no abrir un chat libre
entre menores: no basta con que la pantalla no lo ofrezca.

### Quién puede ver a qué niño: `AccesoAlumnoService`

Toda ruta que reciba el id de un alumno pasa por `exigirPuedeVer(id)`:
coordinación y dirección, cualquiera; un alumno, a sí mismo; un papá, a sus
hijos; un maestro, a los alumnos de sus salones. Hasta el 30-sep,
`/progress/students/{id}/…`, `/achievements/students/{id}` y
`/users/{papá}/students` solo revisaban el rol. **Si agregas una ruta con
`{studentId}`, ponle esta revisión.**

Para la familia, mejor todavía: `/familia/hijos` no recibe ids y devuelve
los hijos de quien pregunta. Es lo que usa todo el módulo de papás.

### La Comunidad del maestro se acota a SUS salones

`ComunidadService` decide qué salones ve cada quien: un maestro, los que
tiene como titular (`classroom.teacher`, la misma regla que usa
`MessageService`); coordinación y dirección, todos. Pedir el id de un salón
ajeno da 403. `ComunidadServiceTest` lo cuida.

La felicitación ES la notificación (`referenceType = "felicitacion"`): para
saber si ya se felicitó un logro se busca esa notificación, sin tabla nueva.

### Quién necesita atención: `SeguimientoService`

`GET /seguimiento/salones/{id}` le da al panel del maestro el avance de cada
alumno y, aparte, una alerta con razón: sin empezar, atorado (corrección sin
reenviar o quiz reprobado varias veces), sin actividad o calificaciones bajas.
Antes se marcaba "Apoyo" a todo el que llevara menos de 40%: al inicio de un
curso, todo el grupo en rojo. Las reglas y sus umbrales viven en
`diagnosticar()` (pura, con pruebas). Ojo: un quiz reprobado queda `enviado`,
no `rechazado`. El permiso por salón es el de `ComunidadService.exigirSalonVisible`.

### El orden de las actividades: `DesbloqueoService`

Usa `mission_prerequisites`. Sin filas, todo abierto: los cursos de paga y
las misiones lanzadas en clase no cambian. Mi Primera IA trae una fila por
pieza (la genera `sql/mi_primera_ia/curso.py`).

Un prerrequisito cuenta como hecho al **entregarlo** (un quiz, al
aprobarlo), no al aprobarlo el maestro: si no, un niño que entrega el
viernes se queda atorado el fin de semana. La aprobación manda en logros y
certificado. La misma regla alimenta el feed, `GET /content/{id}` y las dos
entregas (`SubmissionService.submit`, `QuizService.submitAttempt`).

### El certificado se pide y se entrega

`CertificadoService`. Se puede pedir con **todas** las actividades de la
materia aprobadas; lo entrega un maestro de sus salones o coordinación, y
entonces le llega al alumno y a su familia. El alumno y la familia no lo ven
antes de entregarse. Tabla `certificados`, folio único sin 0/O/1/I/L.

El QR del certificado abre `/verificar/:folio` en la UI, que pide
`GET /certificados/verificar/{folio}`: **público**, sin sesión (está en
`PUBLIC_ENDPOINTS`). Como es la página de un menor, solo da el nombre con la
inicial del apellido ("Maria Z."), el curso y la fecha, y solo de certificados
entregados. No le agregues datos sin pensarlo dos veces.

### ByteBot tiene tope diario para alumnos

`LimiteByteBotService`: 25 mensajes al día (`app.ai.limite-diario-alumno`),
hora de México, en la tabla `ai_uso_diario`. Sin contador visible: al
acabarse, ByteBot contesta `AiTutorService.SIN_BATERIA`. Si la tabla no
existe, deja pasar: un tope que tumba a ByteBot es peor que no tener tope.

### Las notificaciones van en su propia transacción

`NotificationService.avisar()` y `avisarATodos()` usan `REQUIRES_NEW`.
Uniéndose a la transacción del llamador, atrapar la excepción no basta: queda
marcada para rollback y el commit truena después. Un aviso fallido habría
tumbado la entrega del alumno.

`avisarATodos` también se anota porque llama a `avisar()` desde dentro de la
misma clase, y esa llamada no pasa por el proxy de Spring.

### 401 y 403 significan cosas distintas

401 = no hay sesión → el front cierra sesión. 403 = la sesión vale pero el rol
no alcanza → **no** se cierra sesión. Antes cualquier 403 sacaba al usuario al
login sin explicación.

### El XP se paga una sola vez

Siempre contra la `Submission` y verificando `xp_events` por
`referenceId` + `referenceType`, no contra el estado anterior. Así
aprobar → pedir correcciones → aprobar tampoco paga dos veces.

---

## Desplegar

```bash
mvn -q clean compile          # verificar
flyctl deploy --remote-only   # desplegar
```

Ramas: se trabaja en `dev`, se mezcla a `main` con `--no-ff`. El despliegue a
Fly es manual y no lo dispara git.

Comprobación rápida de que quedó viva — debe dar 401, no 500 ni 000:

```bash
curl -s -o /dev/null -w "%{http_code}\n" https://bytekids-api.fly.dev/api/notifications
```

---

### Nunca devuelvas una entidad que traiga un `User` de otra persona

Hasta el 29-sep, `/messages/*` devolvía la entidad `Message` con el `User`
completo de las dos personas, **incluido el hash de la contraseña**: un
alumno que abría su bandeja recibía el de su maestra, más correo, edad y
dirección.

Quedó cerrado en dos capas:

- `User.passwordHash` lleva `@JsonIgnore`. Cubre cualquier entidad cruda que
  todavía salga por la API.
- Los mensajes salen por `MessageResponse`, que de cada persona solo trae
  nombre, rol, iniciales y robot. Tampoco el `username`: es media credencial.

`UserSerializacionTest` lo cuida, y usa el **Jackson 3** (`tools.jackson`)
que usa Spring MVC. En el classpath también está el Jackson 2 que trae jjwt:
probar con ese no prueba nada.

### El texto de una entrega se acota según quién pregunta

`SubmissionResponse.from()` devuelve el texto completo — el alumno lo necesita
porque lo reenvía desde ahí. `SubmissionResponse.resumen()` lo acota, y es el
que usan la Libreta y los listados del maestro.

Sin eso, abrir la Libreta descargaba megabytes para pintar una tabla y se
congelaba. Si agregas un endpoint que liste entregas de varios alumnos, usa
`resumen()`.

---

## Deuda conocida

- Sin registro de consentimiento ni de privacidad para menores.
- **Cambiar la contraseña no invalida los tokens ya emitidos.** No hay
  lista negra y el JWT dura 7 días, así que quien tuviera una sesión abierta
  con la contraseña vieja sigue dentro hasta que ese token expire. Para
  echar a alguien de verdad hoy hay que desactivar la cuenta.
- 51 pruebas en 12 clases, sobre todo reglas de acceso y de negocio
  (comunidad, desbloqueo, certificados, tope de ByteBot). No hay pruebas de
  integración contra la base.
- **Todavía hay 13 tipos de entidad que salen crudos** (notificaciones,
  intentos de quiz, logros, avance, XP...). El hash ya no sale, pero el correo,
  la edad y la dirección del alumno siguen viajando dentro de esas respuestas.
  La mayoría llegan solo a quien es su dueño o al personal, pero hay que
  pasarlas a DTOs como se hizo con `MessageResponse`.
- El evaluador de logros no implementa `ai_conversations`: ByteBot no guarda
  las conversaciones. "AI Explorer" se apaga con
  `sql/2026-09-29_apagar_ai_explorer.sql`. Si agregas un `condition_type`
  nuevo, va en `AchievementCheckerService.evaluate()` Y en la lista de la
  verificación de ese script: un logro con condición desconocida no truena,
  simplemente nadie lo gana nunca.
- Las notificaciones son dentro de la plataforma: no hay correo. Si el niño no
  entra, no se entera. A la familia le llegan los logros de su hijo, su
  certificado y cada calificación (`SubmissionService.avisarALaFamilia`,
  `referenceType "trabajo_hijo"` con el id del hijo), pero solo dentro de la
  plataforma.
