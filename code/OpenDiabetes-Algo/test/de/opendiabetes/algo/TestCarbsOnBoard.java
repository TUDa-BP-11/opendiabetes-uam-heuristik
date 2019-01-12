package de.opendiabetes.algo;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCarbsOnBoard {
    private static double t, tEnd;

    @BeforeAll
    public static void setup() {
        Random random = new Random();
        tEnd = random.nextDouble() * 100;
        t = random.nextDouble() * tEnd;
    }

    @Test
    public void testCob() {
        assertEquals(0, Algorithm.carbsOnBoard(-312, 40));
        assertEquals(1, Algorithm.carbsOnBoard(312, 40));
        if (t <= tEnd / 2) {
            double expected = 2 * (t * t) / (tEnd * tEnd);
            assertEquals(expected, Algorithm.carbsOnBoard(t, tEnd));
        } else {
            double expected = -1 + (4 * t / tEnd) - (2 * (t * t) / (tEnd * tEnd));
            assertEquals(expected, Algorithm.carbsOnBoard(t, tEnd), 1e-12);   //TODO: fix this delta?
        }
    }
}
