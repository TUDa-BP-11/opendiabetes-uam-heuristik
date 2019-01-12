package de.opendiabetes.algo;

import de.opendiabetes.vault.engine.container.VaultEntry;
import de.opendiabetes.vault.engine.container.VaultEntryType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OpenDiabetesAlgo implements Algorithm {
    private double absorptionTime;
    private double insDuration;
    private double carbRatio;
    private double insSensitivityFactor;
    private List<VaultEntry> glucose;
    private List<VaultEntry> bolusTreatments;
    private List<VaultEntry> mealTreatments;
    private List<TempBasal> basalTratments;

    public OpenDiabetesAlgo() {
        absorptionTime = 120;
        insDuration = 180;
        carbRatio = 10;
        insSensitivityFactor = 35;
        glucose = new ArrayList<>();
        bolusTreatments = new ArrayList<>();
        mealTreatments = new ArrayList<>();
        basalTratments = new ArrayList<>();
    }

    public OpenDiabetesAlgo(double absorptionTime, double insDuration, double carbRatio, double insSensitivityFactor) {
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
        mealTreatments = new ArrayList<>();
        VaultEntry current = glucose.remove(0);

        while (!glucose.isEmpty()) {
            VaultEntry next = glucose.get(0);
            long deltaTime = Math.round((next.getTimestamp().getTime() - current.getTimestamp().getTime()) / 60000.0);

            for (int i = 1; i < glucose.size() && deltaTime < 30; i++) {
                next = glucose.get(i);
                deltaTime = Math.round((next.getTimestamp().getTime() - current.getTimestamp().getTime()) / 60000.0);

            }

            double currentPrediction = predict(0, current.getTimestamp().getTime());
            double nextPrediction = predict(0, next.getTimestamp().getTime());
            double deltaBg = next.getValue() - current.getValue();
            double deltaPrediction = (nextPrediction - currentPrediction);

            if (deltaBg - deltaPrediction > 0) {
                createMeal(deltaBg - deltaPrediction, deltaTime, current.getTimestamp());
            }
            current = glucose.remove(0);
        }

        return mealTreatments;
    }

    private void createMeal(double deltaBg, double deltaTime, Date timestamp) {
        double value = deltaBg * carbRatio / (insSensitivityFactor * Algorithm.carbsOnBoard(deltaTime, absorptionTime));
        mealTreatments.add(new VaultEntry(VaultEntryType.MEAL_MANUAL, timestamp, value));
    }

    private double predict(double startValue, long time) {
        double result = startValue;
        for (VaultEntry meal : mealTreatments) {
            long deltaTime = Math.round((time - meal.getTimestamp().getTime()) / 60000.0);//Time in minutes
            if (deltaTime <= 0) {
                break;
            }
            result += deltaBGC(deltaTime, insSensitivityFactor, carbRatio, meal.getValue(), absorptionTime);
        }
        for (VaultEntry bolus : bolusTreatments) {
            long deltaTime = Math.round((time - bolus.getTimestamp().getTime()) / 60000.0);//Time in minutes
            if (deltaTime <= 0) {
                break;
            }
            result += deltaBGI(deltaTime, bolus.getValue(), insSensitivityFactor, insDuration);
        }
        for (TempBasal basal : basalTratments) {
            long deltaTime = Math.round((time - basal.getDate().getTime()) / 60000.0);//Time in minutes
            if (deltaTime <= 0) {
                break;
            }
            double unitsPerMin = basal.getValue() / basal.getDuration();
            result += deltatempBGI(deltaTime, unitsPerMin, insSensitivityFactor, insDuration, 0, basal.getDuration());
        }

        return result;
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

    /*
    //function iob(g,idur)
    public int getIOBWeight(double timeFromEvent, int insDuration) {
        int IOBWeight;
        if (timeFromEvent <= 0) {
            IOBWeight = 100;
        } else if (timeFromEvent >= insDuration) {
            IOBWeight = 0;
        } else {
            IOBWeight = (int) (-3.203e-7 * Math.pow(timeFromEvent, 4) + 1.354e-4 * Math.pow(timeFromEvent, 3) - 1.759e-2 * Math.pow(timeFromEvent, 2) + 9.255e-2 * timeFromEvent + 99.951);
        }
        return IOBWeight;
    }

    //deltaBG(g,sensf,cratio,camount,ct,bolus,idur)
    public double deltaBG(double timeFromEvent, double insSensitivityFactor, double carbRatio, double carbsAmount, double absorptionTime, double insBolus, double insDuration) {
        return deltaBGI(timeFromEvent, insBolus, insSensitivityFactor, insDuration) +
                deltaBGC(timeFromEvent, insSensitivityFactor, carbRatio, carbsAmount, absorptionTime);
    }
    */


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
    public double integrateIOB(double t1, double t2, double insDuration, double timeFromEvent) {
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

    //tempInsAmount in U/min
    //function deltatempBGI(g,dbdt,sensf,idur,t1,t2)
    public double deltatempBGI(double timeFromEvent, double tempInsAmount, double insSensitivityFactor, double insDuration, double t1, double t2) {
        return -tempInsAmount * insSensitivityFactor * ((t2 - t1) - integrateIOB(t1, t2, insDuration, timeFromEvent));
        //return -tempInsAmount * insSensitivityFactor * ((t2 - t1) - 1.0 / 100.0 * integrateIOB(t1, t2, insDuration, timeFromEvent));
    }

    //function deltaBGC(g,sensf,cratio,camount,ct)
    public double deltaBGC(double timeFromEvent, double insSensitivityFactor, double carbRatio, double carbsAmount, double absorptionTime) {
        return insSensitivityFactor / carbRatio * carbsAmount * Algorithm.carbsOnBoard(timeFromEvent, absorptionTime);
    }

    //function deltaBGI(g,bolus,sensf,idur)
    public double deltaBGI(double timeFromEvent, double insBolus, double insSensitivityFactor, double insDuration) {
        return -insBolus * insSensitivityFactor * (1 - fastActingIob(timeFromEvent, insDuration));
        //return -insBolus * insSensitivityFactor * (1 - getIOBWeight(timeFromEvent, insDuration) / 100.0);
    }
}
