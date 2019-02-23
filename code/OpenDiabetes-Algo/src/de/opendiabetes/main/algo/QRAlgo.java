package de.opendiabetes.main.algo;

import de.opendiabetes.main.dataprovider.AlgorithmDataProvider;
import de.opendiabetes.main.math.Predictions;
import de.opendiabetes.parser.Profile;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.util.TimestampUtils;
import org.apache.commons.math3.linear.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.lang.Math.pow;

public class QRAlgo extends Algorithm {

    public QRAlgo(double absorptionTime, double insulinDuration, Profile profile) {
        super(absorptionTime, insulinDuration, profile);
    }

    public QRAlgo(double absorptionTime, double insulinDuration, AlgorithmDataProvider dataProvider) {
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
        int numBG = glucose.size();
        VaultEntry current;

        // debugging: break after 10 days
        current = glucose.get(0);
//        long firstTime = current.getTimestamp().getTime();

        long estimatedTime;
        long currentTime;
        long nextTime;
        long lastTime = 0;
        double currentLimit;
        long estimatedTimeAccepted = 0l;
        double currentPrediction;
        double currentValue;
        double nextPrediction;
        double deltaBg;

//        Plot plt = Plot.create();
        for (int i = 0; i < numBG; i++) {

            nkbg = new ArrayRealVector();
            times = new ArrayRealVector();
            alNkbg = new ArrayList();
            albg = new ArrayList();
            alPred = new ArrayList();
            alTimes = new ArrayList();

            current = glucose.get(i);

            // debugging: break after 1 day
//            if (current.getTimestamp().getTime() - firstTime > 1 * 24 * 60 * 60 * 1000) {
//                break;
//            }
            currentTime = current.getTimestamp().getTime() / 60000;

            if (currentTime > estimatedTimeAccepted) {

                currentLimit = currentTime + absorptionTime / 6;
                currentValue = current.getValue();
                currentPrediction = Predictions.predict(current.getTimestamp().getTime(), mealTreatments, bolusTreatments,
                        basalTreatments, profile.getSensitivity(), insulinDuration, profile.getCarbratio(), absorptionTime);
                int j = 0;
//                while (j < numBG - i) {

                for (; j < numBG - i; j++) {

                    next = glucose.get(i + j);
                    nextTime = next.getTimestamp().getTime() / 60000;
                    if (nextTime <= currentLimit) {

                        nextPrediction = Predictions.predict(next.getTimestamp().getTime(), mealTreatments, bolusTreatments,
                                basalTreatments, profile.getSensitivity(), insulinDuration, profile.getCarbratio(), absorptionTime);

                        deltaBg = next.getValue() - currentValue - (nextPrediction - currentPrediction);
//                        deltaBg = next.getValue() - nextPrediction;
//                        System.out.println("calculateMeals2(): " + deltaBg + " time: " + next.getTimestamp());
                        times = times.append(nextTime - currentTime);
                        lastTime = nextTime;
                        nkbg = nkbg.append(deltaBg);
                        alTimes.add(nextTime * 60);
                        alNkbg.add(deltaBg + i);
                        alPred.add(nextPrediction);
                        albg.add(next.getValue());
                    }
                }

//                plt.plot().addDates(alTimes).add(alNkbg);
//                plt.plot().addDates(alTimes).add(albg).linestyle("dashed");
//                plt.plot().addDates(alTimes).add(alPred).linestyle("dotted");

                if (times.getDimension() >= 3) {
                    matrix = new Array2DRowRealMatrix(times.getDimension(), 2); //3
//                    matrix.setColumnVector(1, times);
                    matrix.setColumnVector(0, times.ebeMultiply(times));
                    times.set(1);
                    matrix.setColumnVector(1, times); //2

                    DecompositionSolver solver = new QRDecomposition(matrix).getSolver();
                    RealVector solution = solver.solve(nkbg);
                    double alpha;//, beta, gamma;
                    alpha = solution.getEntry(0);
//                    beta = solution.getEntry(1);
//                    gamma = solution.getEntry(2);
//                    assert (alpha > 0);
//                    double error = gamma - pow(beta, 2) / (4 * alpha);
//                    estimatedTime = (long) (currentTime - beta / (2 * alpha));
                    estimatedTime = currentTime;
                    double estimatedCarbs = alpha * pow(absorptionTime, 2) * profile.getCarbratio() / (2 * profile.getSensitivity());

                    System.out.println("Date: " + new Date(estimatedTime * 60000) + " Carbs: " + estimatedCarbs);
//                    if (currentTime - estimatedTime < absorptionTime / 2
//                            && estimatedTime < lastTime) {
//                        if (estimatedCarbs > 0 //|| mealTreatments.isEmpty()// && estimatedCarbs < 200 // && error < 10
//                                ) {
                    estimatedTimeAccepted = estimatedTime;
                    meal = new VaultEntry(VaultEntryType.MEAL_MANUAL,
                            TimestampUtils.createCleanTimestamp(new Date(estimatedTime * 60000)),
                            estimatedCarbs);
                    mealTreatments.add(meal);

//                        } else if (currentLimit < absorptionTime / 2) {
//                            currentLimit += absorptionTime / 6;
//                        } else {
//                            break;
//                        }

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
//                        }
//                    }
//                        }
//                    }
                }
            }
        }
//        try {
//            plt.show();
//        } catch (IOException | PythonExecutionException ex) {
//            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
//        }
        return mealTreatments;
    }
}