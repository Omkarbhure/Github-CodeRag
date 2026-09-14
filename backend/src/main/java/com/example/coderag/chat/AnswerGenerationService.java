package com.example.coderag.chat;

import com.example.coderag.dto.MessageDto;

import java.util.List;

public interface AnswerGenerationService {

    /**
     * Generates a grounded answer with inline citations [filePath:startLine-endLine]
     * based on the retrieved code context and previous conversation messages.
     *
     * @param question user question
     * @param codeContext formatted code snippets from retrieved chunks
     * @param history prior messages in the conversation (if any)
     * @return grounded answer text
     */
    String generateAnswer(String question, String codeContext, List<MessageDto> history);
}
