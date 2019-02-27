/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.opendiabetes.main;

import com.github.sh0nk.matplotlib4j.Plot;
import com.github.sh0nk.matplotlib4j.PythonExecutionException;
import de.opendiabetes.main.math.Predictions;
import de.opendiabetes.main.util.Snippet;
import de.opendiabetes.vault.container.VaultEntry;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author anna
 */
public class CGMPlotter {

    Plot plt;
    Plot errorPlt;

    public void plot(Snippet s, List<VaultEntry> meals,
            double sensitivity, int insDuration,
            double carbratio, int absorptionTime) {
        plt = Plot.create();

        List<Double> basalValues;
        List<Double> basalTimes;
        List<Double> bolusValues;
        List<Double> bolusTimes;
        List<Double> mealValues;
        List<Double> mealTimes;

        double startValue;

        List<Double> bgTimes;
        List<Double> bgValues;
        List<Double> algo2Values;

        basalValues = new ArrayList();
        basalTimes = new ArrayList();
        s.getBasals().stream().map((a) -> {
            basalValues.add(a.getValue());// - a.getValue() * profile.getSensitivity() * a.getDuration()
            basalTimes.add(a.getTimestamp().getTime() / 1000.0);
            return a;
        });
        bolusValues = new ArrayList();
        bolusTimes = new ArrayList();
        s.getTreatments().stream().map((a) -> {
            bolusValues.add(a.getValue()); // -a.getValue() * profile.getSensitivity()
            bolusTimes.add(a.getTimestamp().getTime() / 1000.0);
            return a;
        });

        mealValues = new ArrayList();
        mealTimes = new ArrayList();
        meals.stream().map((a) -> {
            mealValues.add(a.getValue()); // a.getValue() * profile.getSensitivity() / profile.getCarbratio()
            mealTimes.add(a.getTimestamp().getTime() / 1000.0);
            return a;
        });

        bgTimes = new ArrayList();
        bgValues = new ArrayList();
        algo2Values = new ArrayList();
        startValue = s.getEntries().get(0).getValue();
        s.getEntries().stream().map((ve) -> {
            algo2Values.add(startValue + Predictions.predict(ve.getTimestamp().getTime(),
                    meals, s.getTreatments(), s.getBasals(),
                    sensitivity, insDuration,
                    carbratio, absorptionTime));
            bgTimes.add((ve.getTimestamp().getTime()) / 1000.0);
            bgValues.add(ve.getValue());
            return ve;
        });

        plt.plot().addDates(bgTimes).add(bgValues).color("blue"); //.label("Testlabel")
        plt.plot().addDates(mealTimes).add(mealValues).color("red").linestyle("").marker("x");
        plt.plot().addDates(bolusTimes).add(bolusValues).color("green").linestyle("").marker("o");
        plt.plot().addDates(basalTimes).add(basalValues).color("cyan").linestyle("").marker("o");
        plt.plot().addDates(bgTimes).add(algo2Values).linestyle("--");//.color("cyan").linestyle("--");
        try {
            plt.show();
        } catch (IOException | PythonExecutionException ex) {
            Logger.getLogger(de.opendiabetes.main.CGMPlotter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void plotError(Snippet s, List<VaultEntry> meals,
            double sensitivity, int insDuration,
            double carbratio, int absorptionTime) {
        errorPlt = Plot.create();

        double startTime;
        double startValue;

        List<Double> errorTimes;
        List<Double> errorValues;

        errorTimes = new ArrayList();
        errorValues = new ArrayList();
        startValue = s.getEntries().get(0).getValue();
        startTime = s.getEntries().get(0).getTimestamp().getTime();
        s.getEntries().stream().map((ve) -> {
            double algoPredict = Predictions.predict(ve.getTimestamp().getTime(),
                    meals, s.getTreatments(), s.getBasals(),
                    sensitivity, insDuration,
                    carbratio, absorptionTime);
            errorValues.add((startValue + algoPredict - ve.getValue()) / ve.getValue() * 100); 
            errorTimes.add((ve.getTimestamp().getTime() - startTime) / 1000.0);
            return ve;
        });

        errorPlt.plot().addDates(errorTimes).add(errorValues);//.color("magenta").linestyle("--");
        errorPlt.plot().addDates(errorTimes).add(Collections.nCopies(errorValues.size(), 10)).color("black");//.linestyle("--");
        errorPlt.plot().addDates(errorTimes).add(Collections.nCopies(errorValues.size(), -10)).color("black");

        try {
            errorPlt.show();
        } catch (IOException | PythonExecutionException ex) {
            Logger.getLogger(de.opendiabetes.main.CGMPlotter.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}
