package com.example.coderag.dto;

public class RelatedFileDto {

    private String filePath;
    private String language;
    private Double relevanceScore;
    private String reason;

    public RelatedFileDto() {
    }

    public RelatedFileDto(String filePath, String language, Double relevanceScore, String reason) {
        this.filePath = filePath;
        this.language = language;
        this.relevanceScore = relevanceScore;
        this.reason = reason;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String filePath;
        private String language;
        private Double relevanceScore;
        private String reason;

        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder language(String language) {
            this.language = language;
            return this;
        }

        public Builder relevanceScore(Double relevanceScore) {
            this.relevanceScore = relevanceScore;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public RelatedFileDto build() {
            return new RelatedFileDto(filePath, language, relevanceScore, reason);
        }
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

    public Double getRelevanceScore() {
        return relevanceScore;
    }

    public void setRelevanceScore(Double relevanceScore) {
        this.relevanceScore = relevanceScore;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
