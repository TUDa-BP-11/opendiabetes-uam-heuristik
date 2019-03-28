package de.opendiabetes.vault.main.algo;

import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.main.math.Predictions;
import de.opendiabetes.vault.parser.Profile;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.nsapi.NSApi;
import de.opendiabetes.vault.util.TimestampUtils;
import org.apache.commons.math3.linear.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.math3.stat.descriptive.UnivariateStatistic;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

public class LMAlgo extends Algorithm {

    private double offset;

    public LMAlgo(long absorptionTime, long insulinDuration, double peak, Profile profile, List<VaultEntry> glucoseMeasurements, List<VaultEntry> bolusTreatments, List<VaultEntry> basalTreatments) {
        super(absorptionTime, insulinDuration, peak, profile, glucoseMeasurements, bolusTreatments, basalTreatments);
    }

    @Override
    public List<VaultEntry> calculateMeals() {
//        List<VaultEntry> mealTreatments;
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
        RealVector delta;
        Double e_old;
        double deltaBg, currentValue;

        UnivariateStatistic bias = new Mean();
        UnivariateStatistic std = new StandardDeviation();
        meals.clear();
        offset = 0;
//        mealTreatments = new ArrayList<>();
        ve = new ArrayRealVector();
        nkbg = new ArrayRealVector();
        times = new ArrayRealVector();

        final long startTime = getStartTime() / 60000;
        final long lastTime = glucose.get(glucose.size() - 1).getTimestamp().getTime() / 60000;
        final long firstMealTime = startTime - absorptionTime;
        final long lastMealTime = lastTime;
        long currentTime;

        VaultEntry current;
        // keep track of best result
        errOpt = Double.POSITIVE_INFINITY;

        //
        for (int i = getStartIndex(); i < glucose.size(); i++) {
            current = glucose.get(i);
            currentTime = current.getTimestamp().getTime() / 60000;
            currentValue = current.getValue();
            deltaBg = currentValue - Predictions.predict(current.getTimestamp().getTime(), meals, bolusTreatments, basalTreatments, profile.getSensitivity(), insulinDuration, profile.getCarbratio(), absorptionTime, peak);
            nkbg = nkbg.append(deltaBg);
            times = times.append(currentTime);
            ve = ve.append(currentValue);
        }

        // initial carbs to be distributed on N start values
        double totalCarbs = 200;

        double mu_k, mu, abs_e;

        if (times.getDimension() >= 3) {

            mealTimes = new ArrayRealVector(0);
            mealValues = new ArrayRealVector(0);
            // estimate error vector with current mealValues and mealTimes
            e = nkbg.subtract(Predictions.cumulativeMealPredict(times, mealTimes, mealValues, profile.getSensitivity(), profile.getCarbratio(), absorptionTime));

            // calculate norm.
            abs_e = e.getNorm();

            // calculate max relative error
            err = Math.max(Math.abs(e.ebeDivide(ve).getMaxValue()), Math.abs(e.ebeDivide(ve).getMinValue()));
            // stop iterations and search if convergence criterion is met (max error <= 10%)
            if (err <= 0.10) {
                NSApi.LOGGER.log(Level.INFO, "N: %d, MT: %d, MV: %d", new Object[]{0, mealTimes.getDimension(), mealValues.getDimension()});
            }

            // store current error
            e_old = abs_e;

            // keep track of best result
            if (mealTimesOpt.getDimension() == 0 || errOpt > abs_e) {
                errOpt = err;
                mealTimesOpt = mealTimes;
                mealValuesOpt = mealValues;
            }

            boolean breakN = false;
            for (int N = 1; N < 15 && !breakN; N += 1) {

                mealTimes = new ArrayRealVector(N);
                mealValues = new ArrayRealVector(N);

                long step = (lastMealTime - firstMealTime) / N;
                I = MatrixUtils.createRealIdentityMatrix(2 * N);

                for (int i = 0; i < N; i++) {
                    mealTimes.setEntry(i, firstMealTime + i * step);
                    mealValues.setEntry(i, totalCarbs / N);
                }

                // basic constant for LM multiplier
                mu = 1e-5;
                // max number of iterations per N
                int N_iter = 10000;
                for (int i = 0; i < N_iter; i++) {
                    // estimate Jacobian with current mealValues and mealTimes
                    J = Predictions.Jacobian(times, mealTimes, mealValues, profile.getSensitivity(), profile.getCarbratio(), absorptionTime);

                    // estimate error vector with current mealValues and mealTimes
                    e = nkbg.subtract(Predictions.cumulativeMealPredict(times, mealTimes, mealValues, profile.getSensitivity(), profile.getCarbratio(), absorptionTime));

                    // calculate norm.
                    abs_e = e.getNorm();

                    // calculate max relative error
                    err = Math.max(Math.abs(e.ebeDivide(ve).getMaxValue()), Math.abs(e.ebeDivide(ve).getMinValue()));
                    // stop iterations and search if convergence criterion is met (max error <= 10%)
                    if (err <= 0.10) {
                        NSApi.LOGGER.log(Level.INFO, "N: %d, MT: %d, MV: %d", new Object[]{N, mealTimes.getDimension(), mealValues.getDimension()});
                        breakN = true;
                        break;
                    }

                    // stop iterations if error vector magnitude changes less than 1e-7
                    if (i > 10 && Math.abs(abs_e - e_old) < 1e-7) {
//                        NSApi.LOGGER.log(Level.INFO, "Converged N: %d, max err: %.2f%%, bias: %.2f, std: %.2f, i: %d", new Object[]{N, err * 100, bias.evaluate(e.toArray()), std.evaluate(e.toArray()), i});
                        break;
                    }
                    // store current error
                    e_old = abs_e;

                    // calculate LM normalization from error vector magnitude squared
                    mu_k = mu * e.dotProduct(e);
                    // A = (JJ+mu_k(ii)*I);
                    RealMatrix JJ = J.transpose().multiply(J);
                    A = JJ.add(I.scalarMultiply(mu_k));
                    Ainv = new SingularValueDecomposition(A).getSolver().getInverse();

                    // calculate least squares solution of gradient step
                    delta = Ainv.multiply(J.transpose()).operate(e);

                    // dismiss trial of gradient step becomes unstable
                    if (delta.isNaN() || delta.isInfinite()) {
                        NSApi.LOGGER.log(Level.WARNING, "NaN in increment");
                        break;
                    }

                    // apply gradient step
                    mealTimes = mealTimes.add(delta.getSubVector(0, N));
                    mealValues = mealValues.add(delta.getSubVector(N, N));

                    // restrict solutions to boundary conditions
                    mealTimes.mapToSelf((x) -> {
                        return Math.min(lastMealTime, Math.max(firstMealTime, x));
                    });
                    mealValues.mapToSelf((x) -> {
                        return Math.max(0, x);
                    });
                }

                // keep track of best result
                if (mealTimesOpt.getDimension() == 0 || errOpt > abs_e) {
                    errOpt = abs_e;
                    mealTimesOpt = mealTimes;
                    mealValuesOpt = mealValues;
                }
            }

            // normalize mealTimes and sum up meals at the same time. Throw everything with < 1g of Carbs away.
            ArrayList<Long> uniqueMealTimes = new ArrayList<>();
            ArrayList<Double> uniqueMealValues = new ArrayList<>();
            for (int i = 0; i < mealTimesOpt.getDimension(); i++) {
                long t = Math.round(mealTimesOpt.getEntry(i));
                double x = mealValuesOpt.getEntry(i);
                if (t <= firstMealTime) {
                    offset += x * profile.getSensitivity() / profile.getCarbratio();
                    continue;
                }
//                t = Math.max(t, firstTime);
                int idx = uniqueMealTimes.indexOf(t);
                if (idx != -1) {
                    uniqueMealValues.set(idx, uniqueMealValues.get(idx) + x);
                } else if (x > 1) {
                    uniqueMealTimes.add(t);
                    uniqueMealValues.add(x);
                }
            }
            if (!uniqueMealValues.isEmpty() && uniqueMealValues.size() == uniqueMealTimes.size()) {
                for (int i = 0; i < uniqueMealValues.size(); i++) {
                    meals.add(new VaultEntry(VaultEntryType.MEAL_MANUAL,
                            TimestampUtils.createCleanTimestamp(new Date(uniqueMealTimes.get(i) * 60000)), uniqueMealValues.get(i)));
                }
            }
        }

        return meals;
    }

    @Override
    public double getStartValue() {
        return offset;
    }
}