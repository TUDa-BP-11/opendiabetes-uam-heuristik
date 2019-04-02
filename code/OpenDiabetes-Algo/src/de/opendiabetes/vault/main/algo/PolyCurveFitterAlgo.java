package de.opendiabetes.vault.main.algo;

import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.main.math.Predictions;
import de.opendiabetes.vault.parser.Profile;
import de.opendiabetes.vault.util.TimestampUtils;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.lang.Math.pow;

/**
 * The algorithm calculates meals based on a polynomial curve fitting.
 * It fits the parabolic first half of the COB-curve to a time-limitted part of the glucose signal under consideration of insulin values and previously estimated meals. It estimates a meal with time and value from the constants of the parabolic equation.
 * For more information please visit our github wiki:
 * https://github.com/TUDa-BP-11/opendiabetes-uam-heuristik/wiki/Algorithm
 */

public class PolyCurveFitterAlgo extends Algorithm {

    /**
     * Creates a new PolyCurveFitterAlgo instance. The given data is checked for
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
    public PolyCurveFitterAlgo(long absorptionTime, long insulinDuration, double peak, Profile profile, List<VaultEntry> glucoseMeasurements, List<VaultEntry> bolusTreatments, List<VaultEntry> basalTreatments) {
        super(absorptionTime, insulinDuration, peak, profile, glucoseMeasurements, bolusTreatments, basalTreatments);
    }

    @Override
    public List<VaultEntry> calculateMeals() {

        double weight = 1;
        meals.clear();
        PolynomialCurveFitter pcf = PolynomialCurveFitter.create(2);
        ArrayList<WeightedObservedPoint> observations = new ArrayList<>();

        // Optional extension: set initial value to medium sized carbs and no offsets
        double[] initialValues = {0, 0, 0};
        pcf = pcf.withStartPoint(initialValues);
        pcf = pcf.withMaxIterations(100);
        VaultEntry meal;
        VaultEntry next;
        VaultEntry current;

        long estimatedTime;
        long currentTime;
        long nextTime;
        long lastTime = 0l;
        double currentLimit;
        long estimatedTimeAccepted = 0l;
        double currentPrediction;
        double nextPrediction;
        double deltaBg;

        double startValue;
        double nextValue;

        final long firstTime = glucose.get(0).getTimestamp().getTime() / 60000 + Math.max(absorptionTime, insulinDuration);
        for (int i = 0; i < glucose.size(); i++) {
            current = glucose.get(i);

            currentTime = current.getTimestamp().getTime() / 60000;

            // skip bg values until start time
            if (currentTime < firstTime) {
                continue;
            }
            currentLimit = currentTime + absorptionTime / 4;
            if (currentTime > estimatedTimeAccepted) {

                startValue = current.getValue();
                currentPrediction = Predictions.predict(current.getTimestamp().getTime(), meals, bolusTreatments, basalTreatments,
                        profile.getSensitivity(), insulinDuration, profile.getCarbratio(), absorptionTime, peak);

                for (int j = i; j < glucose.size(); j++) {
                    next = glucose.get(j);
                    nextTime = next.getTimestamp().getTime() / 60000;
                    nextValue = next.getValue();
                    if (nextTime <= currentLimit) {

                        nextPrediction = Predictions.predict(next.getTimestamp().getTime(), meals, bolusTreatments, basalTreatments, profile.getSensitivity(), insulinDuration, profile.getCarbratio(), absorptionTime, peak);
                        deltaBg = nextValue - startValue - (nextPrediction - currentPrediction);
                        lastTime = nextTime;
                        observations.add(new WeightedObservedPoint(weight, nextTime, deltaBg));
                    }
                }
                // lsq = [c, b, a]
                double[] lsq = pcf.fit(observations);
                double alpha, beta;
                alpha = lsq[2];
                beta = lsq[1];
                if (alpha > 0) {
                    estimatedTime = (long) (-beta / (2 * alpha));
                    double estimatedCarbs = lsq[2] * pow(absorptionTime, 2) * profile.getCarbratio() / (2 * profile.getSensitivity());
                    if (currentTime - estimatedTime < absorptionTime / 2
                            && estimatedTime < lastTime) {

                        estimatedTimeAccepted = estimatedTime;
                        meal = new VaultEntry(VaultEntryType.MEAL_MANUAL,
                                TimestampUtils.createCleanTimestamp(new Date(estimatedTime * 60000)),
                                estimatedCarbs);
                        meals.add(meal);
                    }
                }
            }
            observations.clear();
        }

        return meals;
    }
}
