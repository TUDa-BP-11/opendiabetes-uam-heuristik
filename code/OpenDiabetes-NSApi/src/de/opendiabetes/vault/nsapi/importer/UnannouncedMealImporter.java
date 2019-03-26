package de.opendiabetes.vault.nsapi.importer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.importer.Importer;
import de.opendiabetes.vault.importer.ImporterOptions;
import de.opendiabetes.vault.nsapi.NSApi;
import de.opendiabetes.vault.nsapi.exception.NightscoutDataException;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Imports JSON objects from the uam endpoint of a Nightscout server as {@link VaultEntry}s.
 */
public class UnannouncedMealImporter extends Importer {
    private final JsonParser json;

    public UnannouncedMealImporter() {
        this(new ImporterOptions());
    }

    public UnannouncedMealImporter(ImporterOptions options) {
        super(options);
        this.json = new JsonParser();
    }

    /**
     * Parses the source as a nightscout json representation of vault entries. Expects an array of json objects.
     * Only supports data returned by the uam endpoint of a Nightscout server with the uam plugin enabled.
     *
     * @param source Data source.
     * @return list of generated vault entries.
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

        List<VaultEntry> list = new ArrayList<>();
        SimpleDateFormat format = NSApi.createSimpleDateFormatTreatment();
        try {
            for (JsonElement e : element.getAsJsonArray()) {
                JsonObject o = e.getAsJsonObject();
                Date date = format.parse(o.get("created_at").getAsString());
                double carbs = o.get("carbs").getAsDouble();
                VaultEntry entry = new VaultEntry(VaultEntryType.MEAL_MANUAL, date, carbs);
                list.add(entry);
            }
        } catch (NumberFormatException | IllegalStateException | NullPointerException | ParseException e) {
            throw new NightscoutDataException("invalid source data", e);
        }
        return list;
    }
}
