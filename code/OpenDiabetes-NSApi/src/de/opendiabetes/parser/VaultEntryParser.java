package de.opendiabetes.parser;

import de.opendiabetes.vault.engine.container.VaultEntry;
import de.opendiabetes.vault.engine.container.VaultEntryType;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

public class VaultEntryParser implements Parser<List<VaultEntry>> {

    @Override
    public List<VaultEntry> parse(String vaultEntries) {
        return parse(new JSONArray(vaultEntries));
    }

    /**
     * Parses a {@link JSONArray} containing NightScout entries (e.g. results
     * from the NightScout API) to a List of {@link VaultEntry}s.
     *
     * @param entries The array to parse. All entries in the array have to be
     * JSON Objects
     * @return a list of VaultEntry representing the given input
     */
    public List<VaultEntry> parse(JSONArray entries) {
        List<VaultEntry> result = new ArrayList<>();
        //TODO: Jens fragen, warum er die Sekunden aus den Timestamps entfernen will
        for (int i = 0; i < entries.length(); i++) {
            JSONObject o = entries.getJSONObject(i);

            Date date;
            String type = o.optString("type");
            String eventType = o.optString("eventType");
            if (type != null && type.equals("sgv")) {
                VaultEntryType entryType = VaultEntryType.GLUCOSE_CGM;
                date = new Date(o.getLong("date"));
                result.add(new VaultEntry(entryType, date, o.getDouble("sgv")));
            } 
            if (o.has("insulin") && !o.isNull("insulin")) {
                VaultEntryType entryType = VaultEntryType.BOLUS_NORMAL;
                date = makeDate(o.getString("timestamp"));
                result.add(new VaultEntry(entryType, date, o.getDouble("insulin")));
            } 
            if (o.has("carbs") && !o.isNull("carbs")) {
                VaultEntryType entryType = VaultEntryType.MEAL_MANUAL;
                date = makeDate(o.getString("timestamp"));
                result.add(new VaultEntry(entryType, date, o.getDouble("carbs")));
            } 
            if (eventType != null && eventType.equals("Temp Basal")) {
                VaultEntryType entryType = VaultEntryType.BASAL_MANUAL;
                date = makeDate(o.getString("timestamp"));
                result.add(new VaultEntry(entryType, date, o.getDouble("rate"), o.getDouble("duration")));
            }
        }
        return result;
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

    /*
     * @param vaultEntries
     * @return Entry{
     * <p>
     * type string
     * <p>
     * dateString string
     * <p>
     * date number
     * <p>
     * sgv number (only available for sgv types)
     * <p>
     * direction string (only available for sgv types)
     * <p>
     * noise number (only available for sgv types)
     * <p>
     * filtered number (only available for sgv types)
     * <p>
     * unfiltered number (only available for sgv types)
     * <p>
     * rssi number (only available for sgv types)
     * <p>
     * trend number ( ??? undefined in yaml file)
     * <p>
     * }
     * <p>
     * Treatment{
     * <p>
     * _id string
     * <p>
     * eventType string
     * <p>
     * created_at string
     * <p>
     * glucose string
     * <p>
     * glucoseType string
     * <p>
     * carbs number
     * <p>
     * insulin number
     * <p>
     * units string
     * <p>
     * notes string
     * <p>
     * enteredBy string
     * <p>
     * }
     * <p>
     * Profile{
     * <p>
     * sens	integer
     * <p>
     * dia	integer
     * <p>
     * carbratio	integer
     * <p>
     * carbs_hr	integer
     * <p>
     * _id	string
     * <p>
     * }
     * <p>
     * Status{
     * <p>
     * apiEnabled	boolean
     * <p>
     * careportalEnabled	boolean
     * <p>
     * head	string
     * <p>
     * name	string
     * <p>
     * version	string
     * <p>
     * settings	Settings{...} [Jump to definition]
     * <p>
     * extendedSettings ExtendedSettings{...} [Jump to definition]
     * <p>
     * }
     */
    // "Correction Bolus", "duration", "unabsorbed", "type", "programmed", "insulin"
    // "Meal Bolus", "carbs", "absorptionTime"
    public String toJson(List<VaultEntry> vaultEntries) {
        return toJson(vaultEntries, 120);
    }

    public String toJson(List<VaultEntry> vaultEntries, int absorptionTime) {
        return "[" + vaultEntries.stream()
                .map(e -> toJson(e, absorptionTime))
                .collect(Collectors.joining(",")) + "]";
    }

    private String toJson(VaultEntry entry, int absorptionTime) {
        String result = "{";
        //result += "\"_id\": \"" + 0 + "\","; //TODO id (why? skip this. IDs are generated by mongo on insert and not needed otherwise)

        DateFormat formatter;
        String timestamp;
        switch (entry.getType()) {
            case GLUCOSE_CGM:
                result += "\"type\": \"sgv\",";
                result += "\"sgv\": " + entry.getValue() + ",";
                result += "\"date\": " + entry.getTimestamp().getTime() + ",";
                //TODO direction, trend, device?
                result += "\"direction\": \"" + "\",";
                result += "\"trend\": \"" + "\",";
                result += "\"device\": \"UAMALGO\",";

                formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
                formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
                timestamp = formatter.format(entry.getTimestamp());
                result += "\"dateString\": \"" + timestamp + "\"";
                break;
            case BOLUS_NORMAL:
                result += "\"eventType\": \"Correction Bolus\",";
                result += "\"insulin\": " + entry.getValue() + ",";
                result += "\"programmed\": " + entry.getValue() + ",";
                //TODO programmed, duration, type, unabsorbed, carbs, enteredBy?
                result += "\"duration\": 0,";
                result += "\"type\": \"normal\",";
                result += "\"unabsorbed\": 0,";
                result += "\"carbs\": null,";
                result += "\"enteredBy\": \"UAMALGO\",";

                formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
                formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
                timestamp = formatter.format(entry.getTimestamp());
                result += "\"created_at\": \"" + timestamp + "\",";
                result += "\"timestamp\": \"" + timestamp + "\"";
                break;
            case BASAL_MANUAL:
                result += "\"eventType\": \"Temp Basal\",";
                result += "\"rate\": " + entry.getValue() + ",";
                result += "\"duration\": " + entry.getValue2() + ",";
                //TODO temp, ablsolute, insulin, carbs, enteredBy?
                result += "\"temp\": \"absolute\",";
                result += "\"absolute\": " + entry.getValue() + ",";
                result += "\"insulin\": null,";
                result += "\"carbs\": null,";
                result += "\"enteredBy\": \"UAMALGO\",";

                formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
                formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
                timestamp = formatter.format(entry.getTimestamp());
                result += "\"created_at\": \"" + timestamp + "\",";
                result += "\"timestamp\": \"" + timestamp + "\"";
                break;
            case MEAL_MANUAL:
                result += "\"eventType\": \"Meal Bolus\",";
                result += "\"carbs\": " + entry.getValue() + ",";
                result += "\"absorptionTime\": " + absorptionTime + ",";
                //TODO insulin, enteredBy?
                result += "\"insulin\": null,";
                result += "\"enteredBy\": \"UAMALGO\",";

                formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
                formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
                timestamp = formatter.format(entry.getTimestamp());
                result += "\"created_at\": \"" + timestamp + "\",";
                result += "\"timestamp\": \"" + timestamp + "\"";
                break;
            default:
                break;
        }

        result += "}";
        return result;
    }
}
