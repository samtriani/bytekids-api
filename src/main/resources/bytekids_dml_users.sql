-- ============================================================
--  ByteKids Academy - DML usuarios de prueba
--  Contrasenas:
--    samuel.partida -> Admin123#
--    direccion.general -> Admin123#
--    prof.yamileth -> Maestro123#
--    axel.partida -> Alumno123#
--    monze.gutierrez -> Padre123#
-- ============================================================

INSERT INTO users (id, username, password_hash, display_name, role, initials, is_active)
VALUES
  (gen_random_uuid(), 'samuel.partida',
   '$2a$10$nf/hWzpWqR9P13A5qdK5Z.9NSUnRyDq4AaMZyTkErVYiI97UV8qPS',
   'Samuel Partida', 'admin', 'SP', true),

  (gen_random_uuid(), 'direccion.general',
   '$2a$10$nf/hWzpWqR9P13A5qdK5Z.9NSUnRyDq4AaMZyTkErVYiI97UV8qPS',
   'Direccion General', 'director', 'DG', true),

  (gen_random_uuid(), 'prof.yamileth',
   '$2a$10$OHrNFU91iTU1hvh6ATZA3u5M2g/hAzj8O4cby0Q5muXKKqObGJQgS',
   'Profra. Yamileth', 'teacher', 'PY', true),

  (gen_random_uuid(), 'axel.partida',
   '$2a$10$VkMqv9e7Pp3/qvG9gqrvGOOvil0VY1yE5OjiE1DDcuPpeiAq5zHO2',
   'Axel Partida', 'student', 'AP', true),

  (gen_random_uuid(), 'monze.gutierrez',
   '$2a$10$U8wYbwUK4FiYA774NzOgsOnRxL5tvpP9b4XQp/orfeQ5deEIA7Xzu',
   'Monze Gutierrez', 'parent', 'MG', true);
