package com.example.coderag.chat;

import com.example.coderag.dto.SearchResultDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ContextBuilder {

    public static final int DEFAULT_MAX_CHUNKS = 8;
    public static final int DEFAULT_MAX_CHAR_BUDGET = 8000;

    public String buildContext(List<SearchResultDto> results) {
        return buildContext(results, DEFAULT_MAX_CHUNKS, DEFAULT_MAX_CHAR_BUDGET);
    }

    public String buildContext(List<SearchResultDto> results, int maxChunks, int maxCharBudget) {
        if (results == null || results.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        int chunksIncluded = 0;

        for (SearchResultDto result : results) {
            if (chunksIncluded >= maxChunks) {
                break;
            }

            String header = String.format("--- File: %s (Lines %d-%d) ---\n",
                    result.getFilePath(),
                    result.getStartLine() != null ? result.getStartLine() : 1,
                    result.getEndLine() != null ? result.getEndLine() : 1);

            String content = result.getContent() != null ? result.getContent() : "";
            String block = header + content + "\n\n";

            if (sb.length() + block.length() > maxCharBudget && sb.length() > 0) {
                // If adding this chunk exceeds budget, truncate or stop
                int remainingBudget = maxCharBudget - sb.length();
                if (remainingBudget > header.length() + 50) {
                    sb.append(header);
                    int allowableContent = remainingBudget - header.length() - 10;
                    sb.append(content, 0, Math.min(content.length(), allowableContent));
                    sb.append("\n[...truncated]\n\n");
                }
                break;
            }

            sb.append(block);
            chunksIncluded++;
        }

        return sb.toString().trim();
    }
}
