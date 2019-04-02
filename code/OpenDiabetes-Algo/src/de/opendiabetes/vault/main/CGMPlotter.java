package de.opendiabetes.vault.main;

import com.github.sh0nk.matplotlib4j.Plot;
import com.github.sh0nk.matplotlib4j.PythonExecutionException;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.main.algo.Algorithm;
import de.opendiabetes.vault.main.math.Predictions;

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
    private Plot histPlt;
    private boolean plotPlot = false;
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
    private final List<List<Double>> mealValues = new ArrayList<>();
    private final List<List<Double>> mealTimes = new ArrayList<>();
    private final List<List<Double>> errorTimes = new ArrayList<>();

    private final List<Double> allErrorValues = new ArrayList<>();
    private final List<Double> firstToLast = new ArrayList<>();
    private final List<Double> zeros = new ArrayList<>();

    public CGMPlotter() {
        plt = Plot.create();
        plt = Plot.create();

        zeros.add(0.0);
        zeros.add(0.0);
    }

    /**
     * @param plotHist       should an error histogram be plotted
     * @param bStartValue    uses a different start value that is better suited for highlighting the error
     * @param bStartTime     uses a different start time that is better suited for highlighting the error
     * @param sensitivity    insulin to blood glucose factor
     * @param insDuration    effective insulin duration
     * @param carbratio      carb to insulin ratio
     * @param absorptionTime carb absorption time
     * @param peak           duration in minutes until insulin action reaches it’s peak activity level
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

    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * add Data to plot
     *
     * @param algo algorithm used for calculation
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

    /**
     * add Data to plot
     *
     * @param entries         list of VaultEntries with type {@link de.opendiabetes.vault.container.VaultEntryType#GLUCOSE_CGM}
     * @param basalTreatments list of VaultEntries with type {@link de.opendiabetes.vault.container.VaultEntryType#BASAL_PROFILE}
     * @param bolusTreatments list of VaultEntries with type {@link de.opendiabetes.vault.container.VaultEntryType#BOLUS_NORMAL}
     * @param meals           list of calculated meals with type {@link de.opendiabetes.vault.container.VaultEntryType#MEAL_MANUAL}
     * @param startIndex      index after all data is to be added to plot
     * @param startValue      value that should be added to all predicted values
     */
    public void add(List<VaultEntry> entries, List<VaultEntry> basalTreatments, List<VaultEntry> bolusTreatments, List<VaultEntry> meals, int startIndex, double startValue) {

        plotPlot = true;

        List<Double> bolusValuesSnippet = new ArrayList<>();
        List<Double> bolusTimesSnippet = new ArrayList<>();
        for (VaultEntry a : bolusTreatments) {
            bolusValuesSnippet.add(a.getValue());
            bolusTimesSnippet.add(a.getTimestamp().getTime() / 1000.0);
        }

        List<Double> mealValuesSnippet = new ArrayList<>();
        List<Double> mealTimesSnippet = new ArrayList<>();
        for (VaultEntry ve : meals) {
            mealTimesSnippet.add(ve.getTimestamp().getTime() / 1000.0);
            mealValuesSnippet.add(ve.getValue() / carbratio);
        }
        if (!mealValuesSnippet.isEmpty()) {
            maxMeal = Math.max(maxMeal, Collections.max(mealValuesSnippet));
        }

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

        for (int i = 0; i < entries.size(); i++) {
            VaultEntry ve = entries.get(i);
            bgTimesSnippet.add((ve.getTimestamp().getTime()) / 1000.0);
            bgValuesSnippet.add(ve.getValue());

            // Skip predictions until start time
            if (bStartTime && i < startIndex) {
                continue;
            }

            double algoPredict = Predictions.predict(ve.getTimestamp().getTime(), meals, bolusTreatments, basalTreatments, sensitivity, insDuration, carbratio, absorptionTime, peak);
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
    }

    /**
     * @param errorValues list of errors relative to blood glucose curve
     * @param errorDate   the related dates
     */
    public void addError(List<Double> errorValues, List<Date> errorDate) {
        plotError = true;
        this.errorValues.add(errorValues);
        this.errorTimes.add((errorDate.stream().mapToDouble(e -> e.getTime() / 1000.0)).boxed().collect(Collectors.toList()));
        if (plotHist) {
            allErrorValues.addAll(errorValues);
        }

    }

    /**
     * plots the given data
     *
     * @return generated python script that produces this plot
     */
    public String showAll() throws IOException, PythonExecutionException {

        String scriptLines = "";
        if (plotPlot) {
            int subplots = plotError ? 3 : 2;
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
                    if (!mealTimes.get(i).isEmpty())
                        plt.plot().addDates(mealTimes.get(i)).add(mealValues.get(i)).label("KE").marker("o").linestyle("").markersize("20").markerfacecolor("none");
                    if (!bolusTimes.get(i).isEmpty())
                        plt.bar().addDates(bolusTimes.get(i)).add(bolusValues.get(i)).color("green").width(0.0035).label("IE");
                } else {
                    if (!mealTimes.get(i).isEmpty())
                        plt.plot().addDates(mealTimes.get(i)).add(mealValues.get(i)).marker("o").linestyle("").markersize("20").markerfacecolor("none");
                    if (!bolusTimes.get(i).isEmpty())
                        plt.bar().addDates(bolusTimes.get(i)).add(bolusValues.get(i)).color("green");
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
}
