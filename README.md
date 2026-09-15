# ByteKids API

Backend de la plataforma ByteKids Academy. Spring Boot 3 + PostgreSQL.

> 📌 **¿Retomando el trabajo?** Los pasos de aquí son para *levantar* el
> proyecto. Para saber **en qué se quedó**, qué está a medias y qué trampas
> tiene este código, lee **[`ESTADO.md`](ESTADO.md)** — sobre todo la sección
> "Trampas de este proyecto". Te ahorra tardes.

---

## Antes de nada: dos cosas que rompen el arranque

**1. No hay migraciones.** `ddl-auto: none` y no hay Flyway ni Liquibase. Los
cambios de esquema son SQL a mano, en `sql/`. Si agregas un campo a una entidad
**sin correr el SQL primero, la aplicación no arranca**. El orden correcto
siempre es: `ALTER TABLE` primero, desplegar después.

**2. No hay valores por defecto reales.** Si falta un secreto, la app **falla
al arrancar** a propósito, en vez de caer en silencio a la base de producción.
Este repo es público; esa decisión es deliberada.

---

## Requisitos

| | Versión | Comprobar |
|---|---|---|
| JDK | 21 | `java -version` |
| Maven | 3.9+ | `mvn -version` |
| PostgreSQL | 14+ | una base local, o la de Neon |

No hay Maven Wrapper en el repo. Si no tienes `mvn` instalado, la vía rápida
es descargar el binario y usarlo desde donde quedó, sin instalar nada:

```bash
curl -sSL -o maven.zip https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip
unzip -q maven.zip
./apache-maven-3.9.9/bin/mvn -version
```

---

## Levantarlo en local

### 1. Configura los secretos

Crea `src/main/resources/application-local.yml` — **ya está en `.gitignore`**,
no lo subas:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/bytekids
    username: postgres
    password: tu-password
  jpa:
    show-sql: true          # en local sí conviene verlas

app:
  jwt:
    # Mínimo 32 caracteres o la app no arranca. 64+ para que use HS512.
    secret: pon-aqui-una-cadena-larga-y-aleatoria-de-al-menos-64-caracteres
  cors:
    allowed-origins: http://localhost:4200
```

O por variables de entorno, si prefieres:

```bash
export BK_DB_URL=jdbc:postgresql://localhost:5432/bytekids
export BK_DB_USER=postgres
export BK_DB_PASSWORD=tu-password
export JWT_SECRET=una-cadena-larga-y-aleatoria
```

### 2. Arranca

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### 3. Comprueba que está vivo

```
Health:  http://localhost:8080/api/actuator/health
Swagger: http://localhost:8080/api/swagger-ui/index.html
```

> ⚠️ **Todo va con el prefijo `/api`.** Es el `context-path`. Sin él, cualquier
> endpoint devuelve 404 y parece que la ruta no existe.

---

## Comandos

```bash
mvn clean package      # compila, corre las pruebas y arma el jar
mvn test               # solo las pruebas
mvn clean compile      # solo compilar

java -jar target/bytekids-academy-api-1.0.0.jar    # correr el jar ya armado
```

Las pruebas viven en `src/test` y **no necesitan base de datos**: son unitarias
a propósito, para que `mvn package` funcione en cualquier lado — incluido el
build de Docker, que ya no las salta.

---

## Variables de entorno

### Obligatorias — sin estas no arranca

| Variable | Qué es |
|---|---|
| `BK_DB_URL` | JDBC de PostgreSQL |
| `BK_DB_USER` | Usuario de la base |
| `BK_DB_PASSWORD` | Contraseña de la base |
| `JWT_SECRET` | Firma de los tokens. **Mínimo 32 caracteres** |

### Opcionales — tienen default

| Variable | Default | Qué hace |
|---|---|---|
| `CORS_ORIGINS` | `http://localhost:4200` | Orígenes permitidos, separados por coma |
| `OWNER_USERNAMES` | `samuel.partida` | Únicos que pueden crear cuentas `admin`/`director`. **Si queda vacía o mal escrita, nadie puede tocarlas** |
| `SHOW_SQL` | `false` | Imprime y formatea cada consulta. Solo para depurar |
| `LOGIN_MAX_INTENTOS` | `10` | Fallos de login tolerados por IP |
| `LOGIN_VENTANA_MINUTOS` | `15` | Ventana del contador anterior |
| `JWT_EXPIRATION_MS` | `604800000` | 7 días |
| `AI_BASE_URL` | LM Studio local | En producción apunta a Groq |
| `AI_MODEL` | — | Groq **jubila modelos**; un 404 del bot casi siempre es esto |
| `AI_API_KEY` | vacío | Sin llave usa LM Studio local |
| `JAAS_APP_ID` / `JAAS_KEY_ID` / `JAAS_PRIVATE_KEY` | — | Videollamadas Jitsi. Ojo: `JAAS_KEY_ID` es **solo el sufijo**, no el ID completo (ver `ESTADO.md`) |

En arranque, el log confirma lo que quedó cargado:

```
✅ Dueños autorizados para crear cuentas privilegiadas: [samuel.partida]
🤖 ByteBot LLM → ☁️  GROQ (cloud) | URL: ... | Modelo: ...
```

---

## Desplegar

El deploy de la API es **manual**, a diferencia del frontend (que Vercel
despliega solo al hacer push a `main`).

```bash
flyctl deploy --remote-only -a bytekids-api
```

Los secretos viven solo en Fly, nunca en el repo:

```bash
flyctl secrets list -a bytekids-api
flyctl secrets set NOMBRE="valor" -a bytekids-api     # reinicia la app
flyctl logs -a bytekids-api
```

**Si el cambio toca el esquema de la base, corre el SQL ANTES de desplegar.**
Invertir el orden tumba toda consulta de esa tabla y nadie entra.

### Verificar que quedó

```bash
curl https://bytekids-api.fly.dev/api/actuator/health
# {"status":"UP"} — sin detalles, es público a propósito
```

Un truco que ya sirvió: para comprobar que una columna nueva existe, manda un
login con credenciales falsas. Un **401** prueba que la consulta corrió; un
**500** significa que la columna no está.

### Arranque en frío

`auto_stop_machines = 'suspend'` congela la RAM en vez de apagar, así que
despertar toma ~1-2 s en lugar de ~40 s. Neon también se duerme. Quitar la
espera del todo sería `min_machines_running = 1` (~5-6 USD/mes) y aun así Neon
seguiría durmiendo.

---

## Estructura

```
src/main/java/mx/bytekids/academy/
├── config/       SecurityConfig, CORS, OpenAPI
├── controller/   Endpoints REST
├── dto/          Request/Response  ← aquí se filtra lo que NO ve el alumno
├── entity/       JPA
├── exception/    GlobalExceptionHandler
├── repository/   Spring Data
├── security/     JWT, filtro de login, utilidades
└── service/      Lógica de negocio

sql/              Cambios de esquema y carga de contenido, a mano
```

> 🔒 **Antes de exponer un campo nuevo en un DTO, pregúntate si contiene la
> respuesta.** Ya hubo dos fugas: `expected_output` visible en el workspace del
> alumno, y el `isCorrect` de las opciones del quiz. Por eso el filtrado vive
> en `ContentResponse.cuerpoVisible` y no en cada endpoint: es el único punto
> por el que sale todo el contenido, así que en un endpoint nuevo no se olvida.
