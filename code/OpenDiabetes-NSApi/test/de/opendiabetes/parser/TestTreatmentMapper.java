package de.opendiabetes.parser;

import de.opendiabetes.vault.engine.container.VaultEntry;
import de.opendiabetes.vault.engine.container.VaultEntryType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestTreatmentMapper {

    private final long ONE_MINUTE = 60 * 1000;

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


        List<VaultEntry> result = TreatmentMapper.adjustBasalTreatments(testList);
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
        List<Date> dateList = new ArrayList<>();
        Random random = new Random();
        int size = 1 + random.nextInt(10);
        for (int i = 0; i < size; i++) {
            Date date = new Date(i * 20 * ONE_MINUTE);
            dateList.add(date);
            VaultEntry entry = new VaultEntry(VaultEntryType.BASAL_MANUAL, date, random.nextDouble() * 100, 60);
            testList.add(entry);
        }

        List<VaultEntry> resultList = TreatmentMapper.adjustBasalTreatments(new ArrayList<>(testList));
        assertEquals(testList.size(), resultList.size());
        for (int i = 0; i < size - 1; i++) {
            assertEquals(testList.get(i).getValue2() * 20 / 60, resultList.get(i).getValue2());
            assertEquals(testList.get(i).getValue() * 20 / 60, resultList.get(i).getValue());
        }
        assertEquals(testList.get(size - 1).getValue2(), resultList.get(size - 1).getValue2());
        assertEquals(testList.get(size - 1).getValue(), resultList.get(size - 1).getValue());
    }

    @Test
    public void testRandomDuration() {
        List<VaultEntry> testList = new ArrayList<>();
        List<Date> dateList = new ArrayList<>();
        Random random = new Random();
        int size = 1 + random.nextInt(30);
        Date date = new Date(0);
        for (int i = 0; i < size; i++) {
            date = new Date(date.getTime() + ((1 + random.nextInt(12)) * 5 * ONE_MINUTE)); // in steps of 5 min
            dateList.add(date);
            VaultEntry entry = new VaultEntry(VaultEntryType.BASAL_MANUAL, date, random.nextDouble() * 100, random.nextInt(6) * 10);
            testList.add(entry);
        }

        List<VaultEntry> resultList = TreatmentMapper.adjustBasalTreatments(new ArrayList<>(testList));
        assertEquals(testList.size(), resultList.size());

        for (int i = 0; i < size - 1; i++) {
            long deltaTime = ((testList.get(i + 1).getTimestamp().getTime() - testList.get(i).getTimestamp().getTime()) / ONE_MINUTE);
            if (testList.get(i).getValue2() > deltaTime) {
                assertEquals(deltaTime, resultList.get(i).getValue2());
                assertEquals(testList.get(i).getValue() * resultList.get(i).getValue2() / testList.get(i).getValue2(), resultList.get(i).getValue());
            } else {
                assertEquals(testList.get(i).getValue2(), resultList.get(i).getValue2());
                assertEquals(testList.get(i).getValue(), resultList.get(i).getValue());
            }
        }
        assertEquals(testList.get(size - 1).getValue2(), resultList.get(size - 1).getValue2());
        assertEquals(testList.get(size - 1).getValue(), resultList.get(size - 1).getValue());

    }
}
