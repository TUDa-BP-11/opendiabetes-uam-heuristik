package de.opendiabetes.vault.main.math;

import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.util.TimestampUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Random;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestPredictions {

    private static double t, tEnd;

    @BeforeAll
    public static void setup() {
        Random random = new Random();
        tEnd = random.nextDouble() * 100;
        t = random.nextDouble() * tEnd;
    }

    @Test
    public void testCob() {
        assertEquals(0, Predictions.carbsOnBoard(-312, 40));
        assertEquals(1, Predictions.carbsOnBoard(312, 40));
        if (t <= tEnd / 2) {
            double expected = 2 * (t * t) / (tEnd * tEnd);
            assertEquals(expected, Predictions.carbsOnBoard(t, tEnd), 1e-12);   //TODO delta fix
        } else {
            double expected = -1 + (4 * t / tEnd) - (2 * (t * t) / (tEnd * tEnd));
            assertEquals(expected, Predictions.carbsOnBoard(t, tEnd), 1e-12);
        }
    }

    @Test
    public void testDeltaBGC() {
        double sens = 35;
        double carb = 10;

        double x = 32;
        double expected = (sens / carb) * x * Predictions.carbsOnBoard(t, tEnd);
        assertEquals(expected, Predictions.deltaBGC(t, sens, carb, x, tEnd));
    }

    @Test
    public void testDeltaBGI() {
        double sens = 35;
        double carb = 10;
        double insDuration = 180;
        double peak = 55;

        double y = 3;
        double expected = -1 * y * sens * (1 - Predictions.fastActingIob(t, insDuration, peak));     //TODO fastActingIOB vs getIOBWeight / 100D ?
        assertEquals(expected, Predictions.deltaBGI(t, y, sens, insDuration, peak));
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
        mealTimes = mealTimes.append(timestamp/60000);
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
}
