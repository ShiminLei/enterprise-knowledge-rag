ALTER TABLE knowledge_document
    ADD COLUMN external_document_id VARCHAR(128);

UPDATE knowledge_document
SET external_document_id = id::text
WHERE external_document_id IS NULL;

ALTER TABLE knowledge_document
    ALTER COLUMN external_document_id SET NOT NULL;

CREATE UNIQUE INDEX uq_document_external_version
    ON knowledge_document (tenant_id, external_document_id, version);

ALTER TABLE knowledge_chunk
    RENAME COLUMN paragraph_number TO start_paragraph_number;

ALTER TABLE knowledge_chunk
    ADD COLUMN end_paragraph_number INTEGER;

UPDATE knowledge_chunk
SET end_paragraph_number = start_paragraph_number
WHERE end_paragraph_number IS NULL;

ALTER TABLE knowledge_chunk
    ALTER COLUMN end_paragraph_number SET NOT NULL;
