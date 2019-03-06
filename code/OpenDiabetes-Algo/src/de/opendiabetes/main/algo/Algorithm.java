package de.opendiabetes.main.algo;

import de.opendiabetes.main.dataprovider.AlgorithmDataProvider;
import de.opendiabetes.parser.Profile;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;

import java.util.ArrayList;
import java.util.List;

public abstract class Algorithm {

    protected long absorptionTime;
    protected long insulinDuration;
    protected Profile profile;
    protected List<VaultEntry> glucose;
    protected List<VaultEntry> bolusTreatments;
    protected List<VaultEntry> basalTreatments;

    public Algorithm(long absorptionTime, long insulinDuration, Profile profile) {
        this.absorptionTime = absorptionTime;
        this.insulinDuration = insulinDuration;
        this.profile = profile;
        glucose = new ArrayList<>();
        bolusTreatments = new ArrayList<>();
        basalTreatments = new ArrayList<>();
    }

    public Algorithm(long absorptionTime, long insulinDuration, AlgorithmDataProvider dataProvider) {
        this.absorptionTime = absorptionTime;
        this.insulinDuration = insulinDuration;
        setDataProvider(dataProvider);
    }

    /**
     * Set the time needed to absorb a meal completely
     *
     * @param absorptionTime absoption time in minutes
     */
    public void setAbsorptionTime(long absorptionTime) {
        this.absorptionTime = absorptionTime;
    }

    /**
     * Set the effective time of an insulin treatment
     *
     * @param insulinDuration insulin duration in minutes
     */
    public void setInsulinDuration(long insulinDuration) {
        this.insulinDuration = insulinDuration;
    }

    /**
     * Set a profile
     *
     * @param profile profile
     */
    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    /**
     * Set a list of glucose measurements for calculation
     *
     * @param entries list of VaultEntries with type
     * {@link de.opendiabetes.vault.container.VaultEntryType#GLUCOSE_CGM}
     */
    public void setGlucoseMeasurements(List<VaultEntry> entries) {
        if (!entries.isEmpty()) {
            VaultEntry current = entries.get(0);
            for (VaultEntry entry : entries){
                if(entry.getTimestamp().getTime() - current.getTimestamp().getTime() < 0){
                    throw new IllegalArgumentException("entries have to be sorted by timestamp");
                }
                if (entry.getType().equals(VaultEntryType.GLUCOSE_CGM)){
                    throw new IllegalArgumentException("VaultEntryType should be GLUCOSE_CGM but was" + entry.getType().toString());
                }
            }
        }


        this.glucose = entries;
    }

    /**
     * Set a list of insulin bolus treatments for calculation
     *
     * @param bolusTreatments list of VaultEntries with type
     * {@link de.opendiabetes.vault.container.VaultEntryType#BOLUS_NORMAL}
     */
    public void setBolusTreatments(List<VaultEntry> bolusTreatments) {
        if (!bolusTreatments.isEmpty()) {
            VaultEntry current = bolusTreatments.get(0);
            for (VaultEntry entry : bolusTreatments){
                if(entry.getTimestamp().getTime() - current.getTimestamp().getTime() < 0){
                    throw new IllegalArgumentException("bolusTreatments have to be sorted by timestamp");
                }
                if (entry.getType().equals(VaultEntryType.BOLUS_NORMAL)){
                    throw new IllegalArgumentException("VaultEntryType should be BOLUS_NORMAL but was" + entry.getType().toString());
                }
            }
        }
        this.bolusTreatments = bolusTreatments;
    }

    /**
     * Set a list of insulin bolus treatments for calculation
     *
     * @param basalTreatments list of VaultEntries with type
     * {@link de.opendiabetes.vault.container.VaultEntryType#BASAL_PROFILE}
     */
    public void setBasalTreatments(List<VaultEntry> basalTreatments) {
        if (!basalTreatments.isEmpty()) {
            VaultEntry current = basalTreatments.get(0);
            for (VaultEntry entry : basalTreatments){
                if(entry.getTimestamp().getTime() - current.getTimestamp().getTime() < 0){
                    throw new IllegalArgumentException("basalTreatments have to be sorted by timestamp");
                }
                if (entry.getType().equals(VaultEntryType.BASAL_PROFILE)){
                    throw new IllegalArgumentException("VaultEntryType should be BASAL_PROFILE but was" + entry.getType().toString());
                }
            }
        }
        this.basalTreatments = basalTreatments;
    }

    /**
     * Uses a data provider to invoke {@link #setGlucoseMeasurements(List)}, {@link #setBolusTreatments(List)},
     * {@link #setBasalTreatments(List)} and {@link #setProfile(Profile)}
     *
     * @param dataProvider the data provider
     */
    public void setDataProvider(AlgorithmDataProvider dataProvider) {
        this.setProfile(dataProvider.getProfile());
        this.setGlucoseMeasurements(dataProvider.getGlucoseMeasurements());
        this.setBolusTreatments(dataProvider.getBolusTreatments());
        this.setBasalTreatments(dataProvider.getBasalTratments());
    }

    /**
     * Starts the calculation of predicted meals
     *
     * @return a list of VaultEntries with type
     * {@link de.opendiabetes.vault.container.VaultEntryType#MEAL_MANUAL}
     */
    public abstract List<VaultEntry> calculateMeals();
}
