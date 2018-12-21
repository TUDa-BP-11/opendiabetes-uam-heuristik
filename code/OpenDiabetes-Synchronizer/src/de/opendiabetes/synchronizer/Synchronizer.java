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

    private int findEntriesCount;
    private JSONArray missingEntries;
    private int findTreatmentsCount;
    private JSONArray missingTreatments;

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

    public void findMissingEntries() {
        findEntriesCount = 0;
        missingEntries = new JSONArray();
        GetBuilder getBuilder;
        JSONArray found = new JSONArray();
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
                findEntriesCount += found.length();

                for (int i = 0; i < found.length(); i++) {
                    JSONObject entry = found.getJSONObject(i);
                    String date = entry.getString("dateString");
                    JSONArray target = write.getEntries().find("dateString").eq(date).get().getArray();
                    if (target.length() == 0)    //no result found
                        missingEntries.put(entry);
                }
            } while (found.length() != 0);
        } catch (UnirestException e) {
            System.out.println("Exception while trying to compile list of missingEntries entries");
            e.printStackTrace();
        }
    }

    public int getFindEntriesCount() {
        return findEntriesCount;
    }

    public int getMissingEntriesCount() {
        return missingEntries.length();
    }

    public void postMissingEntries() {
        if (missingEntries.length() > 0) {
            try {
                write.postEntries(missingEntries.toString());
            } catch (UnirestException e) {
                System.out.println("Exception while trying to post missing entries");
                e.printStackTrace();
            }
        }
    }

    public void findMissingTreatments() {
        findTreatmentsCount = 0;
        missingTreatments = new JSONArray();
        GetBuilder getBuilder;
        JSONArray found = new JSONArray();
        try {
            do {
                getBuilder = read.getTreatments().count(batchSize);

                if (found.length() == 0) {  // first search
                    getBuilder.find("created_at").gte(start);
                } else {    // next search, start after oldest found entry
                    String last = found.getJSONObject(0).getString("created_at");
                    getBuilder.find("created_at").gt(last);
                }

                if (end != null)
                    getBuilder.find("created_at").lt(end);

                found = getBuilder.get().getArray();
                findTreatmentsCount += found.length();

                for (int i = 0; i < found.length(); i++) {
                    JSONObject entry = found.getJSONObject(i);
                    String date = entry.getString("created_at");
                    JSONArray target = write.getTreatments().find("created_at").eq(date).get().getArray();
                    if (target.length() == 0)    //no result found
                        missingTreatments.put(entry);
                }
            } while (found.length() != 0);
        } catch (UnirestException e) {
            System.out.println("Exception while trying to compile list of missing treatments");
            e.printStackTrace();
        }
    }

    public int getFindTreatmentsCount() {
        return findTreatmentsCount;
    }

    public int getMissingTreatmentsCount() {
        return missingTreatments.length();
    }

    public void postMissingTreatments() {
        if (missingTreatments.length() > 0) {
            try {
                write.postTreatments(missingTreatments.toString());
            } catch (UnirestException e) {
                System.out.println("Exception while trying to post missing treatments");
                e.printStackTrace();
            }
        }
    }

    public void close() {
        read.close();
        write.close();
    }
}
