ALTER TABLE conversation_message
    ADD COLUMN sequence_number BIGINT GENERATED ALWAYS AS IDENTITY;

CREATE INDEX idx_message_conversation_sequence
    ON conversation_message (conversation_id, sequence_number);
