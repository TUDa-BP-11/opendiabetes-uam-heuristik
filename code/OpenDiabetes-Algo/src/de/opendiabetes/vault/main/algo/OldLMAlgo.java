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

public class OldLMAlgo extends Algorithm {

    public OldLMAlgo(long absorptionTime, long insulinDuration, Profile profile) {
        super(absorptionTime, insulinDuration, profile);
    }

    public OldLMAlgo(long absorptionTime, long insulinDuration, AlgorithmDataProvider dataProvider) {
        super(absorptionTime, insulinDuration, dataProvider);
    }

    @Override
    public List<VaultEntry> calculateMeals() {
        RealMatrix J, I, A, Ainv;
        RealVector nkbg;
        RealVector mealValues;
        RealVector mealTimes;
        RealVector e;
        RealVector ve;
        RealVector times;
        VaultEntry meal;
        RealVector delta;
        ArrayList<Double> E;

        double deltaBg, currentValue;

        ve = new ArrayRealVector();
        nkbg = new ArrayRealVector();
        times = new ArrayRealVector();
        meals.clear();

        final long firstTime = glucose.get(0).getTimestamp().getTime() / 60000;
        final long lastTime = glucose.get(glucose.size() - 1).getTimestamp().getTime() / 60000;

        long currentTime;

        for (int i = 0; i < glucose.size(); i++) {
            VaultEntry current = glucose.get(i);
            currentTime = current.getTimestamp().getTime();
            currentValue = current.getValue();
//            currentValue = Filter.getMedian(glucose, i, 5, absorptionTime / 3);
            deltaBg = currentValue - Predictions.predict(currentTime, meals, bolusTreatments, basalTreatments, profile.getSensitivity(), insulinDuration,profile.getCarbratio(), absorptionTime);
            nkbg = nkbg.append(deltaBg);
            times = times.append(currentTime / 60000);
            ve = ve.append(currentValue);
        }
        boolean breakN = false;

        mealTimes = new ArrayRealVector();
        mealValues = new ArrayRealVector();

        for (int N = 3; N < 15 && !breakN; N += 2) {
            E = new ArrayList();
            mealTimes = new ArrayRealVector(N);
            mealValues = new ArrayRealVector(N);
            // possible discrete meal times within snippet time range each dT/N Minutes.
            long step = (lastTime - firstTime) / N;
            I = MatrixUtils.createRealIdentityMatrix(2 * N);
            //beta_t0 = linspace(t(1), t(end)-T,N)';
            for (int i = 0; i < N; i++) {
                mealTimes.setEntry(i, firstTime + i * step);
                mealValues.setEntry(i, 50);
            }
            double mu_k, mu;
            mu = 1e-5;
            int N_iter = 1000;
            double abs_e;
            for (int i = 0; i < N_iter; i++) {
                J = Predictions.Jacobian(times, mealTimes, mealValues, profile.getSensitivity(), profile.getCarbratio(), absorptionTime);
                e = nkbg.subtract(Predictions.cumulativeMealPredict(times, mealTimes, mealValues, profile.getSensitivity(), profile.getCarbratio(), absorptionTime));
                abs_e = e.getNorm();
                E.add(abs_e);

                if (Math.max(Math.abs(e.ebeDivide(ve).getMaxValue()),
                        Math.abs(e.ebeDivide(ve).getMinValue()))
                        <= 0.10) {
                    breakN = true;
                    break;
                }
                if (i > 10 && Math.abs(abs_e - E.get(E.size() - 2)) < 1e-5) {
                    break;
                }

                mu_k = mu * e.dotProduct(e);
                RealMatrix JJ = J.transpose().multiply(J);
                A = JJ.add(I.scalarMultiply(mu_k));
                Ainv = new SingularValueDecomposition(A).getSolver().getInverse();
                delta = Ainv.multiply(J.transpose()).operate(e);
                for (Double d : delta.toArray()) {
                    if (d.isNaN() || d.isInfinite()) {
                        System.out.println("NaN in increment");
                    }
                    break;
                }
                mealTimes = mealTimes.add(delta.getSubVector(0, N));
                mealValues = mealValues.add(delta.getSubVector(N, N));
                mealTimes.mapToSelf((x) -> Math.min(lastTime, Math.max(firstTime - absorptionTime, x)));
                mealValues.mapToSelf((x) -> Math.max(0, x));
            }
        }

        ArrayList<Long> uniqueMealTimes = new ArrayList();
        RealVector uniqueMealValues = new ArrayRealVector();
        for (int i = 0; i < mealTimes.getDimension(); i++) {

            long t = Math.round(mealTimes.getEntry(i));
            double x = mealValues.getEntry(i);
            int idx = uniqueMealTimes.indexOf(t);
            if (idx != -1) {
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
            if (x > 5) {
                t0 = uniqueMealTimes.get(i);
                meal = new VaultEntry(VaultEntryType.MEAL_MANUAL,
                        TimestampUtils.createCleanTimestamp(new Date(t0 * 60000)), x);
                meals.add(meal);
            }
        }

        //Remove Meals before first Bg entry
        for (int i = 0; i < meals.size(); i++) {
            if (meals.get(i).getTimestamp().getTime() / 60000 < firstTime) {
                meals.remove(i);
                i--;
            } else {
                break;
            }

        }
        return meals;

    }
}
