package de.opendiabetes.algo;

import de.opendiabetes.vault.engine.container.VaultEntry;
import de.opendiabetes.vault.engine.container.VaultEntryType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
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
    public void testCob() {
        assertEquals(0, algo.cob(-312, 40));
        assertEquals(1, algo.cob(312, 40));
        if (t <= tEnd / 2) {
            double expected = 2 * (t * t) / (tEnd * tEnd);
            assertEquals(expected, algo.cob(t, tEnd));
        } else {
            double expected = -1 + (4 * t / tEnd) - (2 * (t * t) / (tEnd * tEnd));
            assertEquals(expected, algo.cob(t, tEnd), 1e-14);   //TODO: fix this delta?
        }
    }

    @Test
    public void testDeltaBGC() {
        double sens = algo.getInsSensitivityFactor();
        double carb = algo.getCarbRatio();

        double x = 32;
        double expected = (sens / carb) * x * algo.cob(t, tEnd);
        assertEquals(expected, algo.deltaBGC(t, sens, carb, x, tEnd));
    }

    @Test
    public void testDeltaBGI() {
        double sens = algo.getInsSensitivityFactor();
        int insDuration = algo.getInsDuration();

        double y = 3;
        double expected = -1 * y * sens * (1 - algo.fastActingIob(t, insDuration));     //TODO fastActingIOB vs getIOBWeight / 100D ?
        assertEquals(expected, algo.deltaBGI(t, y, sens, insDuration));
    }

    @Test
    public void testBruteForce() {
        List<VaultEntry> glucose = new ArrayList<>();
        long start = System.currentTimeMillis();
        double bg = 80;
        Random random = new Random();
        for (int i = 0; i < 5; i++) {
            glucose.add(new VaultEntry(VaultEntryType.GLUCOSE_CGM, new Timestamp(start + i * 5 * 60000), bg += random.nextInt(5)));
        }
        for (VaultEntry vaultEntry : glucose) {
            System.out.println("BG " + vaultEntry.getTimestamp().toString() + ": " + vaultEntry.getValue());
        }
        algo.setGlucose(glucose);
        List<VaultEntry> meals = algo.bruteForce();
        for (VaultEntry meal : meals) {
            System.out.println("+Meal " + meal.getTimestamp().toString() + ": " + meal.getValue());
        }
    }
}
