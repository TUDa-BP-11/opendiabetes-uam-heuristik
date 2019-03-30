package de.opendiabetes.vault.main.algo;

import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.main.math.Predictions;
import de.opendiabetes.vault.parser.Profile;
import de.opendiabetes.vault.util.TimestampUtils;

import java.util.List;

public class MinimumAlgo extends Algorithm {

    /**
     * Creates a new MinimumAlgo instance. The given data is checked for
     * validity.
     *
     * @param absorptionTime carbohydrate absorption time
     * @param insulinDuration effective insulin duration
     * @param peak duration in minutes until insulin action reaches its peak
     * activity level
     * @param profile user profile
     * @param glucoseMeasurements known glucose measurements
     * @param bolusTreatments known bolus treatments
     * @param basalTreatments known basal treatments
     */
    public MinimumAlgo(long absorptionTime, long insulinDuration, double peak, Profile profile, List<VaultEntry> glucoseMeasurements, List<VaultEntry> bolusTreatments, List<VaultEntry> basalTreatments) {
        super(absorptionTime, insulinDuration, peak, profile, glucoseMeasurements, bolusTreatments, basalTreatments);
    }

    @Override
    public List<VaultEntry> calculateMeals() {
        long dTime;
        double currentPrediction;
        double nextPrediction;
        double deltaBg;
        double deltaPrediction;
        double value;
        double mealValue;
        VaultEntry meal;
        VaultEntry current;
        VaultEntry next;
        for (int i = 0; i < glucose.size(); i++) {
            current = glucose.get(i);

            mealValue = 0;

            for (int j = i + 1; j < glucose.size(); j++) {
                next = glucose.get(j);
                dTime = Math.round((next.getTimestamp().getTime() - current.getTimestamp().getTime()) / 60000.0);
                if (dTime > absorptionTime / 4) {
                    break;
                }

                currentPrediction = Predictions.predict(current.getTimestamp().getTime(), meals, bolusTreatments, basalTreatments,
                        profile.getSensitivity(), insulinDuration, profile.getCarbratio(), absorptionTime, peak);

                nextPrediction = Predictions.predict(next.getTimestamp().getTime(), meals, bolusTreatments, basalTreatments,
                        profile.getSensitivity(), insulinDuration, profile.getCarbratio(), absorptionTime, peak);

                deltaBg = next.getValue() - current.getValue();
                deltaPrediction = (nextPrediction - currentPrediction);
                value = Math.round(calcMealValue(deltaBg - deltaPrediction, dTime) * 1000) / 1000.0;
                if (j == i + 1 || value < mealValue) {
                    mealValue = value;
                }
            }

            if (mealValue > 0) {
                meal = new VaultEntry(VaultEntryType.MEAL_MANUAL, TimestampUtils.createCleanTimestamp(current.getTimestamp()), mealValue);
                meals.add(meal);
            }
        }

        return meals;
    }

    private double calcMealValue(double deltaBg, double deltaTime) {
        return deltaBg * profile.getCarbratio() / (profile.getSensitivity() * Predictions.carbsOnBoard(deltaTime, absorptionTime));
    }
}
