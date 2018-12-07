package de.opendiabetes.uam;

import de.opendiabetes.vault.nsapi.NSApi;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Main {

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
    }

}
