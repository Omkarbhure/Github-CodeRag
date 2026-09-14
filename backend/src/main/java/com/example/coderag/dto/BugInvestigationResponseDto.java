package com.example.coderag.dto;

import java.util.List;

public class BugInvestigationResponseDto {

    private String analysis;
    private List<String> identifiedFiles;
    private List<SearchResultDto> relevantChunks;

    public BugInvestigationResponseDto() {
    }

    public BugInvestigationResponseDto(String analysis, List<String> identifiedFiles, List<SearchResultDto> relevantChunks) {
        this.analysis = analysis;
        this.identifiedFiles = identifiedFiles;
        this.relevantChunks = relevantChunks;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String analysis;
        private List<String> identifiedFiles;
        private List<SearchResultDto> relevantChunks;

        public Builder analysis(String analysis) {
            this.analysis = analysis;
            return this;
        }

        public Builder identifiedFiles(List<String> identifiedFiles) {
            this.identifiedFiles = identifiedFiles;
            return this;
        }

        public Builder relevantChunks(List<SearchResultDto> relevantChunks) {
            this.relevantChunks = relevantChunks;
            return this;
        }

        public BugInvestigationResponseDto build() {
            return new BugInvestigationResponseDto(analysis, identifiedFiles, relevantChunks);
        }
    }

    public String getAnalysis() {
        return analysis;
    }

    public void setAnalysis(String analysis) {
        this.analysis = analysis;
    }

    public List<String> getIdentifiedFiles() {
        return identifiedFiles;
    }

    public void setIdentifiedFiles(List<String> identifiedFiles) {
        this.identifiedFiles = identifiedFiles;
    }

    public List<SearchResultDto> getRelevantChunks() {
        return relevantChunks;
    }

    public void setRelevantChunks(List<SearchResultDto> relevantChunks) {
        this.relevantChunks = relevantChunks;
    }
}
