package de.opendiabetes.synchronizer;

import com.google.gson.*;
import de.opendiabetes.vault.nsapi.GetBuilder;
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
        JsonParser parser = new JsonParser();
        JsonArray found = new JsonArray();
        JsonArray missing = new JsonArray();
        int fc = 0;
        do {
            getBuilder = read.getEntries().count(count);

            if (found.size() == 0) {  // first search
                getBuilder.find("dateString").gte(start);
            } else {    // next search, start at last found entry
                String last = found.get(found.size() - 1).getAsJsonObject().get("dateString").getAsString();
                getBuilder.find("dateString").gt(last);
            }

            if (end != null)
                getBuilder.find("dateString").lt(end);

            found = parser.parse(getBuilder.get()).getAsJsonArray();
            fc += found.size();

            for (JsonElement e : found) {
                JsonObject entry = e.getAsJsonObject();
                String date = entry.get("dateString").getAsString();
                String target = write.getEntries().find("dateString").eq(date).get();
                if (target.equals("[]"))    //no result found
                    missing.add(entry);
            }
        } while (found.size() != 0);

        System.out.println("Found " + fc + " entries of which " + missing.size() + " are missing in the target instance.");
        if (missing.size() > 0) {
            System.out.println("Uploading missing entries...");
            Gson gson = new Gson();
            write.postEntries(gson.toJson(missing));
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
