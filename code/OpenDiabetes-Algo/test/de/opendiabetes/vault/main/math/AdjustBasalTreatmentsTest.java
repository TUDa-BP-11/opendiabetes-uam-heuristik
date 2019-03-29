package de.opendiabetes.vault.main.math;

import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AdjustBasalTreatmentsTest {

    private static final long ONE_MINUTE = 60 * 1000;
    private static final double DELTA = 1e-15;

    @Test
    public void testSimpleDuration() {
        List<VaultEntry> testList = new ArrayList<>();
        Date date = new Date(0);
        testList.add(new VaultEntry(VaultEntryType.BASAL_MANUAL, date, 10, 60));
        date = new Date(date.getTime() + 30 * ONE_MINUTE);
        testList.add(new VaultEntry(VaultEntryType.BASAL_MANUAL, date, 10, 60));
        date = new Date(date.getTime() + 30 * ONE_MINUTE);
        testList.add(new VaultEntry(VaultEntryType.BASAL_MANUAL, date, 0, 0));
        date = new Date(date.getTime() + 30 * ONE_MINUTE);
        testList.add(new VaultEntry(VaultEntryType.BASAL_MANUAL, date, 30, 30));


        List<VaultEntry> result = BasalCalculatorTools.adjustBasalTreatments(testList);
        assertEquals(4, result.size());
        assertEquals(30, result.get(0).getValue2());
        assertEquals(5, result.get(0).getValue());
        assertEquals(30, result.get(1).getValue2());
        assertEquals(5, result.get(1).getValue());
        assertEquals(0, result.get(2).getValue2());
        assertEquals(0, result.get(2).getValue());
        assertEquals(30, result.get(3).getValue2());
        assertEquals(30, result.get(3).getValue());
    }

    @Test
    public void testRandomValues() {
        List<VaultEntry> testList = new ArrayList<>();
        Random random = new Random();
        int size = 1 + random.nextInt(10);
        for (int i = 0; i < size; i++) {
            Date date = new Date(i * 20 * ONE_MINUTE);
            VaultEntry entry = new VaultEntry(VaultEntryType.BASAL_MANUAL, date, random.nextDouble() * 100, 60);
            testList.add(entry);
        }

        List<VaultEntry> resultList = BasalCalculatorTools.adjustBasalTreatments(new ArrayList<>(testList));
        assertEquals(testList.size(), resultList.size());
        for (int i = 0; i < size - 1; i++) {
            assertEquals(testList.get(i).getValue2() * 20 / 60, resultList.get(i).getValue2());
            assertEquals(testList.get(i).getValue() * 20 / 60, resultList.get(i).getValue(), DELTA);
            assertEquals(testList.get(i).getTimestamp().getTime(), resultList.get(i).getTimestamp().getTime());
        }
        assertEquals(testList.get(size - 1).getValue2(), resultList.get(size - 1).getValue2());
        assertEquals(testList.get(size - 1).getValue(), resultList.get(size - 1).getValue(), DELTA);
        assertEquals(testList.get(size - 1).getTimestamp().getTime(), resultList.get(size - 1).getTimestamp().getTime());
    }

    @Test
    public void testRandomDuration() {
        List<VaultEntry> testList = new ArrayList<>();
        Random random = new Random();
        int size = 1 + random.nextInt(30);
        Date date = new Date(0);
        for (int i = 0; i < size; i++) {
            date = new Date(date.getTime() + ((1 + random.nextInt(12)) * 5 * ONE_MINUTE)); // in steps of 5 min
            VaultEntry entry = new VaultEntry(VaultEntryType.BASAL_MANUAL, date, random.nextDouble() * 100, random.nextInt(6) * 10);
            testList.add(entry);
        }

        List<VaultEntry> resultList = BasalCalculatorTools.adjustBasalTreatments(new ArrayList<>(testList));
        assertEquals(testList.size(), resultList.size());

        for (int i = 0; i < size - 1; i++) {
            long deltaTime = ((testList.get(i + 1).getTimestamp().getTime() - testList.get(i).getTimestamp().getTime()) / ONE_MINUTE);
            if (testList.get(i).getValue2() > deltaTime) {
                assertEquals(deltaTime, resultList.get(i).getValue2());
                assertEquals(testList.get(i).getValue() * resultList.get(i).getValue2() / testList.get(i).getValue2(), resultList.get(i).getValue(), DELTA);
            } else {
                assertEquals(testList.get(i).getValue2(), resultList.get(i).getValue2());
                assertEquals(testList.get(i).getValue(), resultList.get(i).getValue(), DELTA);
            }
            assertEquals(testList.get(i).getTimestamp().getTime(), resultList.get(i).getTimestamp().getTime());
        }
        assertEquals(testList.get(size - 1).getValue2(), resultList.get(size - 1).getValue2());
        assertEquals(testList.get(size - 1).getValue(), resultList.get(size - 1).getValue(), DELTA);
        assertEquals(testList.get(size - 1).getTimestamp().getTime(), resultList.get(size - 1).getTimestamp().getTime());
    }

    @Test
    public void testExceptions() {
        //not sorted
        List<VaultEntry> testList = new ArrayList<>();
        testList.add(new VaultEntry(VaultEntryType.BASAL_MANUAL, new Date(1000000), 2, 30));
        testList.add(new VaultEntry(VaultEntryType.BASAL_MANUAL, new Date(0), 2, 30));

        assertThrows(IllegalArgumentException.class, () -> BasalCalculatorTools.adjustBasalTreatments(testList));

        testList.add(new VaultEntry(VaultEntryType.BASAL_MANUAL, new Date(2000000), 2, 30));
        assertThrows(IllegalArgumentException.class, () -> BasalCalculatorTools.adjustBasalTreatments(testList));

        //wrong type
        List<VaultEntry> testList2 = new ArrayList<>();
        testList2.add(new VaultEntry(VaultEntryType.BOLUS_NORMAL, new Date(0), 2, 30));

        assertThrows(IllegalArgumentException.class, () -> BasalCalculatorTools.adjustBasalTreatments(testList2));

        testList2.add(new VaultEntry(VaultEntryType.BOLUS_NORMAL, new Date(2000000), 2, 30));
        assertThrows(IllegalArgumentException.class, () -> BasalCalculatorTools.adjustBasalTreatments(testList2));
    }
}
