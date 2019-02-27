package de.opendiabetes.main.algo;

import de.opendiabetes.main.dataprovider.AlgorithmDataProvider;
import de.opendiabetes.main.math.Predictions;
import de.opendiabetes.parser.Profile;

import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.util.TimestampUtils;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.lang.Math.pow;

/**
 * @author anna
 */
public class PolyCurveFitterAlgo extends Algorithm {

    public PolyCurveFitterAlgo(double absorptionTime, double insulinDuration, Profile profile) {
        super(absorptionTime, insulinDuration, profile);
    }

    public PolyCurveFitterAlgo(double absorptionTime, double insulinDuration, AlgorithmDataProvider dataProvider) {
        super(absorptionTime, insulinDuration, dataProvider);
    }

    @Override
    public List<VaultEntry> calculateMeals() {

        double weight = 1;
        List<VaultEntry> mealTreatments = new ArrayList<>();
        PolynomialCurveFitter pcf = PolynomialCurveFitter.create(2);
        ArrayList<WeightedObservedPoint> observations = new ArrayList<>();

        // Optional extension: set initial value to medium sized carbs and no offsets
        double[] initialValues = {0, 0, 2 * 10 * profile.getSensitivity() / (absorptionTime * profile.getCarbratio())};
        pcf = pcf.withStartPoint(initialValues);
        pcf = pcf.withMaxIterations(100);
        VaultEntry meal;
        VaultEntry next;
        VaultEntry current;

        long estimatedTime;
        long currentTime;
        long nextTime;
        double lastTime = 0;
        double currentLimit;
        long estimatedTimeAccepted = 0l;
        double currentPrediction;
        double nextPrediction;
        double deltaBg;

        double startValue;
        for (int i = 0; i < glucose.size(); i++) {
            current = glucose.get(i);

            currentTime = current.getTimestamp().getTime() / 60000;
            currentLimit = currentTime + absorptionTime / 4;
            if (currentTime > estimatedTimeAccepted) {
                startValue = current.getValue();
                currentPrediction = Predictions.predict(current.getTimestamp().getTime(), mealTreatments, bolusTreatments,
                        basalTreatments, profile.getSensitivity(), insulinDuration, profile.getCarbratio(), absorptionTime);

                for (int j = i; j < glucose.size(); j++) {
                    next = glucose.get(j);
                    nextTime = next.getTimestamp().getTime() / 60000;
                    if (nextTime <= currentLimit) {
                        nextPrediction = Predictions.predict(next.getTimestamp().getTime(), mealTreatments, bolusTreatments,
                                basalTreatments, profile.getSensitivity(), insulinDuration, profile.getCarbratio(), absorptionTime);
                        deltaBg = next.getValue() - startValue - (nextPrediction - currentPrediction);
                        lastTime = nextTime;
//                        weight = 1 - (nextTime - currentTime) / (absorptionTime / 2);
                        observations.add(new WeightedObservedPoint(weight, nextTime, deltaBg));
                    }
                }
                // lsq = [c, b, a]
                double[] lsq = pcf.fit(observations);
                assert (lsq[2] > 0);
//                double error = lsq[0] - pow(lsq[1], 2) / (4 * lsq[2]);
                estimatedTime = (long) (-lsq[1] / (2 * lsq[2]));
                double estimatedCarbs = lsq[2] * pow(absorptionTime, 2) * profile.getCarbratio() / (2 * profile.getSensitivity());
//                if (currentTime - estimatedTime < absorptionTime / 2
//                        && estimatedTime < lastTime) {
//                    if (estimatedCarbs > 0
//                            && estimatedCarbs < 200 // && error < 10
//                            ) {
                    estimatedTimeAccepted = estimatedTime;
                    meal = new VaultEntry(VaultEntryType.MEAL_MANUAL,
                            TimestampUtils.createCleanTimestamp(new Date(estimatedTime * 60000)),
                            estimatedCarbs);
                    mealTreatments.add(meal);
//                    }
//                }
            }
            observations.clear();
        }

        return mealTreatments;
    }
}
