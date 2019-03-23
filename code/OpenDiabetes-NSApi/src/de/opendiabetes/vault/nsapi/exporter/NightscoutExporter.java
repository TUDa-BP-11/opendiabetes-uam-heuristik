package de.opendiabetes.vault.nsapi.exporter;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.exporter.Exporter;
import de.opendiabetes.vault.exporter.csv.ExportEntry;
import de.opendiabetes.vault.nsapi.NSApi;
import de.opendiabetes.vault.nsapi.exception.NightscoutDataException;
import de.opendiabetes.vault.util.SortVaultEntryByDate;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Exports {@link VaultEntry}s as JSON objects that can be uploaded to a Nightscout server.
 */
public class NightscoutExporter extends Exporter {
    private final NightscoutExporterOptions options;
    private final Gson gson;

    public NightscoutExporter() {
        this(new NightscoutExporterOptions());
    }

    public NightscoutExporter(NightscoutExporterOptions options) {
        super(options);
        this.options = options;
        this.gson = new Gson();
    }

    /**
     * Formats the data as nightscout json representation. All times are converted to UTC
     * Currently supports the following entries: {@link VaultEntryType#GLUCOSE_CGM}, {@link VaultEntryType#BOLUS_NORMAL},
     * {@link VaultEntryType#MEAL_MANUAL} and {@link VaultEntryType#BASAL_MANUAL}
     *
     * @param sink target for export (e.g., a file)
     * @param data data to be exported. Has to be in descending order as sorted by {@link SortVaultEntryByDate#reversed()}
     * @throws NightscoutDataException if invalid entries are exported or the collection isn't sorted properly
     */
    @Override
    public void exportData(OutputStream sink, List<VaultEntry> data) {
        SortVaultEntryByDate comparator = new SortVaultEntryByDate();
        if (!comparator.isSorted(data, true))
            throw new NightscoutDataException("Data has to be sorted in descending order");
        JsonArray array = new JsonArray();

        // maximum amount of milliseconds for merging entries as one
        long window = options.getMergeWindow() * 1000L;
        for (int i = 0; i < data.size(); i++) {
            VaultEntry entry = data.get(i);
            // if entry is mergeable check for more entries in given window
            if (window > 0 && isMergeable(entry)) {
                // collect all entries within the given window
                List<VaultEntry> toMerge = new ArrayList<>();
                List<VaultEntry> noMerge = new ArrayList<>();
                toMerge.add(entry);
                while (i < data.size() - 1 && entry.getTimestamp().getTime() - data.get(i + 1).getTimestamp().getTime() <= window) {
                    VaultEntry next = data.get(++i);
                    if (isMergeable(next))
                        toMerge.add(next);
                    else noMerge.add(next);
                }

                // merge all mergeable entries
                JsonObject merged = new JsonObject();
                boolean isBolus = false;
                boolean isMeal = false;
                for (VaultEntry e : toMerge) {
                    if (e.getType().equals(VaultEntryType.BOLUS_NORMAL)) {
                        if (isBolus)
                            throw new NightscoutDataException("Cannot merge two bolus entries into one: " + e.getTimestamp());
                        isBolus = true;
                    }
                    if (e.getType().equals(VaultEntryType.MEAL_MANUAL)) {
                        if (isMeal)
                            throw new NightscoutDataException("Cannot merge two meal entries into one: " + e.getTimestamp());
                        isMeal = true;
                    }
                    addProperties(merged, e, false);
                }
                if (isBolus && isMeal)  //TODO: check if this is the correct type
                    merged.addProperty("eventType", "Meal Bolus");
                else if (isBolus)
                    merged.addProperty("eventType", "Correction Bolus");
                else if (isMeal)
                    merged.addProperty("eventType", "Meal Bolus");
                array.add(merged);

                // add remaining entries
                for (VaultEntry e : noMerge) {
                    addEntry(array, e);
                }
            }
            // else simply add the entry
            else {
                addEntry(array, entry);
            }
        }

        try {
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(sink));
            if (options.isPretty())
                writer.setIndent("  ");
            gson.toJson(array, writer);
            writer.close();
        } catch (IOException | JsonIOException e) {
            throw new NightscoutDataException("Exception while flushing stream", e);
        }
    }

    private boolean isMergeable(VaultEntry entry) {
        return entry.getType().equals(VaultEntryType.BOLUS_NORMAL) || entry.getType().equals(VaultEntryType.MEAL_MANUAL);
    }

    private void addEntry(JsonArray array, VaultEntry entry) {
        JsonObject object = new JsonObject();
        addProperties(object, entry, true);
        array.add(object);
    }

    private void addProperties(JsonObject object, VaultEntry entry, boolean setEventType) {
        String dateString;
        SimpleDateFormat formatEntry = NSApi.createSimpleDateFormatEntry();
        SimpleDateFormat formatTreatment = NSApi.createSimpleDateFormatTreatment();
        switch (entry.getType()) {
            case GLUCOSE_CGM:
                object.addProperty("type", "sgv");
                object.addProperty("sgv", entry.getValue());
                object.addProperty("date", entry.getTimestamp().getTime());
                object.addProperty("dateString", formatEntry.format(entry.getTimestamp()));

                //TODO: set these to correct values
                object.addProperty("direction", "");
                object.addProperty("trend", "");
                object.addProperty("device", "");
                break;
            case BOLUS_NORMAL:
                if (setEventType)
                    object.addProperty("eventType", "Correction Bolus");
                object.addProperty("insulin", entry.getValue());
                dateString = formatTreatment.format(entry.getTimestamp());
                object.addProperty("created_at", dateString);
                object.addProperty("timestamp", dateString);
                object.addProperty("programmed", entry.getValue());
                object.addProperty("duration", 0);
                object.addProperty("type", "normal");
                object.addProperty("unabsorbed", 0);
                break;
            case MEAL_MANUAL:
                if (setEventType)
                    object.addProperty("eventType", "Meal Bolus");
                object.addProperty("carbs", entry.getValue());
                //TODO: set absorption time to correct value
                object.addProperty("absorptionTime", 180);
                dateString = formatTreatment.format(entry.getTimestamp());
                object.addProperty("created_at", dateString);
                object.addProperty("timestamp", dateString);
                break;
            case BASAL_MANUAL:
                object.addProperty("eventType", "Temp Basal");
                object.addProperty("rate", entry.getValue());
                object.addProperty("duration", entry.getValue2());
                dateString = formatTreatment.format(entry.getTimestamp());
                object.addProperty("created_at", dateString);
                object.addProperty("timestamp", dateString);
                object.addProperty("temp", "absolute");
                object.addProperty("absolute", entry.getValue());
                object.addProperty("insulin", (String) null);
                object.addProperty("carbs", (String) null);
                break;
            default:
                throw new NightscoutDataException("unknown entry type " + entry.getType());
        }
    }

    @Override
    protected List<ExportEntry> prepareData(List<VaultEntry> data) {
        //TODO: check with jens, how is this used?
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
