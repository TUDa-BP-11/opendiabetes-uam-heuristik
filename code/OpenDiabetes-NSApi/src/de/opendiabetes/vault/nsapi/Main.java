package de.opendiabetes.vault.nsapi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.Random;

public class Main {
    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static JsonParser parser = new JsonParser();

    public static void main(String[] args) {
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

        NSApi api = new NSApi(host, port, token);
        System.out.println("GET status:");
        printJson(api.getStatus());

        System.out.println("POST entries:");
        LocalDateTime time = LocalDateTime.now();

        String entry = "[{" +
                "\"direction\": \"Flat\"," +
                "\"_id\": \"" + getRandomId(24) + "\"," +
                "\"sgv\": 101," +
                "\"dateString\": \"" + time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")) + "\"," +
                "\"date\": " + Timestamp.valueOf(time).getTime() + "," +
                "\"trend\": 4," +
                "\"type\": \"sgv\"," +
                "\"device\": \"share2\"" +
                "}]";

        System.out.println(entry);
//        printJson(api.postEntries(entry));
//
//        System.out.println("GET entries:");
//        printJson(api.getEntries().find("dateString").gt("2018-11-25T18:12:00").get());
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
