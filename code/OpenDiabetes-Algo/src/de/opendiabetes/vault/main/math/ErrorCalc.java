package de.opendiabetes.vault.main.math;

import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.main.algo.Algorithm;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.math3.stat.descriptive.UnivariateStatistic;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.Skewness;
import org.apache.commons.math3.stat.descriptive.moment.Variance;
import org.apache.commons.math3.stat.descriptive.rank.Max;
import org.apache.commons.math3.stat.descriptive.rank.Min;

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

    public void calculateError(Algorithm algo) {
        errorValues = new ArrayList<>();
        errorPercent = new ArrayList<>();
        errorDates = new ArrayList<>();
        List<VaultEntry> entries;
        List<VaultEntry> meals;
        List<VaultEntry> bolusTreatments;
        List<VaultEntry> basalDifference;
        double sensitivity;
        long insDuration;
        double carbratio;
        long absorptionTime;
        entries = algo.getGlucose();
        meals = algo.getMeals();
        bolusTreatments = algo.getBolusTreatments();
        basalDifference = algo.getBasalTreatments();
        sensitivity = algo.getProfile().getSensitivity();
        insDuration = algo.getInsulinDuration();

        carbratio = algo.getProfile().getCarbratio();
        absorptionTime = algo.getAbsorptionTime();

        double startValue = 0;
        int startIndex = algo.getStartIndex();
        if (adjustStartValue) {
            startValue = algo.getStartValue();
        }
//        long startTime = getStartTime(entries, insDuration, absorptionTime);
//        for (VaultEntry ve : entries) {
//            // Skip predictions until start time
//            if (ve.getTimestamp().getTime() < startTime) {
//                continue;
//            }
        for (int i = startIndex; i < entries.size(); i++) {
            VaultEntry ve = entries.get(i);
            double algoPredict = Predictions.predict(ve.getTimestamp().getTime(), meals, bolusTreatments,
                    basalDifference, sensitivity, insDuration, carbratio, absorptionTime);

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
//
//    public void calculateError(List<VaultEntry> entries, List<VaultEntry> basalDifference,
//            List<VaultEntry> bolusTreatments, List<VaultEntry> meals,
//            double sensitivity, int insDuration, double carbratio, int absorptionTime) {
//        errorValues = new ArrayList<>();
//        errorPercent = new ArrayList<>();
//        errorDates = new ArrayList<>();
//        double startValue = 0;
//        int startIndex = getStartIndex(entries, insDuration, absorptionTime);
//        if (adjustStartValue) {
//            startValue = getStartValue(entries, basalDifference, bolusTreatments, meals, sensitivity, insDuration, carbratio, absorptionTime, startIndex);
//        }
////        long startTime = getStartTime(entries, insDuration, absorptionTime);
////        for (VaultEntry ve : entries) {
////            // Skip predictions until start time
////            if (ve.getTimestamp().getTime() < startTime) {
////                continue;
////            }
//        for (int i = startIndex; i < entries.size(); i++) {
//            VaultEntry ve = entries.get(i);
//            double algoPredict = Predictions.predict(ve.getTimestamp().getTime(), meals, bolusTreatments,
//                    basalDifference, sensitivity, insDuration, carbratio, absorptionTime);
//
//            double error = startValue + algoPredict - ve.getValue();
//            errorValues.add(error);
//            errorPercent.add(error / ve.getValue() * 100);
//            errorDates.add(ve.getTimestamp());
//        }
//        double[] ev = errorValues.stream().mapToDouble(i -> i).toArray();
//        double[] evp = errorPercent.stream().mapToDouble(i -> i).toArray();
//
//        UnivariateStatistic bias = new Mean();
//        UnivariateStatistic var = new Variance();
//        UnivariateStatistic min = new Min();
//        UnivariateStatistic max = new Max();
//        UnivariateStatistic skew = new Skewness();
//        meanError = bias.evaluate(ev);
//        maxError = Math.max(max.evaluate(ev), Math.abs(min.evaluate(ev)));
//        variance = var.evaluate(ev);
//        meanSquareError = variance + meanError * meanError;
//        rootMeanSquareError = Math.sqrt(meanSquareError);
//        stdDeviation = Math.sqrt(variance);
//        skewness = skew.evaluate(ev);
////        int N = errorValues.size();
////        if (N > 0) {
////            for (double d : errorValues) {
////                meanError += d;
////                meanSquareError += d * d;
////                if (Math.abs(maxError) < Math.abs(d)) {
////                    maxError = d;
////                }
////            }
////            meanError /= N;
////            meanSquareError /= N;
////            // variance needs fully estimated mean error
////            for (double d : errorValues) {
////                variance += Math.pow(d - meanError, 2);
////            }
////            variance /= N;
////            rootMeanSquareError = Math.sqrt(meanSquareError);
////            stdDeviation = Math.sqrt(variance);
////            for (double d : errorValues) {
////                if (stdDeviation > 0) {
////                    skewness += Math.pow((d - meanError) / stdDeviation, 3);
////                }
////            }
////            skewness /= N;
////        }
//
//        //same for errorPercent
//        meanErrorPercent = bias.evaluate(evp);
//        maxErrorPercent = Math.max(max.evaluate(evp), Math.abs(min.evaluate(evp)));
//        variancePercent = var.evaluate(evp);
//        meanSquareErrorPercent = variancePercent + meanErrorPercent * meanErrorPercent;
//        rootMeanSquareErrorPercent = Math.sqrt(meanSquareErrorPercent);
//        stdDeviationPercent = Math.sqrt(variancePercent);
//        skewnessPercent = skew.evaluate(evp);
////        meanErrorPercent = 0;
////        meanSquareErrorPercent = 0;
////        maxErrorPercent = 0;
////        variancePercent = 0;
////        rootMeanSquareErrorPercent = 0;
////        stdDeviationPercent = 0;
////        skewnessPercent = 0;
////        if (N > 0) {
////            for (double d : errorPercent) {
////                meanErrorPercent += d;
////                meanSquareErrorPercent += d * d;
////                if (Math.abs(maxErrorPercent) < Math.abs(d)) {
////                    maxErrorPercent = d;
////                }
////            }
////            meanErrorPercent /= N;
////            meanSquareErrorPercent /= N;
////
////            for (double d : errorPercent) {
////                variancePercent += Math.pow(d - meanErrorPercent, 2);
////            }
////            variancePercent /= N;
////            rootMeanSquareErrorPercent = Math.sqrt(meanSquareErrorPercent);
////            stdDeviationPercent = Math.sqrt(variancePercent);
////            for (double d : errorPercent) {
////                skewnessPercent += Math.pow((d - meanErrorPercent) / stdDeviationPercent, 3);
////            }
////            skewnessPercent /= N;
////        }
//    }

//    public static double getStartValue(List<VaultEntry> entries, List<VaultEntry> basalTreatments,
//            List<VaultEntry> bolusTreatments, List<VaultEntry> meals,
//            double sensitivity, int insDuration, double carbratio, int absorptionTime, int startIndex) {
//        if (entries.isEmpty()) {
//            return 0;
//        }
////        int startIndex = getStartIndex(entries, insDuration, absorptionTime);
//        double startValue;
//        startValue = entries.get(startIndex).getValue() - Predictions.predict(entries.get(startIndex).getTimestamp().getTime(), meals, bolusTreatments, basalTreatments, sensitivity, insDuration, carbratio, absorptionTime);
//
//        return startValue;
//    }
//    public static int getStartIndex(List<VaultEntry> entries, int insDuration, int absorptionTime) {
//        if (entries.isEmpty()) {
//            return 0;
//        }
//        long startTime = entries.get(0).getTimestamp().getTime();
//        long firstValidTime = startTime + Math.max(insDuration, absorptionTime) * 60000;
//        int i = 0;
//        for (; i < entries.size() - 1; i++) {
//            if (entries.get(i + 1).getTimestamp().getTime() > firstValidTime) {
//                break;
//            }
//        }
//        return i;
//    }
//    public static long getStartTime(List<VaultEntry> entries, int insDuration, int absorptionTime) {
//        if (entries.isEmpty()) {
//            return 0;
//        }
//        long startTime = entries.get(0).getTimestamp().getTime();
//        long firstValidTime = startTime + Math.max(insDuration, absorptionTime) * 60000;
//        for (int i = 0; i < entries.size() - 1; i++) {
//            startTime = entries.get(i).getTimestamp().getTime();
//            if (entries.get(i + 1).getTimestamp().getTime() > firstValidTime) {
//                break;
//            }
//        }
//        return startTime;
//    }
}
