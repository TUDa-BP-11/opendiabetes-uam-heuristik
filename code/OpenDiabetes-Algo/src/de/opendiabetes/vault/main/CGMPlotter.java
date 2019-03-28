package de.opendiabetes.vault.main;

import com.github.sh0nk.matplotlib4j.Plot;
import com.github.sh0nk.matplotlib4j.PythonExecutionException;
import de.opendiabetes.vault.main.math.Predictions;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.main.algo.Algorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class CGMPlotter {

    private String title = "";
    private double sensitivity;
    private double carbratio;
    private int insDuration;
    private int absorptionTime;
    private double peak;
    private double maxMeal = 0;
    private Plot plt;
    private Plot diffPlt;
    private Plot histPlt;
    private boolean plotPlot = false;
    private boolean plotDiff = false;
    private boolean plotHist = false;
    private boolean plotError = false;
    private boolean bStartValue = true;
    private boolean bStartTime = true;

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

    /**
     * @param plotHist
     * @param bStartValue
     * @deprecated use CGMPlotter(boolean plotHist, boolean bStartValue, double
     * sensitivity, int insDuration, double carbratio, int absorptionTime)
     */
    public CGMPlotter(boolean plotHist, boolean bStartValue) {
        this();
        this.plotHist = plotHist;
        this.bStartValue = bStartValue;
    }

    /**
     * @param plotHist
     * @param bStartValue
     * @param bStartTime
     * @param sensitivity
     * @param insDuration
     * @param carbratio
     * @param absorptionTime
     */
    public CGMPlotter(boolean plotHist, boolean bStartValue, boolean bStartTime, double sensitivity, int insDuration, double carbratio, int absorptionTime, double peak) {
        this();
        this.plotHist = plotHist;
        this.bStartValue = bStartValue;
        this.bStartTime = bStartTime;
        this.sensitivity = sensitivity;
        this.insDuration = insDuration;
        this.carbratio = carbratio;
        this.absorptionTime = absorptionTime;
        this.peak = peak;
    }

    /**
     * @param title
     */
    public void title(String title) {
        this.title = title;
    }

    /**
     * @param algo
     */
    public void add(Algorithm algo) {
        List<VaultEntry> entries = algo.getGlucose();
        List<VaultEntry> basalTreatments = algo.getBasalTreatments();
        List<VaultEntry> bolusTreatments = algo.getBolusTreatments();
        List<VaultEntry> meals = algo.getMeals();
        double startValue = 0;
        int startIndex = algo.getStartIndex();
        if (bStartValue) {
            startValue = algo.getStartValue();
        }
        this.add(entries, basalTreatments, bolusTreatments, meals, startIndex, startValue);
    }

    public void add(List<VaultEntry> entries, List<VaultEntry> basalTreatments, List<VaultEntry> bolusTreatments, List<VaultEntry> meals, int startIndex, double startValue) {

        plotPlot = true;
        List<Double> basalValuesSnippet = new ArrayList<>();
        List<Double> basalTimesSnippet = new ArrayList<>();
        for (VaultEntry a : basalTreatments) {
            basalValuesSnippet.add(a.getValue());
            basalTimesSnippet.add(a.getTimestamp().getTime() / 1000.0);
        }

        List<Double> bolusValuesSnippet = new ArrayList<>();
        List<Double> bolusTimesSnippet = new ArrayList<>();
        for (VaultEntry a : bolusTreatments) {
            bolusValuesSnippet.add(a.getValue());
            bolusTimesSnippet.add(a.getTimestamp().getTime() / 1000.0);
        }
//        generatePointsToDraw(bolusTreatments, bolusValuesSnippet, bolusTimesSnippet);

        List<Double> mealValuesSnippet = new ArrayList<>();
        List<Double> mealTimesSnippet = new ArrayList<>();
        for (VaultEntry ve : meals) {
            mealTimesSnippet.add(ve.getTimestamp().getTime() / 1000.0);
            mealValuesSnippet.add(ve.getValue() / carbratio);
        }
        if (!mealValuesSnippet.isEmpty()) {
            maxMeal = Math.max(maxMeal, Collections.max(mealValuesSnippet));
        }
//        generatePointsToDraw(meals, mealValuesSnippet, mealTimesSnippet);

        List<Double> bgTimesSnippet = new ArrayList<>();
        List<Double> algoTimesSnippet = new ArrayList<>();
        List<Double> bgValuesSnippet = new ArrayList<>();
        List<Double> algoValuesSnippet = new ArrayList<>();

        double first, last;
        first = entries.get(0).getTimestamp().getTime() / 1000.0;
        last = entries.get(entries.size() - 1).getTimestamp().getTime() / 1000.0;
        if (!mealTimesSnippet.isEmpty()) {
            first = Math.min(mealTimesSnippet.get(0), first);
            last = Math.max(mealTimesSnippet.get(mealTimesSnippet.size() - 1), last);
        }
        if (!basalTimesSnippet.isEmpty()) {
            first = Math.min(basalTimesSnippet.get(0), first);
            last = Math.max(basalTimesSnippet.get(basalTimesSnippet.size() - 1), last);
        }
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

//        double startTime = ErrorCalc.getStartTime(entries, insDuration, absorptionTime);
//        for (VaultEntry ve : entries) {
        for (int i = 0; i < entries.size(); i++) {
            VaultEntry ve = entries.get(i);
            bgTimesSnippet.add((ve.getTimestamp().getTime()) / 1000.0);
            bgValuesSnippet.add(ve.getValue());

            // Skip predictions until start time
            if (bStartTime && i < startIndex) {
                continue;
            }
            double algoPredict = Predictions.predict(ve.getTimestamp().getTime(), meals, bolusTreatments, basalTreatments,
                    sensitivity, insDuration, carbratio, absorptionTime, peak);
            algoValuesSnippet.add(startValue + algoPredict);
            algoTimesSnippet.add(ve.getTimestamp().getTime() / 1000.0);
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
    }

    public void addError(List<Double> errorValues, List<Date> errorDate) {
        plotError = true;
        this.errorValues.add(errorValues);
        this.errorTimes.add((errorDate.stream().mapToDouble(e -> e.getTime() / 1000.0)).boxed().collect(Collectors.toList()));
        if (plotHist) {
            allErrorValues.addAll(errorValues);
        }

    }

    public String showAll() throws IOException, PythonExecutionException {

        String scriptLines = "";
        if (plotPlot) {
            int subplots = plotError ? 3 : 2;//4 : 3;
            plt.subplot(subplots, 1, 1);
            plt.title(title);
            for (int i = 0; i < bgTimes.size(); i++) {
                if (i == 0) {
                    plt.plot().addDates(bgTimes.get(i)).add(bgValues.get(i)).label("cgm").color("C" + i % 10);
                    plt.plot().addDates(algoTimes.get(i)).add(algoValues.get(i)).linestyle("--").label("predicted values").color("C" + i % 10);
                } else {
                    plt.plot().addDates(bgTimes.get(i)).add(bgValues.get(i)).color("C" + i % 10);
                    plt.plot().addDates(algoTimes.get(i)).add(algoValues.get(i)).linestyle("--").color("C" + i % 10);
                }
//                plt.axvlineDate(bgTimes.get(i).get(0));
//                plt.axvlineDate(bgTimes.get(i).get(bgTimes.get(i).size() - 1));

            }

            plt.title("Blood Glucose");
            plt.xlabel("time");
            plt.ylabel("mg/dl");
            plt.plot().addDates(firstToLast).add(zeros).linestyle("");
            plt.legend().loc(2);
            plt.subplot(subplots, 1, 2);

            plt.xlabel("time");
            plt.ylabel("IE/KE");
            for (int i = 0; i < mealTimes.size(); i++) {
                if (i == 0) {
                    plt.plot().addDates(mealTimes.get(i)).add(mealValues.get(i)).label("KE").marker("o").linestyle("").markersize("20").markerfacecolor("none"); //.color("red")
                    plt.bar().addDates(bolusTimes.get(i)).add(bolusValues.get(i)).color("green").width(0.0035).label("IE");//.linestyle("").marker("_").markersize("3");

                    //plt.plot().addDates(basalTimes.get(i)).add(basalValues.get(i)).color("cyan").linestyle("").label("basal").marker("o");
                } else {
                    plt.plot().addDates(mealTimes.get(i)).add(mealValues.get(i)).marker("o").linestyle("").markersize("20").markerfacecolor("none"); //.color("red")
                    plt.bar().addDates(bolusTimes.get(i)).add(bolusValues.get(i)).color("green");//.linestyle("").marker("_").markersize("3");
                    //plt.plot().addDates(basalTimes.get(i)).add(basalValues.get(i)).color("cyan").linestyle("").marker("o");
                }

                List<Double> t, y;
                t = mealTimes.get(i);
                y = mealValues.get(i);

                for (int j = 0; j < t.size(); j++) {
                    plt.text().addDate(t.get(j), y.get(j), String.format("%.1f", y.get(j))).horizontalalignment("center").verticalalignment("center");
                }
            }
            plt.plot().addDates(firstToLast).add(zeros).linestyle("");
            plt.legend().loc(2);
            plt.ylim(0, maxMeal + 5);
            plt.title("Meals and Bolus");
//            plt.subplot(subplots, 1, 3);
//            plt.xlabel("time");
//            plt.ylabel("mg/dl");
//            for (int i = 0; i < bgTimes.size(); i++) {
//                if (i == 0) {
//                    plt.plot().addDates(bolusTimes.get(i)).add(bolusValues.get(i)).linestyle("").label("bolus").marker("_").markersize("3").color("C" + i % 10);
//                    plt.plot().addDates(basalTimes.get(i)).add(basalValues.get(i)).color("cyan").linestyle("").label("basal").marker("o");
//                } else {
//                    plt.plot().addDates(bolusTimes.get(i)).add(bolusValues.get(i)).linestyle("").marker("_").markersize("3").color("C" + i % 10);
//                    plt.plot().addDates(basalTimes.get(i)).add(basalValues.get(i)).color("cyan").linestyle("").marker("o");
//                }
//            }
//            plt.plot().addDates(firstToLast).add(zeros).linestyle("");
//            plt.legend().loc(2);
            if (plotError) {
                plt.subplot(subplots, 1, subplots);
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
                plt.title("Relative Error");
            }
            plt.tight_layout();
            scriptLines = plt.show();
        }
        if (plotDiff) {
            diffPlt.show();
        }

        if (plotHist && !allErrorValues.isEmpty()) {
            histPlt = Plot.create();
            histPlt.xlabel("error");
            histPlt.ylabel("frequency");
            histPlt.hist().add(allErrorValues).bins(50);
            histPlt.title(title);
            histPlt.show();
        }

        return scriptLines;
    }

//    private void generatePointsToDraw(List<VaultEntry> vaultEntries, List<Double> values, List<Double> times) {
//        vaultEntries.forEach((a) -> {
//            if (a.getValue() > 0) {
//                for (double i = 0; i <= a.getValue(); i += 0.1) {
//                    values.add(i); // a.getValue() * profile.getSensitivity() / profile.getCarbratio()
//                    times.add(a.getTimestamp().getTime() / 1000.0);
//                }
//            } else {
//                for (double i = 0; i >= a.getValue(); i -= 0.1) {
//                    values.add(i); // a.getValue() * profile.getSensitivity() / profile.getCarbratio()
//                    times.add(a.getTimestamp().getTime() / 1000.0);
//                }
//            }
//        });
//    }
//    public void plotDiff(Snippet s, List<VaultEntry> meals,
//            double sensitivity, int insDuration,
//            double carbratio, int absorptionTime) {
//        plotDiff = true;
//        List<Double> basalValuesSnippet = new ArrayList<>();
//        List<Double> basalTimesSnippet = new ArrayList<>();
//        List<Double> bolusValuesSnippet = new ArrayList<>();
//        List<Double> bolusTimesSnippet = new ArrayList<>();
//        List<Double> mealValuesSnippet = new ArrayList<>();
//        List<Double> mealTimesSnippet = new ArrayList<>();
//
//        List<Double> bgTimesSnippet = new ArrayList<>();
//        List<Double> bgValuesSnippet = new ArrayList<>();
//        List<Double> algoValuesSnippet = new ArrayList<>();
//        List<Double> noMealValues = new ArrayList<>();
//
//        for (VaultEntry a : s.getBasals()) {
//            basalValuesSnippet.add(a.getValue());// - a.getValue() * profile.getSensitivity() * a.getDuration()
//            basalTimesSnippet.add(a.getTimestamp().getTime() / 1000.0);
//        }
//
//        for (VaultEntry a : s.getBoli()) {
//            bolusValuesSnippet.add(a.getValue()); // -a.getValue() * profile.getSensitivity()
//            bolusTimesSnippet.add(a.getTimestamp().getTime() / 1000.0);
//        }
//
//        for (VaultEntry a : meals) {
//            mealValuesSnippet.add(a.getValue());
//            mealTimesSnippet.add(a.getTimestamp().getTime() / 1000.0);
//        }
//        VaultEntry prev = s.getEntries().get(0);
//        double prevValue = prev.getValue();
//        double prevTime = prev.getTimestamp().getTime();
//
//        double algoPredictPrev = Predictions.predict(prev.getTimestamp().getTime(),
//                meals, s.getBoli(), s.getBasals(),
//                sensitivity, insDuration,
//                carbratio, absorptionTime);
//        double noMealPredictPrev = Predictions.predict(prev.getTimestamp().getTime(),
//                new ArrayList<>(), s.getBoli(), s.getBasals(),
//                sensitivity, insDuration,
//                carbratio, absorptionTime);
//        for (int i = 1; i < s.getEntries().size(); i++) {
//            VaultEntry ve = s.getEntries().get(i);
//            double algoPredict = Predictions.predict(ve.getTimestamp().getTime(),
//                    meals, s.getBoli(), s.getBasals(),
//                    sensitivity, insDuration,
//                    carbratio, absorptionTime);
//            double noMealPredict = Predictions.predict(ve.getTimestamp().getTime(),
//                    new ArrayList<>(), s.getBoli(), s.getBasals(),
//                    sensitivity, insDuration,
//                    carbratio, absorptionTime);
//            NSApi.LOGGER.log(Level.INFO, "Date: %s Value: %f Diff: %f", new Object[]{ve.getTimestamp().toString(), ve.getValue(), ve.getValue() - prevValue});
//            bgTimesSnippet.add((ve.getTimestamp().getTime()) / 1000.0);
//            noMealValues.add(noMealPredict - noMealPredictPrev);
//            algoValuesSnippet.add(algoPredict - algoPredictPrev);
//            bgValuesSnippet.add((ve.getValue() - prevValue) / ((ve.getTimestamp().getTime() - prevTime) / 1000));
//            prevValue = ve.getValue();
//            prevTime = ve.getTimestamp().getTime();
//            noMealPredictPrev = noMealPredict;
//            algoPredictPrev = algoPredict;
//        }
//
//        diffPlt.xlabel("time");
//        diffPlt.ylabel("dBG/dt");
//        diffPlt.plot().addDates(bgTimesSnippet).add(bgValuesSnippet).color("blue").marker("x"); //.label("Testlabel")
//        diffPlt.plot().addDates(bgTimesSnippet).add(noMealValues).color("orange").marker("x"); //.label("Testlabel")
//        diffPlt.plot().addDates(mealTimesSnippet).add(mealValuesSnippet).color("red").linestyle("").marker("x");
//        diffPlt.plot().addDates(bolusTimesSnippet).add(bolusValuesSnippet).color("green").linestyle("").marker("o");
//        diffPlt.plot().addDates(basalTimesSnippet).add(basalValuesSnippet).color("cyan").linestyle("").marker("o");
//        diffPlt.plot().addDates(bgTimesSnippet).add(algoValuesSnippet).linestyle("--").marker("x");//.color("cyan").linestyle("--");
//
//    }
}
