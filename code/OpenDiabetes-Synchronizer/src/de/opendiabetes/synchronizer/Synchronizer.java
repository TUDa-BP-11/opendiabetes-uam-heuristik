package de.opendiabetes.synchronizer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.opendiabetes.nsapi.GetBuilder;
import de.opendiabetes.nsapi.NSApi;
import de.opendiabetes.nsapi.exception.NightscoutIOException;
import de.opendiabetes.nsapi.exception.NightscoutServerException;

import java.io.IOException;
import java.time.ZonedDateTime;

public class Synchronizer {
    private NSApi read;
    private NSApi write;
    private String start;
    private String end;
    private int batchSize;

    private boolean debug = false;

    public Synchronizer(NSApi read, NSApi write, String start, String end, int batchSize) {
        this.read = read;
        this.write = write;
        this.start = start;
        this.end = end;
        this.batchSize = batchSize;
    }

    public NSApi getReadApi() {
        return read;
    }

    public NSApi getWriteApi() {
        return write;
    }

    public String getStart() {
        return start;
    }

    public String getEnd() {
        return end;
    }

    public int getBatchSize() {
        return batchSize;
    }

    /**
     * Finds all missing entries in the target NS instance. Nightscout returns entries in order from newest to oldest.
     * Therefore we actually compare in reverse order, from end to start
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

            if (sourceEntries.size() == 0) {  // first search
                sourceGetBuilder.find(dateField).lte(end);
            } else {    // next search, start at oldest found entry
                String last = sourceEntries.get(sourceEntries.size() - 1).getAsJsonObject().get(dateField).getAsString();
                sourceGetBuilder.find(dateField).lt(last);
            }
            sourceGetBuilder.find(dateField).gte(start);

            sourceEntries = sourceGetBuilder.getRaw().getAsJsonArray();
            synchronizable.incrFindCount(sourceEntries.size());
            if (debug)
                System.out.println("Found " + sourceEntries.size() + " entries in source");

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
                    targetGetBuilder.find(dateField).gte(start);
                    targetEntries = targetGetBuilder.getRaw().getAsJsonArray();
                    if (debug)
                        System.out.println("Found " + targetEntries.size() + " entries in target");

                    if (targetEntries.size() == 0) {  // if no result in target instance, all entries are missing
                        if (debug)
                            System.out.println("Marking remaining " + (sourceEntries.size() - i) + " entries as missing");
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
                                if (debug)
                                    System.out.println("- " + originalDate + " IS NOT missing");
                                k++;
                                i++;
                            } else {
                                ZonedDateTime originalZDT = ZonedDateTime.parse(originalDate);
                                ZonedDateTime compareZDT = ZonedDateTime.parse(compareDate);
                                if (originalZDT.isAfter(compareZDT)) {   // original is missing
                                    if (debug)
                                        System.out.println("+ " + originalDate + " IS missing");
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

    public void postMissing(Synchronizable synchronizable) throws NightscoutIOException, NightscoutServerException {
        write.createPost(synchronizable.getApiPath())
                .setBody(synchronizable.getMissing().toString())
                .send();
    }

    public void close() throws IOException {
        read.close();
        write.close();
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}
