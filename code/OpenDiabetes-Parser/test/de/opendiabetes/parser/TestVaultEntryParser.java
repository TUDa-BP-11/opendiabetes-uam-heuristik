package de.opendiabetes.parser;


import de.opendiabetes.vault.engine.container.VaultEntry;
import de.opendiabetes.vault.engine.container.VaultEntryType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;


public class TestVaultEntryParser {

    @Test
    public void emptyEntries() {
        VaultEntryParser parser = new VaultEntryParser();
        List<VaultEntry> entries = parser.parse(entryString(0, null, null, null));

        assertTrue(entries.isEmpty());
    }

    @Test
    public void randomEntries() {
        Random random = new Random();
        int size = 1 + random.nextInt(3);
        int[] values = new int[size];
        long[] time = new long[size];
        String[] timeStrings = new String[size];
        long startTime = 1000000000000L;

        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
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

            assertEquals(entry.getValue(), values[i]);
            assertEquals(entry.getTimestamp().getTime(), time[i]);
            assertEquals(entry.getType(), VaultEntryType.GLUCOSE_CGM);
            assertEquals(entry.getValue2(), VaultEntry.VALUE_UNUSED);
        }
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
