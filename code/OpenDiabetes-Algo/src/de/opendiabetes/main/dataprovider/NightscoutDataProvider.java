package de.opendiabetes.main.dataprovider;

import de.opendiabetes.algo.TempBasal;
import de.opendiabetes.main.exception.DataProviderException;
import de.opendiabetes.nsapi.NSApi;
import de.opendiabetes.vault.engine.container.VaultEntry;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class NightscoutDataProvider implements AlgorithmDataProvider {
    private NSApi api;

    public NightscoutDataProvider(String configPath) {
        try (InputStream input = new FileInputStream(configPath)) {
            Properties properties = new Properties();
            properties.load(input);

            String host = properties.getProperty("host");
            String secret = properties.getProperty("secret");
            this.api = new NSApi(host, secret);
        } catch (FileNotFoundException e) {
            throw new DataProviderException(this, "Cannot find config file at " + configPath, e);
        } catch (IOException e) {
            throw new DataProviderException(this, "Exception while reading config file: " + e.getMessage(), e);
        }
        

    }

    @Override
    public List<VaultEntry> getGlucoseMeasurements() {
        return null;
    }

    @Override
    public List<VaultEntry> getBolusTreatments() {
        return null;
    }

    @Override
    public List<TempBasal> getBasalTratments() {
        return null;
    }

    @Override
    public void close() {
        api.close();
    }
}
