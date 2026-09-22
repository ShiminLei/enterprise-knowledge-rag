package com.aishare.knowledgerag.answer;

public class ChatGenerationException extends RuntimeException {

    public ChatGenerationException(String message) {
        super(message);
    }

    public ChatGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
