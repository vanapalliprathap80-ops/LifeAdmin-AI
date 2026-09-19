-- V7: Create Provider Connections Schema
-- Stores OAuth or API connection details for third-party services

CREATE TABLE provider_connections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    provider_name VARCHAR(100) NOT NULL,
    provider_account_id VARCHAR(255),
    access_token TEXT,
    refresh_token TEXT,
    token_expires_at TIMESTAMPTZ,
    status VARCHAR(50) NOT NULL DEFAULT 'CONNECTED',
    last_synced_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    CONSTRAINT fk_provider_connections_user 
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    
    -- A user can only connect to a specific provider once
    CONSTRAINT uq_user_provider UNIQUE (user_id, provider_name)
);

CREATE INDEX idx_provider_connections_user ON provider_connections(user_id);
