package com.example.Exception;

/**
 * Exception levee lorsqu'une ressource demandee (User, Board, Task)
 * n'existe pas dans la base de donnees.
 * Elle sera interceptee par le GlobalExceptionHandler pour renvoyer un code HTTP 404 NOT_FOUND.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
