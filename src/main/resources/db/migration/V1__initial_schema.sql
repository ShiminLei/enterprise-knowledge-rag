CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE tenant (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tenant_user_permission (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    user_id VARCHAR(128) NOT NULL,
    department VARCHAR(128) NOT NULL,
    permission_level VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (tenant_id, user_id, department)
);

CREATE TABLE knowledge_document (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    title VARCHAR(300) NOT NULL,
    source VARCHAR(500) NOT NULL,
    file_name VARCHAR(300) NOT NULL,
    media_type VARCHAR(128) NOT NULL,
    category VARCHAR(64) NOT NULL,
    version VARCHAR(64) NOT NULL,
    document_updated_at TIMESTAMPTZ NOT NULL,
    permission_level VARCHAR(32) NOT NULL,
    department VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    checksum VARCHAR(128) NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (tenant_id, checksum)
);

CREATE TABLE knowledge_chunk (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES knowledge_document(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    title_path VARCHAR(1000),
    page_number INTEGER,
    paragraph_number INTEGER,
    category VARCHAR(64) NOT NULL,
    document_version VARCHAR(64) NOT NULL,
    document_updated_at TIMESTAMPTZ NOT NULL,
    permission_level VARCHAR(32) NOT NULL,
    department VARCHAR(128) NOT NULL,
    source VARCHAR(500) NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    embedding VECTOR(1024),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (document_id, chunk_index)
);

CREATE INDEX idx_document_tenant_status ON knowledge_document (tenant_id, status);
CREATE INDEX idx_document_category ON knowledge_document (tenant_id, category);
CREATE INDEX idx_chunk_document ON knowledge_chunk (document_id);
CREATE INDEX idx_chunk_access ON knowledge_chunk (tenant_id, permission_level, department);
CREATE INDEX idx_chunk_metadata ON knowledge_chunk USING GIN (metadata);
CREATE INDEX idx_chunk_embedding_hnsw ON knowledge_chunk
    USING HNSW (embedding vector_cosine_ops)
    WHERE embedding IS NOT NULL;

CREATE TABLE ingestion_job (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    document_id UUID REFERENCES knowledge_document(id) ON DELETE SET NULL,
    file_name VARCHAR(300) NOT NULL,
    status VARCHAR(32) NOT NULL,
    total_chunks INTEGER NOT NULL DEFAULT 0,
    processed_chunks INTEGER NOT NULL DEFAULT 0,
    error_code VARCHAR(64),
    error_message TEXT,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE conversation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    user_id VARCHAR(128) NOT NULL,
    title VARCHAR(300),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE conversation_message (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversation(id) ON DELETE CASCADE,
    role VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_conversation_owner ON conversation (tenant_id, user_id, updated_at DESC);
CREATE INDEX idx_message_conversation ON conversation_message (conversation_id, created_at);

CREATE TABLE prompt_template (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    prompt_key VARCHAR(128) NOT NULL,
    version VARCHAR(64) NOT NULL,
    content TEXT NOT NULL,
    checksum VARCHAR(128) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    created_by VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (prompt_key, version)
);

CREATE UNIQUE INDEX uq_active_prompt
    ON prompt_template (prompt_key)
    WHERE active = TRUE;

CREATE TABLE evaluation_case (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_key VARCHAR(128) NOT NULL UNIQUE,
    question TEXT NOT NULL,
    expected_document_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    expected_keywords JSONB NOT NULL DEFAULT '[]'::jsonb,
    should_answer BOOLEAN NOT NULL DEFAULT TRUE,
    required_permission_level VARCHAR(32),
    tags JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE evaluation_run (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    status VARCHAR(32) NOT NULL,
    configuration JSONB NOT NULL DEFAULT '{}'::jsonb,
    summary JSONB NOT NULL DEFAULT '{}'::jsonb,
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ
);

CREATE TABLE evaluation_result (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id UUID NOT NULL REFERENCES evaluation_run(id) ON DELETE CASCADE,
    case_id UUID NOT NULL REFERENCES evaluation_case(id),
    answer TEXT,
    retrieved_chunks JSONB NOT NULL DEFAULT '[]'::jsonb,
    metrics JSONB NOT NULL DEFAULT '{}'::jsonb,
    latency_ms BIGINT,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (run_id, case_id)
);

CREATE TABLE rag_request_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id VARCHAR(64) NOT NULL UNIQUE,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    user_id VARCHAR(128) NOT NULL,
    conversation_id UUID,
    question_digest VARCHAR(128) NOT NULL,
    retrieval_mode VARCHAR(32) NOT NULL,
    model_name VARCHAR(128),
    prompt_version VARCHAR(64),
    retrieved_document_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    outcome VARCHAR(32) NOT NULL,
    latency_ms BIGINT,
    error_code VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO tenant (id, code, name)
VALUES ('00000000-0000-0000-0000-000000000001', 'acme', '示例科技有限公司');
