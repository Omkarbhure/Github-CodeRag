CREATE TABLE IF NOT EXISTS code_chunks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repository_file_id UUID NOT NULL REFERENCES repository_files(id) ON DELETE CASCADE,
    repository_id UUID NOT NULL REFERENCES github_repositories(id) ON DELETE CASCADE,
    file_path VARCHAR(1000) NOT NULL,
    start_line INT NOT NULL,
    end_line INT NOT NULL,
    content TEXT NOT NULL,
    chunk_index INT NOT NULL,
    commit_sha VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_code_chunks_repo_id ON code_chunks(repository_id);
CREATE INDEX IF NOT EXISTS idx_code_chunks_repo_file_id ON code_chunks(repository_file_id);
CREATE INDEX IF NOT EXISTS idx_code_chunks_repo_chunk_idx ON code_chunks(repository_id, chunk_index);
