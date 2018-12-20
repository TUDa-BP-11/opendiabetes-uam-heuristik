package de.opendiabetes.parser;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;

import de.opendiabetes.vault.engine.container.VaultEntry;
import de.opendiabetes.vault.engine.container.VaultEntryType;
import org.json.JSONArray;
import org.json.JSONObject;

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
import java.util.TimeZone;
import java.util.stream.Stream;

public class VaultEntryParser {

    //GMT +/-
    TimeZone timezone;
    TimeZone localeTimezone;

    private static final long ONE_HOUR = 3600000;

    public VaultEntryParser() {
        String[] ids = TimeZone.getAvailableIDs(0);
        System.out.println(ids[0]);
        this.timezone = TimeZone.getTimeZone(ids[0]);
        localeTimezone = TimeZone.getDefault();
    }

    /**
     * long to timezone
     */
    public VaultEntryParser(long timezone) {
        String[] ids = TimeZone.getAvailableIDs((int) timezone);
        this.timezone = TimeZone.getTimeZone(ids[0]);
        localeTimezone = TimeZone.getDefault();
    }

    /**
     * long to timezone
     */
    public VaultEntryParser(long timezone, long localeTimezone) {
        String[] ids = TimeZone.getAvailableIDs((int) timezone);
        this.timezone = TimeZone.getTimeZone(ids[0]);
        ids = TimeZone.getAvailableIDs((int) localeTimezone);
        this.localeTimezone = TimeZone.getTimeZone(ids[0]);
    }

