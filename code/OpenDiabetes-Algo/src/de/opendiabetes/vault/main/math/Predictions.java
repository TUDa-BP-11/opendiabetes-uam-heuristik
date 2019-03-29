package de.opendiabetes.vault.main.math;

import de.opendiabetes.vault.container.VaultEntry;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.util.List;

public class Predictions {

    /**
     * Predicts the blood glucose value at a certain time using known meal, bolus and basal treatments.
     *
     * @param time                 time in milliseconds since epoch start
     * @param mealTreatments       known meal treatments
     * @param bolusTreatments      known bolus treatments
     * @param basalTreatments      known basal treatments
     * @param insSensitivityFactor insulin to blood glucose factor
     * @param insDuration          effective insulin duration
     * @param carbRatio            carb to insulin ratio
     * @param absorptionTime       carb absorption time
     * @param peak                 duration in minutes until insulin action reaches it’s peak activity level
     * @return predicted blood glucose value
     */
    public static double predict(long time, List<VaultEntry> mealTreatments, List<VaultEntry> bolusTreatments, List<VaultEntry> basalTreatments, double insSensitivityFactor, double insDuration, double carbRatio, double absorptionTime, double peak) {
        double result = 0;
        for (VaultEntry meal : mealTreatments) {
            long deltaTime = Math.round((time - meal.getTimestamp().getTime()) / 60000.0);  //Time in minutes
            if (deltaTime <= 0) {
                break;
            }
            result += deltaBGC(deltaTime, insSensitivityFactor, carbRatio, meal.getValue(), absorptionTime);
        }
        for (VaultEntry bolus : bolusTreatments) {
            long deltaTime = Math.round((time - bolus.getTimestamp().getTime()) / 60000.0); //Time in minutes
            if (deltaTime <= 0) {
                break;
            }
            result += deltaBGI(deltaTime, bolus.getValue(), insSensitivityFactor, insDuration, peak);
        }
        for (VaultEntry basal : basalTreatments) {
            long deltaTime = Math.round((time - basal.getTimestamp().getTime()) / 60000.0);      //Time in minutes
            if (deltaTime <= 0) {
                break;
            }
            result += deltatempBGI(deltaTime, basal.getValue(), insSensitivityFactor, insDuration, peak, 0, basal.getValue2());
        }

        return result;
    }

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
     * @param insDuration   effective time of insulin in minutes
     * @param peak          duration in minutes until insulin action reaches it’s peak activity level.
     * @return percentage of insulin still on board
     */
    public static double fastActingIob(double timeFromEvent, double insDuration, double peak) {
        double IOBWeight;
        if (timeFromEvent <= 0) {
            IOBWeight = 1;
        } else if (timeFromEvent >= insDuration) {
            IOBWeight = 0;
        } else {
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
     * @param t1            left border of integral - 0
     * @param t2            right border of integral - duration of insulin event
     * @param insDuration   effective time of insulin in minutes
     * @param timeFromEvent time in minutes since insulin event
     * @param peak          duration in minutes until insulin action reaches it’s peak activity level.
     * @return
     */
    public static double integrateIob(double t1, double t2, double insDuration, double timeFromEvent, double peak) {
        double integral;
        double dx;
        int N = 25;
        int nn = 2 * N; //nn needs to be even

        //initialize with first and last terms of simpson series
        //t1 & t2 Grenzen des Intervalls das betrachtet wird
        dx = (t2 - t1) / nn;

        integral = fastActingIob((timeFromEvent - t1), insDuration, peak)
                + fastActingIob(timeFromEvent - (t1 + nn * dx), insDuration, peak);
        int ii = 1;
        while (ii < nn - 2) {
            integral = integral
                    + 4 * fastActingIob(timeFromEvent
                    - (t1 + ii * dx), insDuration, peak)
                    + 2 * fastActingIob(timeFromEvent
                    - (t1 + (ii + 1) * dx), insDuration, peak);
            ii = ii + 2;
        }

        integral = integral * dx / 3.0;
        return integral;
    }

    /**
     * Calculates how much your blood glucose level will change when given a basal insulin treatment.
     * All times are relative to each other.
     *
     * @param timeFromEvent        time in minutes since the event has started
     * @param tempInsAmount        amount of basal insulin per minute given
     * @param insSensitivityFactor insulin to blood glucose factor
     * @param insDuration          effective insulin duration
     * @param peak                 duration in minutes until insulin action reaches it’s peak activity level
     * @param t1                   start of insulin event in minutes
     * @param t2                   end of insulin event in minutes
     * @return relative change of blood glucose level
     */
    public static double deltatempBGI(double timeFromEvent, double tempInsAmount, double insSensitivityFactor, double insDuration, double peak, double t1, double t2) {
        return -tempInsAmount * insSensitivityFactor * ((t2 - t1) - integrateIob(t1, t2, insDuration, timeFromEvent, peak));
    }

    /**
     * Calculates how much your blood glucose level will change when given a meal treatment.
     * All times are relative to each other.
     *
     * @param timeFromEvent        time in minutes since the event has started
     * @param insSensitivityFactor insulin to blood glucose factor
     * @param carbRatio            carb to insulin ratio
     * @param carbsAmount          amount of carbs given
     * @param absorptionTime       carb absorption time
     * @return relative change of blood glucose level
     */
    public static double deltaBGC(double timeFromEvent, double insSensitivityFactor, double carbRatio, double carbsAmount, double absorptionTime) {
        return insSensitivityFactor / carbRatio * carbsAmount * Predictions.carbsOnBoard(timeFromEvent, absorptionTime);
    }

    /**
     * Calculates how much your blood glucose level will change when given a bolus insulin treatment.
     * All times are relative to each other.
     *
     * @param timeFromEvent        time in minutes since the event has started
     * @param insBolus             amount of bolus insulin given
     * @param insSensitivityFactor insulin to blood glucose factor
     * @param insDuration          effective insulin duration
     * @param peak                 duration in minutes until insulin action reaches it’s peak activity level
     * @return relative change of blood glucose level
     */
    public static double deltaBGI(double timeFromEvent, double insBolus, double insSensitivityFactor, double insDuration, double peak) {
        return -insBolus * insSensitivityFactor * (1 - fastActingIob(timeFromEvent, insDuration, peak));
    }

    public static RealVector cumulativeMealPredict(RealVector times, RealVector mealTimes, RealVector mealValues, double insSensitivityFactor, double carbRatio, long absorptionTime) {
        int N = mealTimes.getDimension();
        int Nt = times.getDimension();
        RealVector Y = new ArrayRealVector(Nt);
        for (int i = 0; i < N; i++) {
            double t0 = mealTimes.getEntry(i);
            double carbsAmount = mealValues.getEntry(i);
            for (int j = 0; j < Nt; j++) {
                Y.addToEntry(j, deltaBGC(times.getEntry(j) - t0, insSensitivityFactor, carbRatio, carbsAmount, absorptionTime));
            }
        }
        return Y;
    }

    public static RealMatrix Jacobian(RealVector times, RealVector mealTimes, RealVector mealValues, double insSensitivityFactor, double carbratio, long absorptionTime) {
        int N = mealTimes.getDimension();
        RealMatrix J = new Array2DRowRealMatrix(times.getDimension(), 2 * N);
        for (int i = 0; i < N; i++) {
            double t0 = mealTimes.getEntry(i);
            double carbsAmount = mealValues.getEntry(i);
            J.setColumn(i, carbsOnBoard_dt0(times, t0, carbsAmount, insSensitivityFactor, carbratio, absorptionTime));
            J.setColumn(i + N, carbsOnBoard_dx(times, t0, insSensitivityFactor, carbratio, absorptionTime));
        }
        return J;
    }

    private static double[] carbsOnBoard_dt0(RealVector times, double t0, double carbsAmount, double insSensitivityFactor, double carbRatio, long absorbtionTime) {
        double[] cob_dt0 = new double[times.getDimension()];
        double c = insSensitivityFactor / carbRatio * carbsAmount * 4 / absorbtionTime;
        for (int i = 0; i < times.getDimension(); i++) {

            double dt = times.getEntry(i) - t0;
            if (dt < 0 || dt > absorbtionTime) {
                cob_dt0[i] = 0;
            } else if (dt < absorbtionTime / 2.0) {
                cob_dt0[i] = -c * dt / absorbtionTime;
            } else if (dt >= absorbtionTime / 2.0) {
                cob_dt0[i] = c * (dt / absorbtionTime - 1);
            }
        }
        return cob_dt0;
    }

    private static double[] carbsOnBoard_dx(RealVector times, double t0, double insSensitivityFactor, double carbRatio, long absorbtionTime) {
        double[] cob_dx = new double[times.getDimension()];
        for (int i = 0; i < times.getDimension(); i++) {
            cob_dx[i] = insSensitivityFactor / carbRatio * Predictions.carbsOnBoard(times.getEntry(i) - t0, absorbtionTime);
        }
        return cob_dx;
    }
}
