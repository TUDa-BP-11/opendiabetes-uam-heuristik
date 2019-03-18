package de.opendiabetes.vault.main;

import de.opendiabetes.vault.main.math.Filter;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestFilter {
    private static final long ONE_MINUTE = 60 * 1000;

    @Test
    public void testAverage() {

        List<VaultEntry> testList = new ArrayList<>();
        testList.add(new VaultEntry(VaultEntryType.GLUCOSE_CGM, new Date(0), 1));
        testList.add(new VaultEntry(VaultEntryType.GLUCOSE_CGM, new Date(5 * ONE_MINUTE), 2));
        testList.add(new VaultEntry(VaultEntryType.GLUCOSE_CGM, new Date(10 * ONE_MINUTE), 3));
        testList.add(new VaultEntry(VaultEntryType.GLUCOSE_CGM, new Date(15 * ONE_MINUTE), 4));
        testList.add(new VaultEntry(VaultEntryType.GLUCOSE_CGM, new Date(20 * ONE_MINUTE), 5));
        double result = Filter.getAverage(testList, 0, 5);
        assertEquals(2, result);
        result = Filter.getAverage(testList, 1, 5);
        assertEquals(2.5,result);
        result = Filter.getAverage(testList, 2, 5);
        assertEquals(3,result);
        result = Filter.getAverage(testList, 3, 5);
        assertEquals(3.5,result);
        result = Filter.getAverage(testList, 4, 5);
        assertEquals(4,result);
    }

    @Test
    public void testAverageMaxTimeDiff() {
        List<VaultEntry> testList = new ArrayList<>();
        testList.add(new VaultEntry(VaultEntryType.GLUCOSE_CGM, new Date(0), 1));
        testList.add(new VaultEntry(VaultEntryType.GLUCOSE_CGM, new Date(30 * ONE_MINUTE), 2));
        testList.add(new VaultEntry(VaultEntryType.GLUCOSE_CGM, new Date(60 * ONE_MINUTE), 3));
        testList.add(new VaultEntry(VaultEntryType.GLUCOSE_CGM, new Date(90 * ONE_MINUTE), 4));
        testList.add(new VaultEntry(VaultEntryType.GLUCOSE_CGM, new Date(120 * ONE_MINUTE), 5));

        double result = Filter.getAverage(testList, 0, 5, 30);
        assertEquals(1.5, result);
        result = Filter.getAverage(testList, 1, 5, 30 );
        assertEquals(2,result);
        result = Filter.getAverage(testList, 2, 5,30);
        assertEquals(3,result);
        result = Filter.getAverage(testList, 3, 5,30);
        assertEquals(4,result);
        result = Filter.getAverage(testList, 4, 5,30);
        assertEquals(4.5,result);
    }

    @Test
    public void testMedian(){
        List<VaultEntry> testList = new ArrayList<>();
        testList.add(new VaultEntry(VaultEntryType.GLUCOSE_CGM, new Date(0), 1));
        testList.add(new VaultEntry(VaultEntryType.GLUCOSE_CGM, new Date(5 * ONE_MINUTE), 2));
        testList.add(new VaultEntry(VaultEntryType.GLUCOSE_CGM, new Date(10 * ONE_MINUTE), 3));
        testList.add(new VaultEntry(VaultEntryType.GLUCOSE_CGM, new Date(15 * ONE_MINUTE), 4));
        testList.add(new VaultEntry(VaultEntryType.GLUCOSE_CGM, new Date(20 * ONE_MINUTE), 5));
        double result = Filter.getMedian(testList,0,5);
        assertEquals(2,result);
        result = Filter.getMedian(testList,1,5);
        assertEquals(3,result);
        result = Filter.getMedian(testList,2,5);
        assertEquals(3,result);
        result = Filter.getMedian(testList,3,5);
        assertEquals(4,result);
        result = Filter.getMedian(testList,4,5);
        assertEquals(4,result);
    }

    @Test
    public void testMedianMaxTimeDiff(){
        List<VaultEntry> testList = new ArrayList<>();
        testList.add(new VaultEntry(VaultEntryType.GLUCOSE_CGM, new Date(0), 1));
        testList.add(new VaultEntry(VaultEntryType.GLUCOSE_CGM, new Date(30 * ONE_MINUTE), 2));
        testList.add(new VaultEntry(VaultEntryType.GLUCOSE_CGM, new Date(60 * ONE_MINUTE), 3));
        testList.add(new VaultEntry(VaultEntryType.GLUCOSE_CGM, new Date(90 * ONE_MINUTE), 4));
        testList.add(new VaultEntry(VaultEntryType.GLUCOSE_CGM, new Date(120 * ONE_MINUTE), 5));

        double result = Filter.getMedian(testList, 0, 5, 30);
        assertEquals(2, result);
        result = Filter.getMedian(testList, 1, 5, 30);
        assertEquals(2, result);
        result = Filter.getMedian(testList, 2, 5, 30);
        assertEquals(3, result);
        result = Filter.getMedian(testList, 3, 5, 30);
        assertEquals(4, result);
        result = Filter.getMedian(testList, 4, 5, 30);
        assertEquals(5, result);

    }


}
