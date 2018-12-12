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
import java.util.TimeZone;
import java.util.stream.Stream;

public class VaultEntryParser {

    public static final int ONE_HOUR = 3600000;
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
// oder vielleicht besser: ?
//          field = o.get("eventType");
//          if (type != null && field.getAsString().equals("Meal Bolus")){
//                VaultEntryType entryType = VaultEntryType.MEAL_MANUAL;
//                date = makeDateWithTimeZone(o.get("timestamp").getAsString());
//                result.add(new VaultEntry(entryType, date, o.get("carbs").getAsDouble()));
//          }
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
     * compare epoch in date object with epoch extracted from string. Java
     * interprets dateString as in locale time zone, correction required
	 * @Deprecated vielleicht zumindest
     */
    private void getJSONTimeZone(Date date, String dateString) {
        TimeZone localeTimezone = TimeZone.getDefault();
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        Date tmp = new Date(0);
        try {
            tmp = formatter.parse(dateString);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
        }

        int offset = 0;
        if (localeTimezone.inDaylightTime(tmp)) {
            offset = ONE_HOUR;
        }
        int timezoneOffset = (int)(localeTimezone.getRawOffset() + offset - date.getTime() + tmp.getTime());
        String[] ids = TimeZone.getAvailableIDs(timezoneOffset);
        TimeZone timezone = TimeZone.getTimeZone(ids[0]);
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

    /**
     *
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
     *
     *
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




}
