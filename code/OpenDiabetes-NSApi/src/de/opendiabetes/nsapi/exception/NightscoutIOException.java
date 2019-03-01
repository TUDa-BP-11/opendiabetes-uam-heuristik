package de.opendiabetes.nsapi.exception;

/**
 * Represents an I/O error while reading or writing data from or to a Nightscout server or a file
 */
public class NightscoutIOException extends Exception {
    public NightscoutIOException(String message) {
        super(message);
    }

    public NightscoutIOException(String message, Throwable cause) {
        super(message, cause);
    }
}
