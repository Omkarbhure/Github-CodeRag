-- Flyway migration: V5__init_keyword_search_schema.sql
-- Add generated tsvector column for full-text search and GIN index

ALTER TABLE code_chunks 
ADD COLUMN IF NOT EXISTS content_tsv tsvector 
GENERATED ALWAYS AS (to_tsvector('english', coalesce(content, ''))) STORED;

CREATE INDEX IF NOT EXISTS idx_code_chunks_content_tsv 
ON code_chunks USING GIN(content_tsv);
