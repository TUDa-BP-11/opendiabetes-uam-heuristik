package de.opendiabetes.nsapi;

import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.exceptions.UnirestException;
import de.opendiabetes.parser.Profile;
import de.opendiabetes.parser.Status;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Random;

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
    void testStatus() throws UnirestException {
        Status status = api.getStatus();
        assertNotNull(status);
        assertTrue(status.isStatusOk());      // tests should break if status is not ok
        assertTrue(status.isApiEnabled());    // tests should break if api is not enabled
    }

    @Test
    void testEntries() throws UnirestException {
        String id = getRandomId(24);
        int sgv = 70 + new Random().nextInt(60);

        Instant time = Instant.now();
        long date = time.toEpochMilli();
        String dateString = time.toString();

        JsonNode result = api.postEntries("[{" +
                "\"direction\": \"Flat\"," +
                "\"_id\": \"" + id + "\"," +
                "\"sgv\": " + sgv + "," +
                "\"dateString\": \"" + dateString + "\"," +
                "\"date\": " + date + "," +
                "\"type\": \"sgv\"" +
                "}]");
        assertNotNull(result);

        List<VaultEntry> entries = api.getEntries().find("dateString").eq(dateString).getVaultEntries();
        assertEquals(1, entries.size());
        assertEquals(VaultEntryType.GLUCOSE_CGM, entries.get(0).getType());
        assertEquals(sgv, entries.get(0).getValue());
    }

    @Test
    void testTreatments() throws UnirestException {
        String id = getRandomId(24);
        int carbs = 80 + new Random().nextInt(100);

        String createdAt = Instant.now().toString();

        JsonNode result = api.postTreatments("[{" +
                "\"_id\": \"" + id + "\"," +
                "\"carbs\": " + carbs + "," +
                "\"created_at\": \"" + createdAt + "\"," +
                "\"timestamp\": \"" + createdAt + "\"" +
                "}]");
        assertNotNull(result);

        List<VaultEntry> entries = api.getTreatments().find("created_at").eq(createdAt).getVaultEntries();
        assertEquals(1, entries.size());
        assertEquals(VaultEntryType.MEAL_MANUAL, entries.get(0).getType());
        assertEquals(carbs, entries.get(0).getValue());
    }

    @Test
    void testEchoTimes() throws UnirestException {
        String prefix = "foo";
        String regex = "bar";
        JSONObject echo = api.getTimesEcho(prefix, regex).get().getObject();
        assertTrue(echo.has("req"));
        assertTrue(echo.getJSONObject("req").has("params"));
        assertEquals(prefix, echo.getJSONObject("req").getJSONObject("params").getString("prefix"));
        assertEquals(regex, echo.getJSONObject("req").getJSONObject("params").getString("regex"));
    }

    @Test
    void testTimes() throws UnirestException {
        List<VaultEntry> entries = api.getTimes("2019", "T{15..17}:.*").getVaultEntries();
        entries.stream()
                .map(e -> LocalDateTime.ofInstant(e.getTimestamp().toInstant(), ZoneId.of("UTC")))
                .forEach(t -> assertTrue(t.getHour() >= 15 && t.getHour() <= 17));
    }

    @Test
    void testProfile() throws UnirestException {
        Profile profile = api.getProfile();
        assertNotNull(profile);
        assertTrue(profile.getCarbratio() >= 0);
        assertTrue(profile.getSensitivity() >= 0);
        assertNotNull(profile.getBasalProfiles());
        assertFalse(profile.getBasalProfiles().isEmpty());
        assertNotNull(profile.getTimezone());
    }

    @Test
    void testEntriesBetween() throws UnirestException {
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

    private final static String ID_RANGE = "0123456789abcdef";

    private static String getRandomId(int length) {
        Random random = new Random();
        StringBuilder id = new StringBuilder();
        for (int i = 0; i < length; i++)
            id.append(ID_RANGE.charAt(random.nextInt(ID_RANGE.length())));
        return id.toString();
    }
}