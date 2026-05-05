-- ============================================================
--  ByteKids Academy — DDL PostgreSQL
--  Target: Neon DB (PostgreSQL 15+)
--  Cubre: usuarios, salones, materias, contenido, misiones,
--         proyectos, quizzes, entregas, XP, logros,
--         notificaciones, mensajes y tutor IA.
-- ============================================================

-- gen_random_uuid() es nativo en PG13+, no requiere extensión.
-- Si tu proyecto necesita otras funciones crypto descomenta:
-- CREATE EXTENSION IF NOT EXISTS pgcrypto;


-- ============================================================
--  ENUMS
-- ============================================================

CREATE TYPE user_role AS ENUM (
  'admin', 'director', 'teacher', 'student', 'parent'
);

CREATE TYPE content_type AS ENUM (
  'mision', 'tarea', 'quiz', 'proyecto', 'material'
);

CREATE TYPE difficulty_level AS ENUM (
  'facil', 'medio', 'dificil'
);

CREATE TYPE submission_status AS ENUM (
  'borrador', 'enviado', 'revisado', 'aprobado', 'rechazado'
);

CREATE TYPE xp_reason AS ENUM (
  'mision_completada',
  'proyecto_completado',
  'quiz_completado',
  'logro_desbloqueado',
  'racha_bonus',
  'premio_maestro',
  'penalizacion'
);

CREATE TYPE achievement_category AS ENUM (
  'programacion', 'racha', 'proyectos', 'social', 'especial'
);

CREATE TYPE rarity_level AS ENUM (
  'comun', 'poco_comun', 'raro', 'epico', 'legendario'
);

CREATE TYPE notification_type AS ENUM (
  'alerta_inactividad',
  'mision_asignada',
  'proyecto_asignado',
  'logro_desbloqueado',
  'mensaje',
  'calificacion',
  'sistema'
);

CREATE TYPE question_type AS ENUM (
  'opcion_multiple', 'verdadero_falso', 'respuesta_corta'
);

CREATE TYPE ai_context_type AS ENUM (
  'mision', 'proyecto', 'general', 'padre', 'maestro'
);

CREATE TYPE message_role AS ENUM (
  'user', 'assistant'
);


-- ============================================================
--  TRIGGER: actualiza updated_at automáticamente
-- ============================================================

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- ============================================================
--  USUARIOS
-- ============================================================

