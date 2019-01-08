package de.opendiabetes.synchronizer;


import de.opendiabetes.nsapi.NSApi;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class SynchronizerTest {
    private static Synchronizer synchronizer;

    @BeforeAll
    static void setUp() {
        String readHost = System.getenv("NS_HOST");
        String readSecret = System.getenv("NS_APISECRET");
        String writeHost = System.getenv("NS_HOST_2");
        String writeSecret = System.getenv("NS_APISECRET_2");
        if (readHost == null)
            System.err.println("Environment variable NS_HOST not found!");
        if (readSecret == null)
            System.err.println("Environment variable NS_APISECRET not found!");
        if (writeHost == null)
            System.err.println("Environment variable NS_HOST_2 not found!");
        if (writeSecret == null)
            System.err.println("Environment variable NS_APISECRET_2 not found!");
        if (readHost == null || readSecret == null || writeHost == null || writeSecret == null)
            fail("");

        NSApi read = new NSApi(readHost, readSecret);
        NSApi write = new NSApi(writeHost, writeSecret);
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
