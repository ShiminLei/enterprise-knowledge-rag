package com.aishare.knowledgerag.security;

public class InvalidAuthenticatedIdentityException extends RuntimeException {

    public InvalidAuthenticatedIdentityException(String message) {
        super(message);
    }
}
