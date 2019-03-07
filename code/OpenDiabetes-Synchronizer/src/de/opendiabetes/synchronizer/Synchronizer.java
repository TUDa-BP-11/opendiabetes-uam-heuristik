package de.opendiabetes.synchronizer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.opendiabetes.nsapi.DataCursor;
import de.opendiabetes.nsapi.NSApi;
import de.opendiabetes.nsapi.exception.NightscoutIOException;
import de.opendiabetes.nsapi.exception.NightscoutServerException;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.logging.Level;

public class Synchronizer {
    private NSApi read;
    private NSApi write;
    private TemporalAccessor oldest;
    private TemporalAccessor latest;
    private int batchSize;

    public Synchronizer(NSApi read, NSApi write) {
        this(read, write, ZonedDateTime.parse(de.opendiabetes.nsapi.Main.P_OLDEST.getDefault()[0]),
                ZonedDateTime.parse(de.opendiabetes.nsapi.Main.P_LATEST.getDefault()[0]), 100);
    }

    public Synchronizer(NSApi read, NSApi write, TemporalAccessor oldest, TemporalAccessor latest, int batchSize) {
        this.read = read;
        this.write = write;
        this.oldest = oldest;
        this.latest = latest;
        this.batchSize = batchSize;
    }

    public NSApi getReadApi() {
        return read;
    }

    public NSApi getWriteApi() {
        return write;
    }

    /**
     * Finds all missing entries in the target NS instance. Nightscout returns entries in
     * order from latest to oldest, therefore we actually compare in the same order
     *
     * @param synchronizable The Synchronizable that will be tested.
     */
    public void findMissing(Synchronizable synchronizable) throws NightscoutIOException, NightscoutServerException {
        synchronizable.reset();

        String dateField = synchronizable.getDateField();
        DataCursor readCursor = new DataCursor(read, synchronizable.getApiPath(), dateField, latest, oldest, batchSize);
        DataCursor writeCursor = new DataCursor(write, synchronizable.getApiPath(), dateField, latest, oldest, batchSize);

        JsonObject lastCompare = null;
        while (readCursor.hasNext()) {
            JsonObject current = readCursor.next();
            String currentDateString = current.get(dateField).getAsString();
            synchronizable.incrFindCount();
            boolean found = false;

            // compare if the last entry was not found or if the write cursor has more objects
            while (lastCompare != null || writeCursor.hasNext()) {
                JsonObject compare;
                if (lastCompare == null)
                    compare = writeCursor.next();
                else compare = lastCompare;
                String compareDateString = compare.get(dateField).getAsString();

                if (currentDateString.equals(compareDateString)) {
                    found = true;
                    lastCompare = null;
                    break;  // break out of inner loop
                } else {
                    ZonedDateTime currentDate = NSApi.getZonedDateTime(currentDateString);
                    ZonedDateTime compareDate = NSApi.getZonedDateTime(compareDateString);
                    if (currentDate.isAfter(compareDate)) {
                        lastCompare = compare;
                        break;  // break out of inner loop
                    }
                }
                // keep going until current is either found or before the next compare
                lastCompare = null;
            }
            if (found) {
                Main.logger().log(Level.FINE, "- %s IS NOT missing", current.get(dateField).getAsString());
            } else {
                Main.logger().log(Level.FINE, "+ %s IS missing", current.get(dateField).getAsString());
                synchronizable.putMissing(current);
            }
        }
    }

    /**
     * Posts all missing objects to the write server. Strips all <code>_id</code> fields of all objects if found,
     * to prevent MongoDB errors on the server side.
     *
     * @param synchronizable the synchronizeable
     * @throws NightscoutIOException     if an I/O error occurs during the request
     * @throws NightscoutServerException if the Nightscout server returns a bad response status
     */
    public void postMissing(Synchronizable synchronizable) throws NightscoutIOException, NightscoutServerException {
        for (JsonElement e : synchronizable.getMissing()) {
            JsonObject o = e.getAsJsonObject();
            if (o.has("_id"))
                o.remove("_id");
        }
        for (JsonArray array : NSApi.split(synchronizable.getMissing(), batchSize))
            write.createPost(synchronizable.getApiPath())
                    .setBody(array.toString())
                    .send();
    }

    public void close() throws IOException {
        read.close();
        write.close();
    }
}
