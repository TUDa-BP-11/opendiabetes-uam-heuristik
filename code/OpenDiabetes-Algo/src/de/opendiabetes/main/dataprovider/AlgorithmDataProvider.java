package de.opendiabetes.main.dataprovider;

import de.opendiabetes.main.algo.TempBasal;
import de.opendiabetes.vault.engine.container.VaultEntry;

import java.util.List;

public interface AlgorithmDataProvider {

    /**
     * Get a list of insulin bolus treatments for calculation
     *
     * @return list of VaultEntries with type {@link de.opendiabetes.vault.engine.container.VaultEntryType#GLUCOSE_CGM}
     */
    List<VaultEntry> getGlucoseMeasurements();

    /**
     * Get a list of insulin bolus treatments for calculation
     *
     * @return list of VaultEntries with type {@link de.opendiabetes.vault.engine.container.VaultEntryType#BOLUS_NORMAL}
     */
    List<VaultEntry> getBolusTreatments();

    /**
     * Get a list of insulin bolus treatments for calculation
     *
     * @return list of TempBasal entries
     */
    List<TempBasal> getBasalTratments();

    /**
     * Gets called before exiting the program. Implement this method if you have open resources to close
     */
    default void close() {
    }
}
