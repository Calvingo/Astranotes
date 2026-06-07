package com.astraNotes.web.service;

public class NoteServiceException extends RuntimeException {
    public NoteServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
