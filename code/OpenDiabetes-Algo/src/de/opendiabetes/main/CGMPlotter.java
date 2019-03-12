package de.opendiabetes.main;

import com.github.sh0nk.matplotlib4j.Plot;
import com.github.sh0nk.matplotlib4j.PythonExecutionException;
import de.opendiabetes.main.math.Predictions;
import de.opendiabetes.main.util.Snippet;
import de.opendiabetes.vault.container.VaultEntry;
import java.io.IOException;
import java.util.ArrayList;
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
    Plot diffPlt;
    Plot histPlt;
    List<Double> errorValues = new ArrayList<>();
    boolean plotPlot = false;
    boolean plotDiff = false;
    boolean plotError = false;
    boolean plotHist = false;

    public CGMPlotter() {
        plt = Plot.create();
        errorPlt = Plot.create();
        diffPlt = Plot.create();

    }

    public CGMPlotter(boolean plotHist) {
        this();
        this.plotHist = plotHist;
    }

    public void plot(Snippet s, List<VaultEntry> meals,
            double sensitivity, int insDuration,
            double carbratio, int absorptionTime) {
        plotPlot = true;
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
        List<Double> noMealValues;

        basalValues = new ArrayList();
        basalTimes = new ArrayList();
        for (VaultEntry a : s.getBasals()) {
            basalValues.add(a.getValue());// - a.getValue() * profile.getSensitivity() * a.getDuration()
            basalTimes.add(a.getTimestamp().getTime() / 1000.0);
        }

        bolusValues = new ArrayList();
        bolusTimes = new ArrayList();
        for (VaultEntry a : s.getTreatments()) {
            bolusValues.add(a.getValue()); // -a.getValue() * profile.getSensitivity()
            bolusTimes.add(a.getTimestamp().getTime() / 1000.0);
        }

        mealValues = new ArrayList();
        mealTimes = new ArrayList();
        for (VaultEntry a : meals) {
            mealValues.add(a.getValue()); // a.getValue() * profile.getSensitivity() / profile.getCarbratio()
            mealTimes.add(a.getTimestamp().getTime() / 1000.0);
        }
        bgTimes = new ArrayList();
        bgValues = new ArrayList();
        noMealValues = new ArrayList();
        algo2Values = new ArrayList();
        startValue = s.getEntries().get(0).getValue();
        for (VaultEntry ve : s.getEntries()) {
            double algoPredict = Predictions.predict(ve.getTimestamp().getTime(),
                    meals, s.getTreatments(), s.getBasals(),
                    sensitivity, insDuration,
                    carbratio, absorptionTime);
            double noMealPredict = Predictions.predict(ve.getTimestamp().getTime(),
                    new ArrayList(), s.getTreatments(), s.getBasals(),
                    sensitivity, insDuration,
                    carbratio, absorptionTime);

            noMealValues.add(ve.getValue() - noMealPredict);
            algo2Values.add(startValue + algoPredict);
            bgTimes.add((ve.getTimestamp().getTime()) / 1000.0);
            bgValues.add(ve.getValue());
        }

        plt.xlabel("time");
        plt.ylabel("BGCV");
        plt.plot().addDates(bgTimes).add(bgValues).color("blue").marker("x"); //.label("Testlabel")
        plt.plot().addDates(bgTimes).add(noMealValues).color("orange").marker("x"); //.label("Testlabel")
        plt.plot().addDates(mealTimes).add(mealValues).color("red").linestyle("").marker("x");
        plt.plot().addDates(bolusTimes).add(bolusValues).color("green").linestyle("").marker("o");
        plt.plot().addDates(basalTimes).add(basalValues).color("cyan").linestyle("").marker("o");
        plt.plot().addDates(bgTimes).add(algo2Values).linestyle("--").marker("x");//.color("cyan").linestyle("--");

    }

    public void plotError(Snippet s, List<VaultEntry> meals,
            double sensitivity, int insDuration,
            double carbratio, int absorptionTime) {
        plotError = true;
        double startTime;
        double startValue;

        List<Double> errorTimes;
        List<Double> errorValues;

        errorTimes = new ArrayList();
        errorValues = new ArrayList();
        startValue = s.getEntries().get(0).getValue();
        startTime = s.getEntries().get(0).getTimestamp().getTime();
        for (VaultEntry ve : s.getEntries()) {
            double algoPredict = Predictions.predict(ve.getTimestamp().getTime(),
                    meals, s.getTreatments(), s.getBasals(),
                    sensitivity, insDuration,
                    carbratio, absorptionTime);
            errorValues.add((startValue + algoPredict - ve.getValue()) / ve.getValue() * 100);
            errorTimes.add((ve.getTimestamp().getTime() - startTime) / 1000.0);
        }
        ;
        errorPlt.xlabel("time");
        errorPlt.ylabel("relative error");
        errorPlt.plot().addDates(errorTimes).add(errorValues);//.color("magenta").linestyle("--");
        errorPlt.plot().addDates(errorTimes).add(Collections.nCopies(errorValues.size(), 10)).color("black");//.linestyle("--");
        errorPlt.plot().addDates(errorTimes).add(Collections.nCopies(errorValues.size(), -10)).color("black");
        if (plotHist) {
            this.errorValues.addAll(errorValues);
        }
    }

    public void plotDiff(Snippet s, List<VaultEntry> meals,
            double sensitivity, int insDuration,
            double carbratio, int absorptionTime) {
        plotDiff = true;
        List<Double> basalValues;
        List<Double> basalTimes;
        List<Double> bolusValues;
        List<Double> bolusTimes;
        List<Double> mealValues;
        List<Double> mealTimes;

        double prevValue;
        double prevTime;

        List<Double> bgTimes;
        List<Double> bgValues;
        List<Double> algo2Values;
        List<Double> noMealValues;

        basalValues = new ArrayList();
        basalTimes = new ArrayList();
        for (VaultEntry a : s.getBasals()) {
            basalValues.add(a.getValue());// - a.getValue() * profile.getSensitivity() * a.getDuration()
            basalTimes.add(a.getTimestamp().getTime() / 1000.0);
        }

        bolusValues = new ArrayList();
        bolusTimes = new ArrayList();
        for (VaultEntry a : s.getTreatments()) {
            bolusValues.add(a.getValue()); // -a.getValue() * profile.getSensitivity()
            bolusTimes.add(a.getTimestamp().getTime() / 1000.0);
        }

        mealValues = new ArrayList();
        mealTimes = new ArrayList();
        for (VaultEntry a : meals) {
            mealValues.add(a.getValue()); // a.getValue() * profile.getSensitivity() / profile.getCarbratio()
            mealTimes.add(a.getTimestamp().getTime() / 1000.0);
        }
        bgTimes = new ArrayList();
        bgValues = new ArrayList();
        noMealValues = new ArrayList();
        algo2Values = new ArrayList();
        VaultEntry ve, prev;
        prev = s.getEntries().get(0);
        prevValue = prev.getValue();
        prevTime = prev.getTimestamp().getTime();

        double algoPredictPrev = Predictions.predict(prev.getTimestamp().getTime(),
                meals, s.getTreatments(), s.getBasals(),
                sensitivity, insDuration,
                carbratio, absorptionTime);
        double noMealPredictPrev = Predictions.predict(prev.getTimestamp().getTime(),
                new ArrayList(), s.getTreatments(), s.getBasals(),
                sensitivity, insDuration,
                carbratio, absorptionTime);
        for (int i = 1; i < s.getEntries().size(); i++) {
            ve = s.getEntries().get(i);
            double algoPredict = Predictions.predict(ve.getTimestamp().getTime(),
                    meals, s.getTreatments(), s.getBasals(),
                    sensitivity, insDuration,
                    carbratio, absorptionTime);
            double noMealPredict = Predictions.predict(ve.getTimestamp().getTime(),
                    new ArrayList(), s.getTreatments(), s.getBasals(),
                    sensitivity, insDuration,
                    carbratio, absorptionTime);
            System.out.println("Date: " + ve.getTimestamp().toString() + " Value: " + ve.getValue() + " Diff: " + (ve.getValue() - prevValue));
            bgTimes.add((ve.getTimestamp().getTime()) / 1000.0);
            noMealValues.add(noMealPredict - noMealPredictPrev);
            algo2Values.add(algoPredict - algoPredictPrev);
            bgValues.add((ve.getValue() - prevValue) / ((ve.getTimestamp().getTime() - prevTime) / 1000));
            prevValue = ve.getValue();
            prevTime = ve.getTimestamp().getTime();
            noMealPredictPrev = noMealPredict;
            algoPredictPrev = algoPredict;
        }

        diffPlt.xlabel("time");
        diffPlt.ylabel("dBG/dt");
        diffPlt.plot().addDates(bgTimes).add(bgValues).color("blue").marker("x"); //.label("Testlabel")
        diffPlt.plot().addDates(bgTimes).add(noMealValues).color("orange").marker("x"); //.label("Testlabel")
        diffPlt.plot().addDates(mealTimes).add(mealValues).color("red").linestyle("").marker("x");
        diffPlt.plot().addDates(bolusTimes).add(bolusValues).color("green").linestyle("").marker("o");
        diffPlt.plot().addDates(basalTimes).add(basalValues).color("cyan").linestyle("").marker("o");
        diffPlt.plot().addDates(bgTimes).add(algo2Values).linestyle("--").marker("x");//.color("cyan").linestyle("--");

    }

    public void plotHist(List<Double> data) {
    }

    public void showAll() {
        try {
            if (plotPlot) {
                plt.show();
            }
            if (plotDiff) {
                diffPlt.show();
            }
            if (plotError) {
                errorPlt.show();
            }
            if (plotHist) {

                double MeanError = 0;
                double MeanSquareError = 0;
                int N = errorValues.size();
                for (double d : errorValues) {
                    MeanError += d;
                    MeanSquareError += d * d;
                }
                MeanError /= N;
                MeanSquareError /= N;
                double rms = Math.sqrt(MeanSquareError);
                System.out.printf("Bias: %.3g%%\n", MeanError);
                System.out.printf("RootMeanSquareError: %.3g%%\n", rms);
                System.out.printf("MaxError: %.3g%%\n", Collections.max(errorValues));
//                System.out.println("Varianz: " + (MeanSquareError - MeanError * MeanError));
//                System.out.println("Standardabweichung: " + Math.sqrt(MeanSquareError - MeanError * MeanError));

                histPlt = Plot.create();
                histPlt.xlabel("variance");
                histPlt.ylabel("frequency");
                histPlt.hist().add(errorValues).bins(100);
                histPlt.show();
            }
        } catch (IOException | PythonExecutionException ex) {
            Logger.getLogger(de.opendiabetes.main.CGMPlotter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
