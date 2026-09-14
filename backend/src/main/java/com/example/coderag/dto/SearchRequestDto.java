package com.example.coderag.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class SearchRequestDto {

    @NotBlank(message = "Search query must not be blank")
    private String query;

    @Min(value = 1, message = "topK must be at least 1")
    @Max(value = 50, message = "topK must not exceed 50")
    private Integer topK = 10;

    public SearchRequestDto() {
    }

    public SearchRequestDto(String query, Integer topK) {
        this.query = query;
        if (topK != null) {
            this.topK = topK;
        }
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        if (topK != null) {
            this.topK = topK;
        }
    }
}
