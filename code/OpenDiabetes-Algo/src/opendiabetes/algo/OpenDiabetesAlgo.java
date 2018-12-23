/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opendiabetes.algo;



import de.opendiabetes.vault.engine.container.VaultEntry;

import java.util.ArrayList;
import java.util.List;


public class OpenDiabetesAlgo {


    private final double absorptionTime = 120;
    private final int insDuration = 3;
    private double carbRatio;
    private double insSensivityFactor;
    private List<VaultEntry> glucose;
    private List<VaultEntry> bolusTreatments;
    private List<VaultEntry> mealTreatments;
    private VaultEntry current;
    double startValue;

    /*
    startWert -> Zeitdiff zwischen current und next -> predict next GlucoseValue (tmp)
    -> diff zwischen next und tmp -> Ins oder Mealtreatment bruteforcen -> current = next
    -> von vorne bis liste leer
     */

    OpenDiabetesAlgo() {
        carbRatio = 5;
        insSensivityFactor = 10;
        glucose = new ArrayList<VaultEntry>();
        bolusTreatments = new ArrayList<VaultEntry>();
        mealTreatments = new ArrayList<VaultEntry>();
    }

    public void setGlucose(List<VaultEntry> glucose) {
        current = glucose.remove(0);
        startValue = current.getValue();
        bolusTreatments = new ArrayList<VaultEntry>();
        mealTreatments  = new ArrayList<VaultEntry>();
        this.glucose = glucose;
    }

    public List<VaultEntry> calc() {


        return mealTreatments;

    }

    private double predict(long time){
        double result = startValue;
        for (VaultEntry meal: mealTreatments){
            long deltaTime = Math.round((time - meal.getTimestamp().getTime())/60000.0);//Time in minutes
            if(deltaTime<=0){
                break;
            }
            result += deltaBGC(deltaTime,insSensivityFactor,carbRatio,meal.getValue(),absorptionTime);
        }
        for (VaultEntry bolus: bolusTreatments){
            long deltaTime = Math.round((time - bolus.getTimestamp().getTime())/60000.0);//Time in minutes
            if(deltaTime<=0){
                break;
            }
            result += deltaBGI(deltaTime,bolus.getValue(),insSensivityFactor,insDuration);
        }


        return result;
    }

    /**
     * https://github.com/Perceptus/GlucoDyn/blob/master/js/glucodyn/algorithms.js
     *
     * @param timeFromEvent
     * @param insDuration   //Insulin decomposition rate
     * @return
     */
    //function iob(g,idur)
    public int getIOBWeight(double timeFromEvent, int insDuration) {
        int IOBWeight = 0;
        if (timeFromEvent <= 0) {
            IOBWeight = 100;
        } else if (timeFromEvent >= insDuration * 60) {
            IOBWeight = 0;
        } else {
            IOBWeight =  (int) (-3.203e-7 * Math.pow(timeFromEvent, 4) + 1.354e-4 * Math.pow(timeFromEvent, 3) - 1.759e-2 * Math.pow(timeFromEvent, 2) + 9.255e-2 * timeFromEvent + 99.951);
        }
        return IOBWeight;
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

}
