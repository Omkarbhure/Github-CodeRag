package com.example.coderag.dto;

import java.util.UUID;

public class SearchResultDto {

    private UUID id;
    private UUID repositoryId;
    private UUID repositoryFileId;
    private String filePath;
    private String language;
    private Integer startLine;
    private Integer endLine;
    private Integer chunkIndex;
    private String commitSha;
    private String content;
    private Double score;
    private Double vectorScore;
    private Double keywordScore;
    private Double finalScore;

    public SearchResultDto() {
    }

    public SearchResultDto(UUID id, UUID repositoryId, UUID repositoryFileId, String filePath,
                           String language, Integer startLine, Integer endLine, Integer chunkIndex,
                           String commitSha, String content, Double score) {
        this.id = id;
        this.repositoryId = repositoryId;
        this.repositoryFileId = repositoryFileId;
        this.filePath = filePath;
        this.language = language;
        this.startLine = startLine;
        this.endLine = endLine;
        this.chunkIndex = chunkIndex;
        this.commitSha = commitSha;
        this.content = content;
        this.score = score;
        this.finalScore = score;
    }

    public SearchResultDto(UUID id, UUID repositoryId, UUID repositoryFileId, String filePath,
                           String language, Integer startLine, Integer endLine, Integer chunkIndex,
                           String commitSha, String content, Double score,
                           Double vectorScore, Double keywordScore, Double finalScore) {
        this.id = id;
        this.repositoryId = repositoryId;
        this.repositoryFileId = repositoryFileId;
        this.filePath = filePath;
        this.language = language;
        this.startLine = startLine;
        this.endLine = endLine;
        this.chunkIndex = chunkIndex;
        this.commitSha = commitSha;
        this.content = content;
        this.score = score != null ? score : finalScore;
        this.vectorScore = vectorScore;
        this.keywordScore = keywordScore;
        this.finalScore = finalScore;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID repositoryId;
        private UUID repositoryFileId;
        private String filePath;
        private String language;
        private Integer startLine;
        private Integer endLine;
        private Integer chunkIndex;
        private String commitSha;
        private String content;
        private Double score;
        private Double vectorScore;
        private Double keywordScore;
        private Double finalScore;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder repositoryId(UUID repositoryId) {
            this.repositoryId = repositoryId;
            return this;
        }

        public Builder repositoryFileId(UUID repositoryFileId) {
            this.repositoryFileId = repositoryFileId;
            return this;
        }

        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder language(String language) {
            this.language = language;
            return this;
        }

        public Builder startLine(Integer startLine) {
            this.startLine = startLine;
            return this;
        }

        public Builder endLine(Integer endLine) {
            this.endLine = endLine;
            return this;
        }

        public Builder chunkIndex(Integer chunkIndex) {
            this.chunkIndex = chunkIndex;
            return this;
        }

        public Builder commitSha(String commitSha) {
            this.commitSha = commitSha;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder score(Double score) {
            this.score = score;
            return this;
        }

        public Builder vectorScore(Double vectorScore) {
            this.vectorScore = vectorScore;
            return this;
        }

        public Builder keywordScore(Double keywordScore) {
            this.keywordScore = keywordScore;
            return this;
        }

        public Builder finalScore(Double finalScore) {
            this.finalScore = finalScore;
            return this;
        }

        public SearchResultDto build() {
            Double resolvedScore = score != null ? score : finalScore;
            return new SearchResultDto(id, repositoryId, repositoryFileId, filePath, language, startLine, endLine, chunkIndex, commitSha, content, resolvedScore, vectorScore, keywordScore, finalScore != null ? finalScore : resolvedScore);
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(UUID repositoryId) {
        this.repositoryId = repositoryId;
    }

    public UUID getRepositoryFileId() {
        return repositoryFileId;
    }

    public void setRepositoryFileId(UUID repositoryFileId) {
        this.repositoryFileId = repositoryFileId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Integer getStartLine() {
        return startLine;
    }

    public void setStartLine(Integer startLine) {
        this.startLine = startLine;
    }

    public Integer getEndLine() {
        return endLine;
    }

    public void setEndLine(Integer endLine) {
        this.endLine = endLine;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public String getCommitSha() {
        return commitSha;
    }

    public void setCommitSha(String commitSha) {
        this.commitSha = commitSha;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Double getVectorScore() {
        return vectorScore;
    }

    public void setVectorScore(Double vectorScore) {
        this.vectorScore = vectorScore;
    }

    public Double getKeywordScore() {
        return keywordScore;
    }

    public void setKeywordScore(Double keywordScore) {
        this.keywordScore = keywordScore;
    }

    public Double getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(Double finalScore) {
        this.finalScore = finalScore;
    }
}
