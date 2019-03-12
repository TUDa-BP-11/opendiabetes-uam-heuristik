package de.opendiabetes.main;

import com.github.sh0nk.matplotlib4j.Plot;
import com.github.sh0nk.matplotlib4j.PythonExecutionException;
import de.opendiabetes.main.math.Predictions;
import de.opendiabetes.vault.container.VaultEntry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author anna
 */
public class CGMPlotter {

    private Plot plt;
    private Plot errorPlt;
    private Plot histPlt;
    private List<Double> errorValues = new ArrayList<>();
    private boolean plotPlot = false;
    private boolean plotError = false;
    private boolean plotHist = false;

    public CGMPlotter() {
        plt = Plot.create();
        errorPlt = Plot.create();

    }

    public CGMPlotter(boolean plotHist) {
        this();
        this.plotHist = plotHist;
    }

    public void plot(List<VaultEntry> entries, List<VaultEntry> basalTreatments,
                     List<VaultEntry> bolusTreatments, List<VaultEntry> meals,
                     double sensitivity, int insDuration, double carbratio, int absorptionTime) {

        plotPlot = true;
        List<Double> basalValues = new ArrayList<>();
        List<Double> basalTimes = new ArrayList<>();
        for (VaultEntry a : basalTreatments) {
            basalValues.add(a.getValue());// - a.getValue() * profile.getSensitivity() * a.getDuration()
            basalTimes.add(a.getTimestamp().getTime() / 1000.0);
        }

        List<Double> bolusValues = new ArrayList<>();
        List<Double> bolusTimes = new ArrayList<>();
        for (VaultEntry a : bolusTreatments) {
            bolusValues.add(a.getValue()); // -a.getValue() * profile.getSensitivity()
            bolusTimes.add(a.getTimestamp().getTime() / 1000.0);
        }

        List<Double> mealValues = new ArrayList<>();
        List<Double> mealTimes = new ArrayList<>();
        for (VaultEntry a : meals) {
            mealValues.add(a.getValue()); // a.getValue() * profile.getSensitivity() / profile.getCarbratio()
            mealTimes.add(a.getTimestamp().getTime() / 1000.0);
        }

        List<Double> bgTimes = new ArrayList<>();
        List<Double> bgValues = new ArrayList<>();
        List<Double> noMealValues = new ArrayList<>();
        List<Double> algoValues = new ArrayList<>();
        double startValue = getStartValue(entries, basalTreatments, bolusTreatments, meals, sensitivity, insDuration, carbratio, absorptionTime);
        double startTime = getStartTime(entries, meals, absorptionTime);
        for (VaultEntry ve : entries) {
            if(ve.getTimestamp().getTime() < startTime)
                continue;
            double algoPredict = Predictions.predict(ve.getTimestamp().getTime(),
                    meals, bolusTreatments, basalTreatments,
                    sensitivity, insDuration,
                    carbratio, absorptionTime);
            double noMealPredict = Predictions.predict(ve.getTimestamp().getTime(),
                    new ArrayList<>(), bolusTreatments, basalTreatments,
                    sensitivity, insDuration,
                    carbratio, absorptionTime);

            noMealValues.add(ve.getValue() - noMealPredict);
            algoValues.add(startValue + algoPredict);
            bgTimes.add((ve.getTimestamp().getTime()) / 1000.0);
            bgValues.add(ve.getValue());
        }

        plt.xlabel("time");
        plt.ylabel("BGCV");
        plt.plot().addDates(bgTimes).add(bgValues).color("blue"); //.label("Testlabel")
        plt.plot().addDates(bgTimes).add(noMealValues).color("orange"); //.label("Testlabel")
        plt.plot().addDates(mealTimes).add(mealValues).color("red").linestyle("").marker("x");
        plt.plot().addDates(bolusTimes).add(bolusValues).color("green").linestyle("").marker("o");
        plt.plot().addDates(basalTimes).add(basalValues).color("cyan").linestyle("").marker("o");
        plt.plot().addDates(bgTimes).add(algoValues).linestyle("--");//.color("cyan").linestyle("--");

    }

    public void plotError(List<VaultEntry> entries, List<VaultEntry> basalTreatments,
                          List<VaultEntry> bolusTreatments, List<VaultEntry> meals,
                          double sensitivity, int insDuration, double carbratio, int absorptionTime) {

        plotError = true;

        List<Double> errorTimes = new ArrayList<>();
        List<Double> errorValues = new ArrayList<>();
        double startValue = getStartValue(entries, basalTreatments, bolusTreatments, meals, sensitivity, insDuration, carbratio, absorptionTime);
        double startTime = getStartTime(entries, meals, absorptionTime);
        for (VaultEntry ve : entries) {
            if(ve.getTimestamp().getTime() < startTime)
                continue;
            double algoPredict = Predictions.predict(ve.getTimestamp().getTime(),
                    meals, bolusTreatments, basalTreatments,
                    sensitivity, insDuration,
                    carbratio, absorptionTime);
            errorValues.add((startValue + algoPredict - ve.getValue()) / ve.getValue() * 100);
            errorTimes.add((ve.getTimestamp().getTime() - startTime) / 1000.0);
        }

        errorPlt.xlabel("time");
        errorPlt.ylabel("relative error");
        errorPlt.plot().addDates(errorTimes).add(errorValues);//.color("magenta").linestyle("--");
        errorPlt.plot().addDates(errorTimes).add(Collections.nCopies(errorValues.size(), 10)).color("black");//.linestyle("--");
        errorPlt.plot().addDates(errorTimes).add(Collections.nCopies(errorValues.size(), -10)).color("black");
        if (plotHist) {
            this.errorValues.addAll(errorValues);
        }
    }

    public void showAll() {
        try {
            if (plotPlot) {
                plt.show();
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

    private double getStartValue(List<VaultEntry> entries, List<VaultEntry> basalTreatments,
                                 List<VaultEntry> bolusTreatments, List<VaultEntry> meals,
                                 double sensitivity, int insDuration, double carbratio, int absorptionTime) {
        double startTime = entries.get(0).getTimestamp().getTime();
        double firstValidMealTime = startTime;
        for (int i = 0; i < meals.size(); i++) {
            VaultEntry meal = meals.get(i);
            if (startTime + absorptionTime * 60000 < meal.getTimestamp().getTime()) {
                firstValidMealTime = meal.getTimestamp().getTime();
                break;
            }
        }

        double startValue = entries.get(0).getValue();
        for (int i = 0; i < entries.size() - 1; i++) {
            startValue = entries.get(i).getValue() - Predictions.predict(entries.get(i).getTimestamp().getTime(), meals, bolusTreatments, basalTreatments, sensitivity, insDuration, carbratio, absorptionTime);
            if (entries.get(i + 1).getTimestamp().getTime() > firstValidMealTime)
                break;
        }


        return startValue;
    }

    private double getStartTime(List<VaultEntry> entries, List<VaultEntry> meals, int absorptionTime){
        double startTime = entries.get(0).getTimestamp().getTime();
        double firstValidMealTime = startTime;
        for (int i = 0; i < meals.size(); i++) {
            VaultEntry meal = meals.get(i);
            if (startTime + absorptionTime * 60000 < meal.getTimestamp().getTime()) {
                firstValidMealTime = meal.getTimestamp().getTime();
                break;
            }
        }
        for (int i = 0; i < entries.size() - 1; i++) {
            startTime = entries.get(i).getTimestamp().getTime();
            if (entries.get(i + 1).getTimestamp().getTime() > firstValidMealTime)
                break;
        }
        return startTime;
    }
}
