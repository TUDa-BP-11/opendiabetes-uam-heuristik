package de.opendiabetes.nsapi.exception;

public class NightscoutIOException extends RuntimeException {
    public NightscoutIOException(String message) {
        super(message);
    }

    public NightscoutIOException(String message, Throwable cause) {
        super(message, cause);
    }
}
