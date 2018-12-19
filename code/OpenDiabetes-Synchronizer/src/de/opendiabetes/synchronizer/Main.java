package de.opendiabetes.synchronizer;

import de.opendiabetes.nsapi.NSApi;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) {
        String configpath = "config.properties";
        String start = null;
        String end = null;
        try {
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.startsWith("-")) {
                    arg = arg.substring(1);
                    switch (arg) {
                        case "config":
                            configpath = getValue(arg, args, i);
                            break;
                        case "start":
                            start = getValue(arg, args, i);
                            break;
                        case "end":
                            end = getValue(arg, args, i);
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown argument " + arg);
                    }
                    i++;
                } else if (start == null) {
                    start = arg;
                } else if (end == null) {
                    end = arg;
                } else throw new IllegalArgumentException("Unknown argument " + arg);
            }
            Pattern timepattern = Pattern.compile("^[0-9]{4}-[0-9]{2}-[0-9]{2}(T[0-9]{2}:[0-9]{2}(:[0-9]{2}(.[0-9]{3})?)?)?$");
            if (start == null || !timepattern.matcher(start).find())
                throw new IllegalArgumentException("start date missing or invalid");
            if (end != null && !timepattern.matcher(end).find())
                throw new IllegalArgumentException("end date invalid");
        } catch (IllegalArgumentException e) {
            System.out.println("Error while parsing arguments: " + e.getMessage());
            return;
        }

        String readHost, readPort, readToken, writeHost, writePort, writeToken;

        try (InputStream input = new FileInputStream(configpath)) {
            Properties properties = new Properties();
            properties.load(input);

            readHost = properties.getProperty("read.host");
            readPort = properties.getProperty("read.port");
            readToken = properties.getProperty("read.token");
            writeHost = properties.getProperty("write.host");
            writePort = properties.getProperty("write.port");
            writeToken = properties.getProperty("write.token");
        } catch (FileNotFoundException e) {
            System.out.println("Cannot find config file at " + configpath);
            return;
        } catch (IOException e) {
            System.out.println("Exception while reading config file: ");
            e.printStackTrace();
            return;
        }

        NSApi read = new NSApi(readHost, readPort, readToken);
        NSApi write = new NSApi(writeHost, writePort, writeToken);

        //TODO: do the work

        read.close();
        write.close();
    }

    private static String getValue(String arg, String[] args, int i) {
        if (i < args.length - 1)
            return args[i + 1];
        throw new IllegalArgumentException("Missing value after argument " + arg);
    }
}
