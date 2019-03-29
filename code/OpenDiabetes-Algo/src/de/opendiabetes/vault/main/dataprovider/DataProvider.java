package de.opendiabetes.vault.main.dataprovider;


import com.martiansoftware.jsap.JSAPResult;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.main.exception.DataProviderException;
import de.opendiabetes.vault.parser.Profile;

import java.util.List;

public interface DataProvider {
    /**
     * Set the config for this data provider. This method should check if all required arguments are set
     * using {@link com.martiansoftware.jsap.JSAPResult#contains(String)} and throw an {@link DataProviderException}
     * if any arguments are missing or invalid. If no exception is thrown it is assumed that everything is ok.
     *
     * @param config config result that was created using the main arguments
     * @throws DataProviderException if arguments in the config are missing or invalid
     */
    void setConfig(JSAPResult config) throws DataProviderException;

    /**
     * Get a list of glucose measurements for calculation
     *
     * @return list of VaultEntries with type {@link de.opendiabetes.vault.container.VaultEntryType#GLUCOSE_CGM}
     * @throws DataProviderException if an error occurs while getting the glucose measurements
     */
    List<VaultEntry> getGlucoseMeasurements() throws DataProviderException;

    /**
     * Get a list of insulin bolus treatments for calculation
     *
     * @return list of VaultEntries with type {@link de.opendiabetes.vault.container.VaultEntryType#BOLUS_NORMAL}
     * @throws DataProviderException if an error occurs while getting the bolus treatments
     */
    List<VaultEntry> getBolusTreatments() throws DataProviderException;

    /**
     * Get a a list of basal treatments for calculation
     *
     * @return list of VaultEntries with type {@link de.opendiabetes.vault.container.VaultEntryType#BASAL_PROFILE}
     * @throws DataProviderException if an error occurs while getting the basal differences
     */
    List<VaultEntry> getBasalTreatments() throws DataProviderException;

    /**
     * @return The Nightscout profile to use
     * @throws DataProviderException if an error occurs while getting the profile
     */
    Profile getProfile() throws DataProviderException;

    /**
     * Gets called before exiting the program. Implement this method if you have open resources to close
     */
    default void close() {
    }
}
