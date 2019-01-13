package de.opendiabetes.main.algo;

import de.opendiabetes.main.math.Predictions;
import de.opendiabetes.vault.engine.container.VaultEntry;
import de.opendiabetes.vault.engine.container.VaultEntryType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OpenDiabetesAlgo implements Algorithm {
    private double absorptionTime;
    private double insDuration;
    private double carbRatio;
    private double insSensitivityFactor;
    private List<VaultEntry> glucose;
    private List<VaultEntry> bolusTreatments;
    private List<VaultEntry> mealTreatments;
    private List<TempBasal> basalTreatments;

    public OpenDiabetesAlgo() {
        absorptionTime = 120;
        insDuration = 180;
        carbRatio = 10;
        insSensitivityFactor = 35;
        glucose = new ArrayList<>();
        bolusTreatments = new ArrayList<>();
        mealTreatments = new ArrayList<>();
        basalTreatments = new ArrayList<>();
    }

    public OpenDiabetesAlgo(double absorptionTime, double insDuration, double carbRatio, double insSensitivityFactor) {
        this.absorptionTime = absorptionTime;
        this.insDuration = insDuration;
        this.carbRatio = carbRatio;
        this.insSensitivityFactor = insSensitivityFactor;
        glucose = new ArrayList<>();
        bolusTreatments = new ArrayList<>();
        mealTreatments = new ArrayList<>();
        basalTreatments = new ArrayList<>();
    }

    @Override
    public void setCarbRatio(double carbRatio) {
        this.carbRatio = carbRatio;
    }

    public double getCarbRatio() {
        return carbRatio;
    }

    @Override
    public void setInsulinSensitivity(double insSensitivity) {
        this.insSensitivityFactor = insSensitivity;
    }

    public double getInsulinSensitivity() {
        return insSensitivityFactor;
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
        mealTreatments = new ArrayList<>();
        VaultEntry current = glucose.remove(0);

        while (!glucose.isEmpty()) {
            VaultEntry next = glucose.get(0);
            long deltaTime = Math.round((next.getTimestamp().getTime() - current.getTimestamp().getTime()) / 60000.0);

            for (int i = 1; i < glucose.size() && deltaTime < 30; i++) {
                next = glucose.get(i);
                deltaTime = Math.round((next.getTimestamp().getTime() - current.getTimestamp().getTime()) / 60000.0);

            }

            double currentPrediction = Predictions.predict(current.getTimestamp().getTime(), mealTreatments, bolusTreatments, basalTreatments, insSensitivityFactor, insDuration, carbRatio, absorptionTime);
            double nextPrediction = Predictions.predict(next.getTimestamp().getTime(), mealTreatments, bolusTreatments, basalTreatments, insSensitivityFactor, insDuration, carbRatio, absorptionTime);
            double deltaBg = next.getValue() - current.getValue();
            double deltaPrediction = (nextPrediction - currentPrediction);

            if (deltaBg - deltaPrediction > 0) {
                createMeal(deltaBg - deltaPrediction, deltaTime, current.getTimestamp());
            }
            current = glucose.remove(0);
        }

        return mealTreatments;
    }

    private void createMeal(double deltaBg, double deltaTime, Date timestamp) {
        double value = deltaBg * carbRatio / (insSensitivityFactor * Predictions.carbsOnBoard(deltaTime, absorptionTime));
        mealTreatments.add(new VaultEntry(VaultEntryType.MEAL_MANUAL, timestamp, value));
    }
}
