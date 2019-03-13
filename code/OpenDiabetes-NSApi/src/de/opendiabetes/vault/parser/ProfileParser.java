package de.opendiabetes.vault.parser;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class ProfileParser implements Parser<Profile> {
    /**
     * Parsers a profile. The profile has to contain at least the following fields:<br>
     * - timezone <br>
     * - sens <br>
     * - carbratio <br>
     * - basal <br>
     *
     * @param input profile as a JSON formatted string. Either an array with one profile or the profile directly
     * @return the parsed profile
     */
    @Override
    public Profile parse(String input) {
        JsonParser parser = new JsonParser();
        JsonElement element = parser.parse(input);

        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            if (array.size() == 0)
                throw new RuntimeException("No profile found!");
            element = array.get(0);
        }

        if (!element.isJsonObject())
            throw new RuntimeException("No profile found!");

        JsonObject object = element.getAsJsonObject().getAsJsonObject("store").getAsJsonObject("Default");
        ZoneId timezone = ZoneId.of(object.get("timezone").getAsString());
        double sensitivity = object.getAsJsonArray("sens").get(0).getAsJsonObject().get("value").getAsDouble();
        double carbratio = object.getAsJsonArray("carbratio").get(0).getAsJsonObject().get("value").getAsDouble();
        List<Profile.BasalProfile> basalProfiles = new ArrayList<>();
        for (JsonElement e : object.getAsJsonArray("basal")) {
            JsonObject o = e.getAsJsonObject();
            basalProfiles.add(new Profile.BasalProfile(LocalTime.parse(o.get("time").getAsString()), o.get("value").getAsDouble()));
        }
        return new Profile(timezone, sensitivity, carbratio, basalProfiles);
    }
}
