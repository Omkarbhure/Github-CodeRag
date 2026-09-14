package com.example.coderag.vector;

public interface EmbeddingService {

    /**
     * Generates a vector embedding for the provided text.
     *
     * @param text content to embed
     * @return float array representing the embedding vector
     */
    float[] embed(String text);
}
