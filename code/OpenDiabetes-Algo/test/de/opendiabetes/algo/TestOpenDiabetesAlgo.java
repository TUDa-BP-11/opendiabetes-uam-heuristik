package de.opendiabetes.algo;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestOpenDiabetesAlgo {
    private static OpenDiabetesAlgo algo;
    private static double t, tEnd;

    @BeforeAll
    public static void setup() {
        algo = new OpenDiabetesAlgo();
        Random random = new Random();
        tEnd = random.nextDouble() * 100;
        t = random.nextDouble() * tEnd;
    }

    @Test
    public void testDeltaBGC() {
        double sens = algo.getInsulinSensitivity();
        double carb = algo.getCarbRatio();

        double x = 32;
        double expected = (sens / carb) * x * Algorithm.carbsOnBoard(t, tEnd);
        assertEquals(expected, algo.deltaBGC(t, sens, carb, x, tEnd));
    }

    @Test
    public void testDeltaBGI() {
        double sens = algo.getInsulinSensitivity();
        double insDuration = algo.getInsulinDuration();

        double y = 3;
        double expected = -1 * y * sens * (1 - algo.fastActingIob(t, insDuration));     //TODO fastActingIOB vs getIOBWeight / 100D ?
        assertEquals(expected, algo.deltaBGI(t, y, sens, insDuration));
    }
}
