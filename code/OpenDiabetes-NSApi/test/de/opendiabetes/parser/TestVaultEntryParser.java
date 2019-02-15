package de.opendiabetes.parser;


import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import org.junit.jupiter.api.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class TestVaultEntryParser {

    @Test
    public void emptyArray() {
        VaultEntryParser parser = new VaultEntryParser();
        List<VaultEntry> entries = parser.parse(entryString(0, null, null, null));

        assertTrue(entries.isEmpty());
    }

    @Test
    public void randomEntries() {
        Random random = new Random();
        int size = 1 + random.nextInt(4);
        int[] values = new int[size];
        long[] time = new long[size];
        String[] timeStrings = new String[size];
        long startTime = 1000000000000L;

        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));

        for (int i = 0; i < size; i++) {
            values[i] = 70 + random.nextInt(60);
            time[i] = startTime + random.nextInt(3000) * 1000L;
            timeStrings[i] = formatter.format(time[i]);
        }

        String testString = entryString(size, values, time, timeStrings);
        VaultEntryParser parser = new VaultEntryParser();
        List<VaultEntry> entries = parser.parse(testString);

        assertEquals(size, entries.size());
        for (int i = 0; i < size; i++) {
            VaultEntry entry = entries.get(i);

            assertEquals(values[i], entry.getValue());
            assertEquals(time[i], entry.getTimestamp().getTime());
            assertEquals(VaultEntryType.GLUCOSE_CGM, entry.getType());
            assertEquals(VaultEntry.VALUE_UNUSED, entry.getValue2());
        }
        assertEquals(entries, parser.parse(parser.toJson(entries)));
    }

    @Test
    public void randomTreatments() {
        Random random = new Random();
        int size = 1 + random.nextInt(4);
        long startTime = 1000000000000L;
        VaultEntryType[] types = new VaultEntryType[size];
        int[] values = new int[size];
        int[] duration = new int[size];
        long[] time = new long[size];

        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));

        String testString = "[";
        for (int i = 0; i < size; i++) {
            testString += "{";
            time[i] = startTime + random.nextInt(3000) * 1000L;
            testString += "\"timestamp\": \"" + formatter.format(time[i]) + "\",";
            switch (random.nextInt(3)) {
                case 0://Basal
                    types[i] = VaultEntryType.BASAL_MANUAL;
                    testString += "\"eventType\": \"Temp Basal\",";
                    values[i] = 1 + random.nextInt(10);
                    testString += "\"rate\": " + values[i] + ",";
                    duration[i] = random.nextInt(30);
                    testString += "\"duration\": " + duration[i];
                    break;
                case 1://Meal
                    types[i] = VaultEntryType.MEAL_MANUAL;
                    testString += "\"eventType\": \"Meal Bolus\",";
                    values[i] = 1 + random.nextInt(50);
                    testString += "\"carbs\": " + values[i];
                    break;
                case 2://Bolus
                    types[i] = VaultEntryType.BOLUS_NORMAL;
                    testString += "\"eventType\": \"Correction Bolus\",";
                    values[i] = 1 + random.nextInt(5);
                    testString += "\"insulin\": " + values[i];
                    break;
                default:
                    break;
            }
            testString += "},";
        }
        testString = testString.substring(0, testString.length() - 1);
        testString += "]";

        VaultEntryParser parser = new VaultEntryParser();
        List<VaultEntry> treatments = parser.parse(testString);

        assertEquals(size, treatments.size());
        for (int i = 0; i < size; i++) {
            VaultEntry treatment = treatments.get(i);
            assertEquals(types[i], treatment.getType());
            assertEquals(time[i], treatment.getTimestamp().getTime());
            assertEquals(values[i], treatment.getValue());
            if (treatment.getType().equals(VaultEntryType.BASAL_MANUAL)) {
                assertEquals(duration[i], treatment.getValue2());
            } else {
                assertEquals(VaultEntry.VALUE_UNUSED, treatment.getValue2());
            }
        }
        assertEquals(treatments, parser.parse(parser.toJson(treatments)));
    }

    @Test
    public void tripleTreatment() {
        String testString = "[{\"timestamp\": \"2019-01-01T00:00:00Z\","
                + "\"insulin\": 1,"
                + "\"carbs\": 10,"
                + "\"eventType\": \"Temp Basal\","
                + "\"rate\": 1,"
                + "\"duration\": 30"
                + "}]";
        VaultEntryParser parser = new VaultEntryParser();
        List<VaultEntry> entries = parser.parse(testString);
        assertEquals(3, entries.size());
        assertEquals(VaultEntryType.BOLUS_NORMAL, entries.get(0).getType());
        assertEquals(VaultEntryType.MEAL_MANUAL, entries.get(1).getType());
        assertEquals(VaultEntryType.BASAL_MANUAL, entries.get(2).getType());
    }

    private String entryString(int size, int[] values, long[] time, String[] timestamps) {
        String result = "[";
        if (size > 0) {
            result += singleEntry(values[0], time[0], timestamps[0]);
            for (int i = 1; i < size; i++) {
                result += ",";
                result += singleEntry(values[i], time[i], timestamps[i]);
            }
        }
        result += "]";
        return result;
    }

    private String singleEntry(int value, long time, String timestamp) {
        String result = "{";
        result += "\"type\": \"sgv\",";
        result += "\"sgv\": " + value + ",";
        result += "\"dateString\": \"" + timestamp + "\",";
        result += "\"date\": " + time;
        result += "}";
        return result;
    }
}
