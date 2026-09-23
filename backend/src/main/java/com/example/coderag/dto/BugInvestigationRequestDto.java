package com.example.coderag.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class BugInvestigationRequestDto {

    @NotBlank(message = "Error text or stack trace must not be blank")
    private String errorText;

    private List<String> targetFiles;

    public BugInvestigationRequestDto() {
    }

    public BugInvestigationRequestDto(String errorText) {
        this.errorText = errorText;
    }

    public BugInvestigationRequestDto(String errorText, List<String> targetFiles) {
        this.errorText = errorText;
        this.targetFiles = targetFiles;
    }

    public String getErrorText() {
        return errorText;
    }

    public void setErrorText(String errorText) {
        this.errorText = errorText;
    }

    public List<String> getTargetFiles() {
        return targetFiles;
    }

    public void setTargetFiles(List<String> targetFiles) {
        this.targetFiles = targetFiles;
    }
}

