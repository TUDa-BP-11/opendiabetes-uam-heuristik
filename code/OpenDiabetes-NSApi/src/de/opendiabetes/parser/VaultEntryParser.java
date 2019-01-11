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

public class VaultEntryParser implements Parser<List<VaultEntry>> {

    @Override
    public List<VaultEntry> parse(String vaultEntries) {
        return parse(new JSONArray(vaultEntries));
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
            String eventType = o.optString("eventType");
            if (eventType != null && eventType.equals("Temp Basal")) {
                VaultEntryType entryType = VaultEntryType.BASAL_MANUAL;
                date = makeDate(o.getString("timestamp"));
                result.add(new VaultEntry(entryType, date, o.getDouble("absolute"), o.getDouble("duration")));
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
      @param vaultEntries
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

    /*
    // "Correction Bolus", "duration", "unabsorbed", "type", "programmed", "insulin"
    // "Meal Bolus", "carbs", "absorptionTime"
    public String unparse(List<VaultEntry> vaultEntries) {

        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        String result = "[";

        for (VaultEntry element : vaultEntries) {
            result += "{";
            if (null != element.getType()) {
                switch (element.getType()) {
                    case GLUCOSE_CGM:
                        result += "\"type\": \"sgv\",";
                        result += "\"sgv\": " + element.getValue() + ",";
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

            String dateString = formatter.format(element.getTimestamp());
            result += "\"dateString\": " + dateString + "}";
        }
        result += "]";

        return result;
    }*/
}
