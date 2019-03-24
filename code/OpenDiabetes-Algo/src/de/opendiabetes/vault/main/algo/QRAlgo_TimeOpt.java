package de.opendiabetes.vault.main.algo;

import de.opendiabetes.vault.main.dataprovider.AlgorithmDataProvider;
import de.opendiabetes.vault.main.math.Predictions;
import de.opendiabetes.vault.parser.Profile;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.util.TimestampUtils;
import org.apache.commons.math3.linear.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.lang.Math.pow;

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
        ArrayList<Double> alNkbg;
        ArrayList<Double> alPred;
        ArrayList<Double> albg;
        ArrayList<Long> alTimes;
        List<VaultEntry> mealTreatments = new ArrayList<>();

        VaultEntry meal;
        VaultEntry next;
        VaultEntry current;
        long firstTime = glucose.get(0).getTimestamp().getTime() / 60000;
//        long firstTime, lastTime, step;
//        firstTime = Math.min(Math.min(glucose.get(0).getTimestamp().getTime(),basalTreatments.get(0).getTimestamp().getTime()),bolusTreatments.get(0).getTimestamp().getTime());
//        lastTime = Math.min(Math.min(glucose.get(glucose.size()-1).getTimestamp().getTime(),basalTreatments.get(basalTreatments.size()-1).getTimestamp().getTime()),bolusTreatments.get(bolusTreatments.size()-1).getTimestamp().getTime());
//        step = 5*60000;
        long estimatedTime;
        long currentTime;
        long nextTime = 0l;
        double currentLimit;
        long estimatedTimeAccepted = 0l;
        double currentPrediction;
        double currentValue;
        double nextPrediction;
        double deltaBg;

        for (int i = 0; i < glucose.size(); i++) {

            nkbg = new ArrayRealVector();
            times = new ArrayRealVector();
            alNkbg = new ArrayList();
            albg = new ArrayList();
            alPred = new ArrayList();
            alTimes = new ArrayList();

            current = glucose.get(i);
            System.out.println(current.getTimestamp());
            currentTime = current.getTimestamp().getTime() / 60000;

            if (currentTime >= firstTime + insulinDuration && currentTime > estimatedTimeAccepted) {

                currentLimit = currentTime + absorptionTime / 2;

//                currentValue = Filter.getMedian(glucose, i, 5, absorptionTime / 3);
                //currentValue = Filter.getAverage(glucose, i, 5, absorptionTime / 3);
                currentValue = current.getValue();
                currentPrediction = Predictions.predict(current.getTimestamp().getTime(), mealTreatments, bolusTreatments,
                        basalTreatments, profile.getSensitivity(), insulinDuration, profile.getCarbratio(), absorptionTime);
                for (int j = i; j < glucose.size(); j++) {

                    next = glucose.get(j);
                    nextTime = next.getTimestamp().getTime() / 60000;
//                    double nextValue = Filter.getMedian(glucose, j, 5, absorptionTime / 3);
                    //double nextValue = Filter.getAverage(glucose, j, 5, absorptionTime / 3);
                    double nextValue = next.getValue();
                    if (nextTime <= currentLimit) {

                        nextPrediction = Predictions.predict(next.getTimestamp().getTime(), mealTreatments, bolusTreatments,
                                basalTreatments, profile.getSensitivity(), insulinDuration, profile.getCarbratio(), absorptionTime);

//                        deltaBg = next.getValue() - currentValue - (nextPrediction - currentPrediction);
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
                    double alpha, beta, gamma;
                    alpha = solution.getEntry(0);
                    beta = solution.getEntry(1);
                    gamma = solution.getEntry(2);
//                    assert (alpha > 0);
//                    double error = gamma - pow(beta, 2) / (4 * alpha);
                    estimatedTime = (long) (currentTime - beta / (2 * alpha));
//                    estimatedTime = currentTime;
                    double estimatedCarbs = alpha * pow(absorptionTime, 2) * profile.getCarbratio() / (2 * profile.getSensitivity());

//                    System.out.println("Date: " + new Date(estimatedTime * 60000) + " Carbs: " + estimatedCarbs);
                    if (currentTime - estimatedTime < absorptionTime / 2
                            && estimatedTime < nextTime) {
                        if (estimatedCarbs >= 0 //|| mealTreatments.isEmpty()// && estimatedCarbs < 200 // && error < 10
                                ) {
                            estimatedTimeAccepted = estimatedTime;
                            meal = new VaultEntry(VaultEntryType.MEAL_MANUAL,
                                    TimestampUtils.createCleanTimestamp(new Date(estimatedTime * 60000)),
                                    estimatedCarbs);
                            mealTreatments.add(meal);

//                        } else if (currentLimit < absorptionTime / 2) {
//                            currentLimit += absorptionTime / 6;
//                        } else {
//                            break;
//                    }
//                        int j_max = mealTreatments.size();
//                        double tempValue = estimatedCarbs;
//                        ArrayList<VaultEntry> temps = new ArrayList<>();
//                        for (int j = 1; j <= j_max; j++) {
//                            VaultEntry tempMeal = mealTreatments.get(mealTreatments.size() - j);
//                            tempValue = tempMeal.getValue() + tempValue;
//                            temps.add(tempMeal);
//                            if (tempValue >= 0 || j == j_max) {
//                                mealTreatments.removeAll(temps);
//                                tempMeal.setValue(tempValue);
//                                mealTreatments.add(tempMeal);
//                                break;
//                            }
                        }
                    }
                }
            }
        }
        return mealTreatments;
//        ArrayList<VaultEntry> mealsMA = new ArrayList<>();
//        double mealValue;
//        double mealValue0 = 0;
//        double mealValue1 = 0;
//        double mealValue2 = 0;
//
//        for (VaultEntry a : mealTreatments) {
//            
//            mealValue0 = mealValue1;
//            mealValue1 = mealValue2;
//            mealValue2 = a.getValue();
//            mealValue = (mealValue0 + mealValue1 + mealValue2) / 3;
//            mealsMA.add(new VaultEntry(VaultEntryType.MEAL_MANUAL, a.getTimestamp(), mealValue));
//
//        }
//
//        return mealsMA;
    }
}
