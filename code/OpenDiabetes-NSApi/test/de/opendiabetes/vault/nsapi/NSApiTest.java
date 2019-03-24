package de.opendiabetes.vault.nsapi;

import com.google.gson.JsonObject;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.nsapi.exception.NightscoutIOException;
import de.opendiabetes.vault.nsapi.exception.NightscoutServerException;
import de.opendiabetes.vault.parser.Profile;
import de.opendiabetes.vault.parser.Status;
import de.opendiabetes.vault.util.TimestampUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class NSApiTest {

    private static NSApi api;
    private static Random random;

    @BeforeAll
    static void setUp() {
        String host = System.getenv("NS_HOST");
        String secret = System.getenv("NS_APISECRET");
        if (host == null) {
            System.err.println("Environment variable NS_HOST not found!");
        }
        if (secret == null) {
            System.err.println("Environment variable NS_APISECRET not found!");
        }
        if (host == null || secret == null) {
            fail("");
        }
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
     * @param step time between dates
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

        assertTrue(api.checkStatusOk());
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

        SimpleDateFormat format = NSApi.createSimpleDateFormatEntry();
        GetBuilder builder = api.getEntries()
                .find("dateString").lte(format.format(entries.get(0).getTimestamp()))
                .find("dateString").gte(format.format(entries.get(entries.size() - 1).getTimestamp()))
                .count(100);     // in case there are other random entries from previous tests in this time
        List<VaultEntry> newEntries = builder.getVaultEntries();

        // assert all entries are found (or more)
        assertTrue(newEntries.size() >= entries.size());
        if (newEntries.size() > entries.size()) {
            // if more entries are found test them individually
            entries.forEach((e) -> {
                assertTrue(newEntries.contains(e));
            });
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

        SimpleDateFormat format = NSApi.createSimpleDateFormatTreatment();
        GetBuilder builder = api.getTreatments()
                .find("created_at").lte(format.format(treatments.get(0).getTimestamp()))
                .find("created_at").gte(format.format(treatments.get(treatments.size() - 1).getTimestamp()))
                .count(100);     // in case there are other random treatments from previous tests in this time
        List<VaultEntry> newTreatments = builder.getVaultEntries();

        // assert all entries are found (or more)
        assertTrue(newTreatments.size() >= treatments.size());
        if (newTreatments.size() > treatments.size()) {
            // if more entries are found test them individually
            treatments.forEach((e) -> {
                assertTrue(newTreatments.contains(e));
            });
        } else {
            // else the collections have to be equal
            assertIterableEquals(treatments, newTreatments);
        }
    }

    @Test
    void testSlice() throws NightscoutIOException, NightscoutServerException {
        List<VaultEntry> entries = api.getEntries().getVaultEntries();
        assumeFalse(entries.isEmpty());

        // split entries by day to test prefix
        HashMap<LocalDate, List<VaultEntry>> entriesByDay = entries.stream().collect(HashMap::new, (map, entry) -> {
            // get local date of entry
            LocalDate date = LocalDate.from(entry.getTimestamp().toInstant().atZone(ZoneId.of("UTC")));
            // create a list if there isn't one for this date, then add the entry
            map.computeIfAbsent(date, d -> new ArrayList<>()).add(entry);
        }, (m1, m2) -> {
            // for each day in the second list, take all entries
            m2.forEach((date, list) -> m1.compute(date, (k, v) -> {
                // if the first map already has entries at this date, add them
                if (v != null) {
                    list.addAll(v);
                }
                return list;
            }));
        });

        for (Map.Entry<LocalDate, List<VaultEntry>> entry : entriesByDay.entrySet()) {
            LocalDate date = entry.getKey();
            List<VaultEntry> list = entry.getValue();
            int end = list.get(0).getTimestamp().toInstant().atZone(ZoneId.of("UTC")).get(ChronoField.HOUR_OF_DAY);
            int start = list.get(list.size() - 1).getTimestamp().toInstant().atZone(ZoneId.of("UTC")).get(ChronoField.HOUR_OF_DAY);
            String regex = String.format("T{%02d..%02d}", start, end);
            List<VaultEntry> slice = api.getSlice("entries", "dateString", "sgv", date.toString(), regex)
                    .count(list.size() + 10)
                    .getVaultEntries();
            assertIterableEquals(list, slice);
        }
    }

    @ParameterizedTest
    @CsvSource({
        "entries, sgv",
        "treatments, "
    })
    void testEcho(String storage, String spec) throws NightscoutIOException, NightscoutServerException {
        JsonObject echo = api.getEcho(storage, spec).getRaw().getAsJsonObject();
        assertNotNull(echo);
        assertTrue(echo.has("query"));

        assertTrue(echo.has("input"));
        if (spec != null) {
            JsonObject input = echo.getAsJsonObject("input");
            assertTrue(input.has("find"));
            JsonObject find = input.getAsJsonObject("find");
            assertTrue(find.has("type"));
            assertEquals(spec, find.get("type").getAsString());
        }

        assertTrue(echo.has("params"));
        JsonObject params = echo.getAsJsonObject("params");
        assertTrue(params.has("echo"));
        assertEquals(storage, params.get("echo").getAsString());
        if (spec != null) {
            assertTrue(params.has("model"));
            assertEquals(spec, params.get("model").getAsString());
        }

        assertTrue(echo.has("storage"));
        assertEquals(storage, echo.get("storage").getAsString());
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

    /*
    @Test
    void testEntriesBetween() throws NightscoutIOException, NightscoutServerException {
        List<VaultEntry> entries = api.getEntries()
                .find("dateString").lt(Instant.now().minus(1, ChronoUnit.HOURS).toString())
                .count(20)
                .getVaultEntries();
        // assume that there are at least 3 entries
        assumeTrue(entries.size() > 3);

        // get profile for timezone
        Profile profile = api.getProfile();

        // set latest to second entry
        ZonedDateTime latest = ZonedDateTime
                .ofInstant(entries.get(1).getTimestamp().toInstant(), profile.getTimezone())
                .withZoneSameInstant(ZoneId.of("UTC"));
        // set oldest to second to last entry
        ZonedDateTime oldest = ZonedDateTime
                .ofInstant(entries.get(entries.size() - 2).getTimestamp().toInstant(), profile.getTimezone())
                .withZoneSameInstant(ZoneId.of("UTC"));

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
    }*/

    @Test
    void test404() {
        assertThrows(NightscoutServerException.class, () -> api.createGet("this is an invalid path").getRaw());
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60})
    void testDataCursor(int minutesPast) throws NightscoutIOException, NightscoutServerException {
        ZonedDateTime latest = ZonedDateTime.now().minus(minutesPast, ChronoUnit.MINUTES);
        ZonedDateTime oldest = ZonedDateTime.parse("2017-01-01T00:00:00.000Z");
        int batchSize = 20 + (int) (Math.random() * 100);
        List<VaultEntry> expected = api.getEntries(latest, oldest, batchSize);
        DataCursor cursor = new DataCursor(api, "entries", "dateString", latest, oldest, batchSize / 2);
        List<JsonObject> actual = new ArrayList<>();
        while (cursor.hasNext()) {
            actual.add(cursor.next());
        }
        if (expected.size() != actual.size()) {
            Set<Date> dates = new HashSet<>();
            for (VaultEntry e : expected) {
                if (!dates.add(e.getTimestamp())) {
                    System.out.println("Found two entries at the same date: " + e.getTimestamp());
                }
            }

            fail();
        }
        assertEquals(expected.size(), actual.size());
    }
}
