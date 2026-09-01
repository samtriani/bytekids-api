# Estado del proyecto — ByteKids

> Bitácora para retomar el trabajo desde otra computadora.
> **Última actualización: 1 de septiembre de 2026.**

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

- **API** — `9408ae5` "Merge dev: modelo hibrido de autoria de contenido"
- **UI** — `082e57c` "Merge dev: UI del modelo hibrido"

Ambos en `dev` **y** `main`, ambos desplegados.

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

## 4. Qué se construyó (27-ago → 1-sep)

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

### Cuidado con lo que se le manda al alumno
Dos fugas ya ocurridas: `expected_output` visible en el workspace, y `isCorrect` que habría viajado en las opciones del quiz. Antes de exponer un campo nuevo, pregúntate si contiene la respuesta.

---

## 7. Pendientes

### Sin probar con usuarios reales
- [ ] **Token JaaS para admin/director.** `JaasTokenService` ya daba `moderator: true` a esos roles, pero nunca se había ejercido. Si 8x8 lo rechaza: *"No se pudo obtener el acceso a la videollamada"*.
- [ ] **Candado de dueños.** Necesita dos sesiones: la tuya y una de coordinador no-dueño.
- [ ] **Modelo híbrido.** Verificar que Laura vea 🏛️ Plan base sin botones, y que coordinación pueda agregar piezas desde Materias.
- [ ] **Quiz.** Contestarlo completo y ver que califique. Da 40 XP y se califica solo.

### Deuda conocida
- [ ] **Asignaciones duplicadas** en `content_assignments`: el contenido creado antes del 1-sep tiene 2 filas por salón (create auto-asignaba + la UI llamaba assign). No afecta al alumno porque `findForStudent` deduplica, pero son filas basura.
- [ ] **Sin validación de choques de horario.** La única regla es `end_time > start_time`. Se pueden crear clases encimadas o un maestro en dos salones a la vez.
- [ ] **Sin vista de avance del curso** para el maestro. La Libreta da calificaciones, pero no % de avance por alumno ni dónde se atoró el grupo.
- [ ] **Bundle del frontend:** 3.42 MB contra presupuesto de 2 MB.
- [ ] **Notificaciones** (`722b56d`): desplegadas pero nunca probadas.

### Ideas en la mesa
- Renombrar "Mis Misiones" → "Mis Retos" para el alumno.
- *Lurking mode* de JaaS, para que el supervisor no sea visible en la videollamada.
- Fallback de modelo: si Groq da 404, pedir el catálogo y elegir uno vigente en vez de tirar el bot.
- Historial de intentos: `submissions` guarda todos, pero solo se muestra el último.
- Validar en backend que no se reenvíe una entrega ya aprobada (hoy el candado es solo de UI).
