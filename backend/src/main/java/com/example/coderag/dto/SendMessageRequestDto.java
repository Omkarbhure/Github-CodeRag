package com.example.coderag.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class SendMessageRequestDto {

    @NotBlank(message = "Question must not be blank")
    @JsonProperty("question")
    @JsonAlias({"content", "prompt"})
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

    public String getContent() {
        return question;
    }

    public void setContent(String content) {
        if (this.question == null || this.question.isBlank()) {
            this.question = content;
        }
    }
}
