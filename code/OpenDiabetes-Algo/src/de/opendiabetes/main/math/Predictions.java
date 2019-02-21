package de.opendiabetes.main.math;


import de.opendiabetes.vault.container.VaultEntry;

import java.util.List;

public class Predictions {

    public static double predict(long time, List<VaultEntry> mealTreatments, List<VaultEntry> bolusTreatments, List<VaultEntry> basalTreatments,
            double insSensitivityFactor, double insDuration, double carbRatio, double absorptionTime) {
        double result = 0;
        for (VaultEntry meal : mealTreatments) {
            long deltaTime = Math.round((time - meal.getTimestamp().getTime()) / 60000.0);  //Time in minutes
            if (deltaTime <= 0) {
                break;
            }
            result += deltaBGC(deltaTime, insSensitivityFactor, carbRatio, meal.getValue(), absorptionTime);
//            System.out.println("BGC: "+result);
        }
        for (VaultEntry bolus : bolusTreatments) {
            long deltaTime = Math.round((time - bolus.getTimestamp().getTime()) / 60000.0); //Time in minutes
            if (deltaTime <= 0) {
                break;
            }
            result += deltaBGI(deltaTime, bolus.getValue(), insSensitivityFactor, insDuration);
//            System.out.println("BGI: "+result);
        }
        for (VaultEntry basal : basalTreatments) {
            long deltaTime = Math.round((time - basal.getTimestamp().getTime()) / 60000.0);      //Time in minutes
            if (deltaTime <= 0) {
                break;
            }
            result += deltatempBGI(deltaTime, basal.getValue(), insSensitivityFactor, insDuration, 0, basal.getValue2());
//            System.out.println("tempBGI: "+result);
        }

        return result;
    }

    /**
     * Calculates the percentage of carbs on board
     *
     * @param timeFromEvent time in minutes from last meal
     * @param absorptionTime time in minutes to absorb a hole meal
     * @return percentage of carbs absorbed
     */
    public static double carbsOnBoard(double timeFromEvent, double absorptionTime) {
        double total;

        if (timeFromEvent <= 0) {
            total = 0.0;
        } else if (timeFromEvent >= absorptionTime) {
            total = 1.0;
        } else if (timeFromEvent <= absorptionTime / 2.0) {
            total = 2.0 * (timeFromEvent * timeFromEvent) / (absorptionTime * absorptionTime);
        } else {
            total = -1.0 + 4.0 * timeFromEvent / absorptionTime - 2 * timeFromEvent * timeFromEvent / (absorptionTime * absorptionTime);
        }
        return total;
    }

    /**
     * Calculates the percentage of insulin on board
     *
     * @param timeFromEvent time in minutes from last meal
     * @param insDuration effective time of insulin in minutes
     * @return percentage of insulin still on board
     */
    public static double fastActingIob(double timeFromEvent, double insDuration) {
        double IOBWeight;
        if (timeFromEvent <= 0) {
            IOBWeight = 1;
        } else if ( timeFromEvent >= insDuration) { //timeFromEvent < 0 ||
            IOBWeight = 0;
        } else {

            double peak = 55;
            //double peak = insDuration * 75 / 180.0;

            //Time constant of exp decay
            double decay = peak * (1 - peak / insDuration)
                    / (1 - 2 * peak / insDuration);

            //Rise time factor
            double growth = 2 * decay / insDuration;

            //Auxiliary scale factor
            double scale = 1 / (1 - growth + (1 + growth) * Math.exp(-insDuration / decay));

            IOBWeight = 1 - scale * (1 - growth)
                    * (((timeFromEvent * timeFromEvent)
                    / (decay * insDuration * (1 - growth))
                    - timeFromEvent / decay - 1)
                    * Math.exp(-timeFromEvent / decay) + 1);

        }
        return IOBWeight;
        
    }

