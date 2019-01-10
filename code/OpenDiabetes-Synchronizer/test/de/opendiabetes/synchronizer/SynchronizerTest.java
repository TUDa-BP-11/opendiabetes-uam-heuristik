package de.opendiabetes.synchronizer;


import de.opendiabetes.nsapi.NSApi;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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

    @ParameterizedTest
    @CsvSource(value = {
            "entries, dateString",
            "treatments, created_at",
            "devicestatus, created_at"
    })
    void testMissing(String apiPath, String dateString) {
        Synchronizable sync = new Synchronizable(apiPath, dateString);
        synchronizer.findMissing(sync);
        int missing = sync.getMissingCount();
        synchronizer.postMissing(sync);
        synchronizer.findMissing(sync);
        int missingNew = sync.getMissingCount();
        assertEquals(0, missingNew);
    }
}
