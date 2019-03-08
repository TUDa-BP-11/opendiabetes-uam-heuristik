package de.opendiabetes.main.exception;

import de.opendiabetes.main.dataprovider.AlgorithmDataProvider;

public class DataProviderException extends RuntimeException {
    public DataProviderException(AlgorithmDataProvider provider, String message) {
        super(provider.getClass().getSimpleName() + ": " + message);
    }
    
    public DataProviderException(AlgorithmDataProvider provider, String message, Throwable cause) {
        super(provider.getClass().getSimpleName() + ": " + message, cause);
    }
}
