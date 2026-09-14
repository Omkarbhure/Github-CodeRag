package com.example.coderag.dto;

import jakarta.validation.constraints.NotBlank;

public class BugInvestigationRequestDto {

    @NotBlank(message = "Error text or stack trace must not be blank")
    private String errorText;

    public BugInvestigationRequestDto() {
    }

    public BugInvestigationRequestDto(String errorText) {
        this.errorText = errorText;
    }

    public String getErrorText() {
        return errorText;
    }

    public void setErrorText(String errorText) {
        this.errorText = errorText;
    }
}
