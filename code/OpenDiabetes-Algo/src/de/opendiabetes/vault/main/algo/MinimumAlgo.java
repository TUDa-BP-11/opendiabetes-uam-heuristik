package de.opendiabetes.vault.main.algo;

import de.opendiabetes.vault.main.dataprovider.AlgorithmDataProvider;
import de.opendiabetes.vault.main.math.Predictions;
import de.opendiabetes.vault.parser.Profile;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.util.TimestampUtils;

import java.util.List;

public class MinimumAlgo extends Algorithm {

    public MinimumAlgo(long absorptionTime, long insulinDuration, Profile profile) {
        super(absorptionTime, insulinDuration, profile);
    }

    public MinimumAlgo(long absorptionTime, long insulinDuration, AlgorithmDataProvider dataProvider) {
        super(absorptionTime, insulinDuration, dataProvider);
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

                currentPrediction = Predictions.predict(current.getTimestamp().getTime(), meals, bolusTreatments, basalTreatments, profile.getSensitivity(), insulinDuration, profile.getCarbratio(), absorptionTime);

                nextPrediction = Predictions.predict(next.getTimestamp().getTime(), meals, bolusTreatments, basalTreatments, profile.getSensitivity(), insulinDuration, profile.getCarbratio(), absorptionTime);

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
