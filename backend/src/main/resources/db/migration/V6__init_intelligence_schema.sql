-- Flyway migration: V6__init_intelligence_schema.sql
-- Create table for caching architecture overviews

CREATE TABLE IF NOT EXISTS architecture_overviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repository_id UUID NOT NULL REFERENCES github_repositories(id) ON DELETE CASCADE,
    commit_sha VARCHAR(100),
    overview_text TEXT NOT NULL,
    technologies TEXT,
    modules TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_architecture_overviews_repo_commit 
ON architecture_overviews(repository_id, commit_sha);
