package de.opendiabetes.parser;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestProfileParser {
    @Test
    public void randomProfile() {
        Random random = new Random();
        ZoneId zone = ZoneId.systemDefault();
        double sensitivity = random.nextDouble() * 100;
        double carbratio = random.nextDouble() * 100;
        int basals = 1 + random.nextInt(9);
        LocalTime[] basalTimes = new LocalTime[basals];
        double[] basalValues = new double[basals];
        String input = "[{" +
                "  \"store\": {" +
                "    \"Default\": {" +
                "      \"sens\": [" +
                "        {" +
                "          \"time\": \"00:00\"," +
                "          \"timeAsSeconds\": \"0\"," +
                "          \"value\": \"" + sensitivity + "\"" +
                "        }" +
                "      ]," +
                "      \"basal\": [";
        for (int i = 0; i < basals; i++) {
            basalTimes[i] = LocalTime.of(random.nextInt(24), random.nextInt(60));
            basalValues[i] = random.nextDouble() * 2;
            input += "{" +
                    "\"time\": \"" + basalTimes[i].toString() + "\"," +
                    "\"value\": \"" + basalValues[i] + "\"" +
                    "}";
            if (i < basals - 1)
                input += ",";
        }
        input += "      ]," +
                "      \"carbratio\": [" +
                "        {" +
                "          \"time\": \"00:00\"," +
                "          \"timeAsSeconds\": \"0\"," +
                "          \"value\": \"" + carbratio + "\"" +
                "        }" +
                "      ]," +
                "      \"timezone\": \"" + zone.toString() + "\"" +
                "    }" +
                "  }" +
                "}]";

        Profile profile = new ProfileParser().parse(input);
        assertEquals(zone, profile.getTimezone());
        assertEquals(sensitivity, profile.getSensitivity());
        assertEquals(carbratio, profile.getCarbratio());
        assertEquals(basals, profile.getBasalProfiles().size());
        for (int i = 0; i < basals; i++) {
            assertEquals(basalTimes[i], profile.getBasalProfiles().get(i).getStart());
            assertEquals(basalValues[i], profile.getBasalProfiles().get(i).getValue());
        }
    }
}
