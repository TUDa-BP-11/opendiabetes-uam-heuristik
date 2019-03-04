package de.opendiabetes.synchronizer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.opendiabetes.nsapi.GetBuilder;
import de.opendiabetes.nsapi.NSApi;
import de.opendiabetes.nsapi.exception.NightscoutIOException;
import de.opendiabetes.nsapi.exception.NightscoutServerException;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.logging.Level;

public class Synchronizer {
    private NSApi read;
    private NSApi write;
    private String oldest;
    private String latest;
    private int batchSize;

    public Synchronizer(NSApi read, NSApi write) {
        this(read, write, Instant.EPOCH, Instant.now(), 100);
    }

    public Synchronizer(NSApi read, NSApi write, TemporalAccessor oldest, TemporalAccessor latest, int batchSize) {
        this.read = read;
        this.write = write;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        this.oldest = formatter.format(oldest);
        this.latest = formatter.format(latest);
        this.batchSize = batchSize;
    }

    public NSApi getReadApi() {
        return read;
    }

    public NSApi getWriteApi() {
        return write;
    }

    public String getOldest() {
        return oldest;
    }

    public String getLatest() {
        return latest;
    }

    public int getBatchSize() {
        return batchSize;
    }

    /**
     * Finds all missing entries in the target NS instance. Nightscout returns entries in
     * order from latest to oldest, therefore we actually compare in the same order
     *
     * @param synchronizable The Synchronizable that will be tested.
     */
    public void findMissing(Synchronizable synchronizable) throws NightscoutIOException, NightscoutServerException {
        synchronizable.reset();
        GetBuilder sourceGetBuilder;
        GetBuilder targetGetBuilder;
        JsonArray sourceEntries = new JsonArray();
        JsonArray targetEntries;
        String dateField = synchronizable.getDateField();
        do {
            sourceGetBuilder = read.createGet(synchronizable.getApiPath());
            sourceGetBuilder.count(batchSize);

            if (sourceEntries.size() == 0) {  // first search (include start: lte)
                sourceGetBuilder.find(dateField).lte(latest);
            } else {    // next search, start at oldest found entry (excluded: lt)
                String last = sourceEntries.get(sourceEntries.size() - 1).getAsJsonObject().get(dateField).getAsString();
                sourceGetBuilder.find(dateField).lt(last);
            }
            sourceGetBuilder.find(dateField).gte(oldest);

            sourceEntries = sourceGetBuilder.getRaw().getAsJsonArray();
            synchronizable.incrFindCount(sourceEntries.size());
            Main.logger().log(Level.FINE, "Found %d entries in source", sourceEntries.size());

            if (sourceEntries.size() > 0) {
                int i = 0;
                JsonObject original = sourceEntries.get(i).getAsJsonObject();
                String originalDate = original.get(dateField).getAsString();

                do {
                    // get entries from target to compare
                    targetGetBuilder = write.createGet(synchronizable.getApiPath());
                    targetGetBuilder.count(batchSize);
                    if (i == 0) // first search
                        targetGetBuilder.find(dateField).lte(originalDate);
                    else // next search, start at oldest found entry
                        targetGetBuilder.find(dateField).lt(originalDate);
                    targetGetBuilder.find(dateField).gte(oldest);
                    targetEntries = targetGetBuilder.getRaw().getAsJsonArray();
                    Main.logger().log(Level.FINE, "Found %d entries in target", targetEntries.size());

                    if (targetEntries.size() == 0) {  // if no result in target instance, all entries are missing
                        Main.logger().log(Level.FINE, "Marking remaining %d entries as missing", sourceEntries.size() - i);
                        for (; i < sourceEntries.size(); i++)
                            synchronizable.putMissing(sourceEntries.get(i).getAsJsonObject());
                    } else {
                        // start comparing entries from source with target
                        int k = 0;
                        do {
                            original = sourceEntries.get(i).getAsJsonObject();
                            originalDate = original.get(dateField).getAsString();
                            String compareDate = targetEntries.get(k).getAsJsonObject().get(dateField).getAsString();
                            if (originalDate.equals(compareDate)) {     // not missing
                                Main.logger().log(Level.FINE, "- %s IS NOT missing", originalDate);
                                k++;
                                i++;
                            } else {
                                ZonedDateTime originalZDT = getZonedDateTime(originalDate);
                                ZonedDateTime compareZDT = getZonedDateTime(compareDate);
                                if (originalZDT.isAfter(compareZDT)) {   // original is missing
                                    Main.logger().log(Level.FINE, "+ %s IS missing", originalDate);
                                    synchronizable.putMissing(original);
                                    i++;
                                } else {    // original is newer then compare, test next
                                    k++;
                                }
                            }
                        } while (i < sourceEntries.size() && k < targetEntries.size());
                        // compare as long as there is something to compare
                    }
                } while (i < sourceEntries.size());
                // repeat if there are still entries not compared (target has more results than batch size)
            }
        } while (sourceEntries.size() != 0);  // repeat as long as source returns something
    }

    private ZonedDateTime getZonedDateTime(String value) throws NightscoutIOException {
        try {
            TemporalAccessor t = DateTimeFormatter.ISO_DATE_TIME.parse(value);
            if (t.isSupported(ChronoField.OFFSET_SECONDS))
                return ZonedDateTime.from(t);
            else return LocalDateTime.from(t).atZone(ZoneId.of("UTC"));
        } catch (DateTimeParseException e) {
            throw new NightscoutIOException("Could not parse date: " + value, e);
        }
    }

    public void postMissing(Synchronizable synchronizable) throws NightscoutIOException, NightscoutServerException {
        write.createPost(synchronizable.getApiPath())
                .setBody(synchronizable.getMissing().toString())
                .send();
    }

    public void close() throws IOException {
        read.close();
        write.close();
    }
}
