# Estado del proyecto — ByteKids

> Bitácora para retomar el trabajo desde otra computadora.
> **Última actualización: 27 de agosto de 2026.**

---

## 1. Cómo está montado esto

**No hay un repo único.** La carpeta `Github-Bytekids/` es solo un contenedor y **no está versionada**. Dentro hay **dos repos independientes**:

| Carpeta | Repo | Qué es |
|---|---|---|
| `bytekids-api/` | `samtriani/bytekids-api` | Spring Boot + PostgreSQL |
| `bytekids-ui/` | `samtriani/bytekids-ui` | Angular (standalone components) |

Cada uno tiene sus ramas `dev` y `main`. Hay que hacer `git pull` en **cada carpeta por separado** — un `git status` en la raíz dice "not a git repository" y eso es normal.

### Dónde vive cada cosa

| | URL | Deploy |
|---|---|---|
| API | https://bytekids-api.fly.dev | **Manual** con flyctl |
| UI | https://bytekids-ui.vercel.app | **Automático** al hacer push |

- La API tiene `context-path: /api`. Todo endpoint va con ese prefijo: `https://bytekids-api.fly.dev/api/...`. Sin él da 404.
- Swagger: https://bytekids-api.fly.dev/api/swagger-ui/index.html
- Health: https://bytekids-api.fly.dev/api/actuator/health
- Vercel despliega `main` a producción y `dev` como preview, solo. La API **no** se despliega sola.

### Flujo de trabajo que se ha venido usando

```bash
# 1. trabajar en dev
git add -A && git commit -m "..."
git push origin dev

# 2. subir a produccion
git checkout main
git merge dev --no-ff
git push origin main
git checkout dev

# 3. solo si cambio el backend
cd bytekids-api
flyctl deploy --remote-only -a bytekids-api
```

**Ojo:** las máquinas de Fly tienen `min_machines_running = 0`. El primer request después de un rato tarda ~40s en frío. No es que esté caída.

---

## 2. Dónde quedé

Último commit en cada repo:

- **API** — `b25c1ac` "Restringe la creacion de cuentas privilegiadas a usuarios dueño"
- **UI** — `ba9de3f` "Pantalla de alta de coordinadores, visible solo para dueños"

Ambos ya están en `dev` **y** en `main`, y ambos servicios están desplegados con ese código (**Fly v8**).

### Lo que se construyó en esta tanda

**a) Supervisión de clases en vivo.** El administrador ya puede entrar a la videollamada de cualquier clase activa.

- Backend: `GET /api/sessions/live` (solo ADMIN/DIRECTOR) lista las clases transmitiendo ahora.
- Se abrió `jaas-token`, `attendance`, y la **lectura** de chat y misión a ADMIN/DIRECTOR.
- **No se tocó `join()`**: el admin obtiene el token sin registrarse en `class_sessions`, así que no cuenta como asistencia. Ojo: eso no lo hace invisible — en Jitsi **sí lo ven** los participantes. Entra con mic y cámara apagados y un modal se lo advierte antes.
- Pantallas: `/admin/live` y `/admin/classroom/:scheduleId`, más `/administrator/live` y `/administrator/classroom/:scheduleId`. Son los **mismos componentes**: detectan el módulo por la URL y ajustan menú, rol del shell y navegación de regreso.

**b) Arreglo del layout del shell.** Había una pantalla en blanco completa encima del contenido de **todas** las páginas. Ver sección 4, porque tiene truco.

**c) Candado de cuentas privilegiadas.** Ver sección 3.

---

## 3. Quién puede crear coordinadores

En la base de datos el rol se llama **`admin`**; "Coordinador" es solo la etiqueta que le pone el frontend. El enum es `admin, director, teacher, student, parent`.

Antes, **cualquier** coordinador podía crear más coordinadores y —peor— usar `PUT /users/{id}` para cambiarle la contraseña a otro coordinador y quedarse con su cuenta.

Ahora [`OwnershipService`](src/main/java/mx/bytekids/academy/service/OwnershipService.java) exige ser **dueño** para:

- crear una cuenta con rol `admin` o `director`
- cambiarle el rol a una cuenta para volverla privilegiada
- modificar o desactivar una cuenta que **ya** es privilegiada

La lista de dueños **no está en la base de datos** (el proyecto no tiene migraciones, ver sección 4). Va por variable de entorno:

