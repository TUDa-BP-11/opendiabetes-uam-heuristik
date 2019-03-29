package de.opendiabetes.vault.main.math;

import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;

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

        assertEquals(0, Predictions.predict(0, meals, boli, basals, sens, insDuration, carbRatio, absorptionTime, peak), 0.1);

        assertEquals(expected, Predictions.predict(time * 60000, meals, boli, basals, sens, insDuration, carbRatio, absorptionTime, peak), 0.1);
    }

}
