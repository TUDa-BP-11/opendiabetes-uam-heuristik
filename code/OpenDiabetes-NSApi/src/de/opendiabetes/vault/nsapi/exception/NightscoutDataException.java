package de.opendiabetes.vault.nsapi.exception;

/**
 * Representing that invalid data was provided.
 */
public class NightscoutDataException extends RuntimeException {
    public NightscoutDataException(String message) {
        super(message);
    }

    public NightscoutDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
