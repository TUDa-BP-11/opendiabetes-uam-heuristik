package de.opendiabetes.vault.main;

import de.opendiabetes.vault.main.math.Predictions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Random;

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

        double y = 3;
        double expected = -1 * y * sens * (1 - Predictions.fastActingIob(t, insDuration));     //TODO fastActingIOB vs getIOBWeight / 100D ?
        assertEquals(expected, Predictions.deltaBGI(t, y, sens, insDuration));
    }
}
