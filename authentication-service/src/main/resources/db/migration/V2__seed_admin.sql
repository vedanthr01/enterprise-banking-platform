-- DEMO-ONLY seed credential so there's a way into the system on first boot.
-- Username: admin   Password: Admin@12345
-- Real BCrypt hash (cost 10) — rotate or delete this row before any real deployment.
INSERT INTO credentials (id, username, email, password_hash, status, created_at, updated_at)
VALUES (
    'a0000000-0000-4000-8000-000000000001',
    'admin',
    'admin@bankingplatform.local',
    '$2b$10$ZJf1xcnq7ydpfz/bZJ.Ce.JzO5L28KWEnar6KDkIIhKQnAMvJ2JXm',
    'ACTIVE',
    NOW(),
    NOW()
);

INSERT INTO credential_roles (credential_id, role) VALUES
    ('a0000000-0000-4000-8000-000000000001', 'ROLE_ADMIN'),
    ('a0000000-0000-4000-8000-000000000001', 'ROLE_CUSTOMER');
