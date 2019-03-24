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
import java.util.logging.Level;
import java.util.logging.Logger;

public class LMAlgo extends Algorithm {

    public LMAlgo(long absorptionTime, long insulinDuration, Profile profile) {
        super(absorptionTime, insulinDuration, profile);
    }

    public LMAlgo(long absorptionTime, long insulinDuration, AlgorithmDataProvider dataProvider) {
        super(absorptionTime, insulinDuration, dataProvider);
    }

    @Override
    public List<VaultEntry> calculateMeals() {
        RealMatrix J, I, A, Ainv;
        RealVector nkbg;
        RealVector mealValues;
        RealVector mealTimes;
        RealVector mealValuesOpt = new ArrayRealVector();
        RealVector mealTimesOpt = new ArrayRealVector();
        double err, errOpt;
        RealVector e;
        RealVector ve;
        RealVector times;
        VaultEntry meal;
        RealVector delta;
        List<VaultEntry> mealTreatments;
        ArrayList<Double> E;

        double deltaBg, currentValue;

        ve = new ArrayRealVector();
        nkbg = new ArrayRealVector();
        times = new ArrayRealVector();

        mealTreatments = new ArrayList<>();

        final long firstTime = glucose.get(0).getTimestamp().getTime() / 60000;
        final long lastTime = glucose.get(glucose.size() - 1).getTimestamp().getTime() / 60000;
        final long firstMealTime = firstTime - absorptionTime;
        final long lastMealTime = lastTime; //  - absorptionTime / 4
        long currentTime;
        errOpt = Double.POSITIVE_INFINITY;
        err = Double.POSITIVE_INFINITY;
        for (int i = 0; i < glucose.size(); i++) {
            VaultEntry current = glucose.get(i);
            currentTime = current.getTimestamp().getTime();

            // skip bg values until start time
//            if (currentTime / 60000 < firstTime + insulinDuration) {
//                continue;
//            }
            currentValue = current.getValue();
//            currentValue = Filter.getMedian(glucose, i, 5, absorptionTime / 3);
            deltaBg = currentValue - Predictions.predict(currentTime, mealTreatments, bolusTreatments,
                    basalTreatments, profile.getSensitivity(), insulinDuration, profile.getCarbratio(), absorptionTime);
            nkbg = nkbg.append(deltaBg);
            times = times.append(currentTime / 60000);
            ve = ve.append(currentValue);
        }

        double totalCarbs = 200;
//        insSensitivityFactor / carbRatio * carbsAmount

        if (times.getDimension() >= 3) {
            boolean breakN = false;

            mealTimes = new ArrayRealVector();
            mealValues = new ArrayRealVector();
// uncomment for plot
//        Plot plt = Plot.create();
            for (int N = 1; N < 10 && !breakN; N += 1) {
                E = new ArrayList();
                mealTimes = new ArrayRealVector(N);
                mealValues = new ArrayRealVector(N);
                // possible discrete meal times within snippet time range each dT/N Minutes.
                long step = (lastTime - firstMealTime) / N;
                I = MatrixUtils.createRealIdentityMatrix(2 * N);
                //beta_t0 = linspace(t(1), t(end)-T,N)';
                for (int i = 0; i < N; i++) {
                    mealTimes.setEntry(i, firstMealTime + i * step);
                    mealValues.setEntry(i, totalCarbs / N);
                }
                double mu_k, mu;
                mu = 1e-5;
                int N_iter = 10000;
                double abs_e;
                for (int i = 0; i < N_iter; i++) {
                    J = Predictions.Jacobian(times, mealTimes, mealValues, profile.getSensitivity(), profile.getCarbratio(), absorptionTime);
                    e = nkbg.subtract(Predictions.cumulativeMealPredict(times, mealTimes, mealValues, profile.getSensitivity(), profile.getCarbratio(), absorptionTime));
                    abs_e = e.getNorm();
                    E.add(abs_e);
                    err = Math.max(Math.abs(e.ebeDivide(ve).getMaxValue()), Math.abs(e.ebeDivide(ve).getMinValue()));
                    if (err <= 0.10) {
                        Logger.getLogger(LMAlgo.class.getName()).log(Level.INFO, "N: {0}, MT: {1}, MV: {2}", new Object[]{N, mealTimes.getDimension(), mealValues.getDimension()});
                        breakN = true;
                        break;
                    }
                    if (i > 10 && Math.abs(abs_e - E.get(E.size() - 2)) < 1e-7) {
                        Logger.getLogger(LMAlgo.class.getName()).log(Level.INFO, "Converged N: {0}, max err: {1}%, i: {2}", new Object[]{N, err * 100, i});
                        break;
                    }

                    // mu_k(ii) = mu*e'*e;
                    mu_k = mu * e.dotProduct(e);//abs_e;
                    // A = (JJ+mu_k(ii)*I);
                    RealMatrix JJ = J.transpose().multiply(J);
                    A = JJ.add(I.scalarMultiply(mu_k));
                    Ainv = new SingularValueDecomposition(A).getSolver().getInverse();
                    // delta = A\J'*e;
                    delta = Ainv.multiply(J.transpose()).operate(e);
                    for (Double d : delta.toArray()) {
                        if (d.isNaN() || d.isInfinite()) {
                            Logger.getLogger(LMAlgo.class.getName()).log(Level.WARNING, "NaN in increment");
                        }
                        break;
                    }
                    mealTimes = mealTimes.add(delta.getSubVector(0, N));
                    mealValues = mealValues.add(delta.getSubVector(N, N));
                    mealTimes.mapToSelf((x) -> {
                        return Math.min(lastMealTime, Math.max(firstMealTime, x));
                    });
                    mealValues.mapToSelf((x) -> {
                        return Math.max(0, x);
                    });
                }
// uncomment for plot
//        plt.plot().add(E);
                if (mealTimesOpt.getDimension() == 0 || errOpt > err) {
                    errOpt = err;
                    mealTimesOpt = mealTimes;
                    mealValuesOpt = mealValues;
                    
                }
            }
// uncomment for plot
//        try {
//            plt.show();
//        } catch (IOException | PythonExecutionException ex) {
//            Logger.getLogger(LMAlgo.class.getName()).log(Level.SEVERE, null, ex);
//        }

            ArrayList<Long> uniqueMealTimes = new ArrayList();
//        ArrayList<Double> a_uniqueMealValues = new ArrayList();
            RealVector uniqueMealValues = new ArrayRealVector();
            for (int i = 0; i < mealTimesOpt.getDimension(); i++) {

                long t = Math.round(mealTimesOpt.getEntry(i));
                double x = mealValuesOpt.getEntry(i);
                int idx = uniqueMealTimes.indexOf(t);
//            System.out.println("Times " + uniqueMealTimes.toString());
//            System.out.println("Values " + uniqueMealValues.toString());
                if (idx != -1) {
//                System.out.println(idx + " " + uniqueMealValues.getDimension());
                    uniqueMealValues.addToEntry(idx, x);
                } else if (x > 1) {
                    uniqueMealTimes.add(t);
                    uniqueMealValues = uniqueMealValues.append(x);
                }
            }

            double x;
            long t0;
            for (int i = 0; i < uniqueMealValues.getDimension(); i++) {
                x = uniqueMealValues.getEntry(i);
                t0 = uniqueMealTimes.get(i);
                meal = new VaultEntry(VaultEntryType.MEAL_MANUAL,
                        TimestampUtils.createCleanTimestamp(new Date(t0 * 60000)), x);
//            System.out.println(meal.toString());
                mealTreatments.add(meal);
            }
        }
        return mealTreatments;

    }
}
