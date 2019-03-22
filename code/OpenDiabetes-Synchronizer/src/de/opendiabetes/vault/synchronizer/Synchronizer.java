package de.opendiabetes.vault.synchronizer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.opendiabetes.vault.nsapi.DataCursor;
import de.opendiabetes.vault.nsapi.NSApi;
import de.opendiabetes.vault.nsapi.NSApiTools;
import de.opendiabetes.vault.nsapi.exception.NightscoutIOException;
import de.opendiabetes.vault.nsapi.exception.NightscoutServerException;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.logging.Level;

import static de.opendiabetes.vault.nsapi.Main.P_LATEST;
import static de.opendiabetes.vault.nsapi.Main.P_OLDEST;

public class Synchronizer {
    private NSApi read;
    private NSApi write;
    private TemporalAccessor oldest;
    private TemporalAccessor latest;
    private int batchSize;

    public Synchronizer(NSApi read, NSApi write) {
        this(read, write, ZonedDateTime.parse(P_OLDEST.getDefault()[0]),
                ZonedDateTime.parse(P_LATEST.getDefault()[0]), 100);
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
     * order from latest to oldest, therefore we actually compare in the same order.
     *
     * @param synchronizable The Synchronizable that will be tested.
     */
    public void findMissing(Synchronizable synchronizable) throws NightscoutIOException {
        synchronizable.reset();

        String dateField = synchronizable.getDateField();
        DataCursor readCursor = new DataCursor(read, synchronizable.getApiPath(), dateField, latest, oldest, batchSize);
        DataCursor writeCursor = new DataCursor(write, synchronizable.getApiPath(), dateField, latest, oldest, batchSize);

        JsonObject lastCompare = null;
        while (readCursor.hasNext()) {
            // the current object that we are looking for in the write instance
            JsonObject current = readCursor.next();
            String currentDateString = current.get(dateField).getAsString();

            synchronizable.incrFindCount();
            boolean found = false;

            // compare if the last object was not found or if the write cursor has more objects
            while (lastCompare != null || writeCursor.hasNext()) {
                // if the last object was not found compare again, else get the next object from the write instance
                JsonObject compare;
                if (lastCompare == null)
                    compare = writeCursor.next();
                else compare = lastCompare;
                String compareDateString = compare.get(dateField).getAsString();

                // if the dates equal we found the target
                if (currentDateString.equals(compareDateString)) {
                    found = true;
                    lastCompare = null;
                    break;  // break out of inner loop
                } else {
                    ZonedDateTime currentDate = NSApiTools.getZonedDateTime(currentDateString);
                    ZonedDateTime compareDate = NSApiTools.getZonedDateTime(compareDateString);
                    // if the current object is after the compared object we know that it must be missing
                    if (currentDate.isAfter(compareDate)) {
                        lastCompare = compare;
                        break;  // break out of inner loop
                    }
                }
                // keep going until current is either found or definitely missing
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
     * Posts all missing objects to the write server.
     *
     * @param synchronizable the synchronizeable
     * @throws NightscoutIOException     if an I/O error occurs during the request
     * @throws NightscoutServerException if the Nightscout server returns a bad response status
     */
    public void postMissing(Synchronizable synchronizable) throws NightscoutIOException, NightscoutServerException {
        for (JsonArray array : NSApiTools.split(synchronizable.getMissing(), batchSize))
            write.createPost(synchronizable.getApiPath())
                    .setBody(array.toString())
                    .send();
    }

    public void close() throws IOException {
        read.close();
        write.close();
    }
}
