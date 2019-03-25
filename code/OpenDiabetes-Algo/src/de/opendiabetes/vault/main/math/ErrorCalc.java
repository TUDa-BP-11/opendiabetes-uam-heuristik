package de.opendiabetes.vault.main.math;

import de.opendiabetes.vault.container.VaultEntry;

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


    public void calculateError(List<VaultEntry> entries, List<VaultEntry> basalDifference,
                               List<VaultEntry> bolusTreatments, List<VaultEntry> meals,
                               double sensitivity, int insDuration, double carbratio, int absorptionTime) {
        errorValues = new ArrayList<>();
        errorPercent = new ArrayList<>();
        errorDates = new ArrayList<>();
        double startValue = 0;
        if (adjustStartValue) {
            startValue = getStartValue(entries, basalDifference, bolusTreatments, meals, sensitivity, insDuration, carbratio, absorptionTime);
        }
        double startTime = getStartTime(entries, insDuration, absorptionTime);
        for (VaultEntry ve : entries) {
            // Skip predictions until start time
            if (adjustStartValue && ve.getTimestamp().getTime() < startTime) {
                continue;
            }
            double algoPredict = Predictions.predict(ve.getTimestamp().getTime(), meals, bolusTreatments,
                    basalDifference, sensitivity, insDuration, carbratio, absorptionTime);

            errorValues.add(startValue + algoPredict - ve.getValue());
            errorPercent.add((startValue + algoPredict - ve.getValue()) / ve.getValue() * 100);
            errorDates.add(ve.getTimestamp());
        }

        meanError = 0;
        meanSquareError = 0;
        maxError = 0;
        variance = 0;
        rootMeanSquareError = 0;
        stdDeviation = 0;
        skewness = 0;
        int N = errorValues.size();
        if (N > 0) {
            for (double d : errorValues) {
                meanError += d;
                meanSquareError += d * d;
                variance += Math.pow(d - meanError, 2);
                if (Math.abs(maxError) < Math.abs(d)) {
                    maxError = d;
                }
            }
            meanError /= N;
            meanSquareError /= N;
            variance /= N;
            rootMeanSquareError = Math.sqrt(meanSquareError);
            stdDeviation = Math.sqrt(variance);
            for (double d : errorValues) {
                if (stdDeviation > 0)
                    skewness += Math.pow((d - meanError) / stdDeviation, 3);
            }
            skewness /= N;
        }

        //same for errorPercent
        meanErrorPercent = 0;
        meanSquareErrorPercent = 0;
        maxErrorPercent = 0;
        variancePercent = 0;
        rootMeanSquareErrorPercent = 0;
        stdDeviationPercent = 0;
        skewnessPercent = 0;
        if (N > 0) {
            for (double d : errorPercent) {
                meanErrorPercent += d;
                meanSquareErrorPercent += d * d;
                variancePercent += Math.pow(d - meanErrorPercent, 2);
                if (Math.abs(maxErrorPercent) < Math.abs(d)) {
                    maxErrorPercent = d;
                }
            }
            meanErrorPercent /= N;
            meanSquareErrorPercent /= N;
            variancePercent /= N;
            rootMeanSquareErrorPercent = Math.sqrt(meanSquareErrorPercent);
            stdDeviationPercent = Math.sqrt(variancePercent);
            for (double d : errorPercent) {
                skewnessPercent += Math.pow((d - meanErrorPercent) / stdDeviationPercent, 3);
            }
            skewnessPercent /= N;
        }
    }


    public static double getStartValue(List<VaultEntry> entries, List<VaultEntry> basalTreatments,
                                       List<VaultEntry> bolusTreatments, List<VaultEntry> meals,
                                       double sensitivity, int insDuration, double carbratio, int absorptionTime) {
        if (entries.isEmpty()) {
            return 0;
        }
        double startTime = getStartTime(entries, insDuration, absorptionTime);
        double startValue = entries.get(0).getValue();
        for (VaultEntry ve : entries) {
            startValue = ve.getValue() - Predictions.predict(ve.getTimestamp().getTime(), meals, bolusTreatments, basalTreatments, sensitivity, insDuration, carbratio, absorptionTime);
            if (ve.getTimestamp().getTime() >= startTime) {
                break;
            }
        }

        return startValue;
    }

    public static long getStartTime(List<VaultEntry> entries, int insDuration, int absorptionTime) {
        if (entries.isEmpty())
            return 0;
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
}
