package de.opendiabetes.vault.main.math;

import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.util.TimestampUtils;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestPredictions {
    private static double t, tEnd;
    private static double sens;
    private static double carbRatio;
    private static double peak;
    private static Random random;
    private static int insDuration;
    private static int absorptionTime;

    @BeforeAll
    public static void setup() {
        random = new Random();
        sens = 35;
        carbRatio = 10;
        peak = 55;
        insDuration = 180;
        absorptionTime = 120;
        tEnd = random.nextDouble() * 100;
        t = random.nextDouble() * tEnd;
    }

    @Test
    public void cobTest() {
        assertEquals(0, Predictions.carbsOnBoard(0, absorptionTime));
        assertEquals(1, Predictions.carbsOnBoard(absorptionTime, absorptionTime));
        if (t <= tEnd / 2) {
            double expected = 2 * (t * t) / (tEnd * tEnd);
            assertEquals(expected, Predictions.carbsOnBoard(t, tEnd), 1e-12);
        } else {
            double expected = -1 + (4 * t / tEnd) - (2 * (t * t) / (tEnd * tEnd));
            assertEquals(expected, Predictions.carbsOnBoard(t, tEnd), 1e-12);
        }
    }

    @Test
    public void deltaBGCTest() {

        double x = 1 + random.nextInt(40);
        double expected = (sens / carbRatio) * x * Predictions.carbsOnBoard(t, tEnd);
        assertEquals(expected, Predictions.deltaBGC(t, sens, carbRatio, x, tEnd));
    }

    @Test
    public void deltaBGITest() {

        double t = random.nextInt(insDuration);
        double y = 1 + random.nextInt(5);
        double expected = -1 * y * sens * (1 - Predictions.fastActingIob(t, insDuration, peak));
        assertEquals(expected, Predictions.deltaBGI(t, y, sens, insDuration, peak));
    }

    @Test
    public void iobBoundsTest() {
        assertEquals(1, Predictions.fastActingIob(0, insDuration, peak));
        assertEquals(0, Predictions.fastActingIob(insDuration, insDuration, peak));
    }

    @Test
    public void predictTest() {
        int mealValue = 1 + random.nextInt(50);
        int mealTime = random.nextInt(60);
        int bolusValue = 1 + random.nextInt(10);
        int bolusTime = random.nextInt(60);
        double basalValue = 0.01 + (random.nextDouble() - 0.5) * 0.05;
        int basalTime = random.nextInt(60);
        int basalDuration = 1 + random.nextInt(30);
        int time = random.nextInt(300);

        List<VaultEntry> meals = Collections.singletonList(new VaultEntry(
                VaultEntryType.MEAL_MANUAL, new Date(mealTime * 60000), mealValue
        ));
        List<VaultEntry> boli = Collections.singletonList(new VaultEntry(
                VaultEntryType.BOLUS_NORMAL, new Date(bolusTime * 60000), bolusValue
        ));
        List<VaultEntry> basals = Collections.singletonList(new VaultEntry(
                VaultEntryType.BASAL_PROFILE, new Date(basalTime * 60000), basalValue, basalDuration
        ));

        double expected = Predictions.deltaBGC(time - mealTime, sens, carbRatio, mealValue, absorptionTime)
                + Predictions.deltaBGI(time - bolusTime, bolusValue, sens, insDuration, peak)
                + Predictions.deltatempBGI(time - basalTime, basalValue, sens, insDuration, peak, 0, basalDuration);

        assertEquals(0, Predictions.predict(0, meals, boli, basals, sens, insDuration, carbRatio, absorptionTime, peak));

        assertEquals(expected, Predictions.predict(time * 60000, meals, boli, basals, sens, insDuration, carbRatio, absorptionTime, peak), 0.1);
    }

    @Test
    public void testCummulativeMealPredict() {
        double sens = 35;
        double carb = 10;
        int absTime = 120;
        int peak = 55;
        int insDur = 180;
        int timestamp = 15 * 60 * 1000;
        int value = 50;

        double delta = 1e-7;

        List<VaultEntry> testMeals = new ArrayList<>();
        List<VaultEntry> boli = new ArrayList<>();
        List<VaultEntry> basals = new ArrayList<>();
        testMeals.add(new VaultEntry(VaultEntryType.MEAL_MANUAL, TimestampUtils.createCleanTimestamp(new Date(timestamp)), value));
        RealVector mealTimes = new ArrayRealVector();
        mealTimes = mealTimes.append(timestamp / 60000);
        RealVector mealValues = new ArrayRealVector();
        mealValues = mealValues.append(value);

        RealVector timesV = new ArrayRealVector();

        double[] predictResults = new double[50];
        double[] cmpResults;

        for (int i = 0; i < 50; i++) {
            timesV = timesV.append(i * 5);
            double d = Predictions.predict(i * 5 * 60 * 1000, testMeals, boli, basals, sens, insDur, carb, absTime, peak);
            predictResults[i] = d;
        }

        RealVector cmp = Predictions.cumulativeMealPredict(timesV, mealTimes, mealValues, sens, carb, absTime);
        cmpResults = cmp.toArray();

        assertArrayEquals(cmpResults, predictResults, delta);
    }

    @Test
    public void jacobiTest(){
        RealVector mealValues = new ArrayRealVector(new double[]{30.0, 20.0, 10.0});
        RealVector mealTimes = new ArrayRealVector(new double[]{0, 40, 100});
        RealVector testTimes = new ArrayRealVector(new double[]{10, 50, 120});

        RealMatrix result = Predictions.jacobi(testTimes,mealTimes,mealValues,sens,carbRatio,absorptionTime);

        assertEquals(-7.0/24.0, result.getEntry(0,0),1e-15);
        assertEquals(0, result.getEntry(0,1));
        assertEquals(0, result.getEntry(0,2));
        assertEquals(-35.0/24.0, result.getEntry(1,0), 1e-15);
        assertEquals(-7.0/36.0, result.getEntry(1,1), 1e-15);
        assertEquals(0, result.getEntry(1,2));
        assertEquals(0, result.getEntry(2,0));
        assertEquals(-7.0/9.0, result.getEntry(2,1), 1e-15);
        assertEquals(-7.0/36.0, result.getEntry(2,2), 1e-15);

        assertEquals(7.0/144.0, result.getEntry(0,3),1e-15);
        assertEquals(0, result.getEntry(0,4));
        assertEquals(0, result.getEntry(0,5));
        assertEquals(175.0/144.0, result.getEntry(1,3), 1e-15);
        assertEquals(7.0/144.0, result.getEntry(1,4), 1e-15);
        assertEquals(0, result.getEntry(1,5));
        assertEquals(35.0/10.0, result.getEntry(2,3));
        assertEquals(49.0/18.0, result.getEntry(2,4), 1e-15);
        assertEquals(7.0/36.0, result.getEntry(2,5), 1e-15);
    }

    @Test
    public void randomJacobiTest() {
        int sizeTimes = 3 + random.nextInt(3);
        RealVector testTimes = new ArrayRealVector(sizeTimes);

        int sizeMeals = 1 + random.nextInt(3);
        RealVector mealValues = new ArrayRealVector(sizeMeals);
        RealVector mealTimes = new ArrayRealVector(sizeMeals);

        int oldDate = 0;
        for (int i = 0; i < sizeTimes; i++) {
            int newDate = 1 + random.nextInt(120) + oldDate;
            testTimes.setEntry(i, newDate);
            oldDate = newDate;
        }
        oldDate = 0;

        for (int i = 0; i < sizeMeals; i++) {
            int newDate = 1 + random.nextInt(120) + oldDate;
            mealTimes.setEntry(i, newDate);
            mealValues.setEntry(i, 1 + random.nextInt(50));
            oldDate = newDate;
        }
        RealMatrix result = Predictions.jacobi(testTimes, mealTimes, mealValues, sens, carbRatio, absorptionTime);
        for (int i = 0; i < sizeMeals; i++) {
            RealVector r = result.getColumnVector(i);
            assertEquals(sizeTimes, r.getDimension());
            for (int j = 0; j < sizeTimes; j++) {
                assertEquals(cob_dtMeal(testTimes.getEntry(j), mealTimes.getEntry(i), mealValues.getEntry(i), sens, carbRatio, absorptionTime), r.getEntry(j));
            }
        }
        for (int i = 0; i < sizeMeals; i++) {
            RealVector r = result.getColumnVector(i + sizeMeals);
            assertEquals(sizeTimes, r.getDimension());
            for (int j = 0; j < sizeTimes; j++) {
                assertEquals(sens / carbRatio * Predictions.carbsOnBoard(testTimes.getEntry(j) - mealTimes.getEntry(i), absorptionTime), r.getEntry(j));
            }
        }
    }

    private static double cob_dtMeal(double time, double mealTime, double carbsAmount, double insSensitivityFactor, double carbRatio, long absorptionTime) {
        double c = insSensitivityFactor / carbRatio * carbsAmount * 4 / absorptionTime;
        double deltaTime = time - mealTime;
        if (deltaTime < 0 || deltaTime > absorptionTime) {
            return 0;
        } else if (deltaTime < absorptionTime / 2.0) {
            return -c * deltaTime / absorptionTime;
        } else if (deltaTime >= absorptionTime / 2.0) {
            return c * (deltaTime / absorptionTime - 1);
        }
        return 0;
    }
}
