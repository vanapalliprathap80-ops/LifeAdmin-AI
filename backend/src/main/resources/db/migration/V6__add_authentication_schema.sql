-- V6: Authentication and User Ownership

-- 1. Create users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 2. Create a default user to adopt existing orphaned records
INSERT INTO users (id, email, password_hash)
VALUES ('00000000-0000-0000-0000-000000000000', 'system@lifeadmin.ai', 'unusable_password_hash');

-- 3. Add user_id to documents
ALTER TABLE documents ADD COLUMN user_id UUID;
UPDATE documents SET user_id = '00000000-0000-0000-0000-000000000000' WHERE user_id IS NULL;
ALTER TABLE documents ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE documents ADD CONSTRAINT fk_documents_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
CREATE INDEX idx_documents_user ON documents(user_id);

-- 4. Add user_id to services
ALTER TABLE services ADD COLUMN user_id UUID;
UPDATE services SET user_id = '00000000-0000-0000-0000-000000000000' WHERE user_id IS NULL;
ALTER TABLE services ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE services ADD CONSTRAINT fk_services_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
CREATE INDEX idx_services_user ON services(user_id);

-- 5. Add user_id to obligations
ALTER TABLE obligations ADD COLUMN user_id UUID;
UPDATE obligations SET user_id = '00000000-0000-0000-0000-000000000000' WHERE user_id IS NULL;
ALTER TABLE obligations ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE obligations ADD CONSTRAINT fk_obligations_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
CREATE INDEX idx_obligations_user ON obligations(user_id);

-- 6. Add user_id to actions
ALTER TABLE actions ADD COLUMN user_id UUID;
UPDATE actions SET user_id = '00000000-0000-0000-0000-000000000000' WHERE user_id IS NULL;
ALTER TABLE actions ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE actions ADD CONSTRAINT fk_actions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
CREATE INDEX idx_actions_user ON actions(user_id);

-- 7. Add user_id to ai_analysis_runs
ALTER TABLE ai_analysis_runs ADD COLUMN user_id UUID;
UPDATE ai_analysis_runs SET user_id = '00000000-0000-0000-0000-000000000000' WHERE user_id IS NULL;
ALTER TABLE ai_analysis_runs ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE ai_analysis_runs ADD CONSTRAINT fk_ai_analysis_runs_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
CREATE INDEX idx_ai_analysis_runs_user ON ai_analysis_runs(user_id);
