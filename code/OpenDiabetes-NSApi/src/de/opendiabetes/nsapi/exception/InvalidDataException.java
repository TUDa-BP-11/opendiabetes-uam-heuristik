package de.opendiabetes.nsapi.exception;

/**
 * An exception representing that invalid data was used
 */
public class InvalidDataException extends RuntimeException {
    public InvalidDataException(String message) {
        super(message);
    }

    public InvalidDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
