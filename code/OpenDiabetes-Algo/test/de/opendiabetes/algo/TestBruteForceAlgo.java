package de.opendiabetes.algo;

import de.opendiabetes.vault.engine.container.VaultEntry;
import de.opendiabetes.vault.engine.container.VaultEntryType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TestBruteForceAlgo {
    private static BruteForceAlgo algo;

    @BeforeAll
    public static void setup() {
        algo = new BruteForceAlgo();
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
        algo.setGlucoseMeasurements(glucose);
        List<VaultEntry> meals = algo.calculateMeals();
        for (VaultEntry meal : meals) {
            System.out.println("+Meal " + meal.getTimestamp().toString() + ": " + meal.getValue());
        }
    }
}
