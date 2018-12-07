package de.opendiabetes.uam;

import de.opendiabetes.vault.nsapi.NSApi;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Main {
    /**
     * The main thread doing the work
     */
    private static MainControl mainControl;

    /**
     * The input thread parsing commands
     */
    private static Input input;

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

        NSApi api1 = new NSApi(host, port, token);
        NSApi api2 = new NSApi(host, port, token);
        mainControl = new MainControl(api1, api2);
        mainControl.start();

        input = new Input();
        input.run();
    }


    public static MainControl getMainControl() {
        return mainControl;
    }

}
