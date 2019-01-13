package de.opendiabetes.main.math;

public class Predictions {
    /**
     * Calculates the percentage of carbs on board
     *
     * @param timeFromEvent  time in minutes from last meal
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
            total = 2.0 / (absorptionTime * absorptionTime) * (timeFromEvent * timeFromEvent);
        } else {
            total = -1.0 + 4.0 / absorptionTime * (timeFromEvent - (timeFromEvent * timeFromEvent) / (2.0 * absorptionTime));
        }
        return total;
    }

    /**
     * Calculates the percentage of insulin on board
     *
     * @param timeFromEvent time in minutes from last meal
     * @param insDuration   effective time of insulin in minutes
     * @return percentage of insulin still on board
     */
    public static double fastActingIob(double timeFromEvent, double insDuration) {
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

            IOBWeight = 1 - scale * (1 - growth) * (((timeFromEvent * timeFromEvent) / (decay * insDuration * (1 - growth)) - timeFromEvent / decay - 1) * Math.exp(-timeFromEvent / decay) + 1);
        }
        return IOBWeight;
    }

    /**
     * simpsons rule to integrate insulin on board.
     * https://github.com/Perceptus/GlucoDyn/blob/master/js/glucodyn/algorithms.js
     *
     * @param t1            left border of integral    - 0
     * @param t2            right border of integral   - duration of insulin event
     * @param insDuration   effective time of insulin in minutes
     * @param timeFromEvent time in minutes since insulin event
     * @return
     */
    public static double integrateIob(double t1, double t2, double insDuration, double timeFromEvent) {
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
