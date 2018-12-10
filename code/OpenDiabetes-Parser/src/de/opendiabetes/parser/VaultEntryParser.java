package de.opendiabetes.parser;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;

import de.opendiabetes.vault.engine.container.VaultEntry;
import de.opendiabetes.vault.engine.container.VaultEntryType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

public class VaultEntryParser {


    public VaultEntryParser() {

    }

    public List<VaultEntry> parse(String vaultEntries) {

        ArrayList<VaultEntry> result = new ArrayList<>();
        JsonParser parser = new JsonParser();
        JsonElement jsonArray = parser.parse(vaultEntries);
        for (JsonElement element : jsonArray.getAsJsonArray()) {
            JsonObject o = element.getAsJsonObject();

            Date date;
            JsonElement field = o.get("type");
            if (field != null && field.getAsString().equals("sgv")) {
                VaultEntryType entryType = VaultEntryType.GLUCOSE_CGM;
                date = new Date(o.get("date").getAsLong());
                result.add(new VaultEntry(entryType, date, o.get("sgv").getAsDouble()));

            }
            field = o.get("insulin");
            if (field != null && !field.isJsonNull()) {
                VaultEntryType entryType = VaultEntryType.BOLUS_NORMAL;
                date = makeDate(o.get("timestamp").getAsString());
                result.add(new VaultEntry(entryType, date, field.getAsDouble()));
            }
            field = o.get("carbs");
            if (field != null && !field.isJsonNull()) {
                VaultEntryType entryType = VaultEntryType.MEAL_MANUAL;
                date = makeDate(o.get("timestamp").getAsString());
                result.add(new VaultEntry(entryType, date, field.getAsDouble()));
            }
            field = o.get("eventType");
            if (field != null && field.getAsString().equals("Temp Basal")) {
                VaultEntryType entryType = VaultEntryType.BASAL_MANUAL;
                date = makeDate(o.get("timestamp").getAsString());
                result.add(new VaultEntry(entryType, date, o.get("absolute").getAsDouble(), o.get("duration").getAsDouble()));
            }

        }

        return result;

    }

    public List<VaultEntry> parseFile(String path) {
        StringBuilder builder = new StringBuilder();

        try (Stream<String> stream = Files.lines(Paths.get(path), StandardCharsets.UTF_8)) {
            stream.forEach(line -> builder.append(line));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return this.parse(builder.toString());
    }

    /**
     * get a date object from string with time zone information
     */
    private Date makeDate(String dateString) {
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
        Date date = new Date(0);
        try {
            date = formatter.parse(dateString);

        } catch (ParseException e) {
            System.out.println(e.getMessage());
        }

        return date;
    }

}
