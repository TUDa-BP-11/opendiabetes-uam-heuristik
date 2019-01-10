package de.opendiabetes.synchronizer;

import de.opendiabetes.nsapi.NSApi;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) {
        String configpath = "config.properties";
        String start = null;
        String end = null;
        String batchsize = null;
        int count = 100;
        boolean debug = false;
        try {
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.startsWith("-")) {
                    arg = arg.substring(1);
                    switch (arg) {
                        case "config":
                            configpath = getValue(arg, args, i);
                            i++;
                            break;
                        case "start":
                            start = getValue(arg, args, i);
                            i++;
                            break;
                        case "end":
                            end = getValue(arg, args, i);
                            i++;
                            break;
                        case "n":
                        case "batch":
                        case "batchsize":
                            batchsize = getValue(arg, args, i);
                            i++;
                            break;
                        case "debug":
                            debug = true;
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown argument " + arg);
                    }
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
            if (end == null)
                end = LocalDateTime.now().toString();
            if (batchsize != null) {
                try {
                    count = Integer.parseInt(batchsize);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid batchsize " + batchsize);
                }
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Error while parsing arguments: " + e.getMessage());
            if (debug)
                e.printStackTrace();
            return;
        }

        String readHost, readSecret, writeHost, writeSecret;

        try (InputStream input = new FileInputStream(configpath)) {
            Properties properties = new Properties();
            properties.load(input);

            readHost = properties.getProperty("read.host");
            readSecret = properties.getProperty("read.secret");
            writeHost = properties.getProperty("write.host");
            writeSecret = properties.getProperty("write.secret");
        } catch (FileNotFoundException e) {
            System.out.println("Cannot find config file at " + configpath);
            if (debug)
                e.printStackTrace();
            return;
        } catch (IOException e) {
            System.out.println("Exception while reading config file: " + e.getMessage());
            if (debug)
                e.printStackTrace();
            return;
        }

        NSApi read = new NSApi(readHost, readSecret);
        NSApi write = new NSApi(writeHost, writeSecret);

        Synchronizer synchronizer = new Synchronizer(read, write, start, end, count);
        synchronizer.setDebug(debug);
        Synchronizable entries = new Synchronizable("entries", "dateString");
        Synchronizable treatments = new Synchronizable("treatments", "created_at");
        Synchronizable status = new Synchronizable("devicestatus", "created_at");

        try {
            synchronizer.findMissing(entries);
            System.out.println("Found " + entries.getFindCount() + " entries of which " + entries.getMissingCount() + " are missing in the target instance.");
            if (entries.getMissingCount() > 0)
                synchronizer.postMissing(entries);
            synchronizer.findMissing(treatments);
            System.out.println("Found " + treatments.getFindCount() + " treatments of which " + treatments.getMissingCount() + " are missing in the target instance.");
            if (treatments.getMissingCount() > 0)
                synchronizer.postMissing(treatments);
            synchronizer.findMissing(status);
            System.out.println("Found " + status.getFindCount() + " devicestatus of which " + status.getMissingCount() + " are missing in the target instance.");
            if (status.getMissingCount() > 0)
                synchronizer.postMissing(status);
        } catch (SynchronizerException e) {
            System.out.println(e.getMessage());
            if (debug)
                e.printStackTrace();
        }
        synchronizer.close();
        System.out.println("Done!");
    }

    private static String getValue(String arg, String[] args, int i) {
        if (i < args.length - 1)
            return args[i + 1];
        throw new IllegalArgumentException("Missing value after argument " + arg);
    }
}
