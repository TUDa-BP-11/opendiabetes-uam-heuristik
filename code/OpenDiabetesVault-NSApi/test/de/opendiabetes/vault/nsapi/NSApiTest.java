package de.opendiabetes.vault.nsapi;

import com.google.gson.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class NSApiTest {
    private static NSApi api;
    private static Gson gson;
    private static JsonParser parser;

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
        gson = new GsonBuilder().setPrettyPrinting().create();
        parser = new JsonParser();
    }

    @AfterAll
    static void tearDown() {
        api.close();
    }

    @Test
    void status() {
        String status = api.getStatus();
        assertNotNull(status);
        JsonElement jsonStatus = parser.parse(status);
        assertTrue(jsonStatus.isJsonObject());
        JsonObject statusObject = jsonStatus.getAsJsonObject();
        assumeTrue(statusObject.get("status").getAsString().equals("ok"));
    }

    private static void printJson(String jsonString) {
        System.out.println(gson.toJson(parser.parse(jsonString)));
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