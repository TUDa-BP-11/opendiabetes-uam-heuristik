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
        String batchsize = null;
        int count = 100;
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
                        case "batch":
                        case "batchsize":
                            batchsize = getValue(arg, args, i);
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
            if (start != null && !timepattern.matcher(start).find())
                throw new IllegalArgumentException("start date missing or invalid");
            if (start == null)
                start = "1970-01-01";
            if (end != null && !timepattern.matcher(end).find())
                throw new IllegalArgumentException("end date invalid");
            if (batchsize != null) {
                try {
                    count = Integer.parseInt(batchsize);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid batchsize " + batchsize);
                }
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Error while parsing arguments: " + e.getMessage());
            return;
        }

        String readHost, readPort, readSecret, writeHost, writePort, writeSecret;

        try (InputStream input = new FileInputStream(configpath)) {
            Properties properties = new Properties();
            properties.load(input);

            readHost = properties.getProperty("read.host");
            readPort = properties.getProperty("read.port");
            readSecret = properties.getProperty("read.secret");
            writeHost = properties.getProperty("write.host");
            writePort = properties.getProperty("write.port");
            writeSecret = properties.getProperty("write.secret");
        } catch (FileNotFoundException e) {
            System.out.println("Cannot find config file at " + configpath);
            return;
        } catch (IOException e) {
            System.out.println("Exception while reading config file: ");
            e.printStackTrace();
            return;
        }

        NSApi read = new NSApi(readHost, readPort, readSecret);
        NSApi write = new NSApi(writeHost, writePort, writeSecret);

        Synchronizer synchronizer = new Synchronizer(read, write, start, end, count);

        synchronizer.findMissingEntries();
        System.out.println("Found " + synchronizer.getFindEntriesCount() + " entries of which " + synchronizer.getMissingEntriesCount() + " are missing in the target instance.");
        synchronizer.postMissingEntries();
        synchronizer.findMissingTreatments();
        System.out.println("Found " + synchronizer.getFindTreatmentsCount() + " treatments of which " + synchronizer.getMissingTreatmentsCount() + " are missing in the target instance.");
        synchronizer.postMissingTreatments();
        synchronizer.close();
        System.out.println("Done!");
    }

    private static String getValue(String arg, String[] args, int i) {
        if (i < args.length - 1)
            return args[i + 1];
        throw new IllegalArgumentException("Missing value after argument " + arg);
    }
}
