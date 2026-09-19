-- V2: LifeAdmin Persistence Schema
-- This migration creates all tables for the Phase 2 persistence layer.

-- ───────────────────────────────────────────────────────────
-- documents
-- ───────────────────────────────────────────────────────────
CREATE TABLE documents (
    id              UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    original_filename  TEXT      NOT NULL,
    stored_filename    TEXT      NOT NULL,
    document_type      VARCHAR(50)  NOT NULL DEFAULT 'UNKNOWN',
    processing_status  VARCHAR(50)  NOT NULL DEFAULT 'UPLOADED',
    file_size          BIGINT       NOT NULL,
    content_type       VARCHAR(255) NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_documents_status      ON documents (processing_status);
CREATE INDEX idx_documents_document_type ON documents (document_type);

-- ───────────────────────────────────────────────────────────
-- document_texts
-- ───────────────────────────────────────────────────────────
CREATE TABLE document_texts (
    id          UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    document_id UUID        NOT NULL UNIQUE,
    extracted_text TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_document_texts_document
        FOREIGN KEY (document_id) REFERENCES documents (id) ON DELETE CASCADE
);

-- ───────────────────────────────────────────────────────────
-- key_dates
-- ───────────────────────────────────────────────────────────
CREATE TABLE key_dates (
    id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    document_id UUID         NOT NULL,
    date_value  DATE         NOT NULL,
    date_type   VARCHAR(50)  NOT NULL,
    description TEXT,
    evidence    TEXT,
    confidence  NUMERIC(5,4),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_key_dates_document
        FOREIGN KEY (document_id) REFERENCES documents (id) ON DELETE CASCADE
);

CREATE INDEX idx_key_dates_document ON key_dates (document_id);
CREATE INDEX idx_key_dates_date_value ON key_dates (date_value);

-- ───────────────────────────────────────────────────────────
-- obligations
-- ───────────────────────────────────────────────────────────
CREATE TABLE obligations (
    id              UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    document_id     UUID         NOT NULL,
    obligation_type VARCHAR(100),
    description     TEXT         NOT NULL,
    evidence        TEXT,
    confidence      NUMERIC(5,4),
    status          VARCHAR(50)  NOT NULL DEFAULT 'IDENTIFIED',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_obligations_document
        FOREIGN KEY (document_id) REFERENCES documents (id) ON DELETE CASCADE
);

CREATE INDEX idx_obligations_document ON obligations (document_id);
CREATE INDEX idx_obligations_status   ON obligations (status);

-- ───────────────────────────────────────────────────────────
-- actions
-- ───────────────────────────────────────────────────────────
CREATE TABLE actions (
    id                    UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    document_id           UUID         NOT NULL,
    obligation_id         UUID,
    title                 TEXT         NOT NULL,
    description           TEXT,
    recommended_date      DATE,
    deadline              DATE,
    priority              VARCHAR(50)  NOT NULL DEFAULT 'MEDIUM',
    status                VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    reason                TEXT,
    evidence              TEXT,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    completed_at          TIMESTAMPTZ,
    CONSTRAINT fk_actions_document
        FOREIGN KEY (document_id) REFERENCES documents (id) ON DELETE CASCADE,
    CONSTRAINT fk_actions_obligation
        FOREIGN KEY (obligation_id) REFERENCES obligations (id) ON DELETE SET NULL
);

CREATE INDEX idx_actions_document    ON actions (document_id);
CREATE INDEX idx_actions_obligation  ON actions (obligation_id);
CREATE INDEX idx_actions_status      ON actions (status);
CREATE INDEX idx_actions_deadline    ON actions (deadline);

-- ───────────────────────────────────────────────────────────
-- ai_analysis_runs
-- ───────────────────────────────────────────────────────────
CREATE TABLE ai_analysis_runs (
    id            UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    document_id   UUID         NOT NULL,
    provider      VARCHAR(100),
    model         VARCHAR(100),
    status        VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    raw_response  TEXT,
    error_message TEXT,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    completed_at  TIMESTAMPTZ,
    CONSTRAINT fk_ai_runs_document
        FOREIGN KEY (document_id) REFERENCES documents (id) ON DELETE CASCADE
);

CREATE INDEX idx_ai_runs_document ON ai_analysis_runs (document_id);
CREATE INDEX idx_ai_runs_status   ON ai_analysis_runs (status);
