package de.opendiabetes.main.algo;

import de.opendiabetes.main.dataprovider.AlgorithmDataProvider;
import de.opendiabetes.main.math.Predictions;
import de.opendiabetes.parser.Profile;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.util.TimestampUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OpenDiabetesAlgo extends Algorithm {


    public OpenDiabetesAlgo(double absorptionTime, double insulinDuration, Profile profile) {
        super(absorptionTime, insulinDuration, profile);
    }

    public OpenDiabetesAlgo(double absorptionTime, double insulinDuration, AlgorithmDataProvider dataProvider) {
        super(absorptionTime, insulinDuration, dataProvider);
    }

    @Override
    public List<VaultEntry> calculateMeals() {
        List<VaultEntry> mealTreatments = new ArrayList<>();
        VaultEntry current = glucose.remove(0);
        long firstTime = current.getTimestamp().getTime()/1000;
        VaultEntry meal;

        while (!glucose.isEmpty() && current.getTimestamp().getTime()/1000 - firstTime <= 10*24*60*60) {
            VaultEntry next = glucose.get(0);
            long deltaTime = Math.round((next.getTimestamp().getTime() - current.getTimestamp().getTime()) / 60000.0);

            for (int i = 1; i < glucose.size() && deltaTime < 30; i++) {
                next = glucose.get(i);
                deltaTime = Math.round((next.getTimestamp().getTime() - current.getTimestamp().getTime()) / 60000.0);
            }

            double currentPrediction = Predictions.predict(current.getTimestamp().getTime(), mealTreatments, bolusTreatments, basalTreatments, profile.getSensitivity(), insulinDuration, profile.getCarbratio(), absorptionTime);
            double nextPrediction = Predictions.predict(next.getTimestamp().getTime(), mealTreatments, bolusTreatments, basalTreatments, profile.getSensitivity(), insulinDuration, profile.getCarbratio(), absorptionTime);
            double deltaBg = next.getValue() - current.getValue();
            double deltaPrediction = (nextPrediction - currentPrediction);

            if (deltaBg - deltaPrediction > 0) {
                meal = createMeal(deltaBg - deltaPrediction, deltaTime, current.getTimestamp());
                mealTreatments.add(meal);
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
