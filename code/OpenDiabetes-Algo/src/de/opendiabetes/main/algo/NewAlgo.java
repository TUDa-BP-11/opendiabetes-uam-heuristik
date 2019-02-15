package de.opendiabetes.main.algo;

import de.opendiabetes.main.math.Predictions;
import de.opendiabetes.parser.Profile;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.util.TimestampUtils;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.lang.Math.pow;

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
            if (current.getTimestamp().getTime() / 1000 - firstTime > 10 * 24 * 60 * 60)
                break;
            
            currentTime = current.getTimestamp().getTime() / 60000;
            currentLimit = currentTime + absorptionTime / 2;
            if (currentTime > estimatedTimeAccepted) {

                for (int j = 0; j < numBG - i; j++) {
                    next = glucose.get(i + j);
                    nextTime = next.getTimestamp().getTime();
                    if (nextTime/ 60000 <= currentLimit) {
                        currentPrediction = Predictions.predict(next.getTimestamp().getTime(), mealTreatments, bolusTreatments,
                                basalTreatments, profile.getSensitivity(), insDuration, profile.getCarbratio(), absorptionTime);
                        deltaBg = next.getValue() - currentPrediction;
                        lastTime = nextTime / 60000;
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
        // TODO implement matrix decomposition solution
//        RealMatrix matrix;
//        CholeskyDecomposition cd = new CholeskyDecomposition(matrix);
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
            if (current.getTimestamp().getTime() / 1000 - firstTime > 10 * 24 * 60 * 60)
                break;
            
            currentTime = current.getTimestamp().getTime() / 60000;
            currentLimit = currentTime + absorptionTime / 2;
            if (currentTime > estimatedTimeAccepted) {

                for (int j = 0; j < numBG - i; j++) {
                    next = glucose.get(i + j);
                    nextTime = next.getTimestamp().getTime();
                    if (nextTime/ 60000 <= currentLimit) {
                        currentPrediction = Predictions.predict(next.getTimestamp().getTime(), mealTreatments, bolusTreatments,
                                basalTreatments, profile.getSensitivity(), insDuration, profile.getCarbratio(), absorptionTime);
                        deltaBg = next.getValue() - currentPrediction;
                        lastTime = nextTime / 60000;
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

}
