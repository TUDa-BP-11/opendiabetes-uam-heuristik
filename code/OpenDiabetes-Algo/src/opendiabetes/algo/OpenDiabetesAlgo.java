/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opendiabetes.algo;

/**
 *
 * @author anna
 */
public class OpenDiabetesAlgo {

    
    /**
     * @param args the command line arguments
     * @param IOB Insulin on board
     * @param I_G Insulin need to correct for posoitve deviation
     * @param I_CHO amount  of  insulin  needed to  compensate  for  a  given  meal
     */
    public static void main(String[] args) {
        // TODO code application logic here
    }

    public double getUmax(double I_CHO) {
        double IOB = this.getIOB();
        double I_G = this.getIG();
        
        if(I_CHO + I_G> IOB)
            return I_CHO + I_G - IOB;
        else
            return I_CHO;
    }

    public double getIOB(Date now, Time time) {
        double IOB = 0;
        double dt = 0;

        recent = this.vault.get(insulin + time).(now, time);
        for (int i = 0; i < length(recent); i++) {
            dt = now - recent(i).time;
            IOB += recent(i).insulin * getIOBWeight(dt, time);
        }
        return IOB;
    }

    //sum IOB 
    //Idea without integration
    public double sumIOB(double x1, double x2, int iDuration, double timeFromEvent) {
        double integral;
        double dx;

        //initialize with first and last terms of simpson series
        dx = (x2 - x1) / nn;
        integral = getIOBWeight((timeFromEvent - x1), iDuration) + getIOBWeight(timeFromEvent - (x1 + nn * dx), iDuration);

        for (int ii = 0; ii < nn - 2; ii++) {
            integral = integral + 4 * getIOBWeight(timeFromEvent - (x1 + ii * dx), iDuration) + 2 * getIOBWeight(timeFromEvent - (x1 + (ii + 1) * dx), iDuration);
            ii = ii + 2;
        }

        integral = integral * dx / 3.0;
        return integral;

    }
//simpsons rule to integrate IOB 
    public double intIOB(double x1, double x2, int iDuration, double timeFromEvent) {
        double integral;
        double dx;
        int nn = 50; //nn needs to be even
        int ii = 1;

        //initialize with first and last terms of simpson series
        //x1 & x2 Grenzen des Intervalls das betrachtet wird
        dx = (x2 - x1) / nn;
        integral = getIOBWeight((timeFromEvent - x1), iDuration) + getIOBWeight(timeFromEvent - (x1 + nn * dx), iDuration);

        while (ii < nn - 2) {
            integral = integral + 4 * getIOBWeight(timeFromEvent - (x1 + ii * dx), iDuration) + 2 * getIOBWeight(timeFromEvent - (x1 + (ii + 1) * dx), iDuration);
            ii = ii + 2;
        }

        integral = integral * dx / 3.0;
        return integral;

    }

    /**
     * https://github.com/Perceptus/GlucoDyn/blob/master/js/glucodyn/algorithms.js
     *
     * @param timeFromEvent
     * @param iDuration //Insulin decomposition rate
     * @return
     */
    public static double getIOBWeight(double timeFromEvent, int iDuration) {
        double IOBWeight = 0;
        if (timeFromEvent <= 0) {
            IOBWeight = 100;
        } else if (timeFromEvent >= iDuration * 60) {
            IOBWeight = 0;
        } else {
            switch (iDuration) {
                case 3:
                    IOBWeight = -3.203e-7 * Math.pow(timeFromEvent, 4) + 1.354e-4 * Math.pow(timeFromEvent, 3) - 1.759e-2 * Math.pow(timeFromEvent, 2) + 9.255e-2 * timeFromEvent + 99.951;
                    break;
                case 4:
                    IOBWeight = -3.31e-8 * Math.pow(timeFromEvent, 4) + 2.53e-5 * Math.pow(timeFromEvent, 3) - 5.51e-3 * Math.pow(timeFromEvent, 2) - 9.086e-2 * timeFromEvent + 99.95;
                    break;
                case 5:
                    IOBWeight = -2.95e-8 * Math.pow(timeFromEvent, 4) + 2.32e-5 * Math.pow(timeFromEvent, 3) - 5.55e-3 * Math.pow(timeFromEvent, 2) + 4.49e-2 * timeFromEvent + 99.3;
                    break;
                case 6:
                    IOBWeight = -1.493e-8 * Math.pow(timeFromEvent, 4) + 1.413e-5 * Math.pow(timeFromEvent, 3) - 4.095e-3 * Math.pow(timeFromEvent, 2) + 6.365e-2 * timeFromEvent + 99.7;
                    break;
                case 8: // TODO Jens fragen
                default:
                    break;
            }
        }
        return (IOBWeight);
    }

}
