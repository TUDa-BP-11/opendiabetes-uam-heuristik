package de.opendiabetes.vault.nsapi.exporter;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.exporter.Exporter;
import de.opendiabetes.vault.exporter.ExporterOptions;
import de.opendiabetes.vault.exporter.csv.ExportEntry;
import de.opendiabetes.vault.nsapi.NSApi;
import de.opendiabetes.vault.nsapi.exception.NightscoutDataException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Exports {@link VaultEntry}s as JSON objects that can be uploaded to the uam endpoint of a Nightscout server.
 */
public class UnannouncedMealExporter extends Exporter {
    private final Gson gson;
    private final String algorithm;

    public UnannouncedMealExporter(String algorithm) {
        super(new ExporterOptions());
        this.gson = new Gson();
        this.algorithm = algorithm;
    }

    /**
     * Formats the data as nightscout json representation for the uam endpoint. All times are converted to UTC.
     * Only allowed VaultEntryType is {@link VaultEntryType#MEAL_MANUAL}.
     *
     * @param sink target for export (e.g., a file)
     * @param data data to be exported.
     * @throws NightscoutDataException if invalid entries are exported
     */
    @Override
    public void exportData(OutputStream sink, List<VaultEntry> data) {
        JsonArray array = new JsonArray();
        SimpleDateFormat format = NSApi.createSimpleDateFormatTreatment();
        for (VaultEntry entry : data) {
            if (!entry.getType().equals(VaultEntryType.MEAL_MANUAL))
                throw new IllegalArgumentException("Cannot parse " + entry.getType() + " as unannounced meal!");
            JsonObject object = new JsonObject();
            object.addProperty("carbs", entry.getValue());
            object.addProperty("created_at", format.format(entry.getTimestamp()));
            object.addProperty("algorithm", this.algorithm);
            array.add(object);
        }
        try (JsonWriter writer = new JsonWriter(new OutputStreamWriter(sink))) {
            gson.toJson(array, writer);
        } catch (IOException | JsonIOException e) {
            throw new NightscoutDataException("Exception while flushing stream", e);
        }
    }

    @Override
    protected List<ExportEntry> prepareData(List<VaultEntry> data) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
