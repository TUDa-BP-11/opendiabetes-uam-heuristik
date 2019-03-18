package de.opendiabetes.vault.main;

import com.github.sh0nk.matplotlib4j.Plot;
import com.github.sh0nk.matplotlib4j.PythonExecutionException;
import de.opendiabetes.vault.main.math.Predictions;
import de.opendiabetes.vault.main.util.Snippet;
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
    private Plot diffPlt;
    private Plot histPlt;
    private boolean plotPlot = false;
    private boolean plotDiff = false;
    private boolean plotHist = false;
    private List<Double> errorValues = new ArrayList<>();

    public CGMPlotter() {
        plt = Plot.create();
        plt = Plot.create();
        diffPlt = Plot.create();

    }

    public CGMPlotter(boolean plotHist) {
        this();
        this.plotHist = plotHist;
    }

    public void plot(List<VaultEntry> entries, List<VaultEntry> basalTreatments,
            List<VaultEntry> bolusTreatments, List<VaultEntry> meals,
            double sensitivity, int insDuration, double carbratio, int absorptionTime) {

        /*
        plotPlot = true;
        List<Double> basalValues = new ArrayList<>();
        List<Double> basalTimes = new ArrayList<>();
        for (VaultEntry a : basalTreatments) {
            basalValues.add(a.getValue());// - a.getValue() * profile.getSensitivity() * a.getDuration()
            basalTimes.add(a.getTimestamp().getTime() / 1000.0);
        }

        List<Double> bolusValues = new ArrayList<>();
        List<Double> bolusTimes = new ArrayList<>();
        generatePointsToDraw(bolusTreatments, bolusValues, bolusTimes);

        List<Double> mealValues = new ArrayList<>();
        List<Double> mealTimes = new ArrayList<>();
        generatePointsToDraw(meals, mealValues, mealTimes);

        List<Double> bgTimes = new ArrayList<>();
        List<Double> algoTimes = new ArrayList<>();
        List<Double> bgValues = new ArrayList<>();
        List<Double> noMealValues = new ArrayList<>();
        List<Double> algoValues = new ArrayList<>();
        List<Double> firstToLast = new ArrayList<>();
        List<Double> zeros = new ArrayList<>();

        List<Double> errorTimes = new ArrayList<>();
        List<Double> errorValues = new ArrayList<>();
        firstToLast.add(entries.get(0).getTimestamp().getTime() / 1000.0);
        firstToLast.add(entries.get(entries.size() - 1).getTimestamp().getTime() / 1000.0);
        zeros.add(0.0);
        zeros.add(0.0);

        double startValue = getStartValue(entries, basalTreatments, bolusTreatments, meals, sensitivity, insDuration, carbratio, absorptionTime);
        double startTime = getStartTime(entries, insDuration, absorptionTime);
        double offset = 0;
        for (VaultEntry ve : entries) {
            bgTimes.add((ve.getTimestamp().getTime()) / 1000.0);
            bgValues.add(ve.getValue());


            double noMealPredict = Predictions.predict(ve.getTimestamp().getTime(),
                    new ArrayList<>(), bolusTreatments, basalTreatments,
                    sensitivity, insDuration,
                    carbratio, absorptionTime);




            if (ve.getTimestamp().getTime() < startTime) {
                offset = - noMealPredict;
                continue;
            }
            double algoPredict = Predictions.predict(ve.getTimestamp().getTime(),
                    meals, bolusTreatments, basalTreatments,
                    sensitivity, insDuration,
                    carbratio, absorptionTime);
            algoValues.add(startValue + algoPredict);
            algoTimes.add((ve.getTimestamp().getTime()) / 1000.0);

            //noMealValues.add(ve.getValue() + noMealPredict + offset);
            errorValues.add((startValue + algoPredict - ve.getValue()) / ve.getValue() * 100);
            errorTimes.add(ve.getTimestamp().getTime() / 1000.0);
        }

        plt.subplot(3, 1, 1);
        plt.xlabel("time");
        plt.ylabel("mg/dl");
        plt.plot().addDates(bgTimes).add(bgValues).color("blue").label("cgm"); //.label("Testlabel")
        //plt.plot().addDates(algoTimes).add(noMealValues).color("orange").label("no meal predictions"); //.label("Testlabel")
        plt.plot().addDates(algoTimes).add(algoValues).linestyle("--").label("predicted values");//.color("cyan").linestyle("--");
        plt.plot().addDates(firstToLast).add(zeros).linestyle("");
        plt.legend().loc(2);

        plt.subplot(3, 1, 2);
        plt.plot().addDates(firstToLast).add(zeros).linestyle("");
        plt.plot().addDates(mealTimes).add(mealValues).color("red").label("meals").marker("_").linestyle("").markersize("3");
        plt.plot().addDates(bolusTimes).add(bolusValues).color("green").linestyle("").label("bolus").marker("_").markersize("3");
        //plt.plot().addDates(basalTimes).add(basalValues).color("cyan").linestyle("").label("basal").marker("o");
        plt.legend().loc(2);

        // plot error
        plt.subplot(3, 1, 3);
        plt.xlabel("time");
        plt.ylabel("%");
        plt.plot().addDates(firstToLast).add(zeros).linestyle("");
        plt.plot().addDates(errorTimes).add(errorValues).label("relative error");//.color("magenta").linestyle("--");
        plt.plot().addDates(firstToLast).add(Collections.nCopies(firstToLast.size(), 10)).color("black").label("fault tolerance");//.linestyle("--");
        plt.plot().addDates(firstToLast).add(Collections.nCopies(firstToLast.size(), -10)).color("black");
        if (plotHist) {
            this.errorValues.addAll(errorValues);
        }
        plt.legend().loc(2);*/

    }

    public void plotError(List<VaultEntry> entries, List<VaultEntry> basalTreatments,
            List<VaultEntry> bolusTreatments, List<VaultEntry> meals,
            double sensitivity, int insDuration, double carbratio, int absorptionTime) {

    }

    public void plotDiff(Snippet s, List<VaultEntry> meals,
            double sensitivity, int insDuration,
            double carbratio, int absorptionTime) {
        plotDiff = true;
        List<Double> basalValues = new ArrayList<>();
        List<Double> basalTimes = new ArrayList<>();
        List<Double> bolusValues = new ArrayList<>();
        List<Double> bolusTimes = new ArrayList<>();
        List<Double> mealValues = new ArrayList<>();
        List<Double> mealTimes = new ArrayList<>();

        List<Double> bgTimes = new ArrayList<>();
        List<Double> bgValues = new ArrayList<>();
        List<Double> algoValues = new ArrayList<>();
        List<Double> noMealValues = new ArrayList<>();

        for (VaultEntry a : s.getBasals()) {
            basalValues.add(a.getValue());// - a.getValue() * profile.getSensitivity() * a.getDuration()
            basalTimes.add(a.getTimestamp().getTime() / 1000.0);
        }

        for (VaultEntry a : s.getBoli()) {
            bolusValues.add(a.getValue()); // -a.getValue() * profile.getSensitivity()
            bolusTimes.add(a.getTimestamp().getTime() / 1000.0);
        }

        for (VaultEntry a : meals) {
            mealValues.add(a.getValue()); // a.getValue() * profile.getSensitivity() / profile.getCarbratio()
            mealTimes.add(a.getTimestamp().getTime() / 1000.0);
        }
        VaultEntry prev = s.getEntries().get(0);
        double prevValue = prev.getValue();
        double prevTime = prev.getTimestamp().getTime();

        double algoPredictPrev = Predictions.predict(prev.getTimestamp().getTime(),
                meals, s.getBoli(), s.getBasals(),
                sensitivity, insDuration,
                carbratio, absorptionTime);
        double noMealPredictPrev = Predictions.predict(prev.getTimestamp().getTime(),
                new ArrayList<>(), s.getBoli(), s.getBasals(),
                sensitivity, insDuration,
                carbratio, absorptionTime);
        for (int i = 1; i < s.getEntries().size(); i++) {
            VaultEntry ve = s.getEntries().get(i);
            double algoPredict = Predictions.predict(ve.getTimestamp().getTime(),
                    meals, s.getBoli(), s.getBasals(),
                    sensitivity, insDuration,
                    carbratio, absorptionTime);
            double noMealPredict = Predictions.predict(ve.getTimestamp().getTime(),
                    new ArrayList<>(), s.getBoli(), s.getBasals(),
                    sensitivity, insDuration,
                    carbratio, absorptionTime);
            System.out.println("Date: " + ve.getTimestamp().toString() + " Value: " + ve.getValue() + " Diff: " + (ve.getValue() - prevValue));
            bgTimes.add((ve.getTimestamp().getTime()) / 1000.0);
            noMealValues.add(noMealPredict - noMealPredictPrev);
            algoValues.add(algoPredict - algoPredictPrev);
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
        diffPlt.plot().addDates(bgTimes).add(algoValues).linestyle("--").marker("x");//.color("cyan").linestyle("--");

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

                System.out.printf("MaxError: %.3g%%\n", Collections.max(errorValues, (Double o1, Double o2) -> {
                    Double result = Math.abs(o1) - Math.abs(o2);
                    return result > 0 ? 1 : result < 0 ? -1 : 0;
                }));
//                System.out.println("Varianz: " + (MeanSquareError - MeanError * MeanError));
//                System.out.println("Standardabweichung: " + Math.sqrt(MeanSquareError - MeanError * MeanError));

                histPlt = Plot.create();
                histPlt.xlabel("variance");
                histPlt.ylabel("frequency");
                histPlt.hist().add(errorValues).bins(100);
                histPlt.show();
            }
        } catch (IOException | PythonExecutionException ex) {
            Logger.getLogger(CGMPlotter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private double getStartValue(List<VaultEntry> entries, List<VaultEntry> basalTreatments,
                                 List<VaultEntry> bolusTreatments, List<VaultEntry> meals,
                                 double sensitivity, int insDuration, double carbratio, int absorptionTime) {
        double startTime = getStartTime(entries, insDuration, absorptionTime);

        double startValue = entries.get(0).getValue();
        for (int i = 0; i < entries.size() ; i++) {
            startValue = entries.get(i).getValue() - Predictions.predict(entries.get(i).getTimestamp().getTime(), meals, bolusTreatments, basalTreatments, sensitivity, insDuration, carbratio, absorptionTime);
            if (entries.get(i).getTimestamp().getTime() >= startTime) {
                break;
            }
        }

        return startValue;
    }

    private double getStartTime(List<VaultEntry> entries, int insDuration, int absorptionTime) {
        double startTime = entries.get(0).getTimestamp().getTime();
        double firstValidTime = startTime + Math.max(insDuration , absorptionTime) * 60000;
        for (int i = 0; i < entries.size() - 1; i++) {
            startTime = entries.get(i).getTimestamp().getTime();
            if (entries.get(i + 1).getTimestamp().getTime() > firstValidTime) {
                break;
            }
        }
        return startTime;
    }

    private void generatePointsToDraw(List<VaultEntry> vaultEntries, List<Double> values, List<Double> times) {
        for (VaultEntry a : vaultEntries) {
            if (a.getValue() > 0) {
                for (double i = 0; i <= a.getValue(); i += 0.1) {
                    values.add(i); // a.getValue() * profile.getSensitivity() / profile.getCarbratio()
                    times.add(a.getTimestamp().getTime() / 1000.0);
                }
            } else {
                for (double i = 0; i >= a.getValue(); i -= 0.1) {
                    values.add(i); // a.getValue() * profile.getSensitivity() / profile.getCarbratio()
                    times.add(a.getTimestamp().getTime() / 1000.0);
                }
            }
        }
    }
}
