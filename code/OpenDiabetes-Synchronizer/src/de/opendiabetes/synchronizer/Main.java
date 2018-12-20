package de.opendiabetes.synchronizer;

import com.mashape.unirest.http.exceptions.UnirestException;
import de.opendiabetes.nsapi.GetBuilder;
import de.opendiabetes.nsapi.NSApi;
import org.json.JSONArray;
import org.json.JSONObject;

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
            if (start == null || !timepattern.matcher(start).find())
                throw new IllegalArgumentException("start date missing or invalid");
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

        GetBuilder getBuilder;
        JSONArray found = new JSONArray();
        JSONArray missing = new JSONArray();
        int fc = 0;
        try {
            do {
                getBuilder = read.getEntries().count(count);

                if (found.length() == 0) {  // first search
                    getBuilder.find("dateString").gte(start);
                } else {    // next search, start after oldest found entry
                    String last = found.getJSONObject(0).getString("dateString");
                    getBuilder.find("dateString").gt(last);
                }

                if (end != null)
                    getBuilder.find("dateString").lt(end);

                found = getBuilder.get().getArray();
                fc += found.length();

                for (int i = 0; i < found.length(); i++) {
                    JSONObject entry = found.getJSONObject(i);
                    String date = entry.getString("dateString");
                    JSONArray target = write.getEntries().find("dateString").eq(date).get().getArray();
                    if (target.length() == 0)    //no result found
                        missing.put(entry);
                }
            } while (found.length() != 0);
        } catch (UnirestException e) {
            System.out.println("Exception while trying to compile list of missing entries");
            e.printStackTrace();
            read.close();
            write.close();
            return;
        }

        System.out.println("Found " + fc + " entries of which " + missing.length() + " are missing in the target instance.");
        if (missing.length() > 0) {
            System.out.println("Uploading missing entries...");
            try {
                write.postEntries(missing.toString());
            } catch (UnirestException e) {
                System.out.println("Exception while trying to POST missing entries");
                e.printStackTrace();
            }
        }

        read.close();
        write.close();
        System.out.println("Done!");
    }

    private static String getValue(String arg, String[] args, int i) {
        if (i < args.length - 1)
            return args[i + 1];
        throw new IllegalArgumentException("Missing value after argument " + arg);
    }
}
