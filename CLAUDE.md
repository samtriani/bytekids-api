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
- Los tests son uno solo (`LoginRateLimitFilterTest`): hay arranque, no red.
- El evaluador de logros no implementa `ai_conversations`, así que el logro
  "AI Explorer" es inalcanzable por ahora.
- Las notificaciones son dentro de la plataforma: no hay correo. Si el niño no
  entra, no se entera — y al familiar solo le llegan mensajes, no las
  calificaciones ni los logros de su hijo.
