package de.opendiabetes.vault.nsapi.importer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.importer.Importer;
import de.opendiabetes.vault.nsapi.NSApi;
import de.opendiabetes.vault.nsapi.exception.NightscoutDataException;
import de.opendiabetes.vault.util.TimestampUtils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

/**
 * Imports JSON objects from a Nightscout server as {@link VaultEntry}s.
 */
public class NightscoutImporter extends Importer {
    private final NightscoutImporterOptions options;
    private final JsonParser json;

    public NightscoutImporter() {
        this(new NightscoutImporterOptions());
    }

    public NightscoutImporter(NightscoutImporterOptions options) {
        super(options);
        this.options = options;
        this.json = new JsonParser();
    }

    /**
     * Parses the source as a nightscout json representation of vault entries. Expects an array of json objects.
     * If an object in the array contains information for multiple vault entry types, it will be split up into individual entries.
     * Currently supports the following entries: {@link VaultEntryType#GLUCOSE_CGM}, {@link VaultEntryType#BOLUS_NORMAL},
     * {@link VaultEntryType#MEAL_MANUAL} and {@link VaultEntryType#BASAL_MANUAL}.
     *
     * @param source Data source.
     * @return list of generated vault entries. May contain more entries than the source did.
     * @throws NightscoutDataException if the given source is not formatted correctly
     */
    @Override
    public List<VaultEntry> importData(InputStream source) {
        Reader reader = new InputStreamReader(source);
        JsonElement element;
        try {
            element = json.parse(reader);
        } catch (JsonParseException e) {
            throw new NightscoutDataException("exception while reading data", e);
        }

        if (!element.isJsonArray())
            throw new NightscoutDataException("source is not an array");

        List<VaultEntry> entries = new ArrayList<>();
        try {
            for (JsonElement e : element.getAsJsonArray()) {
                JsonObject o = e.getAsJsonObject();
                boolean valid = false;

                // BG measurements
                if (o.has("type") && o.get("type").getAsString().equals("sgv")) {
                    Date date = TimestampUtils.createCleanTimestamp(new Date(o.get("date").getAsLong()));
                    entries.add(new VaultEntry(VaultEntryType.GLUCOSE_CGM, date, o.get("sgv").getAsDouble()));
                    valid = true;
                }

                // insulin bolus
                if (o.has("insulin") && !o.get("insulin").isJsonNull()) {
                    Date date = makeDate(o.get("timestamp").getAsString());
                    entries.add(new VaultEntry(VaultEntryType.BOLUS_NORMAL, date, o.get("insulin").getAsDouble()));
                    valid = true;
                }

                // meals
                if (o.has("carbs") && !o.get("carbs").isJsonNull()) {
                    Date date = makeDate(o.get("timestamp").getAsString());
                    entries.add(new VaultEntry(VaultEntryType.MEAL_MANUAL, date, o.get("carbs").getAsDouble()));
                    valid = true;
                }

                // basal
                if (o.has("eventType") && o.get("eventType").getAsString().equals("Temp Basal")) {
                    Date date = makeDate(o.get("timestamp").getAsString());
                    entries.add(new VaultEntry(VaultEntryType.BASAL_MANUAL, date, o.get("rate").getAsDouble(), o.get("duration").getAsDouble()));
                    valid = true;
                }
                if (!valid) {
                    if (options.requireValidData())
                        throw new NightscoutDataException("invalid source data, could not identify vault entry type");
                    else NSApi.LOGGER.log(Level.WARNING, "Could not parse JSON Object: " + o.toString());
                }
            }
        } catch (NumberFormatException | IllegalStateException | NullPointerException e) {
            throw new NightscoutDataException("invalid source data", e);
        }
        return entries;
    }


    /**
     * Converts a date and time string to a {@link Date} object in local time. Strips seconds and milliseconds
     *
     * @param dateString ISO 8601 compliant date time string
     * @return local Date object representing the input with seconds and milliseconds set to zero
     */
    private Date makeDate(String dateString) {
        TemporalAccessor t;
        try {
            t = DateTimeFormatter.ISO_DATE_TIME.parse(dateString);
        } catch (DateTimeParseException e) {
            throw new NightscoutDataException("invalid date string: " + dateString, e);
        }
        // dates are automatically converted to local time by the toInstant() method of ZonedDateTime
        Date date = Date.from(ZonedDateTime.from(t).toInstant());
        return TimestampUtils.createCleanTimestamp(date);
    }
}
