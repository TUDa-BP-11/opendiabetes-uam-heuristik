package de.opendiabetes.vault.nsapi;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.opendiabetes.vault.nsapi.exception.NightscoutIOException;
import de.opendiabetes.vault.nsapi.exception.NightscoutServerException;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import static de.opendiabetes.vault.nsapi.NSApi.LOGGER;

/**
 * A buffer for Nightscout data. Lazily loads objects from the server as needed. The internal buffer is refreshed
 * if it runs out of objects until the server does not return any more data.
 */
public class DataCursor implements Iterator<JsonElement> {
    private final NSApi api;
    private final String path;
    private final String dateField;
    private final String oldest;
    private final int batchSize;

    private final LinkedBlockingQueue<JsonObject> buffer;
    private String current;
    private boolean first;
    private boolean finished;

    /**
     * Creates a new cursor. Latest and oldest point in time are formatted using the {@link NSApi#DATETIME_PATTERN_ENTRY} pattern.
     *
     * @param api       Nightscout API instance
     * @param path      API path used with {@link NSApi#createGet(String)}. Has to return an array of json objects.
     * @param dateField the name of the field which holds information about the date and time of your data object
     * @param latest    latest point in time to load data for
     * @param oldest    oldest point in time to load data for
     * @param batchSize amount of entries which will be loaded at once
     */
    public DataCursor(NSApi api, String path, String dateField, TemporalAccessor latest, TemporalAccessor oldest, int batchSize) {
        this(api, path, dateField, latest, oldest, batchSize, NSApi.DATETIME_FORMATTER_ENTRY);
    }

    /**
     * Creates a new cursor.
     *
     * @param api       Nightscout API instance
     * @param path      API path used with {@link NSApi#createGet(String)}. Has to return an array of json objects.
     * @param dateField the name of the field which holds information about the date and time of your data object
     * @param latest    latest point in time to load data for
     * @param oldest    oldest point in time to load data for
     * @param batchSize amount of entries which will be loaded at once
     * @param formatter DateTimeFormatter used to format latest and oldest point in time
     */
    public DataCursor(NSApi api, String path, String dateField, TemporalAccessor latest, TemporalAccessor oldest, int batchSize, DateTimeFormatter formatter) {
        this.api = api;
        this.path = path;
        this.dateField = dateField;
        this.oldest = formatter.format(oldest);
        this.batchSize = batchSize;

        this.buffer = new LinkedBlockingQueue<>(batchSize);
        this.current = formatter.format(latest);
        this.first = true;
        this.finished = false;
    }

    /**
     * Checks if there is more data. May block the thread to request new data from the Nightscout server.
     * Logs exceptions using {@link NSApi#LOGGER}.
     *
     * @return true if there is more data
     */
    @Override
    public boolean hasNext() {
        if (buffer.size() > 0)
            return true;
        if (finished)
            return false;
        try {
            GetBuilder builder = api.createGet(path);
            // use lte on first fetch, lt for all remaining requests.
            if (first) {
                builder.find(dateField).lte(current);
                first = false;
            } else builder.find(dateField).lt(current);
            // finish request
            JsonArray array = builder
                    .find(dateField).gte(oldest)
                    .count(batchSize)
                    .getRaw().getAsJsonArray();
            array.forEach(e -> buffer.offer(e.getAsJsonObject()));
            // we are done if the returned array has less data then the batch size
            if (array.size() < batchSize)
                finished = true;
            else current = array.get(array.size() - 1).getAsJsonObject().get(dateField).getAsString();

            return array.size() > 0;
        } catch (NightscoutIOException | NightscoutServerException | IllegalStateException e) {
            LOGGER.log(Level.SEVERE, e, e::getMessage);
            return false;
        }
    }

    /**
     * Gets the next object of the current buffer. Note that this method will never block, but may return null if not
     * used in conjunction with {@link this#hasNext()} as the buffer will only be refreshed by that method.
     *
     * @return the next entry parsed as some kind of json element.
     */
    @Override
    public JsonObject next() {
        return buffer.poll();
    }
}