    public long getTimezone() {
        return timezone.getRawOffset();
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
                date = makeDate(o.get("date").getAsLong());
                result.add(new VaultEntry(entryType, date, o.get("sgv").getAsDouble()));

            }
            field = o.get("insulin");
            if (field != null && !field.isJsonNull()) {
                VaultEntryType entryType = VaultEntryType.BOLUS_NORMAL;
                date = makeDateWithTimeZone(o.get("timestamp").getAsString());
                result.add(new VaultEntry(entryType, date, field.getAsDouble()));
            }
            field = o.get("carbs");
            if (field != null && !field.isJsonNull()) {
                VaultEntryType entryType = VaultEntryType.MEAL_MANUAL;
                date = makeDateWithTimeZone(o.get("timestamp").getAsString());
                result.add(new VaultEntry(entryType, date, field.getAsDouble()));
// oder vielleicht besser: ?
//          field = o.get("eventType");
//          if (type != null && field.getAsString().equals("Meal Bolus")){
//                VaultEntryType entryType = VaultEntryType.MEAL_MANUAL;
//                date = makeDateWithTimeZone(o.get("timestamp").getAsString());
//                result.add(new VaultEntry(entryType, date, o.get("carbs").getAsDouble()));
            }
            field = o.get("eventType");
            if (field != null && field.getAsString().equals("Temp Basal")) {
                VaultEntryType entryType = VaultEntryType.BASAL_MANUAL;
                date = makeDateWithTimeZone(o.get("timestamp").getAsString());
                result.add(new VaultEntry(entryType, date, o.get("absolute").getAsDouble(), o.get("duration").getAsDouble()));
            }

        }

        return result;
    }

    /**
     * Parses a {@link JSONArray} containing NightScout entries (e.g. results from the NightScout API) to a List of {@link VaultEntry}s.
     *
     * @param entries The array to parse. All entries in the array have to be JSON Objects
     * @return a list of VaultEntry representing the given input
     */
    public List<VaultEntry> parse(JSONArray entries) {
        List<VaultEntry> result = new ArrayList<>();
        for (int i = 0; i < entries.length(); i++) {
            JSONObject o = entries.getJSONObject(i);

            Date date;
            String type = o.optString("type");
            if (type != null && type.equals("sgv")) {
                VaultEntryType entryType = VaultEntryType.GLUCOSE_CGM;
                date = makeDate(o.getLong("date"));
                result.add(new VaultEntry(entryType, date, o.getDouble("sgv")));

            }
            if (o.has("insulin")) {
                VaultEntryType entryType = VaultEntryType.BOLUS_NORMAL;
                date = makeDateWithTimeZone(o.getString("timestamp"));
                result.add(new VaultEntry(entryType, date, o.getDouble("insulin")));
            }
            if (o.has("carbs")) {
                VaultEntryType entryType = VaultEntryType.MEAL_MANUAL;
                date = makeDateWithTimeZone(o.getString("timestamp"));
                result.add(new VaultEntry(entryType, date, o.getDouble("carbs")));
            }
            String eventType = o.optString("eventType");
            if (eventType != null && eventType.equals("Temp Basal")) {
                VaultEntryType entryType = VaultEntryType.BASAL_MANUAL;
                date = makeDateWithTimeZone(o.getString("timestamp"));
                result.add(new VaultEntry(entryType, date, o.getDouble("absolute"), o.getDouble("duration")));
            }
        }
        return result;
    }

    /**
     * @param vaultEntries
     * @return Entry{
     *
     * type string
     *
     * dateString string
     *
     * date number
     *
     * sgv number (only available for sgv types)
     *
     * direction string (only available for sgv types)
     *
     * noise number (only available for sgv types)
     *
     * filtered number (only available for sgv types)
     *
     * unfiltered number (only available for sgv types)
     *
     * rssi number (only available for sgv types)
     * 
     * trend number ( ??? undefined in yaml file) 
     *
     * }
     *
     * Treatment{
     *
     * _id string
     *
     * eventType string
     *
     * created_at string
     *
     * glucose string
     *
     * glucoseType string
     *
     * carbs number
     *
     * insulin number
     *
     * units string
     *
     * notes string
     *
     * enteredBy string
     *
     * }
     *
     * Profile{
     *
     * sens	integer
     *
     * dia	integer
     *
     * carbratio	integer
     *
     * carbs_hr	integer
     *
     * _id	string
     *
     * }
     *
     * Status{
     *
     * apiEnabled	boolean
     *
     * careportalEnabled	boolean
     *
     * head	string
     *
     * name	string
     *
     * version	string
     *
     * settings	Settings{...} [Jump to definition]
     *
     * extendedSettings ExtendedSettings{...} [Jump to definition]
     *
     * }
     */

    // "Correction Bolus", "duration", "unabsorbed", "type", "programmed", "insulin"
    // "Meal Bolus", "carbs", "absorptionTime"
    public String unparse(List<VaultEntry> vaultEntries) {

        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        String result = "[";

        long date;
        String dateString;
        String type;
        for (VaultEntry element : vaultEntries) {
            result += "{";
            if (null != element.getType()) {
                switch (element.getType()) {
                    case GLUCOSE_CGM:
                        result += "\"type\": \"sgv\",";
                        result += "\"sgv\": " + element.getValue() + ",";
                        // date only with entries. Anything an entry except GLUCOSE_CGM?
                        result += "\"date\": " + element.getTimestamp().getTime() + ",";
                        result += "\"direction\":" + ",";
                        result += "\"trend\":" + ",";
                        result += "\"_id\":" + ",";
                        result += "\"device\":" + ",";
                        break;
                    case BOLUS_NORMAL:
                        result += "\"insulin\": " + element.getValue() + ",";
                        break;
                    case BASAL_MANUAL:
                        result += "\"eventType\": \"Temp Basal\",";
                        result += "\"absolute\": " + element.getValue() + ",";
                        result += "\"duration\": " + element.getValue2() + ",";
                        break;
                    case MEAL_MANUAL:
                        result += "\"eventType\": \"Meal Bolus\",";
                        result += "\"carbs\": " + element.getValue() + ",";
                        break;
                    default:
                        break;
                }
            }

            dateString = formatter.format(element.getTimestamp());
            result += "\"dateString\": " + dateString + "}";
        }
        result += "]";

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
     * compare epoch in date object with epoch extracted from string. Java
     * interprets dateString as in locale time zone, correction required
     *
     * @deprecated vielleicht zumindest
     */
    private void getJSONTimeZone(Date date, String dateString) {

        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        Date tmp = new Date(0);
        try {
            tmp = formatter.parse(dateString);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
        }

        long offset = 0;
        if (localeTimezone.inDaylightTime(tmp)) {
            offset = ONE_HOUR;
        }
        long timezoneOffset = (localeTimezone.getRawOffset() + offset - date.getTime() + tmp.getTime());
        String[] ids = TimeZone.getAvailableIDs((int) timezoneOffset);
        timezone = TimeZone.getTimeZone(ids[0]);
    }


    /**
     * get a date object from epoch number
     */
    private Date makeDate(long epoch) {
        return new Date(epoch);
    }


    private Date makeDateWithTimeZone(String dateString) {
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
        Date tmp = new Date(0);
        try {
            tmp = formatter.parse(dateString);

        } catch (ParseException e) {
            System.out.println(e.getMessage());

        }

        return tmp;
    }
}
