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
        RealVector e;
        RealVector times;
        VaultEntry meal;
        RealVector delta;
        List<VaultEntry> mealTreatments;
        ArrayList<Double> E;

        double deltaBg;

        nkbg = new ArrayRealVector();
        times = new ArrayRealVector();

        mealTreatments = new ArrayList<>();

        final long firstTime = glucose.get(0).getTimestamp().getTime() / 60000;
        final long lastTime = glucose.get(glucose.size() - 1).getTimestamp().getTime() / 60000;

        long currentTime;

        for (int i = 0; i < glucose.size(); i++) {
            VaultEntry current = glucose.get(i);
            currentTime = current.getTimestamp().getTime();

//            currentValue = Filter.getMedian(glucose, i, 5, absorptionTime / 3);
            deltaBg = current.getValue() - Predictions.predict(currentTime, mealTreatments, bolusTreatments,
                    basalTreatments, profile.getSensitivity(), insulinDuration, profile.getCarbratio(), absorptionTime);
            nkbg = nkbg.append(deltaBg);
            times = times.append(currentTime / 60000);
        }

// uncomment for plot
//        Plot plt = Plot.create();

        int N = 3;
//        mealTimes = new ArrayRealVector();
//        mealValues = new ArrayRealVector();
//        for (; N < 10; N += 2) {
        E = new ArrayList();
        mealTimes = new ArrayRealVector(N);
        mealValues = new ArrayRealVector(N);
        // possible discrete meal times within snippet time range each dT/N Minutes.
        long step = (lastTime - firstTime) / (N - 1);
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

            if (i > 10 && Math.abs(abs_e - E.get(E.size() - 2)) < 1e-5) {
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
                    System.out.println("NaN in increment");
                }
                break;
            }
            mealTimes = mealTimes.add(delta.getSubVector(0, N));
            mealValues = mealValues.add(delta.getSubVector(N, N));
            mealTimes.mapToSelf((x) -> {
                return Math.min(lastTime, Math.max(firstTime - absorptionTime, x));
            });
            mealValues.mapToSelf((x) -> {
                return Math.max(0, x);
            });
        }
// uncomment for plot
//        plt.plot().add(E);

//            if (E.get(E.size() - 1) < 50) {
//                System.out.println("N: "+N+", MT: "+mealTimes.getDimension()+", MV: "+mealValues.getDimension());
//                break;
//            }
//        }

// uncomment for plot
//        try {
//            plt.show();
//        } catch (IOException | PythonExecutionException ex) {
//            Logger.getLogger(LMAlgo.class.getName()).log(Level.SEVERE, null, ex);
//        }
        
//        beta_t0_mod = round(beta_t0);
//        beta_meal_mod = beta_meal;
//        U = unique(beta_t0_mod);
//        for ii = 1:length(U)
//          t0 = U(ii);
//          idx = find(beta_t0_mod == t0);
//          if length(idx) > 1
//            beta_meal_mod(idx(1)) = sum(beta_meal_mod(idx));
//            beta_meal_mod(idx(2:end)) = [];
//          end
//        end
//        beta_t0_mod = U;
        double x, t0;
        for (int i = 0; i < N; i++) {
            x = mealValues.getEntry(i);
            t0 = mealTimes.getEntry(i);
            meal = new VaultEntry(VaultEntryType.MEAL_MANUAL,
                    TimestampUtils.createCleanTimestamp(new Date((long) t0 * 60000)), x);
            System.out.println(meal.toString());
            mealTreatments.add(meal);
        }
        return mealTreatments;

    }
}
