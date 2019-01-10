package de.opendiabetes.synchronizer;

public class SynchronizerException extends RuntimeException {
    public SynchronizerException(String message) {
        super(message);
    }

    public SynchronizerException(String message, Throwable cause) {
        super(message, cause);
    }
}
