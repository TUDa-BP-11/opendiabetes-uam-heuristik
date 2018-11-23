package de.opendiabetes.vault.nsapi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

import java.io.*;
import java.util.Properties;

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
        printJson(api.getStatus());
        printJson(api.getEntries().get());
    }

    private static void printJson(String jsonString) {
        System.out.println(gson.toJson(parser.parse(jsonString)));
    }
}
