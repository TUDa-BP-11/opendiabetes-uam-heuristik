package de.opendiabetes.main.algo;

import de.opendiabetes.main.math.Predictions;
import de.opendiabetes.parser.Profile;
import de.opendiabetes.vault.engine.container.VaultEntry;
import de.opendiabetes.vault.engine.container.VaultEntryType;
import de.opendiabetes.vault.engine.util.TimestampUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OpenDiabetesAlgo implements Algorithm {
    private double absorptionTime;
    private double insDuration;
    private Profile profile;
    private List<VaultEntry> glucose;
    private List<VaultEntry> bolusTreatments;
    private List<TempBasal> basalTreatments;

    public OpenDiabetesAlgo() {
        absorptionTime = 120;
        insDuration = 180;
        glucose = new ArrayList<>();
        bolusTreatments = new ArrayList<>();
        basalTreatments = new ArrayList<>();
    }

    public OpenDiabetesAlgo(double absorptionTime, double insDuration, Profile profile) {
        this.absorptionTime = absorptionTime;
        this.insDuration = insDuration;
        this.profile = profile;
        glucose = new ArrayList<>();
        bolusTreatments = new ArrayList<>();
        basalTreatments = new ArrayList<>();
    }

    public double getCarbRatio() {
        return profile.getCarbratio();
    }

    public double getInsulinSensitivity() {
        return profile.getSensitivity();
    }

    @Override
    public void setAbsorptionTime(double absorptionTime) {
        this.absorptionTime = absorptionTime;
    }

    public double getAbsorptionTime() {
        return absorptionTime;
    }

    @Override
    public void setInsulinDuration(double insulinDuration) {
        this.insDuration = insulinDuration;
    }

    public double getInsulinDuration() {
        return insDuration;
    }

    @Override
    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    @Override
    public void setGlucoseMeasurements(List<VaultEntry> glucose) {
        this.glucose = new ArrayList<>(glucose);
    }

    @Override
    public void setBolusTreatments(List<VaultEntry> bolusTreatments) {
        this.bolusTreatments = bolusTreatments;
    }

    @Override
    public void setBasalTreatments(List<TempBasal> basalTreatments) {
        this.basalTreatments = basalTreatments;
    }

    @Override
    public List<VaultEntry> calculateMeals() {
        List<VaultEntry> mealTreatments = new ArrayList<>();
        VaultEntry current = glucose.remove(0);

        while (!glucose.isEmpty()) {
            VaultEntry next = glucose.get(0);
            long deltaTime = Math.round((next.getTimestamp().getTime() - current.getTimestamp().getTime()) / 60000.0);

            for (int i = 1; i < glucose.size() && deltaTime < 30; i++) {
                next = glucose.get(i);
                deltaTime = Math.round((next.getTimestamp().getTime() - current.getTimestamp().getTime()) / 60000.0);

            }

            double currentPrediction = Predictions.predict(current.getTimestamp().getTime(), mealTreatments, bolusTreatments, basalTreatments, profile.getSensitivity(), insDuration, profile.getCarbratio(), absorptionTime);
            double nextPrediction = Predictions.predict(next.getTimestamp().getTime(), mealTreatments, bolusTreatments, basalTreatments, profile.getSensitivity(), insDuration, profile.getCarbratio(), absorptionTime);
            double deltaBg = next.getValue() - current.getValue();
            double deltaPrediction = (nextPrediction - currentPrediction);

            if (deltaBg - deltaPrediction > 0) {
                mealTreatments.add(createMeal(deltaBg - deltaPrediction, deltaTime, current.getTimestamp()));
            }
            current = glucose.remove(0);
        }

        return mealTreatments;
    }

    private VaultEntry createMeal(double deltaBg, double deltaTime, Date timestamp) {
        double value = deltaBg * profile.getCarbratio() / (profile.getSensitivity() * Predictions.carbsOnBoard(deltaTime, absorptionTime));
        return new VaultEntry(VaultEntryType.MEAL_MANUAL, TimestampUtils.createCleanTimestamp(timestamp), value);
    }
}
