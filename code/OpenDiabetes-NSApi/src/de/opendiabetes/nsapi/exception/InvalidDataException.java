package de.opendiabetes.nsapi.exception;

/**
 * Representing that invalid data was provided.
 */
public class InvalidDataException extends RuntimeException {
    public InvalidDataException(String message) {
        super(message);
    }

    public InvalidDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