    /**
     * simpsons rule to integrate insulin on board.
     * https://github.com/Perceptus/GlucoDyn/blob/master/js/glucodyn/algorithms.js
     *
     * @param t1 left border of integral - 0
     * @param t2 right border of integral - duration of insulin event
     * @param insDuration effective time of insulin in minutes
     * @param timeFromEvent time in minutes since insulin event
     * @return
     */
    public static double integrateIob(double t1, double t2, double insDuration, double timeFromEvent) {
        // timeFromEvent - scale*(1-growth) *(Math.exp(-timeFromEvent/decay) * (2*decay+timeFromEvent-(2*decay^2+2*decay*timeFromEvent+timeFromEvent^2)/((1-growth) * insDuration))+timeFromEvent)

        double integral;
        double dx;
        int N = 25;
        int nn = 2 * N; //nn needs to be even

        //initialize with first and last terms of simpson series
        //t1 & t2 Grenzen des Intervalls das betrachtet wird
        dx = (t2 - t1) / nn;
        //integral = getIOBWeight((timeFromEvent - t1), insDuration) + getIOBWeight(timeFromEvent - (t1 + nn * dx), insDuration);

        // Orig:
        integral = fastActingIob((timeFromEvent - t1), insDuration)
                + fastActingIob(timeFromEvent - (t1 + nn * dx), insDuration);
        int ii = 1;
        while (ii < nn - 2) {
            //integral = integral + 4 * getIOBWeight(timeFromEvent - (t1 + ii * dx), insDuration) + 2 * getIOBWeight(timeFromEvent - (t1 + (ii + 1) * dx), insDuration);
            integral = integral
                    + 4 * fastActingIob(timeFromEvent
                            - (t1 + ii * dx), insDuration)
                    + 2 * fastActingIob(timeFromEvent
                            - (t1 + (ii + 1) * dx), insDuration);
            ii = ii + 2;
        }

        integral = integral * dx / 3.0;
// Ende - orig
//        integral = fastActingIob((timeFromEvent - t1), insDuration)
//                + fastActingIob(timeFromEvent - (t1 + nn * dx), insDuration);
//
//        for (int i = 1; i < N; i++) {
//            integral = integral
//                    + 4 * fastActingIob(timeFromEvent
//                            - (t1 + (2 * i - 1) * dx), insDuration)
//                    + 2 * fastActingIob(timeFromEvent
//                            - (t1 + (2 * i) * dx), insDuration);
//        }
//        integral = integral
//                + 4 * fastActingIob(timeFromEvent
//                        - (t1 + (nn - 1) * dx), insDuration);
//        integral = integral * dx / 3.0;
        return integral;
    }

    //tempInsAmount in U/min
    //function deltatempBGI(g,dbdt,sensf,idur,t1,t2)
    public static double deltatempBGI(double timeFromEvent, double tempInsAmount, double insSensitivityFactor, double insDuration, double t1, double t2) {
        return -tempInsAmount * insSensitivityFactor * ((t2 - t1) - integrateIob(t1, t2, insDuration, timeFromEvent));
        //return -tempInsAmount * insSensitivityFactor * ((t2 - t1) - 1.0 / 100.0 * integrateIob(t1, t2, insDuration, timeFromEvent));
    }

    //function deltaBGC(g,sensf,cratio,camount,ct)
    public static double deltaBGC(double timeFromEvent, double insSensitivityFactor, double carbRatio, double carbsAmount, double absorptionTime) {
        return insSensitivityFactor / carbRatio * carbsAmount * Predictions.carbsOnBoard(timeFromEvent, absorptionTime);
    }

    //function deltaBGI(g,bolus,sensf,idur)
    public static double deltaBGI(double timeFromEvent, double insBolus, double insSensitivityFactor, double insDuration) {
        return -insBolus * insSensitivityFactor * (1 - fastActingIob(timeFromEvent, insDuration));
        //return -insBolus * insSensitivityFactor * (1 - getIOBWeight(timeFromEvent, insDuration) / 100.0);
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
}
