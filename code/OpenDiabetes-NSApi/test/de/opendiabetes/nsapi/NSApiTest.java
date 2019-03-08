package de.opendiabetes.nsapi;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.opendiabetes.nsapi.exception.NightscoutIOException;
import de.opendiabetes.nsapi.exception.NightscoutServerException;
import de.opendiabetes.parser.Profile;
import de.opendiabetes.parser.Status;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class NSApiTest {
    private static NSApi api;

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
    }

    @AfterAll
    static void tearDown() throws IOException {
        api.close();
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
        String id = getRandomId(24);
        int sgv = 70 + new Random().nextInt(60);
        Date date = new Date();

        VaultEntry entry = new VaultEntry(VaultEntryType.GLUCOSE_CGM, date, sgv);
        api.postEntries(Collections.singletonList(entry));

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        List<VaultEntry> entries = api.getEntries().find("dateString").eq(formatter.format(date)).getVaultEntries();
        assertEquals(1, entries.size());
        assertEquals(VaultEntryType.GLUCOSE_CGM, entries.get(0).getType());
        assertEquals(sgv, entries.get(0).getValue());
    }

    @Test
    void testTreatments() throws NightscoutIOException, NightscoutServerException {
        String id = getRandomId(24);
        int carbs = 80 + new Random().nextInt(100);
        Date date = new Date();

        VaultEntry treatment = new VaultEntry(VaultEntryType.MEAL_MANUAL, date, carbs);
        api.postTreatments(Collections.singletonList(treatment));

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        List<VaultEntry> entries = api.getTreatments().find("created_at").eq(formatter.format(date)).getVaultEntries();
        assertEquals(1, entries.size());
        assertEquals(VaultEntryType.MEAL_MANUAL, entries.get(0).getType());
        assertEquals(carbs, entries.get(0).getValue());
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
        List<VaultEntry> entries = api.getEntries().count(20).find("dateString").lt(Instant.now().toString()).getVaultEntries();
        // assume that there are at least 3 entries
        assumeTrue(entries.size() > 3);

        // set latest to second entry
        LocalDateTime latest = LocalDateTime.ofInstant(entries.get(1).getTimestamp().toInstant(), ZoneId.of("UTC"));
        // set oldest to second to last entry
        LocalDateTime oldest = LocalDateTime.ofInstant(entries.get(entries.size() - 2).getTimestamp().toInstant(), ZoneId.of("UTC"));

        List<VaultEntry> between = api.getEntries(latest, oldest, 20);
        // test that exactly 2 entries less are returned (first and last from original request should be missing)
        assertEquals(entries.size() - 2, between.size());
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

    private final static String ID_RANGE = "0123456789abcdef";

    private static String getRandomId(int length) {
        Random random = new Random();
        StringBuilder id = new StringBuilder();
        for (int i = 0; i < length; i++)
            id.append(ID_RANGE.charAt(random.nextInt(ID_RANGE.length())));
        return id.toString();
    }
}