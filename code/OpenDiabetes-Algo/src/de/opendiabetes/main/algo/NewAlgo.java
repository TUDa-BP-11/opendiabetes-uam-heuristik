package de.opendiabetes.main.algo;

import com.github.sh0nk.matplotlib4j.Plot;
import com.github.sh0nk.matplotlib4j.PythonExecutionException;
import de.opendiabetes.main.math.Predictions;
import de.opendiabetes.parser.Profile;
import de.opendiabetes.vault.engine.container.VaultEntry;
import de.opendiabetes.vault.engine.container.VaultEntryType;
import de.opendiabetes.vault.engine.util.TimestampUtils;
import java.io.IOException;
import static java.lang.Math.pow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.math3.linear.*;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;

/**
 *
 * @author anna
 */
public class NewAlgo implements Algorithm {

    private double absorptionTime;
    private double insDuration;
    private double insSensitivity;
    private double carbRate;
    private Profile profile;
    private List<VaultEntry> glucose;
    private List<VaultEntry> bolusTreatments;
    private List<TempBasal> basalTreatments;

    public NewAlgo() {
        absorptionTime = 120;
        insDuration = 180;
        insSensitivity = 35;
        carbRate = 10;
        glucose = new ArrayList<>();
        bolusTreatments = new ArrayList<>();
        basalTreatments = new ArrayList<>();
    }

    public NewAlgo(double absorptionTime, double insDuration, Profile profile) {
        this.absorptionTime = absorptionTime;
        this.insDuration = insDuration;
        this.profile = profile;
        glucose = new ArrayList<>();
        bolusTreatments = new ArrayList<>();
        basalTreatments = new ArrayList<>();
    }

    public double getCarbRatio() {
        return profile.getCarbratio();
    }

    public double getInsulinSensitivity() {
        return profile.getSensitivity();
    }

    @Override
    public void setAbsorptionTime(double absorptionTime) {
        this.absorptionTime = absorptionTime;
    }

    public double getAbsorptionTime() {
        return absorptionTime;
    }

    @Override
    public void setInsulinDuration(double insulinDuration) {
        this.insDuration = insulinDuration;
    }

    public double getInsulinDuration() {
        return insDuration;
    }

    @Override
    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    @Override
    public void setGlucoseMeasurements(List<VaultEntry> glucose) {
        this.glucose = new ArrayList<>(glucose);
    }

    @Override
    public void setBolusTreatments(List<VaultEntry> bolusTreatments) {
        this.bolusTreatments = new ArrayList<>(bolusTreatments);
    }

    @Override
    public void setBasalTreatments(List<TempBasal> basalTreatments) {
        this.basalTreatments = new ArrayList<>(basalTreatments);
    }

    @Override
    public List<VaultEntry> calculateMeals() {

        double weight = 1;
        List<VaultEntry> mealTreatments = new ArrayList<>();
        PolynomialCurveFitter pcf = PolynomialCurveFitter.create(2);
        ArrayList<WeightedObservedPoint> observations = new ArrayList<>();

        // Optional extension: set initial value to medium sized carbs and no offsets
        double[] initialValues = {0, 0, 2 * 10 * profile.getSensitivity() / (absorptionTime * profile.getCarbratio())};
        pcf = pcf.withStartPoint(initialValues);
        pcf = pcf.withMaxIterations(100);
        VaultEntry meal;
        VaultEntry next;
        int numBG = glucose.size();
        VaultEntry current;

        // debugging: break after 10 days
        current = glucose.get(0);
        long firstTime = current.getTimestamp().getTime() / 1000;

        long estimatedTime;
        long currentTime;
        long nextTime;
        double lastTime = 0;
        double currentLimit;
        long estimatedTimeAccepted = 0l;
        double currentPrediction;
        double deltaBg;

        for (int i = 0; i < numBG; i++) {
            current = glucose.get(i);

            // debugging: break after 10 days
            if (current.getTimestamp().getTime() / 1000 - firstTime > 10 * 24 * 60 * 60) {
                break;
            }

            currentTime = current.getTimestamp().getTime() / 60000;
            currentLimit = currentTime + absorptionTime / 4;
            if (currentTime > estimatedTimeAccepted) {

                for (int j = 0; j < numBG - i; j++) {
                    next = glucose.get(i + j);
                    nextTime = next.getTimestamp().getTime() / 60000;
                    if (nextTime <= currentLimit) {
                        currentPrediction = Predictions.predict(next.getTimestamp().getTime(), mealTreatments, bolusTreatments,
                                basalTreatments, profile.getSensitivity(), insDuration, profile.getCarbratio(), absorptionTime);
                        deltaBg = next.getValue() - currentPrediction;
                        lastTime = nextTime;
                        weight = 1 - (nextTime - currentTime) / (absorptionTime / 2);
                        observations.add(new WeightedObservedPoint(weight, lastTime, deltaBg));
                    }
                }
                // lsq = [c, b, a]
                double[] lsq = pcf.fit(observations);
                assert (lsq[2] > 0);
                double error = lsq[0] - pow(lsq[1], 2) / (4 * lsq[2]);
                estimatedTime = (long) (-lsq[1] / (2 * lsq[2]));
                double estimatedCarbs = lsq[2] * pow(absorptionTime, 2) * profile.getCarbratio() / (2 * profile.getSensitivity());
                if (currentTime - estimatedTime < absorptionTime / 2
                        && estimatedTime < lastTime) {
//                    System.out.println("estimatedCarbs: " + estimatedCarbs + " estimatedTime: " + new Date(estimatedTime*60000).toString() + " Num Obs: " + observations.size());
                    if (estimatedCarbs > 0
                            && estimatedCarbs < 200 //                            && error < 10
                            ) {
                        estimatedTimeAccepted = estimatedTime;
//                        System.out.println(new Date(estimatedTime * 60000));
                        meal = new VaultEntry(VaultEntryType.MEAL_MANUAL, TimestampUtils.createCleanTimestamp(new Date(estimatedTime * 60000)), estimatedCarbs);
                        mealTreatments.add(meal);
//                    System.out.println(meal.toString());
                    }
                }
            }
            observations.clear();
        }

        return mealTreatments;
    }