CREATE TABLE users (
  id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  username      VARCHAR(50)  NOT NULL UNIQUE,
  password_hash TEXT         NOT NULL,
  display_name  VARCHAR(100) NOT NULL,
  role          user_role    NOT NULL,
  initials      VARCHAR(5),
  avatar_url    TEXT,
  age     SMALLINT,
  address TEXT,
  is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_role     ON users(role);
CREATE INDEX idx_users_username ON users(username);

CREATE TRIGGER trg_users_updated_at
  BEFORE UPDATE ON users
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ============================================================
--  ESTRUCTURA ACADÉMICA
-- ============================================================

CREATE TABLE subjects (
  id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  name        VARCHAR(50) NOT NULL UNIQUE,
  icon        VARCHAR(10),
  color       VARCHAR(7),           -- hex: '#06B6D4'
  description TEXT,
  is_active   BOOLEAN     NOT NULL DEFAULT TRUE,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


CREATE TABLE IF NOT EXISTS classroom_subjects (
    classroom_id UUID NOT NULL REFERENCES classrooms(id) ON DELETE CASCADE,
    subject_id   UUID NOT NULL REFERENCES subjects(id)   ON DELETE CASCADE,
    PRIMARY KEY (classroom_id, subject_id)
);
-- -------------------------------------------------------

CREATE TABLE classrooms (
  id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  name        VARCHAR(200) NOT NULL,             -- '4°A'
  grade_level SMALLINT    NOT NULL,             -- 4
  section     VARCHAR(5)  NOT NULL,             -- 'A'
  description TEXT,
  teacher_id  UUID        REFERENCES users(id) ON DELETE SET NULL,
  school_year VARCHAR(9)  NOT NULL DEFAULT '2025-2026',
  is_active   BOOLEAN     NOT NULL DEFAULT TRUE,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (grade_level, section, school_year)
);

CREATE INDEX idx_classrooms_teacher ON classrooms(teacher_id);

CREATE TRIGGER trg_classrooms_updated_at
  BEFORE UPDATE ON classrooms
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- -------------------------------------------------------

-- Inscripción de alumno a salón
CREATE TABLE classroom_enrollments (
  id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  student_id   UUID        NOT NULL REFERENCES users(id)      ON DELETE CASCADE,
  classroom_id UUID        NOT NULL REFERENCES classrooms(id) ON DELETE CASCADE,
  enrolled_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  is_active    BOOLEAN     NOT NULL DEFAULT TRUE,
  UNIQUE (student_id, classroom_id)
);

CREATE INDEX idx_enrollments_student   ON classroom_enrollments(student_id);
CREATE INDEX idx_enrollments_classroom ON classroom_enrollments(classroom_id);

-- -------------------------------------------------------

-- Relación padre ↔ alumno (un padre puede tener N hijos)
CREATE TABLE parent_student (
  id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  parent_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  student_id        UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  relationship_type VARCHAR(20) NOT NULL DEFAULT 'padre',   -- 'padre','madre','tutor'
  created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (parent_id, student_id)
);

CREATE INDEX idx_parent_student_parent  ON parent_student(parent_id);
CREATE INDEX idx_parent_student_student ON parent_student(student_id);


-- ============================================================
--  CONTENIDO (misiones, proyectos, quizzes, tareas, material)
-- ============================================================

CREATE TABLE content (
  id                UUID             PRIMARY KEY DEFAULT gen_random_uuid(),
  title             VARCHAR(200)     NOT NULL,
  description       TEXT,
  type              content_type     NOT NULL,
  subject_id        UUID             REFERENCES subjects(id) ON DELETE SET NULL,
  created_by        UUID             NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
  xp_reward         SMALLINT         NOT NULL DEFAULT 50 CHECK (xp_reward >= 0),
  difficulty        difficulty_level NOT NULL DEFAULT 'medio',
  estimated_minutes SMALLINT         CHECK (estimated_minutes > 0),
  -- JSONB flexible por tipo:
  --   mision:   { "instructions","starter_code","solution_check","expected_output" }
  --   proyecto: { "checklist": ["item1","item2"] }
  --   material: { "url","resource_type": "video|documento|enlace" }
  --   tarea:    { "instructions" }
  content_body      JSONB,
  order_index       SMALLINT,        -- posición en el currículo de la materia
  is_published      BOOLEAN          NOT NULL DEFAULT FALSE,
  is_active         BOOLEAN          NOT NULL DEFAULT TRUE,
  created_at        TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
  updated_at        TIMESTAMPTZ      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_content_type       ON content(type);
CREATE INDEX idx_content_subject    ON content(subject_id);
CREATE INDEX idx_content_created_by ON content(created_by);

CREATE TRIGGER trg_content_updated_at
  BEFORE UPDATE ON content
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- -------------------------------------------------------

-- Prerequisitos entre misiones (cadena de desbloqueo)
CREATE TABLE mission_prerequisites (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  mission_id      UUID NOT NULL REFERENCES content(id) ON DELETE CASCADE,
  prerequisite_id UUID NOT NULL REFERENCES content(id) ON DELETE CASCADE,
  UNIQUE (mission_id, prerequisite_id),
  CHECK (mission_id <> prerequisite_id)
);

-- -------------------------------------------------------

-- Asignación de contenido a un salón completo O a un alumno específico
CREATE TABLE content_assignments (
  id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  content_id   UUID        NOT NULL REFERENCES content(id)    ON DELETE CASCADE,
  classroom_id UUID        REFERENCES classrooms(id)          ON DELETE CASCADE,
  student_id   UUID        REFERENCES users(id)               ON DELETE CASCADE,
  assigned_by  UUID        NOT NULL REFERENCES users(id)      ON DELETE RESTRICT,
  due_date     DATE,
  assigned_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  is_active    BOOLEAN     NOT NULL DEFAULT TRUE,
  -- Exactamente uno de los dos debe estar poblado
  CHECK (
    (classroom_id IS NOT NULL AND student_id IS NULL) OR
    (classroom_id IS NULL     AND student_id IS NOT NULL)
  )
);

CREATE INDEX idx_assignments_content   ON content_assignments(content_id);
CREATE INDEX idx_assignments_classroom ON content_assignments(classroom_id);
CREATE INDEX idx_assignments_student   ON content_assignments(student_id);


-- ============================================================
--  QUIZZES
-- ============================================================

CREATE TABLE quiz_questions (
  id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  content_id    UUID          NOT NULL REFERENCES content(id) ON DELETE CASCADE,
  question_text TEXT          NOT NULL,
  question_type question_type NOT NULL DEFAULT 'opcion_multiple',
  points        SMALLINT      NOT NULL DEFAULT 1 CHECK (points > 0),
  order_index   SMALLINT      NOT NULL DEFAULT 0,
  created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_quiz_questions_content ON quiz_questions(content_id);

-- -------------------------------------------------------

CREATE TABLE quiz_options (
  id          UUID     PRIMARY KEY DEFAULT gen_random_uuid(),
  question_id UUID     NOT NULL REFERENCES quiz_questions(id) ON DELETE CASCADE,
  option_text TEXT     NOT NULL,
  is_correct  BOOLEAN  NOT NULL DEFAULT FALSE,
  order_index SMALLINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_quiz_options_question ON quiz_options(question_id);


-- ============================================================
--  ACTIVIDAD DEL ALUMNO
-- ============================================================

-- Entrega de misión, tarea o proyecto
CREATE TABLE submissions (
  id               UUID              PRIMARY KEY DEFAULT gen_random_uuid(),
  student_id       UUID              NOT NULL REFERENCES users(id)               ON DELETE CASCADE,
  content_id       UUID              NOT NULL REFERENCES content(id)             ON DELETE CASCADE,
  assignment_id    UUID              REFERENCES content_assignments(id)          ON DELETE SET NULL,
  code_submitted   TEXT,
  status           submission_status NOT NULL DEFAULT 'enviado',
  score            SMALLINT          CHECK (score BETWEEN 0 AND 100),
  attempts_count   SMALLINT          NOT NULL DEFAULT 1 CHECK (attempts_count > 0),
  teacher_feedback TEXT,
  submitted_at     TIMESTAMPTZ       NOT NULL DEFAULT NOW(),
  reviewed_at      TIMESTAMPTZ,
  reviewed_by      UUID              REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_submissions_student  ON submissions(student_id);
CREATE INDEX idx_submissions_content  ON submissions(content_id);
CREATE INDEX idx_submissions_status   ON submissions(status);

-- -------------------------------------------------------

-- Intento de quiz (auto-calificado)
CREATE TABLE quiz_attempts (
  id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  student_id    UUID        NOT NULL REFERENCES users(id)               ON DELETE CASCADE,
  content_id    UUID        NOT NULL REFERENCES content(id)             ON DELETE CASCADE,
  assignment_id UUID        REFERENCES content_assignments(id)          ON DELETE SET NULL,
  score         SMALLINT    CHECK (score BETWEEN 0 AND 100),
  completed_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_quiz_attempts_student ON quiz_attempts(student_id);
CREATE INDEX idx_quiz_attempts_content ON quiz_attempts(content_id);

-- -------------------------------------------------------

CREATE TABLE quiz_attempt_answers (
  id                 UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
  attempt_id         UUID    NOT NULL REFERENCES quiz_attempts(id)    ON DELETE CASCADE,
  question_id        UUID    NOT NULL REFERENCES quiz_questions(id)   ON DELETE CASCADE,
  selected_option_id UUID    REFERENCES quiz_options(id)              ON DELETE SET NULL,
  text_answer        TEXT,   -- usado cuando question_type = 'respuesta_corta'
  is_correct         BOOLEAN
);

CREATE INDEX idx_attempt_answers_attempt ON quiz_attempt_answers(attempt_id);

-- -------------------------------------------------------

-- Log transaccional de XP (fuente de verdad del total de XP)
CREATE TABLE xp_events (
  id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  student_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  amount         SMALLINT    NOT NULL,         -- positivo = ganar, negativo = descontar
  reason         xp_reason   NOT NULL,
  reference_id   UUID,                         -- submission_id, achievement_id, etc.
  reference_type VARCHAR(30),                  -- 'submission', 'achievement', 'quiz_attempt'
  awarded_by     UUID        REFERENCES users(id) ON DELETE SET NULL,
  created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_xp_events_student    ON xp_events(student_id);
CREATE INDEX idx_xp_events_created_at ON xp_events(created_at);

-- -------------------------------------------------------

-- Un registro por alumno por día — base para calcular racha
CREATE TABLE daily_activity (
  id                 UUID     PRIMARY KEY DEFAULT gen_random_uuid(),
  student_id         UUID     NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  activity_date      DATE     NOT NULL,
  missions_completed SMALLINT NOT NULL DEFAULT 0,
  xp_earned          SMALLINT NOT NULL DEFAULT 0,
  minutes_active     SMALLINT NOT NULL DEFAULT 0,
  UNIQUE (student_id, activity_date)
);

CREATE INDEX idx_daily_activity_student ON daily_activity(student_id);
CREATE INDEX idx_daily_activity_date    ON daily_activity(activity_date);

-- -------------------------------------------------------

-- Progreso del alumno por materia (nivel, XP acumulado)
CREATE TABLE student_subject_progress (
  id                 UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  student_id         UUID        NOT NULL REFERENCES users(id)      ON DELETE CASCADE,
  subject_id         UUID        NOT NULL REFERENCES subjects(id)   ON DELETE CASCADE,
  level              SMALLINT    NOT NULL DEFAULT 1 CHECK (level >= 1),
  xp_in_subject      INT         NOT NULL DEFAULT 0 CHECK (xp_in_subject >= 0),
  missions_completed SMALLINT    NOT NULL DEFAULT 0,
  updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (student_id, subject_id)
);

CREATE INDEX idx_subj_progress_student ON student_subject_progress(student_id);

CREATE TRIGGER trg_subj_progress_updated_at
  BEFORE UPDATE ON student_subject_progress
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ============================================================
--  GAMIFICACIÓN
-- ============================================================

-- Catálogo de logros disponibles en la plataforma
CREATE TABLE achievement_definitions (
  id              UUID                 PRIMARY KEY DEFAULT gen_random_uuid(),
  title           VARCHAR(100)         NOT NULL UNIQUE,
  description     TEXT,
  icon            VARCHAR(10),
  xp_reward       SMALLINT             NOT NULL DEFAULT 0,
  category        achievement_category NOT NULL,
  rarity          rarity_level         NOT NULL DEFAULT 'comun',
  -- Cómo se evalúa el logro:
  --   condition_type:  'missions_count' | 'streak_days' | 'subject_level' | 'ai_conversations'
  --   condition_value: { "count": 10 } | { "days": 7 } | { "subject": "python", "level": 5 }
  condition_type  VARCHAR(50),
  condition_value JSONB,
  is_active       BOOLEAN              NOT NULL DEFAULT TRUE,
  created_at      TIMESTAMPTZ          NOT NULL DEFAULT NOW()
);

-- -------------------------------------------------------

-- Logros ganados por cada alumno
CREATE TABLE student_achievements (
  id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  student_id     UUID        NOT NULL REFERENCES users(id)                  ON DELETE CASCADE,
  achievement_id UUID        NOT NULL REFERENCES achievement_definitions(id) ON DELETE CASCADE,
  earned_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (student_id, achievement_id)
);

CREATE INDEX idx_student_achievements_student ON student_achievements(student_id);


-- ============================================================
--  COMUNICACIÓN
-- ============================================================

-- Alertas y notificaciones del sistema
CREATE TABLE notifications (
  id             UUID              PRIMARY KEY DEFAULT gen_random_uuid(),
  recipient_id   UUID              NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  sender_id      UUID              REFERENCES users(id)          ON DELETE SET NULL,
  type           notification_type NOT NULL,
  title          VARCHAR(200)      NOT NULL,
  body           TEXT,
  reference_id   UUID,             -- a qué entidad se refiere (contenido, alumno, etc.)
  reference_type VARCHAR(30),
  is_read        BOOLEAN           NOT NULL DEFAULT FALSE,
  created_at     TIMESTAMPTZ       NOT NULL DEFAULT NOW(),
  read_at        TIMESTAMPTZ
);

CREATE INDEX idx_notifications_recipient ON notifications(recipient_id);
CREATE INDEX idx_notifications_unread    ON notifications(recipient_id, is_read)
  WHERE is_read = FALSE;

-- -------------------------------------------------------

-- Mensajes directos entre usuarios (maestro ↔ padre, etc.)
CREATE TABLE messages (
  id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  sender_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  recipient_id      UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  subject           VARCHAR(200),
  body              TEXT        NOT NULL,
  is_read           BOOLEAN     NOT NULL DEFAULT FALSE,
  parent_message_id UUID        REFERENCES messages(id) ON DELETE SET NULL,  -- hilos
  created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  read_at           TIMESTAMPTZ
);

CREATE INDEX idx_messages_recipient ON messages(recipient_id);
CREATE INDEX idx_messages_sender    ON messages(sender_id);


-- ============================================================
--  TUTOR IA
-- ============================================================

CREATE TABLE ai_conversations (
  id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
  student_id      UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  context_type    ai_context_type NOT NULL DEFAULT 'general',
  context_id      UUID,           -- content.id cuando context_type = 'mision' | 'proyecto'
  started_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
  last_message_at TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
  message_count   SMALLINT        NOT NULL DEFAULT 0
);

CREATE INDEX idx_ai_conversations_student ON ai_conversations(student_id);

-- -------------------------------------------------------

CREATE TABLE ai_messages (
  id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  conversation_id UUID         NOT NULL REFERENCES ai_conversations(id) ON DELETE CASCADE,
  role            message_role NOT NULL,
  content         TEXT         NOT NULL,
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ai_messages_conv ON ai_messages(conversation_id);


-- ============================================================
--  SEED: Materias iniciales
-- ============================================================

INSERT INTO subjects (name, icon, color, description) VALUES
  ('Python',        '🐍', '#06B6D4', 'Programación con Python'),
  ('HTML/CSS/JS',   '🌐', '#7C3AED', 'Desarrollo web básico'),
  ('Scratch',       '🧩', '#2563EB', 'Programación visual con Scratch'),
  ('Robótica',      '🤖', '#F59E0B', 'Arduino y electrónica básica'),
  ('Roblox Studio', '🎮', '#10B981', 'Creación de juegos en Roblox Studio'),
  ('Matemáticas',   '📐', '#EC4899', 'Lógica y matemáticas aplicadas'),
  ('Arte Digital',  '🎨', '#8B5CF6', 'Diseño y arte digital'),
  ('Ciencias',      '🔬', '#64748B', 'Ciencias básicas y naturales');


-- ============================================================
--  SEED: Logros iniciales (catálogo)
-- ============================================================

INSERT INTO achievement_definitions
  (title, description, icon, xp_reward, category, rarity, condition_type, condition_value)
VALUES
  ('Primer Código',  'Completaste tu primera misión de programación',   '⚡', 25,  'programacion', 'comun',       'missions_count',   '{"count":1}'),
  ('Bug Hunter',     'Encontraste y corregiste 5 errores en tu código', '🐛', 50,  'programacion', 'poco_comun',  'missions_count',   '{"count":5}'),
  ('Loop Master',    'Usaste bucles en 10 misiones diferentes',         '🔄', 75,  'programacion', 'poco_comun',  'missions_count',   '{"count":10}'),
  ('Racha 7 días',   'Estudió 7 días consecutivos sin parar',           '🔥', 100, 'racha',        'raro',        'streak_days',      '{"days":7}'),
  ('Racha 30 días',  'Estudió 30 días consecutivos',                    '💎', 300, 'racha',        'epico',       'streak_days',      '{"days":30}'),
  ('Web Wizard',     'Completó todas las misiones de HTML/CSS',         '🌐', 120, 'programacion', 'raro',        'subject_complete', '{"subject":"HTML/CSS/JS"}'),
  ('Roblox Creator', 'Publicó su primer juego en Roblox Studio',        '🎮', 200, 'proyectos',    'epico',       'project_complete', '{"subject":"Roblox Studio"}'),
  ('Team Player',    'Ayudó a 3 compañeros en la comunidad',            '🤝', 80,  'social',       'poco_comun',  'community_helps',  '{"count":3}'),
  ('Python Pro',     'Alcanzó el nivel 5 en Python',                    '🐍', 150, 'programacion', 'raro',        'subject_level',    '{"subject":"Python","level":5}'),
  ('AI Explorer',    'Tuvo 20 conversaciones con ByteBot',              '🤖', 60,  'especial',     'comun',       'ai_conversations', '{"count":20}'),
  ('Top Estudiante', 'Quedó en el top 10 del ranking mensual',          '🌟', 500, 'especial',     'legendario',  'ranking_top',      '{"top":10}'),
  ('Robot Builder',  'Completó el proyecto de robótica completo',       '⚙️', 180, 'proyectos',    'epico',       'project_complete', '{"subject":"Robótica"}');
