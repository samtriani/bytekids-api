-- ============================================================================
--  ByteKids Academy - Correo electronico en usuarios
--  Fecha: 8-sep-2026
--
--  POR QUE
--    La tabla users no guardaba correo. Sin el no hay recibo, ni bienvenida,
--    ni recordatorio de renovacion, ni "olvide mi contrasena": todo el ciclo
--    de vida de un cliente pasa por ahi. Es la pieza que desbloquea el resto
--    de la capa comercial, y por eso va primero.
--
--  ⚠️  ESTE SCRIPT VA ANTES DEL DESPLIEGUE
--    El proyecto usa ddl-auto: none, asi que Hibernate NO crea la columna.
--    Si se despliega el codigo nuevo sin correr esto, toda consulta de
--    usuarios truena con "column email does not exist" y nadie puede entrar.
--    Orden correcto:  1) este script   2) el despliegue del API.
--
--  DECISIONES
--    - Nullable. Los usuarios que ya existen no tienen correo y no se les va
--      a inventar uno; se pide cuando se editen.
--    - Unico, pero solo entre los que SI tienen. Un indice unico normal
--      trataria cada NULL como distinto en Postgres y dejaria pasar
--      duplicados en blanco, asi que va parcial (WHERE email IS NOT NULL).
--    - Se guarda en minusculas. "Ana@x.com" y "ana@x.com" son el mismo buzon
--      y no deben poder registrarse dos veces.
-- ============================================================================


-- ============================================================================
--  PASO 1 - ANTES (debe decir que la columna no existe)
-- ============================================================================

SELECT count(*) AS ya_existe_la_columna
FROM information_schema.columns
WHERE table_name = 'users' AND column_name = 'email';


-- ============================================================================
--  PASO 2 - EL CAMBIO
-- ============================================================================

BEGIN;

ALTER TABLE users ADD COLUMN IF NOT EXISTS email VARCHAR(255);

-- Unicidad solo entre los que tienen correo.
CREATE UNIQUE INDEX IF NOT EXISTS ux_users_email
  ON users (lower(email))
  WHERE email IS NOT NULL;

COMMENT ON COLUMN users.email IS
  'Correo de contacto. En alumnos suele ser el del padre o tutor. Se guarda en minusculas.';

COMMIT;


-- ============================================================================
--  PASO 3 - VERIFICACION
--  Deben salir la columna y el indice.
-- ============================================================================

SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'users' AND column_name = 'email';

SELECT indexname FROM pg_indexes
WHERE tablename = 'users' AND indexname = 'ux_users_email';

-- Cuantos usuarios quedan sin correo (normal al principio: todos).
SELECT role,
       count(*)                                  AS total,
       count(*) FILTER (WHERE email IS NULL)     AS sin_correo
FROM users
WHERE is_active
GROUP BY role
ORDER BY role;
