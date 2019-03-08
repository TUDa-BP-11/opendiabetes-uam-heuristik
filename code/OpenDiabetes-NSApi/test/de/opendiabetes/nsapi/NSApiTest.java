package de.opendiabetes.nsapi;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.opendiabetes.nsapi.exception.NightscoutIOException;
import de.opendiabetes.nsapi.exception.NightscoutServerException;
import de.opendiabetes.parser.Profile;
import de.opendiabetes.parser.Status;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.util.TimestampUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class NSApiTest {
    private static NSApi api;
    private static Random random;

    @BeforeAll
    static void setUp() {
        String host = System.getenv("NS_HOST");
        String secret = System.getenv("NS_APISECRET");
        if (host == null)
            System.err.println("Environment variable NS_HOST not found!");
        if (secret == null)
            System.err.println("Environment variable NS_APISECRET not found!");
        if (host == null || secret == null)
            fail("");

        api = new NSApi(host, secret);
        random = new Random();
    }

    @AfterAll
    static void tearDown() throws IOException {
        api.close();
    }

    /**
     * Creates a supplier that produces dates in descending order.
     *
     * @param start time of first date
     * @param step  time between dates
     */
    private Supplier<Date> createDateSupplier(long start, long step) {
        return new Supplier<Date>() {
            long current = start;

            @Override
            public Date get() {
                Date date = TimestampUtils.createCleanTimestamp(new Date(current));
                current -= step;
                return date;
            }
        };
    }

    @Test
    void testStatus() throws NightscoutIOException, NightscoutServerException {
        Status status = api.getStatus();
        assertNotNull(status);
        assertTrue(status.isStatusOk());      // tests should break if status is not ok
        assertTrue(status.isApiEnabled());    // tests should break if api is not enabled
    }

    @Test
    void testEntries() throws NightscoutIOException, NightscoutServerException {
        // start somewhere in the last month
        long start = System.currentTimeMillis() - (1 + (random.nextInt(30 * 24)) * 60 * 60 * 1000);
        Supplier<Date> dates = createDateSupplier(start, 5 * 60 * 1000);
        // generate 10 random CGM measurements
        List<VaultEntry> entries = random.ints(10, 40, 300)
                .mapToObj(i -> new VaultEntry(VaultEntryType.GLUCOSE_CGM, dates.get(), i))
                .collect(Collectors.toList());

        // split into three batches
        api.postEntries(entries, 4);

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        GetBuilder builder = api.getEntries()
                .find("dateString").lte(formatter.format(entries.get(0).getTimestamp()))
                .find("dateString").gte(formatter.format(entries.get(entries.size() - 1).getTimestamp()))
                .count(100);     // in case there are other random entries from previous tests in this time
        List<VaultEntry> newEntries = builder.getVaultEntries();

        // assert all entries are found (or more)
        assertTrue(newEntries.size() >= entries.size());
        if (newEntries.size() > entries.size()) {
            // if more entries are found test them individually
            for (VaultEntry e : entries) {
                assertTrue(newEntries.contains(e));
            }
        } else {
            // else the collections have to be equal
            assertIterableEquals(entries, newEntries);
        }
    }

    @Test
    void testTreatments() throws NightscoutIOException, NightscoutServerException {
        // start somewhere in the last month
        long start = System.currentTimeMillis() - (1 + (random.nextInt(30 * 24)) * 60 * 60 * 1000);
        Supplier<Date> dates = createDateSupplier(start, 5 * 60 * 1000);
        // generate 10 random CGM measurements
        List<VaultEntry> treatments = random.ints(10, 50, 1000)
                .mapToObj(i -> new VaultEntry(VaultEntryType.MEAL_MANUAL, dates.get(), i))
                .collect(Collectors.toList());

        // split into three batches
        api.postTreatments(treatments, 4);

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        GetBuilder builder = api.getTreatments()
                .find("created_at").lte(formatter.format(treatments.get(0).getTimestamp()))
                .find("created_at").gte(formatter.format(treatments.get(treatments.size() - 1).getTimestamp()))
                .count(100);     // in case there are other random treatments from previous tests in this time
        List<VaultEntry> newTreatments = builder.getVaultEntries();

        // assert all entries are found (or more)
        assertTrue(newTreatments.size() >= treatments.size());
        if (newTreatments.size() > treatments.size()) {
            // if more entries are found test them individually
            for (VaultEntry e : treatments) {
                assertTrue(newTreatments.contains(e));
            }
        } else {
            // else the collections have to be equal
            assertIterableEquals(treatments, newTreatments);
        }
    }

    @Test
    void testEchoTimes() throws NightscoutIOException, NightscoutServerException {
        String prefix = "foo";
        String regex = "bar";
        JsonObject echo = api.getTimesEcho(prefix, regex).getRaw().getAsJsonObject();
        assertTrue(echo.has("req"));
        assertTrue(echo.getAsJsonObject("req").has("params"));
        assertEquals(prefix, echo.getAsJsonObject("req").getAsJsonObject("params").get("prefix").getAsString());
        assertEquals(regex, echo.getAsJsonObject("req").getAsJsonObject("params").get("regex").getAsString());
    }

    @Test
    void testTimes() throws NightscoutIOException, NightscoutServerException {
        List<VaultEntry> entries = api.getTimes("2019", "T{15..17}:.*").getVaultEntries();
        entries.stream()
                .map(e -> LocalDateTime.ofInstant(e.getTimestamp().toInstant(), ZoneId.of("UTC")))
                .forEach(t -> assertTrue(t.getHour() >= 15 && t.getHour() <= 17));
    }

    @Test
    void testProfile() throws NightscoutIOException, NightscoutServerException {
        Profile profile = api.getProfile();
        assertNotNull(profile);
        assertTrue(profile.getCarbratio() >= 0);
        assertTrue(profile.getSensitivity() >= 0);
        assertNotNull(profile.getBasalProfiles());
        assertFalse(profile.getBasalProfiles().isEmpty());
        assertNotNull(profile.getTimezone());
    }

    @Test
    void testEntriesBetween() throws NightscoutIOException, NightscoutServerException {
        List<VaultEntry> entries = api.getEntries()
                .find("dateString").lt(Instant.now().minus(1, ChronoUnit.HOURS).toString())
                .count(20)
                .getVaultEntries();
        // assume that there are at least 3 entries
        assumeTrue(entries.size() > 3);

        // set latest to second entry
        ZonedDateTime latest = ZonedDateTime.ofInstant(entries.get(1).getTimestamp().toInstant(), ZoneId.of("UTC"));
        // set oldest to second to last entry
        ZonedDateTime oldest = ZonedDateTime.ofInstant(entries.get(entries.size() - 2).getTimestamp().toInstant(), ZoneId.of("UTC"));

        List<VaultEntry> between = api.getEntries(latest, oldest, entries.size() / 2);
        // test that exactly 2 entries less are returned (first and last from original request should be missing)
        assertEquals(entries.size() - 2, between.size());
    }

    @Test
    void testTreatmentsBetween() throws NightscoutIOException, NightscoutServerException {
        List<VaultEntry> treatments = api.getTreatments()
                .find("created_at").lt(Instant.now().minus(1, ChronoUnit.HOURS).toString())
                .count(20)
                .getVaultEntries();
        // assume that there are at least 3 treatments
        assumeTrue(treatments.size() > 3);

        // set latest to second entry
        ZonedDateTime latest = ZonedDateTime.ofInstant(treatments.get(1).getTimestamp().toInstant(), ZoneId.of("UTC"));
        // set oldest to second to last entry
        ZonedDateTime oldest = ZonedDateTime.ofInstant(treatments.get(treatments.size() - 2).getTimestamp().toInstant(), ZoneId.of("UTC"));

        List<VaultEntry> between = api.getTreatments(latest, oldest, treatments.size() / 2);
        // test that exactly 2 entries less are returned (first and last from original request should be missing)
        assertEquals(treatments.size() - 2, between.size());
    }

    @Test
    void test404() {
        assertThrows(NightscoutServerException.class, () -> api.createGet("this is an invalid path").getRaw());
    }

    @Test
    void testDataCursor() throws NightscoutIOException, NightscoutServerException {
        ZonedDateTime latest = ZonedDateTime.now();
        ZonedDateTime oldest = ZonedDateTime.parse("2017-01-01T00:00:00.000Z");
        int batchSize = 20 + (int) (Math.random() * 100);
        List<VaultEntry> expected = api.getEntries(latest, oldest, batchSize);
        DataCursor cursor = new DataCursor(api, "entries", "dateString", latest, oldest, batchSize / 2);
        List<JsonObject> actual = new ArrayList<>();
        while (cursor.hasNext()) {
            actual.add(cursor.next());
        }
        assertEquals(expected.size(), actual.size());
    }

    @Test
    void testSplit() {
        Random random = new Random();
        int size = 50 + random.nextInt(100);
        int batchSize = size / (2 + random.nextInt(3));

        // test with list of random integers
        List<Integer> list = random.ints(size).boxed().collect(Collectors.toList());
        List<List<Integer>> partitions = NSApi.split(list, batchSize);
        List<Integer> newList = new ArrayList<>();
        partitions.forEach(newList::addAll);
        assertIterableEquals(list, newList);

        // test with json array of random integers
        JsonArray array = random.ints(size).collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
        List<JsonArray> arrayPartitions = NSApi.split(array, batchSize);
        JsonArray newArray = new JsonArray();
        arrayPartitions.forEach(newArray::addAll);
        assertIterableEquals(array, newArray);
    }

    @Test
    void testGetZonedDatetime() throws NightscoutIOException {
        ZonedDateTime now = LocalDateTime.now().atZone(ZoneId.of("UTC"));

        // test that all information is kept
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        assertEquals(now.toInstant(), NSApi.getZonedDateTime(formatter.format(now)).toInstant());

        // test that timezone is restored to UTC
        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        assertEquals(now, NSApi.getZonedDateTime(formatter.format(now)));

        // remove milliseconds, should be set to 0
        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        now = now.minus(now.get(ChronoField.MILLI_OF_SECOND), ChronoUnit.MILLIS);
        assertEquals(now, NSApi.getZonedDateTime(formatter.format(now)));

        // remove seconds, should be set to 0
        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        now = now.minus(now.get(ChronoField.SECOND_OF_MINUTE), ChronoUnit.SECONDS);
        assertEquals(now, NSApi.getZonedDateTime(formatter.format(now)));

        // time is missing, should throw exception
        String broken = DateTimeFormatter.ISO_DATE.format(ZonedDateTime.now());
        assertThrows(NightscoutIOException.class, () -> NSApi.getZonedDateTime(broken));
    }
}