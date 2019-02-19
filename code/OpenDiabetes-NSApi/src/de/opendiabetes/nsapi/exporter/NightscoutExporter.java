package de.opendiabetes.nsapi.exporter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import de.opendiabetes.nsapi.exception.InvalidDataException;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.exporter.Exporter;
import de.opendiabetes.vault.exporter.ExporterOptions;
import de.opendiabetes.vault.exporter.csv.ExportEntry;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

public class NightscoutExporter extends Exporter {
    private final Gson json;

    public NightscoutExporter() {
        this(new ExporterOptions());
    }

    public NightscoutExporter(ExporterOptions options) {
        super(options);
        this.json = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }

    /**
     * Formats the data as nightscout json representation.
     * Currently supports the following entries: {@link VaultEntryType#GLUCOSE_CGM}, {@link VaultEntryType#BOLUS_NORMAL},
     * {@link VaultEntryType#MEAL_MANUAL} and {@link VaultEntryType#BASAL_MANUAL}
     *
     * @param sink target for export (e.g., a file)
     * @param data data to be exported
     */
    @Override
    public void exportData(OutputStream sink, List<VaultEntry> data) {
        JsonArray array = new JsonArray();

        for (VaultEntry entry : data) {
            JsonObject object = new JsonObject();
            DateFormat formatter;
            String dateString;
            switch (entry.getType()) {
                case GLUCOSE_CGM:
                    object.addProperty("type", "sgv");
                    object.addProperty("sgv", entry.getValue());
                    object.addProperty("date", entry.getTimestamp().getTime());
                    formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
                    formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                    object.addProperty("dateString", formatter.format(entry.getTimestamp()));

                    //TODO: set these to correct values
                    object.addProperty("direction", "");
                    object.addProperty("trend", "");
                    object.addProperty("device", "");
                    break;
                case BOLUS_NORMAL:
                    object.addProperty("eventType", "Correction Bolus");
                    object.addProperty("insulin", entry.getValue());
                    formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
                    formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                    dateString = formatter.format(entry.getTimestamp());
                    object.addProperty("created_at", dateString);
                    object.addProperty("timestamp", dateString);

                    //TODO: set these to correct values
                    object.addProperty("programmed", entry.getValue());
                    object.addProperty("duration", 0);
                    object.addProperty("type", "normal");
                    object.addProperty("unabsorbed", 0);
                    object.addProperty("carbs", (String) null);
                    object.addProperty("enteredBy", "UAMALGO");
                    break;
                case MEAL_MANUAL:
                    object.addProperty("eventType", "Meal Bolus");
                    object.addProperty("carbs", entry.getValue());
                    //TODO: set absorption time to correct value
                    object.addProperty("absorptionTime", 120);
                    formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
                    formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                    dateString = formatter.format(entry.getTimestamp());
                    object.addProperty("created_at", dateString);
                    object.addProperty("timestamp", dateString);

                    //TODO: set these to correct values
                    object.addProperty("insulin", (String) null);
                    object.addProperty("enteredBy", "UAMALGO");
                    break;
                case BASAL_MANUAL:
                    object.addProperty("eventType", "Temp Basal");
                    object.addProperty("rate", entry.getValue());
                    object.addProperty("duration", entry.getValue2());
                    formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
                    formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                    dateString = formatter.format(entry.getTimestamp());
                    object.addProperty("created_at", dateString);
                    object.addProperty("timestamp", dateString);

                    //TODO: set these to correct values
                    object.addProperty("temp", "absolute");
                    object.addProperty("absolute", entry.getValue());
                    object.addProperty("insulin", (String) null);
                    object.addProperty("carbs", (String) null);
                    object.addProperty("enteredBy", "UAMALGO");
                    break;
                default:
                    throw new InvalidDataException("unknown entry type " + entry.getType());
            }
            array.add(object);
        }

        JsonWriter writer = new JsonWriter(new OutputStreamWriter(sink));
        json.toJson(array, writer);
        //TODO: close the sink?
    }

    @Override
    protected List<ExportEntry> prepareData(List<VaultEntry> data) {
        //TODO: check with jens, how is this used?
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
