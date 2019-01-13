package de.opendiabetes.main.algo;

import de.opendiabetes.main.dataprovider.AlgorithmDataProvider;
import de.opendiabetes.parser.Profile;
import de.opendiabetes.vault.engine.container.VaultEntry;

import java.util.List;

public interface Algorithm {
    /**
     * Set the time needed to absorb a meal completely
     *
     * @param absorptionTime absoption time in minutes
     */
    void setAbsorptionTime(double absorptionTime);

    /**
     * Set the effective time of an insulin treatment
     *
     * @param insulinDuration insulin duration in minutes
     */
    void setInsulinDuration(double insulinDuration);

    /**
     * Provide a profile
     *
     * @param profile profile
     */
    void setProfile(Profile profile);

    /**
     * Provide a list of glucose measurements for calculation
     *
     * @param entries list of VaultEntries with type {@link de.opendiabetes.vault.engine.container.VaultEntryType#GLUCOSE_CGM}
     */
    void setGlucoseMeasurements(List<VaultEntry> entries);

    /**
     * Provide a list of insulin bolus treatments for calculation
     *
     * @param bolusTreatments list of VaultEntries with type {@link de.opendiabetes.vault.engine.container.VaultEntryType#BOLUS_NORMAL}
     */
    void setBolusTreatments(List<VaultEntry> bolusTreatments);

    /**
     * Provide a list of insulin bolus treatments for calculation
     *
     * @param basalTreatments list of TempBasal treatments
     */
    void setBasalTreatments(List<TempBasal> basalTreatments);

    /**
     * Uses a data provider to invoke {@link #setGlucoseMeasurements(List)}, {@link #setBolusTreatments(List)},
     * {@link #setBasalTreatments(List)} and {@link #setProfile(Profile)}
     *
     * @param dataProvider the data provider
     */
    default void setDataProvider(AlgorithmDataProvider dataProvider) {
        this.setProfile(dataProvider.getProfile());
        this.setGlucoseMeasurements(dataProvider.getGlucoseMeasurements());
        this.setBolusTreatments(dataProvider.getBolusTreatments());
        this.setBasalTreatments(dataProvider.getBasalTratments());
    }

    /**
     * Starts the calculation of predicted meals
     *
     * @return a list of VaultEntries with type {@link de.opendiabetes.vault.engine.container.VaultEntryType#MEAL_MANUAL}
     */
    List<VaultEntry> calculateMeals();
}
