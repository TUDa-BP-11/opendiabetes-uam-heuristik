package de.opendiabetes.synchronizer;

import com.mashape.unirest.http.exceptions.UnirestException;
import de.opendiabetes.nsapi.GetBuilder;
import de.opendiabetes.nsapi.NSApi;
import org.json.JSONArray;
import org.json.JSONObject;

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
                    sourceGetBuilder.find(dateField).gte(start);
                } else {    // next search, start after oldest found entry
                    String last = sourceEntries.getJSONObject(0).getString(dateField);
                    sourceGetBuilder.find(dateField).gt(last);
                }

                if (end != null)
                    sourceGetBuilder.find(dateField).lt(end);

                sourceEntries = sourceGetBuilder.get().getArray();
                synchronizable.incrFindCount(sourceEntries.length());

                if (sourceEntries.length() > 0) {
                    // entries are always sorted in descending order
                    int i = sourceEntries.length() - 1;
                    JSONObject original = sourceEntries.getJSONObject(i);
                    String originalDate = original.getString(dateField);

                    do {
                        // get entries from target to compare
                        targetGetBuilder = new GetBuilder(write.get(synchronizable.getApiPath()))
                                .count(batchSize)
                                .find(dateField).gte(originalDate);
                        if (end != null)
                            targetGetBuilder.find(dateField).lt(end);

                        targetEntries = targetGetBuilder.get().getArray();
                        if (targetEntries.length() == 0) {  // if no result in target instance, all entries are missing
                            for (; i >= 0; i--)
                                synchronizable.putMissing(sourceEntries.getJSONObject(i));
                        } else {
                            // start comparing entries from source with target
                            int k = targetEntries.length() - 1;
                            do {
                                original = sourceEntries.getJSONObject(i);
                                originalDate = original.getString(dateField);
                                String compareDate = targetEntries.getJSONObject(k).getString(dateField);
                                if (originalDate.equals(compareDate)) {     // not missing
                                    k--;
                                    i--;
                                } else {
                                    ZonedDateTime originalZDT = ZonedDateTime.parse(originalDate);
                                    ZonedDateTime compareZDT = ZonedDateTime.parse(compareDate);
                                    if (originalZDT.isBefore(compareZDT)) {   // original is missing
                                        synchronizable.putMissing(original);
                                        i--;
                                    } else {    // compare is older then original, test next
                                        k--;
                                    }
                                }
                            } while (i >= 0 && k >= 0); // compare as long as there is something to compare
                        }
                    } while (i >= 0);
                    // repeat if there are still entries not compared (target returned more results than batch size)
                }
            } while (sourceEntries.length() != 0);  // repeat as long as source returns something
        } catch (UnirestException e) {
            throw new SynchronizerException("Exception while trying to compile list of missing " + synchronizable.getApiPath() + " entries!");
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
        read.close();
        write.close();
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}
