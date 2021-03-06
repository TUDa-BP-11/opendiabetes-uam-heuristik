package de.opendiabetes.vault.main.algo;

import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.main.math.BasalCalculatorTools;
import de.opendiabetes.vault.main.math.Predictions;
import de.opendiabetes.vault.parser.Profile;

import java.util.ArrayList;
import java.util.List;

public abstract class Algorithm {

    protected long absorptionTime;
    protected long insulinDuration;
    protected double peak;
    protected Profile profile;
    protected List<VaultEntry> glucose;
    protected List<VaultEntry> bolusTreatments;
    protected List<VaultEntry> basalTreatments;
    protected List<VaultEntry> meals;

    /**
     * Creates a new Algorithm instance. The given data is checked for validity.
     *
     * @param absorptionTime      carbohydrate absorption time
     * @param insulinDuration     insulin duration
     * @param peak                duration in minutes until insulin action reaches it’s peak activity level
     * @param profile             user profile
     * @param glucoseMeasurements glucose measurements
     * @param bolusTreatments     bolus treatments
     * @param basalTreatments     raw basal treatments
     */
    public Algorithm(long absorptionTime, long insulinDuration, double peak, Profile profile, List<VaultEntry> glucoseMeasurements, List<VaultEntry> bolusTreatments, List<VaultEntry> basalTreatments) {
        this.absorptionTime = absorptionTime;
        this.insulinDuration = insulinDuration;
        this.peak = peak;
        this.profile = profile;
        setGlucoseMeasurements(glucoseMeasurements);
        setBolusTreatments(bolusTreatments);
        setBasalTreatments(basalTreatments);
        this.meals = new ArrayList<>();
    }

    /**
     * Set a list of glucose measurements for calculation
     *
     * @param entries list of VaultEntries with type {@link de.opendiabetes.vault.container.VaultEntryType#GLUCOSE_CGM}
     * @throws IllegalArgumentException if the entries are not sorted
     */
    public final void setGlucoseMeasurements(List<VaultEntry> entries) {
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
     * @param bolusTreatments list of VaultEntries with type {@link de.opendiabetes.vault.container.VaultEntryType#BOLUS_NORMAL}
     */
    public final void setBolusTreatments(List<VaultEntry> bolusTreatments) {
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
     * Set a list of insulin bolus treatments for calculation. Adjusts all treatments using {@link BasalCalculatorTools#calcBasalDifference(List, Profile)}.
     *
     * @param basalTreatments list of VaultEntries with type {@link de.opendiabetes.vault.container.VaultEntryType#BASAL_PROFILE}
     */
    public final void setBasalTreatments(List<VaultEntry> basalTreatments) {
        basalTreatments = BasalCalculatorTools.calcBasalDifference(BasalCalculatorTools.adjustBasalTreatments(basalTreatments), profile);
        this.basalTreatments = basalTreatments;
    }

    /**
     * Sets the Profile for this calculation.
     *
     * @param profile Profile
     */
    public void setProfile(Profile profile) {
        this.profile = profile;
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
        startValue = glucose.get(startIndex).getValue() -
                Predictions.predict(glucose.get(startIndex).getTimestamp().getTime(), meals, bolusTreatments, basalTreatments, profile.getSensitivity(), insulinDuration, profile.getCarbratio(), absorptionTime, peak);

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
        return i;
    }

    public long getStartTime() {
        if (glucose.isEmpty()) {
            return 0;
        }
        long startTime = glucose.get(0).getTimestamp().getTime();
        long firstValidTime = startTime + Math.max(insulinDuration, absorptionTime) * 60000;
        for (int i = 0; i < glucose.size() - 1; i++) {
            startTime = glucose.get(i).getTimestamp().getTime();
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

    /**
     * @return Duration in minutes until insulin action reaches it’s peak activity level
     */
    public double getPeak() {
        return peak;
    }
}
