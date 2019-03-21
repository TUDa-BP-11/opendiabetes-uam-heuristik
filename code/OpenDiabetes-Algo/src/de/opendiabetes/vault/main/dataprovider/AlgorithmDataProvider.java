package de.opendiabetes.vault.main.dataprovider;


import de.opendiabetes.vault.parser.Profile;
import de.opendiabetes.vault.container.VaultEntry;

import java.util.List;

public interface AlgorithmDataProvider {
    /**
     * Get a list of insulin bolus treatments for calculation
     *
     * @return list of VaultEntries with type {@link de.opendiabetes.vault.container.VaultEntryType#GLUCOSE_CGM}
     */
    List<VaultEntry> getGlucoseMeasurements();

    /**
     * Get a list of insulin bolus treatments for calculation
     *
     * @return list of VaultEntries with type {@link de.opendiabetes.vault.container.VaultEntryType#BOLUS_NORMAL}
     */
    List<VaultEntry> getBolusTreatments();

    /**
     * Get a list of unmodified basal treatments
     *
     * @return @return list of VaultEntries with type {@link de.opendiabetes.vault.container.VaultEntryType#BASAL_MANUAL}
     */
    List<VaultEntry> getRawBasalTreatments();

    /**
     * Get a a list of differences between your temp basal Treatments and the basal rate in the profile
     *
     * @return list of VaultEntries with type {@link de.opendiabetes.vault.container.VaultEntryType#BASAL_PROFILE}
     */
    List<VaultEntry> getBasalDifferences();

    /**
     * @return The Nightscout profile to use
     */
    Profile getProfile();

    /**
     * Gets called before exiting the program. Implement this method if you have open resources to close
     */
    default void close() {
    }
}
