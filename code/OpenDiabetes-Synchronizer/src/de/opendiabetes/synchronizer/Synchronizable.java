package de.opendiabetes.synchronizer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class Synchronizable {
    private final String apiPath;
    private final String dateField;

    private int findCount;
    private JsonArray missing;

    public Synchronizable(String apiPath, String dateField) {
        this.apiPath = apiPath;
        this.dateField = dateField;

        this.findCount = 0;
        this.missing = new JsonArray();
    }

    public String getApiPath() {
        return apiPath;
    }

    public String getDateField() {
        return dateField;
    }

    public int getFindCount() {
        return findCount;
    }

    public void incrFindCount(int increment) {
        findCount += increment;
    }

    public JsonArray getMissing() {
        return missing;
    }

    public int getMissingCount() {
        return missing.size();
    }

    public void putMissing(JsonObject entry) {
        missing.add(entry);
    }

    public void reset() {
        findCount = 0;
        missing = new JsonArray();
    }
}
