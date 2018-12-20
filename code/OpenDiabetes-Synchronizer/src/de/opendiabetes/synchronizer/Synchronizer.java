package de.opendiabetes.synchronizer;

import com.mashape.unirest.http.exceptions.UnirestException;
import de.opendiabetes.nsapi.GetBuilder;
import de.opendiabetes.nsapi.NSApi;
import org.json.JSONArray;
import org.json.JSONObject;

public class Synchronizer {
    private NSApi read;
    private NSApi write;
    private String start;
    private String end;
    private int batchSize;

    private JSONArray missing = new JSONArray();

    public Synchronizer(NSApi read, NSApi write, String start, String end, int batchSize) {
        this.read = read;
        this.write = write;
        this.start = start;
        this.end = end;
        this.batchSize = batchSize;
    }

    public void findMissing() {
        GetBuilder getBuilder;
        JSONArray found = new JSONArray();
        int fc = 0;
        try {
            do {
                getBuilder = read.getEntries().count(batchSize);

                if (found.length() == 0) {  // first search
                    getBuilder.find("dateString").gte(start);
                } else {    // next search, start after oldest found entry
                    String last = found.getJSONObject(0).getString("dateString");
                    getBuilder.find("dateString").gt(last);
                }

                if (end != null)
                    getBuilder.find("dateString").lt(end);

                found = getBuilder.get().getArray();
                fc += found.length();

                for (int i = 0; i < found.length(); i++) {
                    JSONObject entry = found.getJSONObject(i);
                    String date = entry.getString("dateString");
                    JSONArray target = write.getEntries().find("dateString").eq(date).get().getArray();
                    if (target.length() == 0)    //no result found
                        missing.put(entry);
                }
            } while (found.length() != 0);
            System.out.println("Found " + fc + " entries of which " + missing.length() + " are missing in the target instance.");
        } catch (UnirestException e) {
            System.out.println("Exception while trying to compile list of missing entries");
            e.printStackTrace();
        }
    }

    public void postMissing() {
        if (missing.length() > 0) {
            System.out.println("Uploading missing entries...");
            try {
                write.postEntries(missing.toString());
            } catch (UnirestException e) {
                System.out.println("Exception while trying to POST missing entries");
                e.printStackTrace();
            }
        }
    }

    public void close() {
        read.close();
        write.close();
    }
}
