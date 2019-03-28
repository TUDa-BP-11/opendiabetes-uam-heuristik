package de.opendiabetes.vault.main.exception;

import de.opendiabetes.vault.main.dataprovider.DataProvider;

public class DataProviderException extends RuntimeException {
    public DataProviderException(DataProvider provider, String message) {
        super(provider.getClass().getSimpleName() + ": " + message);
    }
    
    public DataProviderException(DataProvider provider, String message, Throwable cause) {
        super(provider.getClass().getSimpleName() + ": " + message, cause);
    }
}
