package de.opendiabetes.vault.parser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.opendiabetes.vault.nsapi.exception.NightscoutDataException;

public class StatusParser implements Parser<Status> {
    /**
     * Parses the Nightscout server status to a {@link Status} object.
     */
    @Override
    public Status parse(String input) {
        Gson gson = new GsonBuilder().create();
        Status status = gson.fromJson(input, Status.class);
        if (status == null)
            throw new NightscoutDataException("Input cannot be null");
        return status;
    }
}
