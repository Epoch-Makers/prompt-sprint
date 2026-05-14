-- Demo seed data for RetroAI
-- All seed users share the password: demo1234
-- BCrypt hash verified against Spring Security BCryptPasswordEncoder

MERGE INTO users (id, email, full_name, password_hash, auth_provider, created_at) KEY(id) VALUES
  (1, 'ayse@example.com',  'Ayşe Yılmaz', '$2a$10$ttOzfFNDd4o3bilcmGCYteUzqSpyl7JlADpBx8URllHZWyrizACy.', 'LOCAL', CURRENT_TIMESTAMP),
  (2, 'burak@example.com', 'Burak Demir', '$2a$10$ttOzfFNDd4o3bilcmGCYteUzqSpyl7JlADpBx8URllHZWyrizACy.', 'LOCAL', CURRENT_TIMESTAMP),
  (3, 'can@example.com',   'Can Kaya',    '$2a$10$ttOzfFNDd4o3bilcmGCYteUzqSpyl7JlADpBx8URllHZWyrizACy.', 'LOCAL', CURRENT_TIMESTAMP);

MERGE INTO teams (id, name, created_at, created_by_user_id) KEY(id) VALUES
  (1, 'Payment Squad', CURRENT_TIMESTAMP, 1);

MERGE INTO team_members (id, team_id, user_id, role, joined_at) KEY(id) VALUES
  (1, 1, 1, 'LEADER', CURRENT_TIMESTAMP),
  (2, 1, 2, 'MEMBER', CURRENT_TIMESTAMP),
  (3, 1, 3, 'MEMBER', CURRENT_TIMESTAMP);

MERGE INTO retro_sessions (id, team_id, sprint_name, retro_name, status, current_phase, anonymous_mode, created_at, created_by_user_id) KEY(id) VALUES
  (1, 1, 'Sprint 24', 'Sprint 24 Retro', 'ACTIVE', 'WRITING', TRUE, CURRENT_TIMESTAMP, 1);

MERGE INTO retro_cards (id, retro_id, author_user_id, content, column_name, source, created_at, updated_at) KEY(id) VALUES
  (1, 1, 1, 'Sprint hedeflerine ulaştık',          'GOOD',    'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, 1, 2, 'Deploy süresi 40 dakikaya çıktı',     'IMPROVE', 'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (3, 1, 3, 'PR review süreci yavaş',              'IMPROVE', 'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Bump IDENTITY sequences past seed IDs so user/team creation continues from a safe value
ALTER TABLE users         ALTER COLUMN id RESTART WITH 100;
ALTER TABLE teams         ALTER COLUMN id RESTART WITH 100;
ALTER TABLE team_members  ALTER COLUMN id RESTART WITH 100;
ALTER TABLE retro_sessions ALTER COLUMN id RESTART WITH 100;
ALTER TABLE retro_cards   ALTER COLUMN id RESTART WITH 100;
