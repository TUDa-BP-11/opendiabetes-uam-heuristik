package de.opendiabetes.main.algo;

import de.opendiabetes.main.math.Predictions;
import de.opendiabetes.parser.Profile;
import de.opendiabetes.vault.engine.container.VaultEntry;
import de.opendiabetes.vault.engine.container.VaultEntryType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BruteForceAlgo implements Algorithm {
    private double absorptionTime;
    private double insDuration;
    private Profile profile;
    private List<VaultEntry> glucose;
    private List<VaultEntry> bolusTreatments;
    private List<VaultEntry> mealTreatments;
    private List<TempBasal> basalTratments;

    public BruteForceAlgo() {
        glucose = new ArrayList<>();
        bolusTreatments = new ArrayList<>();
        mealTreatments = new ArrayList<>();
        basalTratments = new ArrayList<>();
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
        this.basalTratments = basalTreatments;
    }

    @Override
    public List<VaultEntry> calculateMeals() {
        glucose.sort(Comparator.comparing(VaultEntry::getTimestamp));
        List<VaultEntry> meals = new ArrayList<>();
        VaultEntry current = glucose.remove(0);
        while (!glucose.isEmpty()) {
            VaultEntry next;
            long deltaTime;
            double deltaBG;
            do {
                next = glucose.remove(0);
                deltaTime = Math.round((next.getTimestamp().getTime() - current.getTimestamp().getTime()) / 60000D);
                deltaBG = next.getValue() - current.getValue();
            } while (!glucose.isEmpty() && deltaTime < 10 && Math.abs(deltaBG) < 1);

            if (deltaTime < 10 && Math.abs(deltaBG) < 1) {
                double find = findBruteForce(deltaTime, deltaBG, 20);
                meals.add(new VaultEntry(VaultEntryType.MEAL_MANUAL, current.getTimestamp(), find));
            }
            current = next;
        }
        return meals;
    }

    private double findBruteForce(long deltaT, double deltaBG, double carbsAmount) {
        double test = deltaBGC(deltaT, profile.getSensitivity(), profile.getCarbratio(), carbsAmount, absorptionTime);
        if (Math.abs(test - deltaBG) <= 0.1)
            return carbsAmount;
        if (test < deltaBG)
            return findBruteForce(deltaT, deltaBG, carbsAmount * 1.5);
        return findBruteForce(deltaT, deltaBG, carbsAmount * 0.75);
    }

    private double deltaBGC(double timeFromEvent, double insSensitivityFactor, double carbRatio, double carbsAmount, double absorptionTime) {
        return insSensitivityFactor / carbRatio * carbsAmount * Predictions.carbsOnBoard(timeFromEvent, absorptionTime);
    }
}
