package de.opendiabetes.vault.main.algo;

import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.main.math.Predictions;
import de.opendiabetes.vault.parser.Profile;
import de.opendiabetes.vault.util.TimestampUtils;

import java.util.Date;
import java.util.List;

import static java.lang.Math.pow;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.QRDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

/**
 * The algorithm calculates meals based on curve fitting using QR decomposition.
 * It fits the parabolic first half of the COB-curve to a time-limitted part of
 * the glucose signal under consideration of insulin values and previously
 * estimated meals. It estimates a meal value for the current sample time from
 * the quadratic constant of the parabolic equation. For more information please
 * visit our github wiki:
 * https://github.com/TUDa-BP-11/opendiabetes-uam-heuristik/wiki/Algorithm
 */
public class QRAlgo extends Algorithm {

    /**
     * Creates a new QRAlgo instance. The given data is checked for validity.
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
    public QRAlgo(long absorptionTime, long insulinDuration, double peak, Profile profile, List<VaultEntry> glucoseMeasurements, List<VaultEntry> bolusTreatments, List<VaultEntry> basalTreatments) {
        super(absorptionTime, insulinDuration, peak, profile, glucoseMeasurements, bolusTreatments, basalTreatments);
    }

    @Override
    public List<VaultEntry> calculateMeals() {
        RealMatrix matrix;
        RealVector nkbg;
        RealVector times;

        VaultEntry meal;
        VaultEntry next;
        VaultEntry current;

        long estimatedTime;
        long currentTime;
        long nextTime;
        double currentLimit;
        long estimatedTimeAccepted = 0l;
        double nextPrediction;
        double deltaBg;

        final long firstTime = glucose.get(0).getTimestamp().getTime() / 60000 + Math.max(absorptionTime, insulinDuration);

        meals.clear();
        int startIndex = getStartIndex();
        double startValue = glucose.get(startIndex).getValue();
        for (int i = startIndex; i < glucose.size(); i++) {
            current = glucose.get(i);
            currentTime = current.getTimestamp().getTime() / 60000;

            nkbg = new ArrayRealVector();
            times = new ArrayRealVector();
            if (currentTime > estimatedTimeAccepted) {

                currentLimit = currentTime + absorptionTime / 6;

                for (int j = i; j < glucose.size(); j++) {

                    next = glucose.get(j);
                    nextTime = next.getTimestamp().getTime() / 60000;
//                    double nextValue = Filter.getMedian(glucose, j, 5, absorptionTime / 3);
                    //double nextValue = Filter.getAverage(glucose, j, 5, absorptionTime / 3);
                    //double nextValue = next.getValue();
                    if (nextTime <= currentLimit) {

                        nextPrediction = Predictions.predict(next.getTimestamp().getTime(), meals, bolusTreatments, basalTreatments,
                                profile.getSensitivity(), insulinDuration, profile.getCarbratio(), absorptionTime, peak);

                        deltaBg = next.getValue() - nextPrediction;
                        times = times.append(nextTime - currentTime);
                        nkbg = nkbg.append(deltaBg - startValue);
                    }
                }

                if (times.getDimension() >= 3) {
                    matrix = new Array2DRowRealMatrix(times.getDimension(), 2);
                    matrix.setColumnVector(0, times.ebeMultiply(times));
                    times.set(1);
                    matrix.setColumnVector(1, times);

                    DecompositionSolver solver = new QRDecomposition(matrix).getSolver();
                    RealVector solution = solver.solve(nkbg);
                    double alpha;
                    alpha = solution.getEntry(0);
                    estimatedTime = currentTime;
                    double estimatedCarbs = alpha * pow(absorptionTime, 2) * profile.getCarbratio() / (2 * profile.getSensitivity());

                    if (estimatedCarbs > 0) {
                        estimatedTimeAccepted = estimatedTime;
                        meal = new VaultEntry(VaultEntryType.MEAL_MANUAL,
                                TimestampUtils.createCleanTimestamp(new Date(estimatedTime * 60000)),
                                estimatedCarbs);
                        meals.add(meal);
                    }
                }
            }
        }

        return meals;
    }
}
