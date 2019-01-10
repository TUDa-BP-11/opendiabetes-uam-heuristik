package de.opendiabetes.synchronizer;

import org.json.JSONArray;
import org.json.JSONObject;

public class Synchronizable {
    private final String apiPath;
    private final String dateField;

    private int findCount;
    private JSONArray missing;

    public Synchronizable(String apiPath, String dateField) {
        this.apiPath = apiPath;
        this.dateField = dateField;

        this.findCount = 0;
        this.missing = new JSONArray();
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

    public JSONArray getMissing() {
        return missing;
    }

    public int getMissingCount() {
        return missing.length();
    }

    public void putMissing(JSONObject entry) {
        missing.put(entry);
    }

    public void reset() {
        findCount = 0;
        missing = new JSONArray();
    }
}
