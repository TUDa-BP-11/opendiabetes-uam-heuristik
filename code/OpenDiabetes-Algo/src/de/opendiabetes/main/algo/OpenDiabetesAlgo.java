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
    private List<TempBasal> basalTratments;

    public OpenDiabetesAlgo() {
        absorptionTime = 120;
        insDuration = 180;
        carbRatio = 10;
        insSensitivityFactor = 35;
        glucose = new ArrayList<>();
        bolusTreatments = new ArrayList<>();
        mealTreatments = new ArrayList<>();
        basalTratments = new ArrayList<>();
    }

    public OpenDiabetesAlgo(double absorptionTime, double insDuration, double carbRatio, double insSensitivityFactor) {
        this.absorptionTime = absorptionTime;
        this.insDuration = insDuration;
        this.carbRatio = carbRatio;
        this.insSensitivityFactor = insSensitivityFactor;
        glucose = new ArrayList<>();
        bolusTreatments = new ArrayList<>();
        mealTreatments = new ArrayList<>();
        basalTratments = new ArrayList<>();
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
    public void setBasalTratments(List<TempBasal> basalTratments) {
        this.basalTratments = basalTratments;
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

            double currentPrediction = predict(0, current.getTimestamp().getTime());
            double nextPrediction = predict(0, next.getTimestamp().getTime());
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

    private double predict(double startValue, long time) {
        double result = startValue;
        for (VaultEntry meal : mealTreatments) {
            long deltaTime = Math.round((time - meal.getTimestamp().getTime()) / 60000.0);  //Time in minutes
            if (deltaTime <= 0) {
                break;
            }
            result += Predictions.deltaBGC(deltaTime, insSensitivityFactor, carbRatio, meal.getValue(), absorptionTime);
        }
        for (VaultEntry bolus : bolusTreatments) {
            long deltaTime = Math.round((time - bolus.getTimestamp().getTime()) / 60000.0); //Time in minutes
            if (deltaTime <= 0) {
                break;
            }
            result += Predictions.deltaBGI(deltaTime, bolus.getValue(), insSensitivityFactor, insDuration);
        }
        for (TempBasal basal : basalTratments) {
            long deltaTime = Math.round((time - basal.getDate().getTime()) / 60000.0);      //Time in minutes
            if (deltaTime <= 0) {
                break;
            }
            double unitsPerMin = basal.getValue() / basal.getDuration();
            result += Predictions.deltatempBGI(deltaTime, unitsPerMin, insSensitivityFactor, insDuration, 0, basal.getDuration());
        }

        return result;
    }
}
