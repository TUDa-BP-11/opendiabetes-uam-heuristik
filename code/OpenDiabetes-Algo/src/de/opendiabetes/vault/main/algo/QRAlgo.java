package de.opendiabetes.vault.main.algo;

import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.main.math.Predictions;
import de.opendiabetes.vault.parser.Profile;
import de.opendiabetes.vault.util.TimestampUtils;
import org.apache.commons.math3.linear.*;

import java.util.Date;
import java.util.List;

import static java.lang.Math.pow;

public class QRAlgo extends Algorithm {

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
