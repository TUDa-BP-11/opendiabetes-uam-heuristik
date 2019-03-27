package de.opendiabetes.vault.main.algo;

import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.main.dataprovider.AlgorithmDataProvider;
import de.opendiabetes.vault.main.math.Predictions;
import de.opendiabetes.vault.parser.Profile;
import de.opendiabetes.vault.util.TimestampUtils;
import org.apache.commons.math3.linear.*;

import java.util.Date;
import java.util.List;

import static java.lang.Math.pow;
import java.util.ArrayList;

public class QRAlgo_TimeOpt extends Algorithm {

    public QRAlgo_TimeOpt(long absorptionTime, long insulinDuration, Profile profile) {
        super(absorptionTime, insulinDuration, profile);
    }

    public QRAlgo_TimeOpt(long absorptionTime, long insulinDuration, AlgorithmDataProvider dataProvider) {
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
        long firstTime = getGlucose().get(0).getTimestamp().getTime() / 60000 + Math.max(getAbsorptionTime(), getInsulinDuration());

        long estimatedTime;
        long currentTime;
        long nextTime = 0l;
        double currentLimit;
        long estimatedTimeAccepted = 0l;
        double nextPrediction;

        List<VaultEntry> mealTreatments;
        mealTreatments = new ArrayList<>();
        for (int i = 0; i < getGlucose().size(); i++) {

            current = getGlucose().get(i);
            currentTime = current.getTimestamp().getTime() / 60000;
            // skip bg values until start time
            if (currentTime < firstTime) {
                continue;
            }

            nkbg = new ArrayRealVector();
            times = new ArrayRealVector();

            if (currentTime >= firstTime + getInsulinDuration() && currentTime > estimatedTimeAccepted) {

                currentLimit = currentTime + getAbsorptionTime() / 2;

                for (int j = i; j < getGlucose().size(); j++) {

                    next = getGlucose().get(j);
                    nextTime = next.getTimestamp().getTime() / 60000;
                    double nextValue = next.getValue();
                    if (nextTime <= currentLimit) {

                        nextPrediction = Predictions.predict(next.getTimestamp().getTime(), mealTreatments, getBolusTreatments(), getBasalTreatments(), getProfile().getSensitivity(), getInsulinDuration(), getProfile().getCarbratio(), getAbsorptionTime());

                        times = times.append(nextTime - currentTime);
                        nkbg = nkbg.append(nextValue - nextPrediction);
                    }
                }

                if (times.getDimension() >= 3) {
                    matrix = new Array2DRowRealMatrix(times.getDimension(), 3);
                    matrix.setColumnVector(1, times);
                    matrix.setColumnVector(0, times.ebeMultiply(times));
                    times.set(1);
                    matrix.setColumnVector(2, times);

                    DecompositionSolver solver = new QRDecomposition(matrix).getSolver();
                    RealVector solution = solver.solve(nkbg);
                    double alpha, beta;
                    alpha = solution.getEntry(0);
                    beta = solution.getEntry(1);
                    estimatedTime = (long) (currentTime - beta / (2 * alpha));
                    double estimatedCarbs = alpha * pow(getAbsorptionTime(), 2) * getProfile().getCarbratio() / (2 * getProfile().getSensitivity());

                    if (currentTime - estimatedTime < getAbsorptionTime() / 2
                            && estimatedTime < nextTime) {
                        if (estimatedCarbs >= 0) {
                            if (mealTreatments.isEmpty() || mealTreatments.get(mealTreatments.size() - 1).getTimestamp().getTime() / 60000 < estimatedTime) {
                                meal = new VaultEntry(VaultEntryType.MEAL_MANUAL,
                                        TimestampUtils.createCleanTimestamp(new Date(estimatedTime * 60000)),
                                        estimatedCarbs);
                                estimatedTimeAccepted = estimatedTime;
                                mealTreatments.add(meal);
                            }
                        }
                    }
                }
            }
        }
        return mealTreatments;
    }
}
