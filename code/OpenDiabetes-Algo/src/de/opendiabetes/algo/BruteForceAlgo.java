package de.opendiabetes.algo;

import de.opendiabetes.vault.engine.container.VaultEntry;
import de.opendiabetes.vault.engine.container.VaultEntryType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class BruteForceAlgo implements Algorithm {
    private double absorptionTime;
    private double insDuration;
    private double carbRatio;
    private double insSensitivityFactor;
    private List<VaultEntry> glucose;
    private List<VaultEntry> bolusTreatments;
    private List<VaultEntry> mealTreatments;
    private List<TempBasal> basalTratments;

    public BruteForceAlgo() {
        absorptionTime = 120;
        insDuration = 180;
        carbRatio = 10;
        insSensitivityFactor = 35;
        glucose = new ArrayList<>();
        bolusTreatments = new ArrayList<>();
        mealTreatments = new ArrayList<>();
        basalTratments = new ArrayList<>();
    }

    public BruteForceAlgo(double absorptionTime, double insDuration, double carbRatio, double insSensitivityFactor) {
        this.absorptionTime = absorptionTime;
        this.insDuration = insDuration;
        this.carbRatio = carbRatio;
        this.insSensitivityFactor = insSensitivityFactor;
        glucose = new ArrayList<>();
        bolusTreatments = new ArrayList<>();
        mealTreatments = new ArrayList<>();
        basalTratments = new ArrayList<>();
    }

    @Override
    public void setCarbRatio(double carbRatio) {
        this.carbRatio = carbRatio;
    }

    public double getCarbRatio() {
        return carbRatio;
    }

    @Override
    public void setInsulinSensitivity(double insSensitivity) {
        this.insSensitivityFactor = insSensitivity;
    }

    public double getInsulinSensitivity() {
        return insSensitivityFactor;
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
    public void setGlucoseMeasurements(List<VaultEntry> glucose) {
        this.glucose = new ArrayList<>(glucose);
    }

    @Override
    public void setBolusTreatments(List<VaultEntry> bolusTreatments) {
        this.bolusTreatments = bolusTreatments;
    }

    @Override
    public void setBasalTratments(List<TempBasal> basalTratments) {
        this.basalTratments = basalTratments;
    }


    @Override
    public List<VaultEntry> calculateMeals() {
        glucose.sort(Comparator.comparing(VaultEntry::getTimestamp));
        List<VaultEntry> meals = new ArrayList<>();
        VaultEntry current = glucose.remove(0);
        while (!glucose.isEmpty()) {
            VaultEntry next;
            long deltaTime;
            double deltaBG;
            do {
                next = glucose.remove(0);
                deltaTime = Math.round((next.getTimestamp().getTime() - current.getTimestamp().getTime()) / 60000D);
                deltaBG = next.getValue() - current.getValue();
            } while (!glucose.isEmpty() && deltaTime < 10 && Math.abs(deltaBG) < 1);

            if (deltaTime < 10 && Math.abs(deltaBG) < 1) {
                double find = findBruteForce(deltaTime, deltaBG, 20);
                meals.add(new VaultEntry(VaultEntryType.MEAL_MANUAL, current.getTimestamp(), find));
            }
            current = next;
        }
        return meals;
    }

    private double findBruteForce(long deltaT, double deltaBG, double carbsAmount) {
        double test = deltaBGC(deltaT, insSensitivityFactor, carbRatio, carbsAmount, absorptionTime);
        if (Math.abs(test - deltaBG) <= 0.1)
            return carbsAmount;
        if (test < deltaBG)
            return findBruteForce(deltaT, deltaBG, carbsAmount * 1.5);
        return findBruteForce(deltaT, deltaBG, carbsAmount * 0.75);
    }

    public double fastActingIob(double timeFromEvent, double insDuration) {
        double IOBWeight;
        if (timeFromEvent <= 0) {
            IOBWeight = 1;
        } else if (timeFromEvent >= insDuration) {
            IOBWeight = 0;
        } else {

            double peak = 55;
            //double peak = insDuration * 75 / 180.0;

            //Time constant of exp decay
            double decay = peak * (1 - peak / insDuration) / (1 - 2 * peak / insDuration);

            //Rise time factor
            double growth = 2 * decay / insDuration;

            //Auxiliary scale factor
            double scale = 1 / (1 - growth + (1 + growth) * Math.exp(-insDuration / decay));

            IOBWeight = 1 - scale * (1 - growth) * ((Math.pow(timeFromEvent, 2) / (decay * insDuration * (1 - growth)) - timeFromEvent / decay - 1) * Math.exp(-timeFromEvent / decay) + 1);
        }
        return IOBWeight;
    }

    /**
     * https://github.com/Perceptus/GlucoDyn/blob/master/js/glucodyn/algorithms.js
     * <p>
     * simpsons rule to integrate IOB
     * oldFunction intIOB(x1,x2,idur,g)
     *
     * @param timeFromEvent
     * @param insDuration   //Insulin decomposition rate in minutes
     * @return
     */
    private double integrateIOB(double t1, double t2, double insDuration, double timeFromEvent) {
        double integral;
        double dx;
        int nn = 50; //nn needs to be even
        int ii = 1;

        //initialize with first and last terms of simpson series
        //t1 & t2 Grenzen des Intervalls das betrachtet wird
        dx = (t2 - t1) / nn;
        //integral = getIOBWeight((timeFromEvent - t1), insDuration) + getIOBWeight(timeFromEvent - (t1 + nn * dx), insDuration);
        integral = fastActingIob((timeFromEvent - t1), insDuration) + fastActingIob(timeFromEvent - (t1 + nn * dx), insDuration);

        while (ii < nn - 2) {
            //integral = integral + 4 * getIOBWeight(timeFromEvent - (t1 + ii * dx), insDuration) + 2 * getIOBWeight(timeFromEvent - (t1 + (ii + 1) * dx), insDuration);
            integral = integral + 4 * fastActingIob(timeFromEvent - (t1 + ii * dx), insDuration) + 2 * fastActingIob(timeFromEvent - (t1 + (ii + 1) * dx), insDuration);
            ii = ii + 2;
        }

        integral = integral * dx / 3.0;
        return integral;

    }

    //g is time in minutes,gt is carb type
    //function cob(g,ct)
    public double cob(double timeFromEvent, double absorptionTime) {
        double total;

        if (timeFromEvent <= 0) {
            total = 0.0;
        } else if (timeFromEvent >= absorptionTime) {
            total = 1.0;
        } else if (timeFromEvent <= absorptionTime / 2.0) {
            total = 2.0 / Math.pow(absorptionTime, 2) * Math.pow(timeFromEvent, 2);
        } else {
            total = -1.0 + 4.0 / absorptionTime * (timeFromEvent - Math.pow(timeFromEvent, 2) / (2.0 * absorptionTime));
        }
        return total;
    }

    //tempInsAmount in U/min
    //function deltatempBGI(g,dbdt,sensf,idur,t1,t2)
    public double deltatempBGI(double timeFromEvent, double tempInsAmount, double insSensitivityFactor, double insDuration, double t1, double t2) {
        return -tempInsAmount * insSensitivityFactor * ((t2 - t1) - integrateIOB(t1, t2, insDuration, timeFromEvent));
        //return -tempInsAmount * insSensitivityFactor * ((t2 - t1) - 1.0 / 100.0 * integrateIOB(t1, t2, insDuration, timeFromEvent));
    }

    //function deltaBGC(g,sensf,cratio,camount,ct)
    public double deltaBGC(double timeFromEvent, double insSensitivityFactor, double carbRatio, double carbsAmount, double absorptionTime) {
        return insSensitivityFactor / carbRatio * carbsAmount * cob(timeFromEvent, absorptionTime);
    }

    //function deltaBGI(g,bolus,sensf,idur)
    public double deltaBGI(double timeFromEvent, double insBolus, double insSensitivityFactor, double insDuration) {
        return -insBolus * insSensitivityFactor * (1 - fastActingIob(timeFromEvent, insDuration));
        //return -insBolus * insSensitivityFactor * (1 - getIOBWeight(timeFromEvent, insDuration) / 100.0);
    }

}
