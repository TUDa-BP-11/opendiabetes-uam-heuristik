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
    private boolean bStartValue = true;

    private final List<List<Double>> errorValues = new ArrayList<>();
    private final List<List<Double>> bgTimes = new ArrayList<>();
    private final List<List<Double>> algoTimes = new ArrayList<>();
    private final List<List<Double>> bgValues = new ArrayList<>();
    private final List<List<Double>> algoValues = new ArrayList<>();
    private final List<List<Double>> bolusValues = new ArrayList<>();
    private final List<List<Double>> bolusTimes = new ArrayList<>();
    private final List<List<Double>> basalValues = new ArrayList<>();
    private final List<List<Double>> basalTimes = new ArrayList<>();
    private final List<List<Double>> mealValues = new ArrayList<>();
    private final List<List<Double>> mealTimes = new ArrayList<>();
    private final List<List<Double>> errorTimes = new ArrayList<>();

    private final List<Double> allErrorValues = new ArrayList<>();
    private final List<Double> firstToLast = new ArrayList<>();
    private final List<Double> zeros = new ArrayList<>();

    public CGMPlotter() {
        plt = Plot.create();
        plt = Plot.create();
        diffPlt = Plot.create();

        zeros.add(0.0);
        zeros.add(0.0);

    }

    public CGMPlotter(boolean plotHist, boolean bStartValue) {
        this();
        this.plotHist = plotHist;
        this.bStartValue = bStartValue;
    }

    public void plot(List<VaultEntry> entries, List<VaultEntry> basalTreatments,
            List<VaultEntry> bolusTreatments, List<VaultEntry> meals,
            double sensitivity, int insDuration, double carbratio, int absorptionTime) {

        plotPlot = true;
        List<Double> basalValuesSnippet = new ArrayList<>();
        List<Double> basalTimesSnippet = new ArrayList<>();
        List<Double> bolusValuesSnippet = new ArrayList<>();
        List<Double> bolusTimesSnippet = new ArrayList<>();
        List<Double> mealValuesSnippet = new ArrayList<>();
        List<Double> mealTimesSnippet = new ArrayList<>();        
        List<Double> bgTimesSnippet = new ArrayList<>();
        List<Double> algoTimesSnippet = new ArrayList<>();
        List<Double> bgValuesSnippet = new ArrayList<>();
//        List<Double> noMealValues = new ArrayList<>();
        List<Double> algoValuesSnippet = new ArrayList<>();
        List<Double> errorTimesSnippet = new ArrayList<>();
        List<Double> errorValuesSnippet = new ArrayList<>();
        
        for (VaultEntry a : basalTreatments) {
            basalValuesSnippet.add(a.getValue());// - a.getValue() * profile.getSensitivity() * a.getDuration()
            basalTimesSnippet.add(a.getTimestamp().getTime() / 1000.0);
        }

        generatePointsToDraw(bolusTreatments, bolusValuesSnippet, bolusTimesSnippet);

        generatePointsToDraw(meals, mealValuesSnippet, mealTimesSnippet);



        double first, last;
        first = entries.get(0).getTimestamp().getTime() / 1000.0;
        first = Math.min(Math.min(mealTimesSnippet.get(0), basalTimesSnippet.get(0)), first);
        last = entries.get(entries.size() - 1).getTimestamp().getTime() / 1000.0;
        last = Math.max(Math.max(mealTimesSnippet.get(mealTimesSnippet.size() - 1), basalTimesSnippet.get(basalTimesSnippet.size() - 1)), last);
        if (firstToLast.isEmpty()) {
            firstToLast.add(first);
            firstToLast.add(last);
        } else {
            if (first < firstToLast.get(0)) {
                firstToLast.set(0, first);
            }
            if (last > firstToLast.get(1)) {
                firstToLast.set(1, last);
            }
        }

//        zeros.add(0.0);
//        zeros.add(0.0);
        double startValue = 0;
        if (bStartValue) {
            startValue = getStartValue(entries, basalTreatments, bolusTreatments, meals, sensitivity, insDuration, carbratio, absorptionTime);
        }
        double startTime = getStartTime(entries, insDuration, absorptionTime);
//        double offset = 0;
        for (VaultEntry ve : entries) {
            bgTimesSnippet.add((ve.getTimestamp().getTime()) / 1000.0);
            bgValuesSnippet.add(ve.getValue());

//            double noMealPredict = Predictions.predict(ve.getTimestamp().getTime(),
//                    new ArrayList<>(), bolusTreatments, basalTreatments,
//                    sensitivity, insDuration,
//                    carbratio, absorptionTime);
// Skip predictions until start time
            if (bStartValue && ve.getTimestamp().getTime() < startTime) {
                continue;
            }
            double algoPredict = Predictions.predict(ve.getTimestamp().getTime(),
                    meals, bolusTreatments, basalTreatments,
                    sensitivity, insDuration,
                    carbratio, absorptionTime);
            algoValuesSnippet.add(startValue + algoPredict);
            algoTimesSnippet.add(ve.getTimestamp().getTime() / 1000.0);

            //noMealValues.add(ve.getValue() + noMealPredict + offset);
            errorValuesSnippet.add((startValue + algoPredict - ve.getValue()) / ve.getValue() * 100);
            errorTimesSnippet.add(ve.getTimestamp().getTime() / 1000.0);
        }

        this.bgTimes.add(bgTimesSnippet);
        this.bgValues.add(bgValuesSnippet);
        this.algoTimes.add(algoTimesSnippet);
        this.algoValues.add(algoValuesSnippet);
        this.mealTimes.add(mealTimesSnippet);
        this.mealValues.add(mealValuesSnippet);
        this.bolusTimes.add(bolusTimesSnippet);
        this.bolusValues.add(bolusValuesSnippet);
        this.basalTimes.add(basalTimesSnippet);
        this.basalValues.add(basalValuesSnippet);
        this.errorTimes.add(errorTimesSnippet);
        this.errorValues.add(errorValuesSnippet);
//        plt.subplot(3, 1, 1);
//        plt.xlabel("time");
//        plt.ylabel("mg/dl");
//        plt.plot().addDates(bgTimes).add(bgValues).color("blue").label("cgm"); //.label("Testlabel")
//        plt.plot().addDates(algoTimes).add(algoValues).linestyle("--").label("predicted values");//.color("cyan").linestyle("--");
        //plt.plot().addDates(algoTimes).add(noMealValues).color("orange").label("no meal predictions"); //.label("Testlabel")
//        plt.plot().addDates(firstToLast).add(zeros).linestyle("");
//        plt.legend().loc(2);

//        plt.subplot(3, 1, 2);
//        plt.plot().addDates(firstToLast).add(zeros).linestyle("");
//        plt.plot().addDates(mealTimes).add(mealValues).color("red").label("meals").marker("_").linestyle("").markersize("3");
//        plt.plot().addDates(bolusTimes).add(bolusValues).color("green").linestyle("").label("bolus").marker("_").markersize("3");
        //plt.plot().addDates(basalTimes).add(basalValues).color("cyan").linestyle("").label("basal").marker("o");
//        plt.legend().loc(2);
        // plot error
//        plt.subplot(3, 1, 3);
//        plt.xlabel("time");
//        plt.ylabel("%");
//        plt.plot().addDates(firstToLast).add(zeros).linestyle("");
//        plt.plot().addDates(errorTimes).add(errorValues).label("relative error");//.color("magenta").linestyle("--");
//        plt.plot().addDates(firstToLast).add(Collections.nCopies(firstToLast.size(), 10)).color("black").label("fault tolerance");
//        plt.plot().addDates(firstToLast).add(Collections.nCopies(firstToLast.size(), -10)).color("black");
//        plt.legend().loc(2);
        if (plotHist) {
            allErrorValues.addAll(errorValuesSnippet);
        }
    }

    public void plotDiff(Snippet s, List<VaultEntry> meals,
            double sensitivity, int insDuration,
            double carbratio, int absorptionTime) {
        plotDiff = true;
        List<Double> basalValuesSnippet = new ArrayList<>();
        List<Double> basalTimesSnippet = new ArrayList<>();
        List<Double> bolusValuesSnippet = new ArrayList<>();
        List<Double> bolusTimesSnippet = new ArrayList<>();
        List<Double> mealValuesSnippet = new ArrayList<>();
        List<Double> mealTimesSnippet = new ArrayList<>();

        List<Double> bgTimesSnippet = new ArrayList<>();
        List<Double> bgValuesSnippet = new ArrayList<>();
        List<Double> algoValuesSnippet = new ArrayList<>();
        List<Double> noMealValues = new ArrayList<>();

        for (VaultEntry a : s.getBasals()) {
            basalValuesSnippet.add(a.getValue());// - a.getValue() * profile.getSensitivity() * a.getDuration()
            basalTimesSnippet.add(a.getTimestamp().getTime() / 1000.0);
        }

        for (VaultEntry a : s.getBoli()) {
            bolusValuesSnippet.add(a.getValue()); // -a.getValue() * profile.getSensitivity()
            bolusTimesSnippet.add(a.getTimestamp().getTime() / 1000.0);
        }

        for (VaultEntry a : meals) {
            mealValuesSnippet.add(a.getValue()); // a.getValue() * profile.getSensitivity() / profile.getCarbratio()
            mealTimesSnippet.add(a.getTimestamp().getTime() / 1000.0);
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
            bgTimesSnippet.add((ve.getTimestamp().getTime()) / 1000.0);
            noMealValues.add(noMealPredict - noMealPredictPrev);
            algoValuesSnippet.add(algoPredict - algoPredictPrev);
            bgValuesSnippet.add((ve.getValue() - prevValue) / ((ve.getTimestamp().getTime() - prevTime) / 1000));
            prevValue = ve.getValue();
            prevTime = ve.getTimestamp().getTime();
            noMealPredictPrev = noMealPredict;
            algoPredictPrev = algoPredict;
        }

        diffPlt.xlabel("time");
        diffPlt.ylabel("dBG/dt");
        diffPlt.plot().addDates(bgTimesSnippet).add(bgValuesSnippet).color("blue").marker("x"); //.label("Testlabel")
        diffPlt.plot().addDates(bgTimesSnippet).add(noMealValues).color("orange").marker("x"); //.label("Testlabel")
        diffPlt.plot().addDates(mealTimesSnippet).add(mealValuesSnippet).color("red").linestyle("").marker("x");
        diffPlt.plot().addDates(bolusTimesSnippet).add(bolusValuesSnippet).color("green").linestyle("").marker("o");
        diffPlt.plot().addDates(basalTimesSnippet).add(basalValuesSnippet).color("cyan").linestyle("").marker("o");
        diffPlt.plot().addDates(bgTimesSnippet).add(algoValuesSnippet).linestyle("--").marker("x");//.color("cyan").linestyle("--");

    }

    public void showAll() {
        try {
            if (plotPlot) {
                plt.subplot(4, 1, 1);
                for (int i = 0; i < bgTimes.size(); i++) {
                    if (i == 0) {
                        plt.plot().addDates(bgTimes.get(i)).add(bgValues.get(i)).label("cgm").color("C" + i % 10);
                        plt.plot().addDates(algoTimes.get(i)).add(algoValues.get(i)).linestyle("--").label("predicted values").color("C" + i % 10);
                    } else {
                        plt.plot().addDates(bgTimes.get(i)).add(bgValues.get(i)).color("C" + i % 10);
                        plt.plot().addDates(algoTimes.get(i)).add(algoValues.get(i)).linestyle("--").color("C" + i % 10);
                    }
                    plt.axvlineDate(bgTimes.get(i).get(0));
                    plt.axvlineDate(bgTimes.get(i).get(bgTimes.get(i).size() - 1));

                }

                plt.xlabel("time");
                plt.ylabel("mg/dl");
                plt.plot().addDates(firstToLast).add(zeros).linestyle("");
                plt.legend().loc(2);
                plt.subplot(4, 1, 2);

                for (int i = 0; i < bgTimes.size(); i++) {
                    if (i == 0) {
                        plt.plot().addDates(mealTimes.get(i)).add(mealValues.get(i)).label("meals").marker("_").linestyle("").markersize("3"); //.color("red")
//                        plt.plot().addDates(bolusTimes.get(i)).add(bolusValues.get(i)).color("green").linestyle("").label("bolus").marker("_").markersize("3");
                        //plt.plot().addDates(basalTimes.get(i)).add(basalValues.get(i)).color("cyan").linestyle("").label("basal").marker("o");
                    } else {
                        plt.plot().addDates(mealTimes.get(i)).add(mealValues.get(i)).marker("_").linestyle("").markersize("3"); //.color("red")
//                        plt.plot().addDates(bolusTimes.get(i)).add(bolusValues.get(i)).color("green").linestyle("").marker("_").markersize("3");
                        //plt.plot().addDates(basalTimes.get(i)).add(basalValues.get(i)).color("cyan").linestyle("").marker("o");
                    }
                }
                plt.plot().addDates(firstToLast).add(zeros).linestyle("");
                plt.legend().loc(2);
                plt.subplot(4, 1, 3);
                plt.xlabel("time");
                plt.ylabel("%");

                for (int i = 0; i < errorTimes.size(); i++) {
                    if (i == 0) {
                        plt.plot().addDates(errorTimes.get(i)).add(errorValues.get(i)).label("relative error");
                    } else {
                        plt.plot().addDates(errorTimes.get(i)).add(errorValues.get(i));
                    }
                }
                plt.plot().addDates(firstToLast).add(Collections.nCopies(firstToLast.size(), 10)).color("black").label("fault tolerance");
                plt.plot().addDates(firstToLast).add(Collections.nCopies(firstToLast.size(), -10)).color("black");
                plt.legend().loc(2);

                plt.subplot(4, 1, 4);
                plt.xlabel("time");
                plt.ylabel("mg/dl");
                for (int i = 0; i < bgTimes.size(); i++) {
                    if (i == 0) {
                        plt.plot().addDates(bolusTimes.get(i)).add(bolusValues.get(i)).linestyle("").label("bolus").marker("_").markersize("3").color("C" + i % 10);
                        plt.plot().addDates(basalTimes.get(i)).add(basalValues.get(i)).linestyle("").label("basal").marker("o").color("C" + i % 10);
                    } else {
                        plt.plot().addDates(bolusTimes.get(i)).add(bolusValues.get(i)).linestyle("").marker("_").markersize("3").color("C" + i % 10);
                        plt.plot().addDates(basalTimes.get(i)).add(basalValues.get(i)).linestyle("").marker("o").color("C" + i % 10);
                    }
                }
                plt.plot().addDates(firstToLast).add(zeros).linestyle("");
                plt.legend().loc(2);
                String scriptLines = plt.show();

//                try (BufferedWriter writer = new BufferedWriter(new FileWriter("./plotPlot.py"))) {
//                    writer.write(scriptLines);
//                } catch (IOException ex) {
//                    Logger.getLogger(CGMPlotter.class.getName()).log(Level.SEVERE, null, ex);
//                }
//        }
            }
            if (plotDiff) {
                diffPlt.show();
            }
            if (plotHist && !allErrorValues.isEmpty()) {

                double MeanError = 0;
                double MeanSquareError = 0;
                int N = allErrorValues.size();
                for (double d : allErrorValues) {
                    MeanError += d;
                    MeanSquareError += d * d;
                }

                MeanError /= N;
                MeanSquareError /= N;
                double var = 0;
                for (double d : allErrorValues) {
                    var += Math.pow(d - MeanError, 2);
                }
                var /= N;
                double std = Math.sqrt(var);
                double skew = 0;
                for (double d : allErrorValues) {
                    skew += Math.pow((d - MeanError) / std, 3);
                }
                skew /= N;
                double rms = Math.sqrt(MeanSquareError);
                System.out.printf("Bias: %.3g%%%n", MeanError);
                System.out.printf("RootMeanSquareError: %.3g%%%n", rms);
                System.out.printf("Standard Deviation: %.3g%n", std);
                System.out.printf("Skewness: %.3g%n", skew);

                System.out.printf("MaxError: %.3g%%%n", Collections.max(allErrorValues, (Double o1, Double o2) -> {
                    Double result = Math.abs(o1) - Math.abs(o2);
                    return result > 0 ? 1 : result < 0 ? -1 : 0;
                }));

                histPlt = Plot.create();
                histPlt.xlabel("error");
                histPlt.ylabel("frequency");
                histPlt.hist().add(allErrorValues).bins(100);
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

        if (entries.isEmpty()) {
            return 0;
        }

        double startValue = entries.get(0).getValue();
        for (VaultEntry ve : entries) {
            startValue = ve.getValue() - Predictions.predict(ve.getTimestamp().getTime(), meals, bolusTreatments, basalTreatments, sensitivity, insDuration, carbratio, absorptionTime);
            if (ve.getTimestamp().getTime() >= startTime) {
                break;
            }
        }

        return startValue;
    }

    private long getStartTime(List<VaultEntry> entries, int insDuration, int absorptionTime) {
        long startTime = entries.get(0).getTimestamp().getTime();
        long firstValidTime = startTime + Math.max(insDuration, absorptionTime) * 60000;
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
