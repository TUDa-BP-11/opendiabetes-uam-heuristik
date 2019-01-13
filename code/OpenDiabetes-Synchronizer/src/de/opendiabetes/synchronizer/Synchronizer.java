package de.opendiabetes.synchronizer;

import com.mashape.unirest.http.exceptions.UnirestException;
import de.opendiabetes.nsapi.GetBuilder;
import de.opendiabetes.nsapi.NSApi;
import org.json.JSONArray;
import org.json.JSONObject;

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
    public void findMissing(Synchronizable synchronizable) {
        synchronizable.reset();
        GetBuilder sourceGetBuilder;
        GetBuilder targetGetBuilder;
        JSONArray sourceEntries = new JSONArray();
        JSONArray targetEntries;
        String dateField = synchronizable.getDateField();
        try {
            do {
                sourceGetBuilder = new GetBuilder(read.get(synchronizable.getApiPath()));
                sourceGetBuilder.count(batchSize);

                if (sourceEntries.length() == 0) {  // first search
                    sourceGetBuilder.find(dateField).lte(end);
                } else {    // next search, start at oldest found entry
                    String last = sourceEntries.getJSONObject(sourceEntries.length() - 1).getString(dateField);
                    sourceGetBuilder.find(dateField).lt(last);
                }
                sourceGetBuilder.find(dateField).gte(start);

                sourceEntries = sourceGetBuilder.get().getArray();
                synchronizable.incrFindCount(sourceEntries.length());
                if (debug)
                    System.out.println("Found " + sourceEntries.length() + " entries in source");

                if (sourceEntries.length() > 0) {
                    int i = 0;
                    JSONObject original = sourceEntries.getJSONObject(i);
                    String originalDate = original.getString(dateField);

                    do {
                        // get entries from target to compare
                        targetGetBuilder = new GetBuilder(write.get(synchronizable.getApiPath()));
                        targetGetBuilder.count(batchSize);
                        if (i == 0) // first search
                            targetGetBuilder.find(dateField).lte(originalDate);
                        else // next search, start at oldest found entry
                            targetGetBuilder.find(dateField).lt(originalDate);
                        targetGetBuilder.find(dateField).gte(start);
                        targetEntries = targetGetBuilder.get().getArray();
                        if (debug)
                            System.out.println("Found " + targetEntries.length() + " entries in target");

                        if (targetEntries.length() == 0) {  // if no result in target instance, all entries are missing
                            if (debug)
                                System.out.println("Marking remaining " + (sourceEntries.length() - i) + " entries as missing");
                            for (; i < sourceEntries.length(); i++)
                                synchronizable.putMissing(sourceEntries.getJSONObject(i));
                        } else {
                            // start comparing entries from source with target
                            int k = 0;
                            do {
                                original = sourceEntries.getJSONObject(i);
                                originalDate = original.getString(dateField);
                                String compareDate = targetEntries.getJSONObject(k).getString(dateField);
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
                            } while (i < sourceEntries.length() && k < targetEntries.length());
                            // compare as long as there is something to compare
                        }
                    } while (i < sourceEntries.length());
                    // repeat if there are still entries not compared (target has more results than batch size)
                }
            } while (sourceEntries.length() != 0);  // repeat as long as source returns something
        } catch (UnirestException e) {
            throw new SynchronizerException("Exception while trying to compile list of missing " + synchronizable.getApiPath() + " entries!", e);
        }
    }

    public void postMissing(Synchronizable synchronizable) {
        try {
            write.post(synchronizable.getApiPath(), synchronizable.getMissing().toString());
        } catch (UnirestException e) {
            throw new SynchronizerException("Exception while trying to post missing " + synchronizable.getApiPath() + " entries!", e);
        }
    }

    public void close() {
        try {
            read.close();
            write.close();
        } catch (IOException e) {
            throw new SynchronizerException("Exception while trying to close connection to Nightscout apis!", e);
        }
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}