    public List<VaultEntry> calculateMeals2() {
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
        long firstTime = current.getTimestamp().getTime();

        long estimatedTime;
        long currentTime;
        long nextTime;
        long lastTime = 0;
        double currentLimit;
        long estimatedTimeAccepted = 0l;
//        double currentPrediction;
//        double correction;
        double nextPrediction;
        double deltaBg;

        Plot plt = Plot.create();
        for (int i = 0; i < numBG; i++) {

            nkbg = new ArrayRealVector();
            times = new ArrayRealVector();
            alNkbg = new ArrayList();
            albg = new ArrayList();
            alPred = new ArrayList();
            alTimes = new ArrayList();

            current = glucose.get(i);
//            currentPrediction = Predictions.predict(current.getTimestamp().getTime(), mealTreatments, bolusTreatments,
//                    basalTreatments, profile.getSensitivity(), insDuration, profile.getCarbratio(), absorptionTime);
//            correction = current.getValue() - currentPrediction;

            // debugging: break after 1 day
            if (current.getTimestamp().getTime() - firstTime > 1 * 24 * 60 * 60 * 1000) {
                break;
            }
            currentTime = current.getTimestamp().getTime() / 60000;
            currentLimit = currentTime + absorptionTime/6;
            if (currentTime > estimatedTimeAccepted) {
                for (int j = 0; j < numBG - i; j++) {
                    next = glucose.get(i + j);
                    nextTime = next.getTimestamp().getTime() / 60000;
                    if (nextTime <= currentLimit) {

                        nextPrediction = Predictions.predict(next.getTimestamp().getTime(), mealTreatments, bolusTreatments,
                                basalTreatments, profile.getSensitivity(), insDuration, profile.getCarbratio(), absorptionTime);

                        deltaBg = next.getValue() - nextPrediction;
//                        System.out.println("calculateMeals2(): " + deltaBg + " time: " + next.getTimestamp());
                        times = times.append(nextTime - currentTime);
                        lastTime = nextTime;
                        nkbg = nkbg.append(deltaBg);
                        alTimes.add(nextTime*60);
                        alNkbg.add(deltaBg+i);
                        alPred.add(nextPrediction);
                        albg.add(next.getValue());
                    }
                }

                plt.plot().addDates(alTimes).add(alNkbg);
                plt.plot().addDates(alTimes).add(albg).linestyle("dashed");
                plt.plot().addDates(alTimes).add(alPred).linestyle("dotted");

//                System.out.println("de.opendiabetes.main.algo.NewAlgo.calculateMeals2(): " + times.getDimension());
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
                    
                    System.out.println("Date: "+new Date(estimatedTime * 60000)+" Carbs: "+estimatedCarbs);
//                    System.out.println("Tstart: "+new Date(currentTime*60000)+" Tend: "+new Date(lastTime*60000));
                    
//                    if (currentTime - estimatedTime < absorptionTime / 2
//                            && estimatedTime < lastTime) {
//                    System.out.println("estimatedCarbs: " + estimatedCarbs + " estimatedTime: " + new Date(estimatedTime*60000).toString() + " Num Obs: " + observations.size());
//                        if (estimatedCarbs >= 0 // && estimatedCarbs < 200 //                            && error < 10
//                                ) {
                            estimatedTimeAccepted = estimatedTime;
                            meal = new VaultEntry(VaultEntryType.MEAL_MANUAL, TimestampUtils.createCleanTimestamp(new Date(estimatedTime * 60000)), estimatedCarbs);
                            mealTreatments.add(meal);
//                    System.out.println(meal.toString());
//                        }
//                    }
                }
            }
        }
        try {
            plt.show();
//            plt2.show();
        } catch (IOException | PythonExecutionException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        return mealTreatments;
    }

}
