package de.opendiabetes.vault.main.algo;

import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.main.dataprovider.AlgorithmDataProvider;
import de.opendiabetes.vault.main.math.Predictions;
import de.opendiabetes.vault.parser.Profile;

import java.util.ArrayList;
import java.util.List;

public abstract class Algorithm {

    protected long absorptionTime;
    protected long insulinDuration;
    protected Profile profile;
    protected List<VaultEntry> meals;
    protected List<VaultEntry> glucose;
    protected List<VaultEntry> bolusTreatments;
    protected List<VaultEntry> basalTreatments;

    public Algorithm(long absorptionTime, long insulinDuration, Profile profile) {
        this.absorptionTime = absorptionTime;
        this.insulinDuration = insulinDuration;
        this.profile = profile;
        meals = new ArrayList<>();
        glucose = new ArrayList<>();
        bolusTreatments = new ArrayList<>();
        basalTreatments = new ArrayList<>();
    }

    public Algorithm(long absorptionTime, long insulinDuration, AlgorithmDataProvider dataProvider) {
        this.absorptionTime = absorptionTime;
        this.insulinDuration = insulinDuration;
        meals = new ArrayList<>();
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
            for (VaultEntry entry : entries) {
                if (entry.getTimestamp().getTime() < current.getTimestamp().getTime()) {
                    throw new IllegalArgumentException("entries have to be sorted by timestamp");
                }
                if (!entry.getType().equals(VaultEntryType.GLUCOSE_CGM)) {
                    throw new IllegalArgumentException("VaultEntryType should be GLUCOSE_CGM but was " + entry.getType().toString());
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
            for (VaultEntry entry : bolusTreatments) {
                if (entry.getTimestamp().getTime() < current.getTimestamp().getTime()) {
                    throw new IllegalArgumentException("bolusTreatments have to be sorted by timestamp");
                }
                if (!entry.getType().equals(VaultEntryType.BOLUS_NORMAL)) {
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
            for (VaultEntry entry : basalTreatments) {
                if (entry.getTimestamp().getTime() < current.getTimestamp().getTime()) {
                    throw new IllegalArgumentException("basalTreatments have to be sorted by timestamp");
                }
                if (!entry.getType().equals(VaultEntryType.BASAL_PROFILE)) {
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
    public final void setDataProvider(AlgorithmDataProvider dataProvider) {
        this.setProfile(dataProvider.getProfile());
        this.setGlucoseMeasurements(dataProvider.getGlucoseMeasurements());
        this.setBolusTreatments(dataProvider.getBolusTreatments());
        this.setBasalTreatments(dataProvider.getBasalDifferences());
    }

    /**
     * Starts the calculation of predicted meals
     *
     * @return a list of VaultEntries with type
     * {@link de.opendiabetes.vault.container.VaultEntryType#MEAL_MANUAL}
     */
    public abstract List<VaultEntry> calculateMeals();

    public double getStartValue() {
        if (glucose.isEmpty()) {
            return 0;
        }
        int startIndex = getStartIndex();
        double startValue;
        startValue = glucose.get(startIndex).getValue() - Predictions.predict(glucose.get(startIndex).getTimestamp().getTime(), meals, bolusTreatments, basalTreatments, profile.getSensitivity(), insulinDuration, profile.getCarbratio(), absorptionTime);

        return startValue;
    }

    public int getStartIndex() {
        if (glucose.isEmpty()) {
            return 0;
        }
        long startTime = glucose.get(0).getTimestamp().getTime();
        long firstValidTime = startTime + Math.max(insulinDuration, absorptionTime) * 60000;
        int i = 0;
        for (; i < glucose.size() - 1; i++) {
            if (glucose.get(i + 1).getTimestamp().getTime() > firstValidTime) {
                break;
            }
        }
        return 0;
    }
    
    public long getStartTime() {
        if (glucose.isEmpty()) {
            return 0;
        }
        long startTime = glucose.get(0).getTimestamp().getTime();
        long firstValidTime = startTime + Math.max(insulinDuration, absorptionTime) * 60000;
        for (int i = 0; i < glucose.size() - 1; i++) {
            //startTime = glucose.get(i).getTimestamp().getTime();
            if (glucose.get(i + 1).getTimestamp().getTime() > firstValidTime) {
                break;
            }
        }
        return startTime;
    }

    /**
     * @return the profile
     */
    public Profile getProfile() {
        return profile;
    }

    /**
     * @return the absorptionTime
     */
    public long getAbsorptionTime() {
        return absorptionTime;
    }

    /**
     * @return the insulinDuration
     */
    public long getInsulinDuration() {
        return insulinDuration;
    }

    /**
     * @return the meals
     */
    public List<VaultEntry> getMeals() {
        return meals;
    }

    /**
     * @return the glucose
     */
    public List<VaultEntry> getGlucose() {
        return glucose;
    }

    /**
     * @return the bolusTreatments
     */
    public List<VaultEntry> getBolusTreatments() {
        return bolusTreatments;
    }

    /**
     * @return the basalTreatments
     */
    public List<VaultEntry> getBasalTreatments() {
        return basalTreatments;
    }
}
