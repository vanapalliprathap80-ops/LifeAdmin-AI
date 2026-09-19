-- V9: Update Services Schema
-- Make actions and obligations independent of documents to support services

-- 1. Actions table
ALTER TABLE actions ALTER COLUMN document_id DROP NOT NULL;
ALTER TABLE actions ADD COLUMN service_id UUID;
ALTER TABLE actions ADD CONSTRAINT fk_actions_service FOREIGN KEY (service_id) REFERENCES services (id) ON DELETE CASCADE;
CREATE INDEX idx_actions_service ON actions(service_id);

-- 2. Obligations table
ALTER TABLE obligations ALTER COLUMN document_id DROP NOT NULL;
ALTER TABLE obligations ADD COLUMN service_id UUID;
ALTER TABLE obligations ADD CONSTRAINT fk_obligations_service FOREIGN KEY (service_id) REFERENCES services (id) ON DELETE CASCADE;
CREATE INDEX idx_obligations_service ON obligations(service_id);

-- 3. Provider connections state (Already handled by status VARCHAR(50) in V7)

-- 4. Services table (expanding for assets/warranties)
ALTER TABLE services ADD COLUMN provider_connection_id UUID;
ALTER TABLE services ADD CONSTRAINT fk_services_connection FOREIGN KEY (provider_connection_id) REFERENCES provider_connections (id) ON DELETE SET NULL;

ALTER TABLE services ADD COLUMN service_type VARCHAR(50) NOT NULL DEFAULT 'SUBSCRIPTION';
ALTER TABLE services ADD COLUMN asset_name VARCHAR(255);
