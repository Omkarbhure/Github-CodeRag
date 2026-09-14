package com.example.coderag.dto;

import jakarta.validation.constraints.NotBlank;

public class SendMessageRequestDto {

    @NotBlank(message = "Question must not be blank")
    private String question;

    public SendMessageRequestDto() {
    }

    public SendMessageRequestDto(String question) {
        this.question = question;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}
