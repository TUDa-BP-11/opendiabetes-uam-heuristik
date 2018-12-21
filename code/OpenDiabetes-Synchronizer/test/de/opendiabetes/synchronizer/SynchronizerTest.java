package de.opendiabetes.synchronizer;


import de.opendiabetes.nsapi.NSApi;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class SynchronizerTest {
    private static Synchronizer synchronizer;

    @BeforeAll
    static void setUp() {
        String readHost, readPort, readSecret, writeHost, writePort, writeSecret;

        try (InputStream input = new FileInputStream("resources/config.properties")) {
            Properties properties = new Properties();
            properties.load(input);

            readHost = properties.getProperty("read.host");
            readPort = properties.getProperty("read.port");
            readSecret = properties.getProperty("read.secret");
            writeHost = properties.getProperty("write.host");
            writePort = properties.getProperty("write.port");
            writeSecret = properties.getProperty("write.secret");
        } catch (IOException e) {
            fail(e);
            return;
        }

        NSApi read = new NSApi(readHost, readPort, readSecret);
        NSApi write = new NSApi(writeHost, writePort, writeSecret);
        synchronizer = new Synchronizer(read, write, "1970-01-01", null, 100);
    }

    @AfterAll
    static void tearDown() {
        synchronizer.close();
    }

    @Test
    void testMissingEntries() {
        synchronizer.findMissingEntries();
        int missing = synchronizer.getMissingEntriesCount();
        synchronizer.postMissingEntries();
        synchronizer.findMissingEntries();
        int missingNew = synchronizer.getMissingEntriesCount();
        assertEquals(0, missingNew);
    }

    @Test
    void testMissingTreatments() {
        synchronizer.findMissingTreatments();
        int missing = synchronizer.getMissingTreatmentsCount();
        synchronizer.postMissingTreatments();
        synchronizer.findMissingTreatments();
        int missingNew = synchronizer.getMissingTreatmentsCount();
        assertEquals(0, missingNew);
    }
}
