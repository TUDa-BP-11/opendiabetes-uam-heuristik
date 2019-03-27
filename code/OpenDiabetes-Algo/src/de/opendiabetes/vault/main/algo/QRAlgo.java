package de.opendiabetes.vault.main.algo;

import de.opendiabetes.vault.main.dataprovider.AlgorithmDataProvider;
import de.opendiabetes.vault.main.math.Predictions;
import de.opendiabetes.vault.parser.Profile;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.util.TimestampUtils;
import org.apache.commons.math3.linear.*;

import java.util.Date;
import java.util.List;

import static java.lang.Math.pow;
import java.util.ArrayList;

public class QRAlgo extends Algorithm {

    public QRAlgo(long absorptionTime, long insulinDuration, Profile profile) {
        super(absorptionTime, insulinDuration, profile);
    }

    public QRAlgo(long absorptionTime, long insulinDuration, AlgorithmDataProvider dataProvider) {
        super(absorptionTime, insulinDuration, dataProvider);
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

        final long firstTime = glucose.get(0).getTimestamp().getTime() / 60000;
        
        List<VaultEntry> mealTreatments;
        mealTreatments = new ArrayList<>();

        for (int i = 0; i < glucose.size(); i++) {
            current = glucose.get(i);
            currentTime = current.getTimestamp().getTime() / 60000;
            // skip bg values until start time
            if (currentTime < firstTime) {
                continue;
            }

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

                        nextPrediction = Predictions.predict(next.getTimestamp().getTime(), mealTreatments, bolusTreatments,
                                basalTreatments, profile.getSensitivity(), insulinDuration, profile.getCarbratio(), absorptionTime);

                        deltaBg = next.getValue() - nextPrediction;
                        times = times.append(nextTime - currentTime);
                        nkbg = nkbg.append(deltaBg);
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
                        mealTreatments.add(meal);
                    }
                }
            }
        }
        //Remove Meals before first Bg entry
        for (int i = 0; i < mealTreatments.size(); i++) {
            if (mealTreatments.get(i).getTimestamp().getTime() / 60000  < firstTime){
                mealTreatments.remove(i);
                i--;
            } else {
                break;
            }

        }
        return mealTreatments;
    }
}
