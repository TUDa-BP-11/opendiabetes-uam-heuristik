package de.opendiabetes.synchronizer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.opendiabetes.nsapi.NSApi;

/**
 * A Synchronizable represents a Nightscout data type that can be obtained (GET) and uploaded (POST) via an API path.
 * The datatype has to contain and a date field which will be used to check if the entry exists on the target instance.
 */
public class Synchronizable {
    private final String apiPath;
    private final String dateField;

    private int findCount;
    private JsonArray missing;

    /**
     * @param apiPath   API path used with {@link NSApi#createGet(String)}. Has to return an array of json objects.
     * @param dateField the name of the field which holds information about the date and time of your data object
     */
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

    /**
     * @return All entries that are missing in the target instance
     */
    public JsonArray getMissing() {
        return missing;
    }

    /**
     * @return Amount of entries that are missing in the target instance
     */
    public int getMissingCount() {
        return missing.size();
    }

    /**
     * @return Amount of entries found in the source instance
     */
    public int getFindCount() {
        return findCount;
    }

    void incrFindCount() {
        findCount++;
    }

    void putMissing(JsonObject entry) {
        missing.add(entry);
    }

    void reset() {
        findCount = 0;
        missing = new JsonArray();
    }
}
