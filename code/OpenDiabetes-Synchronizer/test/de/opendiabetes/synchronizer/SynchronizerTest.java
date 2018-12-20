package de.opendiabetes.synchronizer;


import de.opendiabetes.nsapi.NSApi;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class SynchronizerTest {
    private static Synchronizer synchronizer;

    @BeforeAll
    static void setUp() {
        String readHost, readPort, readToken, writeHost, writePort, writeToken;

        try (InputStream input = new FileInputStream("resources/config.properties")) {
            Properties properties = new Properties();
            properties.load(input);

            readHost = properties.getProperty("read.host");
            readPort = properties.getProperty("read.port");
            readToken = properties.getProperty("read.token");
            writeHost = properties.getProperty("write.host");
            writePort = properties.getProperty("write.port");
            writeToken = properties.getProperty("write.token");
        } catch (IOException e) {
            fail(e);
            return;
        }

        NSApi read = new NSApi(readHost, readPort, readToken);
        NSApi write = new NSApi(writeHost, writePort, writeToken);
        synchronizer = new Synchronizer(read, write, "1970-01-01", null, 100);
    }

    @AfterAll
    static void tearDown() {
        synchronizer.close();
    }

    @Test
    void testMissing() {
        synchronizer.findMissing();
        int missing = synchronizer.getMissingCount();
        synchronizer.postMissing();
        synchronizer.findMissing();
        int missingNew = synchronizer.getMissingCount();
        assertEquals(0, missingNew);
    }
}
