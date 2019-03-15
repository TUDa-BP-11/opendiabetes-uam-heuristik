package de.opendiabetes.vault.synchronizer;

import com.google.gson.JsonArray;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.nsapi.NSApi;
import de.opendiabetes.vault.nsapi.exception.NightscoutIOException;
import de.opendiabetes.vault.nsapi.exception.NightscoutServerException;
import de.opendiabetes.vault.util.TimestampUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Why there are sleeps in here:
 * The Nightscout api is as a Node.JS express server that serves its data from an underlying MongoDB database. It seems
 * to utilize multiple threads to accomplish this and the different threads don't always know of changes that were just
 * made in another thread. This is noticeable here, as sending data to Nightscout and then requesting it immediately
 * afterwards sometimes returns the inserted data and sometimes it does not. In any case the data gets saved to the
 * database eventually so we are using {@link Thread#sleep(long)} here to minimize the risk of these tests failing
 * because of multithreading issues.
 */
public class SynchronizerTest {
    private static NSApi read;
    private static NSApi write;

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

        read = new NSApi(readHost, readSecret);
        write = new NSApi(writeHost, writeSecret);
    }

    @AfterAll
    static void tearDown() throws IOException {
        read.close();
        write.close();
    }

    @Test
    void testMissingEntry() throws NightscoutIOException, NightscoutServerException, InterruptedException {
        Synchronizer synchronizer = new Synchronizer(read, write, ZonedDateTime.now().minus(6, ChronoUnit.HOURS), ZonedDateTime.now(), 100);
        Synchronizable sync = new Synchronizable("entries", "dateString");
        synchronizer.findMissing(sync);
        int found = sync.getFindCount();
        int missing = sync.getMissingCount();
        // see class doc
        synchronizer.getReadApi().postEntries(Collections.singletonList(
                new VaultEntry(VaultEntryType.GLUCOSE_CGM, TimestampUtils.createCleanTimestamp(new Date(System.currentTimeMillis() - 60 * 60 * 1000)), 80)
        ));
        // see class doc
        Thread.sleep(5000);
        synchronizer.findMissing(sync);

        assertEquals(found + 1, sync.getFindCount());
        assertEquals(missing + 1, sync.getMissingCount());

        JsonArray missingEntries = sync.getMissing();
        synchronizer.postMissing(sync);

        List<VaultEntry> posted = synchronizer.getWriteApi().getEntries()
                .find("dateString").eq(missingEntries.get(0).getAsJsonObject().get("dateString").getAsString())
                .getVaultEntries();
        assertEquals(1, posted.size());
    }

    @Test
    void testMissingTreatments() throws NightscoutIOException, NightscoutServerException, InterruptedException {
        Synchronizer synchronizer = new Synchronizer(read, write);
        Synchronizable sync = new Synchronizable("treatments", "created_at");
        synchronizer.findMissing(sync);
        int found = sync.getFindCount();
        int missing = sync.getMissingCount();
        // see class doc
        synchronizer.getReadApi().postTreatments(Collections.singletonList(
                new VaultEntry(VaultEntryType.BOLUS_NORMAL, TimestampUtils.createCleanTimestamp(new Date(System.currentTimeMillis() - 60 * 60 * 1000)), 4)
        ));
        // see class doc
        Thread.sleep(5000);
        synchronizer.findMissing(sync);

        assertEquals(found + 1, sync.getFindCount());
        assertEquals(missing + 1, sync.getMissingCount());

        JsonArray missingTreatments = sync.getMissing();
        synchronizer.postMissing(sync);

        List<VaultEntry> posted = synchronizer.getWriteApi().getTreatments()
                .find("created_at").eq(missingTreatments.get(0).getAsJsonObject().get("created_at").getAsString())
                .getVaultEntries();
        assertEquals(1, posted.size());
    }
}
