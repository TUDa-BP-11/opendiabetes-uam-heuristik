package de.opendiabetes.nsapi.exception;

/**
 * Represents an IO error while reading or writing data from or to a Nightscout server or a file
 */
public class NightscoutIOException extends RuntimeException {
    public NightscoutIOException(String message) {
        super(message);
    }

    public NightscoutIOException(String message, Throwable cause) {
        super(message, cause);
    }
}
