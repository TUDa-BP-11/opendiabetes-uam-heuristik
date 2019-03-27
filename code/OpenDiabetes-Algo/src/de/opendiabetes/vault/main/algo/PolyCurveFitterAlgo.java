package de.opendiabetes.vault.main.algo;

import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.main.dataprovider.AlgorithmDataProvider;
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
 * @author anna
 */
public class PolyCurveFitterAlgo extends Algorithm {

    public PolyCurveFitterAlgo(long absorptionTime, long insulinDuration, Profile profile) {
        super(absorptionTime, insulinDuration, profile);
    }

    public PolyCurveFitterAlgo(long absorptionTime, long insulinDuration, AlgorithmDataProvider dataProvider) {
        super(absorptionTime, insulinDuration, dataProvider);
    }

    @Override
    public List<VaultEntry> calculateMeals() {

        double weight = 1;
        List<VaultEntry> mealTreatments;
        mealTreatments = new ArrayList<>();
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
            currentLimit = currentTime + getAbsorptionTime() / 4;
            if (currentTime > estimatedTimeAccepted) {

//                startValue = Filter.getMedian(glucose, i, 5, absorptionTime / 3);
                //startValue = Filter.getAverage(glucose, i, 5, absorptionTime / 3);
                startValue = current.getValue();
                //double deltaBg = Filter.getAverage(glucose, j, 5, 30) - Filter.getAverage(glucose, i, 5, 30);
                //double deltaBg = next.getValue() - current.getValue();
                currentPrediction = Predictions.predict(current.getTimestamp().getTime(), mealTreatments, getBolusTreatments(), getBasalTreatments(), getProfile().getSensitivity(), getInsulinDuration(), getProfile().getCarbratio(), getAbsorptionTime());

                for (int j = i; j < getGlucose().size(); j++) {
                    next = getGlucose().get(j);
                    nextTime = next.getTimestamp().getTime() / 60000;

//                    double nextValue = Filter.getMedian(glucose, j, 5, absorptionTime / 3);
                    //double nextValue = Filter.getAverage(glucose, j, 5, absorptionTime / 3);
                    nextValue = next.getValue();
                    if (nextTime <= currentLimit) {
                        nextPrediction = Predictions.predict(next.getTimestamp().getTime(), mealTreatments, getBolusTreatments(), getBasalTreatments(), getProfile().getSensitivity(), getInsulinDuration(), getProfile().getCarbratio(), getAbsorptionTime());
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
                    double estimatedCarbs = lsq[2] * pow(getAbsorptionTime(), 2) * getProfile().getCarbratio() / (2 * getProfile().getSensitivity());
                    if (currentTime - estimatedTime < getAbsorptionTime() / 2
                            && estimatedTime < lastTime) {

                        estimatedTimeAccepted = estimatedTime;
                        meal = new VaultEntry(VaultEntryType.MEAL_MANUAL,
                                TimestampUtils.createCleanTimestamp(new Date(estimatedTime * 60000)),
                                estimatedCarbs);
                        mealTreatments.add(meal);
                    }
                }
            }
            observations.clear();
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
