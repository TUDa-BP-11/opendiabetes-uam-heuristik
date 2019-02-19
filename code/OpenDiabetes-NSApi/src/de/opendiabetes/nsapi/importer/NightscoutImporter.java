package de.opendiabetes.nsapi.importer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import de.opendiabetes.nsapi.exception.InvalidDataException;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.importer.Importer;
import de.opendiabetes.vault.importer.ImporterOptions;

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

public class NightscoutImporter extends Importer {
    private final JsonParser json;

    public NightscoutImporter() {
        this(new ImporterOptions());
    }

    public NightscoutImporter(ImporterOptions options) {
        super(options);
        json = new JsonParser();
    }

    /**
     * Parses the source as a nightscout json representation of vault entries. Expects an array of json objects.
     * If an object in the array contains information for multiple vault entry types, it will be split up into individual entries.
     * Currently supports the following entries: {@link VaultEntryType#GLUCOSE_CGM}, {@link VaultEntryType#BOLUS_NORMAL},
     * {@link VaultEntryType#MEAL_MANUAL} and {@link VaultEntryType#BASAL_MANUAL}
     *
     * @param source Data source.
     * @return list of generated vault entries. May contain more entries than the source did.
     * @throws InvalidDataException if the given source is not formatted correctly
     */
    @Override
    public List<VaultEntry> importData(InputStream source) {
        Reader reader = new InputStreamReader(source);
        JsonElement element;
        try {
            element = json.parse(reader);
        } catch (JsonSyntaxException e) {
            throw new InvalidDataException("invalid JSON syntax", e);
        }
        //TODO: close the source?

        if (!element.isJsonArray())
            throw new InvalidDataException("source is not an array");

        List<VaultEntry> entries = new ArrayList<>();
        try {
            for (JsonElement e : element.getAsJsonArray()) {
                JsonObject o = e.getAsJsonObject();

                // BG measurements
                if (o.has("type") && o.get("type").getAsString().equals("sgv")) {
                    Date date = new Date(o.get("date").getAsLong());
                    entries.add(new VaultEntry(VaultEntryType.GLUCOSE_CGM, date, o.get("sgv").getAsDouble()));
                }

                // insulin bolus
                if (o.has("insulin") && !o.get("insulin").isJsonNull()) {
                    Date date = makeDate(o.get("timestamp").getAsString());
                    entries.add(new VaultEntry(VaultEntryType.BOLUS_NORMAL, date, o.get("insulin").getAsDouble()));
                }

                // meals
                if (o.has("carbs") && !o.get("carbs").isJsonNull()) {
                    Date date = makeDate(o.get("timestamp").getAsString());
                    entries.add(new VaultEntry(VaultEntryType.MEAL_MANUAL, date, o.get("carbs").getAsDouble()));
                }

                // basal
                if (o.has("eventType") && o.get("eventType").getAsString().equals("Temp Basal")) {
                    Date date = makeDate(o.get("timestamp").getAsString());
                    entries.add(new VaultEntry(VaultEntryType.BASAL_MANUAL, date, o.get("rate").getAsDouble(), o.get("duration").getAsDouble()));
                }
            }
        } catch (ClassCastException | UnsupportedOperationException | NullPointerException e) {
            throw new InvalidDataException("invalid source data", e);
        }
        return entries;
    }


    /**
     * get a date object from string with time zone information
     */
    private Date makeDate(String dateString) {
        try {
            TemporalAccessor t = DateTimeFormatter.ISO_DATE_TIME.parse(dateString);
            return Date.from(ZonedDateTime.from(t).toInstant());
        } catch (DateTimeParseException e) {
            throw new InvalidDataException("invalid date string: " + dateString, e);
        }
    }
}
