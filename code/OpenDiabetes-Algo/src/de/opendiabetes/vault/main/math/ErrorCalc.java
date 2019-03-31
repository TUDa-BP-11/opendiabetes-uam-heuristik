package de.opendiabetes.vault.main.math;

import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.main.algo.Algorithm;
import org.apache.commons.math3.stat.descriptive.UnivariateStatistic;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.Skewness;
import org.apache.commons.math3.stat.descriptive.moment.Variance;
import org.apache.commons.math3.stat.descriptive.rank.Max;
import org.apache.commons.math3.stat.descriptive.rank.Min;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ErrorCalc {

    private List<Double> errorValues;
    private List<Double> errorPercent;
    private List<Date> errorDates;
    private boolean adjustStartValue;
    private double meanError;
    private double meanSquareError;
    private double maxError;
    private double variance;
    private double skewness;
    private double rootMeanSquareError;
    private double stdDeviation;

    private double meanErrorPercent;
    private double meanSquareErrorPercent;
    private double maxErrorPercent;
    private double variancePercent;
    private double skewnessPercent;
    private double rootMeanSquareErrorPercent;
    private double stdDeviationPercent;

    public ErrorCalc() {
        this(true);
    }

    public ErrorCalc(boolean adjustStartValue) {
        this.adjustStartValue = adjustStartValue;
    }

    public List<Double> getErrorValues() {
        return errorValues;
    }

    public List<Double> getErrorPercent() {
        return errorPercent;
    }

    public List<Date> getErrorDates() {
        return errorDates;
    }

    public double getMeanError() {
        return meanError;
    }

    public double getMeanSquareError() {
        return meanSquareError;
    }

    public double getMaxError() {
        return maxError;
    }

    public double getVariance() {
        return variance;
    }

    public double getSkewness() {
        return skewness;
    }

    public double getRootMeanSquareError() {
        return rootMeanSquareError;
    }

    public double getStdDeviation() {
        return stdDeviation;
    }

    public double getMeanErrorPercent() {
        return meanErrorPercent;
    }

    public double getMeanSquareErrorPercent() {
        return meanSquareErrorPercent;
    }

    public double getMaxErrorPercent() {
        return maxErrorPercent;
    }

    public double getVariancePercent() {
        return variancePercent;
    }

    public double getSkewnessPercent() {
        return skewnessPercent;
    }

    public double getRootMeanSquareErrorPercent() {
        return rootMeanSquareErrorPercent;
    }

    public double getStdDeviationPercent() {
        return stdDeviationPercent;
    }

    /**
     * calculates error statistics for a given calculation of meals
     *
     * @param algo Algorithm that is used for claculation
     */
    public void calculateError(Algorithm algo) {
        errorValues = new ArrayList<>();
        errorPercent = new ArrayList<>();
        errorDates = new ArrayList<>();
        List<VaultEntry> entries = algo.getGlucose();
        List<VaultEntry> meals = algo.getMeals();
        List<VaultEntry> bolusTreatments = algo.getBolusTreatments();
        List<VaultEntry> basalDifference = algo.getBasalTreatments();
        double sensitivity = algo.getProfile().getSensitivity();
        long insDuration = algo.getInsulinDuration();
        double peak = algo.getPeak();
        double carbratio = algo.getProfile().getCarbratio();
        long absorptionTime = algo.getAbsorptionTime();

        double startValue = 0;
        int startIndex = algo.getStartIndex();
        if (adjustStartValue) {
            startValue = algo.getStartValue();
        }
        for (int i = startIndex; i < entries.size(); i++) {
            VaultEntry ve = entries.get(i);
            double algoPredict = Predictions.predict(ve.getTimestamp().getTime(), meals, bolusTreatments,
                    basalDifference, sensitivity, insDuration, carbratio, absorptionTime, peak);

            double error = startValue + algoPredict - ve.getValue();
            errorValues.add(error);
            errorPercent.add(error / ve.getValue() * 100);
            errorDates.add(ve.getTimestamp());
        }
        double[] ev = errorValues.stream().mapToDouble(i -> i).toArray();
        double[] evp = errorPercent.stream().mapToDouble(i -> i).toArray();

        UnivariateStatistic bias = new Mean();
        UnivariateStatistic var = new Variance();
        UnivariateStatistic min = new Min();
        UnivariateStatistic max = new Max();
        UnivariateStatistic skew = new Skewness();
        meanError = bias.evaluate(ev);
        maxError = Math.max(max.evaluate(ev), Math.abs(min.evaluate(ev)));
        variance = var.evaluate(ev);
        meanSquareError = variance + meanError * meanError;
        rootMeanSquareError = Math.sqrt(meanSquareError);
        stdDeviation = Math.sqrt(variance);
        skewness = skew.evaluate(ev);

        //same for errorPercent
        meanErrorPercent = bias.evaluate(evp);
        maxErrorPercent = Math.max(max.evaluate(evp), Math.abs(min.evaluate(evp)));
        variancePercent = var.evaluate(evp);
        meanSquareErrorPercent = variancePercent + meanErrorPercent * meanErrorPercent;
        rootMeanSquareErrorPercent = Math.sqrt(meanSquareErrorPercent);
        stdDeviationPercent = Math.sqrt(variancePercent);
        skewnessPercent = skew.evaluate(evp);
    }
}
