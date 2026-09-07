# Estado del proyecto — ByteKids

> Bitácora para retomar el trabajo desde otra computadora.
> **Última actualización: 7 de septiembre de 2026.**

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

- **API** — "Merge dev: guia del maestro y filtrado de content_body"
- **UI** — "Merge dev: guia del maestro en Mis Contenidos"

Ambos en `dev` **y** `main`, ambos desplegados. El último despliegue de UI se
verificó buscando la cadena `Buscando en todos los ciclos` dentro del bundle
servido en producción.

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

La barra de "Despertando el servidor" que se agregó el 1-sep vive en el shell, y
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

### Pendiente de seguridad
- [ ] **Volver el repo privado.** Aunque las credenciales viejas ya no sirven,
      el historial público conserva el esquema completo de la base.

---

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
- [ ] **`show-sql: true` y `format_sql: true` activos en producción.** Formatea e
      imprime cada consulta: cuesta rendimiento y ahoga los logs.
- [ ] **Mis Contenidos no distingue salón.** Con varios salones del mismo maestro,
      las piezas se agrupan solo por materia y no se sabe cuál es de cuál grupo.
- [ ] **Sin vista de avance del curso** para el maestro. La Libreta da
      calificaciones, no % de avance por alumno ni dónde se atoró el grupo.
- [ ] **Bundle del frontend:** 3.47 MB contra presupuesto de 2 MB.
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
