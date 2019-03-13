package de.opendiabetes.vault.nsapi;

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
import java.util.Collections;
import java.util.Date;
import java.util.List;

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
}
