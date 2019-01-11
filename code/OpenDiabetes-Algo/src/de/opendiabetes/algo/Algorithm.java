package de.opendiabetes.algo;

import de.opendiabetes.main.dataprovider.AlgorithmDataProvider;
import de.opendiabetes.vault.engine.container.VaultEntry;

import java.util.List;

public interface Algorithm {
    /**
     * Set the ratio of carbs to glucose conversion
     *
     * @param carbRatio ratio
     */
    void setCarbRatio(double carbRatio);

    /**
     * Set the insulin sensitivity
     *
     * @param insSensitivity insulin sensitivity
     */
    void setInsulinSensitivity(double insSensitivity);

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
     * @param basalTratments list of TempBasal treatments
     */
    void setBasalTratments(List<TempBasal> basalTratments);

    /**
     * Uses a data provider to invoke {@link #setGlucoseMeasurements(List)}, {@link #setBolusTreatments(List)} and {@link #setBasalTratments(List)}
     *
     * @param dataProvider the data provider
     */
    default void setDataProvider(AlgorithmDataProvider dataProvider) {
        this.setGlucoseMeasurements(dataProvider.getGlucoseMeasurements());
        this.setBolusTreatments(dataProvider.getBolusTreatments());
        this.setBasalTratments(dataProvider.getBasalTratments());
    }

    /**
     * Starts the calculation of predicted meals
     *
     * @return a list of VaultEntries with type {@link de.opendiabetes.vault.engine.container.VaultEntryType#MEAL_MANUAL}
     */
    List<VaultEntry> calculateMeals();
}
