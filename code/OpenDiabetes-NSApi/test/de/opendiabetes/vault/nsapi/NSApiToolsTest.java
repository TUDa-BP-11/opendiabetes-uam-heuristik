package de.opendiabetes.vault.nsapi;

import com.google.gson.JsonArray;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.nsapi.exception.NightscoutIOException;
import de.opendiabetes.vault.nsapi.exporter.NightscoutExporter;
import de.opendiabetes.vault.util.SortVaultEntryByDate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class NSApiToolsTest {
    private static Path testdata;
    private static Path test1;

    @BeforeAll
    static void setup() {
        Path module = Paths.get("code", "OpenDiabetes-NSApi");
        if (Files.isDirectory(module)) {
            // Test is executed in global directory
            testdata = Paths.get(module.toString(), "testdata");
        } else {
            // Test is executed in module directory
            testdata = Paths.get("testdata");
        }
        if (!Files.isDirectory(testdata))
            fail("Could not find testdata directory!");
        test1 = Paths.get(testdata.toString(), "test1.json");
    }

    @Test
    void testFileNotFound() {
        assertThrows(NightscoutIOException.class, () -> NSApiTools.loadDataFromFile("invalid path"));
    }

    @Test
    void testDirectory() {
        assertThrows(NightscoutIOException.class, () -> NSApiTools.loadDataFromFile(testdata.toString()));
        assertThrows(NightscoutIOException.class, () -> NSApiTools.writeDataToFile(testdata.toString(), Collections.emptyList()));
    }

    @Test
    void testRead() throws NightscoutIOException {
        List<VaultEntry> data = NSApiTools.loadDataFromFile(test1.toString());
        assertEquals(8, data.size());
    }

    @Test
    void testReadAndSort() throws NightscoutIOException {
        List<VaultEntry> data = NSApiTools.loadDataFromFile(test1.toString(), false);
        List<VaultEntry> sorted = NSApiTools.loadDataFromFile(test1.toString(), true);

        data.sort(new SortVaultEntryByDate());
        assertIterableEquals(data, sorted);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "GLUCOSE_CGM",
            "MEAL_MANUAL",
            "BOLUS_NORMAL",
            "BASAL_MANUAL"
    })
    void testReadAndFilter(String typeName) throws NightscoutIOException {
        VaultEntryType type = VaultEntryType.valueOf(typeName);
        Path test1 = Paths.get(testdata.toString(), "test1.json");
        List<VaultEntry> data = NSApiTools.loadDataFromFile(test1.toString(), type, false);
        data.forEach(e -> assertEquals(type, e.getType()));
    }

    @Test
    void testWrite() throws NightscoutIOException, IOException, InterruptedException {
        Path test2 = Paths.get(testdata.toString(), "test2.json");
        if (Files.exists(test2))
            Files.delete(test2);

        List<VaultEntry> data = Collections.singletonList(new VaultEntry(VaultEntryType.GLUCOSE_CGM, new Date(), 100));
        NSApiTools.writeDataToFile(test2.toString(), data);

        assertTrue(Files.exists(test2));

        // check overwrite fails by default
        assertThrows(NightscoutIOException.class, () -> NSApiTools.writeDataToFile(test2.toString(), data));
        // confirm that overwrite works
        FileTime lastModifield = Files.getLastModifiedTime(test2);
        Thread.sleep(1000);
        assertDoesNotThrow(() -> NSApiTools.writeDataToFile(test2.toString(), data, true, new NightscoutExporter()));
        assertTrue(lastModifield.compareTo(Files.getLastModifiedTime(test2)) < 0);
    }

    @Test
    void testSplit() {
        Random random = new Random();
        int size = 50 + random.nextInt(100);
        int batchSize = size / (2 + random.nextInt(3));

        // test with list of random integers
        List<Integer> list = random.ints(size).boxed().collect(Collectors.toList());
        List<List<Integer>> partitions = NSApiTools.split(list, batchSize);
        List<Integer> newList = new ArrayList<>();
        partitions.forEach(newList::addAll);
        assertIterableEquals(list, newList);

        // test with json array of random integers
        JsonArray array = random.ints(size).collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
        List<JsonArray> arrayPartitions = NSApiTools.split(array, batchSize);
        JsonArray newArray = new JsonArray();
        arrayPartitions.forEach(newArray::addAll);
        assertIterableEquals(array, newArray);
    }

    @Test
    void testGetZonedDatetime() throws NightscoutIOException {
        ZonedDateTime now = LocalDateTime.now().atZone(ZoneId.of("UTC"));

        // test that all information is kept
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        assertEquals(now.toInstant(), NSApiTools.getZonedDateTime(formatter.format(now)).toInstant());

        // test that timezone is restored to UTC
        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        assertEquals(now, NSApiTools.getZonedDateTime(formatter.format(now)));

        // remove milliseconds, should be set to 0
        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        assertEquals(
                now.minus(now.get(ChronoField.MILLI_OF_SECOND), ChronoUnit.MILLIS),
                NSApiTools.getZonedDateTime(formatter.format(now))
        );

        // remove milliseconds, should be set to 0, but keep timezone
        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX");
        assertEquals(
                now.minus(now.get(ChronoField.MILLI_OF_SECOND), ChronoUnit.MILLIS).toInstant(),
                NSApiTools.getZonedDateTime(formatter.format(now)).toInstant()
        );

        // remove seconds and milliseconds, should be set to 0
        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        assertEquals(
                now.minus(now.get(ChronoField.SECOND_OF_MINUTE), ChronoUnit.SECONDS)
                        .minus(now.get(ChronoField.MILLI_OF_SECOND), ChronoUnit.MILLIS),
                NSApiTools.getZonedDateTime(formatter.format(now))
        );

        // time is missing, should throw exception
        String broken = DateTimeFormatter.ISO_DATE.format(ZonedDateTime.now());
        assertThrows(NightscoutIOException.class, () -> NSApiTools.getZonedDateTime(broken));
    }
}