```bash
flyctl secrets set OWNER_USERNAMES="samuel.partida,otro.usuario" -a bytekids-api
```

Hoy **no** está puesta como secret en Fly: toma el default de `application.yml`, que es `samuel.partida`. Se puede confirmar en los logs de arranque:

```
✅ Dueños autorizados para crear cuentas privilegiadas: [samuel.partida]
```

Si la lista queda vacía o mal escrita, **nadie** podrá crear ni tocar cuentas privilegiadas. No se pierde nada, pero hay que corregir la variable y reiniciar.

**Pantalla:** 🔑 Coordinadores, al final del menú del módulo Coordinador (`/administrator/staff`). Solo la ven los dueños.

---

## 4. Trampas de este proyecto

Cosas que ya costaron tiempo. Vale la pena leerlas antes de tocar código.

### El shell NO tiene `<ng-content>`

`ShellComponent` **no proyecta contenido**. El patrón de las 40+ páginas es:

```html
<app-shell role="admin" [userName]="..." [navItems]="..."></app-shell>

<div class="page-wrap">
  ...aquí va el contenido...
</div>
```

El `<app-shell>` va **autocerrado** y el contenido es un **hermano**. Si metes el contenido dentro del componente, Angular lo descarta **en silencio** y la página sale vacía sin ningún error.

### El layout se posiciona con variables, no con flujo

`.page-wrap` (y sus equivalentes `.projects-page`, `.ai-page`, `.msg-layout`, `.s-dashboard`, `.app-shell-content`…) se posicionan con `margin-left: var(--sw)` y `margin-top: var(--th)`, asumiendo que el sidebar y el topbar son **fijos**.

`--sw: 265px` y `--th: 62px` en `styles.scss` **deben coincidir con el cromo real que pinta el shell**. Si cambias el ancho del sidebar o el alto del topbar, cambia también las variables o todas las páginas se descuadran.

### Sin migraciones de base de datos

`ddl-auto: none` y **no hay Flyway ni Liquibase**. Los cambios de esquema son scripts SQL en `src/main/resources/` que se corren **a mano** contra Postgres.

Consecuencia: **agregar una columna a una entidad rompe el arranque** si no corriste el SQL antes. Por eso el candado de dueños se resolvió con variable de entorno en vez de una columna `is_owner`.

### Los archivos tienen CRLF

Scripts de búsqueda y reemplazo que asuman `\n` no hacen match. Un `replace()` que falla en silencio combinado con abrir el archivo en modo escritura **lo deja vacío**. Ya pasó una vez.

---

## 5. Pendientes de probar

Nada de esto está verificado con usuarios reales. Compila y despliega, pero eso no prueba que funcione.

- [ ] **Token JaaS para admin/director.** Es el pendiente más importante. `JaasTokenService` ya daba `moderator: true` a esos roles, pero **nunca se había ejercido** porque el endpoint no los dejaba pedir token. Si 8x8 lo rechaza, se verá como *"No se pudo obtener el acceso a la videollamada"* en la vista de observador.
- [ ] **Candado de dueños.** Necesita **dos sesiones**: la tuya y una de coordinador no-dueño, para comprobar que a la segunda sí la rechaza al intentar crear un coordinador.
- [ ] **Layout arreglado.** Recargar con `Ctrl+Shift+R` (el CSS viejo se queda cacheado) y confirmar que el espacio en blanco desapareció. El contenido ahora empieza 13px más a la derecha y 6px más arriba — eso es la corrección, no un error.
- [ ] **Alta de Monze** como coordinadora desde la pantalla nueva.

### ⚠️ Paso obligatorio antes de probar

El flag `owner` se guarda en `localStorage` al iniciar sesión. **Las sesiones abiertas antes de este cambio no lo tienen.**

**Hay que cerrar sesión y volver a entrar una vez**, o la pantalla de Coordinadores no aparece aunque seas dueño.

---

## 6. Ideas que quedaron en la mesa

- **Modo *lurking* de JaaS**, para que el supervisor entre a la videollamada sin que los participantes lo vean. Hoy sí lo ven.
- **Notificaciones**: la feature (`722b56d`) llegó a `main` en el merge del 27 de agosto. Está desplegada pero **no se probó**.
- **Bundle del frontend**: 3.3 MB contra un presupuesto de 2 MB. El warning de `ng build` es viejo y no es de esta tanda, pero ahí sigue.
