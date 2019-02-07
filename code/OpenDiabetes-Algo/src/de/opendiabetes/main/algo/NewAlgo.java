package de.opendiabetes.main.algo;

import de.opendiabetes.main.math.Predictions;
import de.opendiabetes.parser.Profile;
import de.opendiabetes.vault.engine.container.VaultEntry;
import de.opendiabetes.vault.engine.container.VaultEntryType;
import de.opendiabetes.vault.engine.util.TimestampUtils;
import static java.lang.Math.pow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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
        this.glucose = glucose;
    }

    @Override
    public void setBolusTreatments(List<VaultEntry> bolusTreatments) {
        this.bolusTreatments = bolusTreatments;
    }

    @Override
    public void setBasalTreatments(List<TempBasal> basalTreatments) {
        this.basalTreatments = basalTreatments;
    }

    @Override
    public List<VaultEntry> calculateMeals() {

        double weight = 1;
        List<VaultEntry> mealTreatments = new ArrayList<>();
        List<VaultEntry> glucoseEvents = new ArrayList<>();
        PolynomialCurveFitter pcf = PolynomialCurveFitter.create(2);
        ArrayList<WeightedObservedPoint> observations = new ArrayList<>();

        // Optional extension: set initial value to medium sized carbs and no offsets
//        double[] initialValues = {0,0,2*10*profile.getSensitivity()/(absorptionTime*profile.getCarbratio())};
//        pcf = pcf.withStartPoint(initialValues);
        pcf = pcf.withMaxIterations(100);
        VaultEntry meal;
        VaultEntry current = glucose.remove(0);
        long estimatedTime = 0l;
        long estimatedTimeAccepted = 0l;
        while (!glucose.isEmpty()) {

            if (current.getTimestamp().getTime() > estimatedTimeAccepted) {
                glucoseEvents.add(current);
                VaultEntry next = glucose.get(0);
                long deltaTime = Math.round((next.getTimestamp().getTime() - current.getTimestamp().getTime()) / 60000.0);

                for (int i = 1; i < glucose.size() && deltaTime <= absorptionTime / 2; i++) {
                    glucoseEvents.add(next);
                    next = glucose.get(i);
                    deltaTime = Math.round((next.getTimestamp().getTime() - current.getTimestamp().getTime()) / 60000.0);
                    if (i == glucose.size()) {
                        glucoseEvents.add(next);
                    }
                }
                double currentPrediction;
                double deltaBg;
                for (VaultEntry event : glucoseEvents) {

                    currentPrediction = Predictions.predict(event.getTimestamp().getTime(), mealTreatments, bolusTreatments,
                            basalTreatments, profile.getSensitivity(), insDuration, profile.getCarbratio(), absorptionTime);
                    deltaBg = event.getValue() - currentPrediction;
                    observations.add(new WeightedObservedPoint(weight, event.getTimestamp().getTime() / 60000.0, deltaBg));
//                System.out.println(event.getTimestamp().getTime());
//                System.out.println(deltaBg);

                }
//            for (WeightedObservedPoint obs : observations)
//                System.out.println("x="+obs.getX()+"y="+obs.getY());

                double[] lsq = pcf.fit(observations);
                assert (lsq[2] > 0);
                double error = (4*lsq[2]*lsq[0]-pow(lsq[1],2))/(4*lsq[2]);
                estimatedTime = (long) (-lsq[1] / (2 * lsq[2]));
//            System.out.println("LSQ: "+Arrays.toString(lsq)+" Num Obs: "+observations.size());
                double estimatedCarbs = lsq[2] * pow(absorptionTime, 2) * profile.getCarbratio() / (2 * profile.getSensitivity());
                if (estimatedCarbs > 0
//                        && error < 10 
                        && current.getTimestamp().getTime() - estimatedTime < absorptionTime/2 
                        && estimatedTime < next.getTimestamp().getTime()/60000 
                        ) 
                {
                    estimatedTimeAccepted = estimatedTime;
                    meal = new VaultEntry(VaultEntryType.MEAL_MANUAL, TimestampUtils.createCleanTimestamp(new Date(estimatedTime*60000l)), estimatedCarbs);
                    mealTreatments.add(meal);

                    System.out.println(meal.toString());
                }
//            double currentPrediction = Predictions.predict(current.getTimestamp().getTime(), mealTreatments, bolusTreatments, basalTreatments, profile.getSensitivity(), insDuration, profile.getCarbratio(), absorptionTime);
//            double nextPrediction = Predictions.predict(next.getTimestamp().getTime(), mealTreatments, bolusTreatments, basalTreatments, profile.getSensitivity(), insDuration, profile.getCarbratio(), absorptionTime);
//            double deltaBg = next.getValue() - current.getValue();
//            double deltaPrediction = (nextPrediction - currentPrediction);

//            if (deltaBg - deltaPrediction > 0) {
//                mealTreatments.add(createMeal(deltaBg - deltaPrediction, deltaTime, current.getTimestamp()));
//            }
            }
            current = glucose.remove(0);
            glucoseEvents.clear();
            observations.clear();
        }

        return mealTreatments;
    }

    private VaultEntry createMeal(double deltaBg, double deltaTime, Date timestamp) {
        double value = deltaBg * profile.getCarbratio() / (profile.getSensitivity() * Predictions.carbsOnBoard(deltaTime, absorptionTime));
        return new VaultEntry(VaultEntryType.MEAL_MANUAL, TimestampUtils.createCleanTimestamp(timestamp), value);
    }

}

//c i  c i  C
//g g g g g g=c1+i1+c2+i2+C
//NKBG = BG - BGI - BGC

