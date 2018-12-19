package de.opendiabetes.vault.nsapi;

import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class NSApiTest {
    private static NSApi api;


    @BeforeAll
    static void setUp() {
        String host, port, token;

        try (InputStream input = new FileInputStream("resources/config.properties")) {
            Properties properties = new Properties();
            properties.load(input);

            host = properties.getProperty("host");
            port = properties.getProperty("port");
            token = properties.getProperty("token");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
            return;
        }

        api = new NSApi(host, port, token);
    }

    @AfterAll
    static void tearDown() {
        api.close();
    }


    @Test
    void testStatus() {
        JSONObject status = null;
        try {
            status = api.getStatus();
        } catch (UnirestException e) {
            fail(e);
        }
        assertNotNull(status);
        assertEquals("ok", status.getString("status"));        // tests should break if status is not ok
        assertTrue(status.getBoolean("apiEnabled"));    // test should break if api is not enabled
    }

    @Test
    void testEntries() {
        String entryId = getRandomId(24);
        int entrySgv = 70 + new Random().nextInt(60);

        LocalDateTime time = LocalDateTime.now();
        long entryDate = Timestamp.valueOf(time).getTime();
        String entryDateString = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(time);

        JsonNode result = null;
        try {
            result = api.postEntries("[{" +
                    "\"direction\": \"Flat\"," +
                    "\"_id\": \"" + entryId + "\"," +
                    "\"sgv\": " + entrySgv + "," +
                    "\"dateString\": \"" + entryDateString + "\"," +
                    "\"date\": " + entryDate + "," +
                    "\"type\": \"sgv\"" +
                    "}]");
        } catch (UnirestException e) {
            fail(e);
        }
        assertNotNull(result);

        JsonNode entries = null;
        try {
            entries = api.getEntries().find("dateString").eq(entryDateString).get();
        } catch (UnirestException e) {
            fail(e);
        }
        assertTrue(entries.isArray());
        JSONArray entryArray = entries.getArray();
        assertEquals(1, entryArray.length());
        JSONObject entry = entryArray.getJSONObject(0);
        assertEquals(entrySgv, entry.getInt("sgv"));
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