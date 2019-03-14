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
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

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
        Synchronizer synchronizer = new Synchronizer(read, write);
        Synchronizable sync = new Synchronizable("entries", "dateString");
        synchronizer.findMissing(sync);
        int found = sync.getFindCount();
        int missing = sync.getMissingCount();
        synchronizer.getReadApi().postEntries(Collections.singletonList(
                // set one minute in the past to prevent issues with clocks out of sync
                new VaultEntry(VaultEntryType.GLUCOSE_CGM, TimestampUtils.createCleanTimestamp(new Date(System.currentTimeMillis() - 60 * 1000)), 80)
        ));
        // sleep to give Nightscout a chance to sort out its cache
        Thread.sleep(1000);
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
        synchronizer.getReadApi().postTreatments(Collections.singletonList(
                // set one minute in the past to prevent issues with clocks out of sync
                new VaultEntry(VaultEntryType.BOLUS_NORMAL, TimestampUtils.createCleanTimestamp(new Date(System.currentTimeMillis() - 60 * 1000)), 4)
        ));
        // sleep to give Nightscout a chance to sort out its cache
        Thread.sleep(1000);
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
