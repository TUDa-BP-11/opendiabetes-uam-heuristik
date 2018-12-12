/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opendiabetes.algo;



import de.opendiabetes.vault.engine.container.VaultEntry;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author anna
 */
public class OpenDiabetesAlgo {


    private double absorptionTime;
    private int insDuration;
    private double carbRatio;
    private double insSensivityFactor;
    private List<VaultEntry> glucose;
    private List<VaultEntry> bolusTreatments;
    private List<VaultEntry> mealTreatments;
    private List<VaultEntry> output;
    private VaultEntry current;
    




    OpenDiabetesAlgo() {
        absorptionTime = 180;
        insDuration = 6 ;
        carbRatio = 5;
        insSensivityFactor = 10;
        glucose = new ArrayList<VaultEntry>();
        bolusTreatments = new ArrayList<VaultEntry>();
        mealTreatments = new ArrayList<VaultEntry>();
        output = new ArrayList<VaultEntry>();
    }

    public void setGlucose(List<VaultEntry> glucose) {
        current = glucose.get(0);
        this.glucose = glucose;
    }

    public void addGlucose(List<VaultEntry> glucose) {
        this.glucose.addAll(glucose);
    }

    public List<VaultEntry> getOutput() {
        List<VaultEntry> tmp = output;
        output = new ArrayList<VaultEntry>();
        return tmp;
    }

    /**
     * https://github.com/Perceptus/GlucoDyn/blob/master/js/glucodyn/algorithms.js
     *
     * @param timeFromEvent
     * @param insDuration   //Insulin decomposition rate
     * @return
     */
    //function iob(g,idur)
    public double getIOBWeight(double timeFromEvent, int insDuration) {
        double IOBWeight = 0;
        if (timeFromEvent <= 0) {
            IOBWeight = 100;
        } else if (timeFromEvent >= insDuration * 60) {
            IOBWeight = 0;
        } else {
            switch (insDuration) {
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


    //simpsons rule to integrate IOB
    //function intIOB(x1,x2,idur,g)
    public double integrateIOB(double t1, double t2, int insDuration, double timeFromEvent) {
        double integral;
        double dx;
        int nn = 50; //nn needs to be even
        int ii = 1;

        //initialize with first and last terms of simpson series
        //t1 & t2 Grenzen des Intervalls das betrachtet wird
        dx = (t2 - t1) / nn;
        integral = getIOBWeight((timeFromEvent - t1), insDuration) + getIOBWeight(timeFromEvent - (t1 + nn * dx), insDuration);

        while (ii < nn - 2) {
            integral = integral + 4 * getIOBWeight(timeFromEvent - (t1 + ii * dx), insDuration) + 2 * getIOBWeight(timeFromEvent - (t1 + (ii + 1) * dx), insDuration);
            ii = ii + 2;
        }

        integral = integral * dx / 3.0;
        return integral;

    }


    //scheiner gi curves fig 7-8 from Think Like a Pancreas, fit with a triangle shaped absorbtion rate curve
    //see basic math pdf on repo for details
    //g is time in minutes,gt is carb type
    //function cob(g,ct)
    public double cob(double timeFromEvent, double absorptionTime) {
        double total;

        if (timeFromEvent <= 0) {
            total = 0.0;
        } else if (timeFromEvent >= absorptionTime) {
            total = 1.0;
        } else if ((timeFromEvent > 0) && (timeFromEvent <= absorptionTime / 2.0)) {
            total = 2.0 / Math.pow(absorptionTime, 2) * Math.pow(timeFromEvent, 2);
        } else
            total = -1.0 + 4.0 / absorptionTime * (timeFromEvent - Math.pow(timeFromEvent, 2) / (2.0 * absorptionTime));
        return total;
    }

    //function deltatempBGI(g,dbdt,sensf,idur,t1,t2)
    public double deltatempBGI(double timeFromEvent, double tempInsAmount, double insSensivityFactor, int insDuration, double t1, double t2) {
        return -tempInsAmount * insSensivityFactor * ((t2 - t1) - 1 / 100 * integrateIOB(t1, t2, insDuration, timeFromEvent));
    }

    //function deltaBGC(g,sensf,cratio,camount,ct)
    public double deltaBGC(double timeFromEvent, double insSensivityFactor, double carbRatio, double carbsAmount, double absorptionTime) {
        return insSensivityFactor / carbRatio * carbsAmount * cob(timeFromEvent, absorptionTime);
    }

    //function deltaBGI(g,bolus,sensf,idur)
    public double deltaBGI(double timeFromEvent, double insBolus, double insSensivityFactor, int insDuration) {
        return -insBolus * insSensivityFactor * (1 - getIOBWeight(timeFromEvent, insDuration) / 100.0);
    }

    //deltaBG(g,sensf,cratio,camount,ct,bolus,idur)
    public double deltaBG(double timeFromEvent, double insSensivityFactor, double carbRatio, double carbsAmount, double absorptionTime, double insBolus, int insDuration) {
        return deltaBGI(timeFromEvent, insBolus, insSensivityFactor, insDuration) +
                deltaBGC(timeFromEvent, insSensivityFactor, carbRatio, carbsAmount, absorptionTime);
    }






    /*

    /**
     * @param args the command line arguments
     * @param IOB Insulin on board
     * @param I_G Insulin need to correct for posoitve deviation
     * @param I_CHO amount  of  insulin  needed to  compensate  for  a  given  meal
     */
/*    public static void main(String[] args) {
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

    public double getIOB(Date now, long time) {
        double IOB = 0;
        double dt = 0;

        recent = this.vault.get(insulin + time).(now, time);
        for (int i = 0; i < length(recent); i++) {
            dt = now - recent(i).time;
            IOB += recent(i).insulin * getIOBWeight(dt, time);
        }
    }
        return IOB;
    }

    //sum IOB 
    //Idea without integration
    public double sumIOB(double x1, double x2, int iDuration, double timeFromEvent) {
        double integral;
        double dx;
        int nn = 50;

        //initialize with first and last terms of simpson series
        dx = (x2 - x1) / nn;
        integral = getIOBWeight((timeFromEvent - x1), iDuration) + getIOBWeight(timeFromEvent - (x1 + nn * dx), iDuration);

        for (int ii = 0; ii < nn - 2; ii++) {
            integral = integral + 4 * getIOBWeight(timeFromEvent - (x1 + ii * dx), iDuration) + 2 * getIOBWeight(timeFromEvent - (x1 + (ii + 1) * dx), iDuration);
            ii = ii + 2;
        }

        integral = integral * dx / 3.0;
        return integral;

    }*/


}
