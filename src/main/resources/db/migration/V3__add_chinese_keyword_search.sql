CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_chunk_content_trgm ON knowledge_chunk
    USING GIN (lower(content) gin_trgm_ops);

CREATE INDEX idx_chunk_title_path_trgm ON knowledge_chunk
    USING GIN (lower(COALESCE(title_path, '')) gin_trgm_ops);
