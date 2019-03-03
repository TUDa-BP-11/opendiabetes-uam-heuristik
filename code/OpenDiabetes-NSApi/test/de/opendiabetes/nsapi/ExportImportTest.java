package de.opendiabetes.nsapi;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.opendiabetes.nsapi.exception.NightscoutDataException;
import de.opendiabetes.nsapi.exporter.NightscoutExporter;
import de.opendiabetes.nsapi.exporter.NightscoutExporterOptions;
import de.opendiabetes.nsapi.importer.NightscoutImporter;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.util.SortVaultEntryByDate;
import de.opendiabetes.vault.util.TimestampUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class ExportImportTest {
    private static NightscoutExporter exporter;
    private static NightscoutImporter importer;

    @BeforeAll
    public static void setup() {
        importer = new NightscoutImporter();
        exporter = new NightscoutExporter();
    }

    /**
     * Tests weather the timestamp of a parsed vault entry remains the same after exporting and importing it
     *
     * @param type the type of the vault entry. All four currently implemented types are tested
     */
    @ParameterizedTest
    @EnumSource(value = VaultEntryType.class, names = {"GLUCOSE_CGM", "BOLUS_NORMAL", "MEAL_MANUAL", "BASAL_MANUAL"})
    public void testTimezones(VaultEntryType type) throws IOException {
        Date date = TimestampUtils.createCleanTimestamp(new Date());
        VaultEntry entry = new VaultEntry(type, date, 10, 10);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        exporter.exportData(output, Collections.singletonList(entry));
        output.close();

        ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
        List<VaultEntry> entries = importer.importData(input);
        input.close();

        assertEquals(1, entries.size());
        VaultEntry parsed = entries.get(0);
        assertEquals(date, parsed.getTimestamp());
    }

    @Test
    public void testImport() throws IOException {
        String data = "[" +
                "{\"type\":\"sgv\",\"sgv\":80.0,\"date\":1550689782007,\"dateString\":\"2019-02-20T19:09:42.007Z\",\"direction\":\"\",\"trend\":\"\",\"device\":\"\"}," +
                "{\"type\":\"sgv\",\"sgv\":90.0,\"date\":1550689782008,\"dateString\":\"2019-02-20T19:09:42.008Z\",\"direction\":\"\",\"trend\":\"\",\"device\":\"\"}," +
                "{\"eventType\":\"Correction Bolus\",\"insulin\":10.0,\"created_at\":\"2019-02-20T19:09:42Z\",\"timestamp\":\"2019-02-20T19:09:42Z\",\"programmed\":10.0,\"duration\":0,\"type\":\"normal\",\"unabsorbed\":0,\"enteredBy\":\"UAMALGO\"}," +
                "{\"eventType\":\"Correction Bolus\",\"insulin\":20.0,\"created_at\":\"2019-02-20T19:09:42Z\",\"timestamp\":\"2019-02-20T19:09:42Z\",\"programmed\":20.0,\"duration\":0,\"type\":\"normal\",\"unabsorbed\":0,\"enteredBy\":\"UAMALGO\"}," +
                "{\"eventType\":\"Meal Bolus\",\"carbs\":200.0,\"absorptionTime\":120,\"created_at\":\"2019-02-20T19:09:42Z\",\"timestamp\":\"2019-02-20T19:09:42Z\",\"enteredBy\":\"UAMALGO\"}," +
                "{\"eventType\":\"Meal Bolus\",\"carbs\":250.0,\"absorptionTime\":120,\"created_at\":\"2019-02-20T19:09:42Z\",\"timestamp\":\"2019-02-20T19:09:42Z\",\"enteredBy\":\"UAMALGO\"}," +
                "{\"eventType\":\"Temp Basal\",\"rate\":0.8,\"duration\":30.0,\"created_at\":\"2019-02-20T19:09:42Z\",\"timestamp\":\"2019-02-20T19:09:42Z\",\"temp\":\"absolute\",\"absolute\":0.8,\"enteredBy\":\"UAMALGO\"}," +
                "{\"eventType\":\"Temp Basal\",\"rate\":0.6348762378,\"duration\":20.0,\"created_at\":\"2019-02-20T19:09:42Z\",\"timestamp\":\"2019-02-20T19:09:42Z\",\"temp\":\"absolute\",\"absolute\":0.6348762378,\"enteredBy\":\"UAMALGO\"}" +
                "]";
        ByteArrayInputStream stream = new ByteArrayInputStream(data.getBytes());
        List<VaultEntry> entries = importer.importData(stream);
        stream.close();
        assertEquals(8, entries.size());
    }

    @Test
    public void testExport() throws IOException {
        Date date = new Date();
        List<VaultEntry> entries = Arrays.asList(
                new VaultEntry(VaultEntryType.GLUCOSE_CGM, date, 80),
                new VaultEntry(VaultEntryType.GLUCOSE_CGM, new Date(date.getTime() - 5 * 60 * 1000), 90),
                new VaultEntry(VaultEntryType.BOLUS_NORMAL, new Date(date.getTime() - 10 * 60 * 1000), 10),
                new VaultEntry(VaultEntryType.BOLUS_NORMAL, new Date(date.getTime() - 15 * 60 * 1000), 20),
                new VaultEntry(VaultEntryType.MEAL_MANUAL, new Date(date.getTime() - 20 * 60 * 1000), 200),
                new VaultEntry(VaultEntryType.MEAL_MANUAL, new Date(date.getTime() - 25 * 60 * 1000), 250),
                new VaultEntry(VaultEntryType.BASAL_MANUAL, new Date(date.getTime() - 30 * 60 * 1000), 0.8, 30),
                new VaultEntry(VaultEntryType.BASAL_MANUAL, new Date(date.getTime() - 35 * 60 * 1000), 0.6348762378, 20)
        );
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        // check that order is enforced
        assertThrows(NightscoutDataException.class, () -> exporter.exportData(stream, entries.stream()
                .sorted(new SortVaultEntryByDate())
                .collect(Collectors.toList())));
        exporter.exportData(stream, entries);
        stream.close();

        InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(stream.toByteArray()));
        JsonElement element = new JsonParser().parse(reader);
        reader.close();

        assertTrue(element.isJsonArray());
        JsonArray array = element.getAsJsonArray();
        assertEquals(entries.size(), array.size());
    }

    @Test
    public void testExportMerge() throws IOException {
        Date date = new Date();
        List<VaultEntry> entries = Arrays.asList(
                new VaultEntry(VaultEntryType.GLUCOSE_CGM, date, 80),
                // first mergeable entry
                new VaultEntry(VaultEntryType.BOLUS_NORMAL, new Date(date.getTime() - 10 * 1000), 10),
                // has to merge with
                new VaultEntry(VaultEntryType.MEAL_MANUAL, new Date(date.getTime() - 20 * 1000), 200),
                // not mergeable
                new VaultEntry(VaultEntryType.BASAL_MANUAL, new Date(date.getTime() - 30 * 1000), 0.8, 30),
                // more then 60 seconds after first mergeable entry
                new VaultEntry(VaultEntryType.MEAL_MANUAL, new Date(date.getTime() - 71 * 1000), 250)
        );

        // test that merge occurs
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        exporter.exportData(stream, entries);
        stream.close();
        InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(stream.toByteArray()));
        JsonElement element = new JsonParser().parse(reader);
        reader.close();

        assertTrue(element.isJsonArray());
        JsonArray array = element.getAsJsonArray();
        assertEquals(entries.size() - 1, array.size());

        // test with smaller merge window
        NightscoutExporter newExporter = new NightscoutExporter(new NightscoutExporterOptions(5));
        stream = new ByteArrayOutputStream();
        newExporter.exportData(stream, entries);
        stream.close();
        reader = new InputStreamReader(new ByteArrayInputStream(stream.toByteArray()));
        element = new JsonParser().parse(reader);
        reader.close();

        assertTrue(element.isJsonArray());
        array = element.getAsJsonArray();
        assertEquals(entries.size(), array.size());

        // check that trying to merge two bolus or two meal events triggers an exception
        assertThrows(NightscoutDataException.class, () -> exporter.exportData(new ByteArrayOutputStream(), Arrays.asList(
                new VaultEntry(VaultEntryType.BOLUS_NORMAL, date, 10),
                new VaultEntry(VaultEntryType.BOLUS_NORMAL, date, 12),
                new VaultEntry(VaultEntryType.MEAL_MANUAL, date, 250)
        )));
        assertThrows(NightscoutDataException.class, () -> exporter.exportData(new ByteArrayOutputStream(), Arrays.asList(
                new VaultEntry(VaultEntryType.BOLUS_NORMAL, date, 10),
                new VaultEntry(VaultEntryType.MEAL_MANUAL, date, 250),
                new VaultEntry(VaultEntryType.MEAL_MANUAL, date, 180)
        )));
    }

    @Test
    public void testExportType() throws IOException {
        VaultEntry entry = new VaultEntry(VaultEntryType.STRESS, new Date(), 10);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        assertThrows(NightscoutDataException.class, () -> exporter.exportData(stream, Collections.singletonList(entry)));
        stream.close();
    }

    @Test
    public void testExportIOError() {
        VaultEntry entry = new VaultEntry(VaultEntryType.GLUCOSE_CGM, new Date(), 10);
        assertThrows(NightscoutDataException.class, () -> exporter.exportData(new ExceptionOutputStream(), Collections.singletonList(entry)));
    }

    @Test
    public void testImportIOError() {
        assertThrows(NightscoutDataException.class, () -> importer.importData(new ExceptionInputStream()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "[{}",  // invalid syntax
            "{}",   // not an array
            "[{}]", // invalid entry type (no type)
            "[{\"eventType\":\"Meal Bolus\",\"carbs\":\"invalid\",\"absorptionTime\":120,\"created_at\":\"2019-02-20T19:09:42Z\",\"timestamp\":\"2019-02-20T19:09:42Z\",\"enteredBy\":\"UAMALGO\"}]",    // invalid carbs type (not double)
            "[{\"eventType\":\"Meal Bolus\",\"carbs\":200.0,\"absorptionTime\":120,\"created_at\":\"2019-02-20T19:09:42Z\",\"timestamp\":\"invalid date\",\"enteredBy\":\"UAMALGO\"}]"  // invalid timestamp
    })
    public void testImportData(String data) throws IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream(data.getBytes());
        assertThrows(NightscoutDataException.class, () -> importer.importData(stream));
        stream.close();
    }

    private static class ExceptionOutputStream extends ByteArrayOutputStream {
        @Override
        public void close() throws IOException {
            throw new IOException();
        }
    }

    private static class ExceptionInputStream extends InputStream {
        @Override
        public int read() throws IOException {
            throw new IOException();
        }
    }
}
