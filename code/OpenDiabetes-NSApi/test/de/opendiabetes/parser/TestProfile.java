package de.opendiabetes.parser;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestProfile {
    @Test
    public void testOneBasalTime() {
        ZoneId zone = ZoneId.of("GMT+1");
        Random random = new Random();
        double sens = 1 + random.nextInt(45);
        double carbratio = 1 + 1 + random.nextInt(45);
        Profile.BasalProfile basalProfile = new Profile.BasalProfile(LocalTime.of(0, 0), random.nextDouble() * 5);
        List<Profile.BasalProfile> basalTime = new ArrayList<>();
        basalTime.add(basalProfile);
        Profile profile = new Profile(zone, sens, carbratio, basalTime);

        //before
        assertEquals(sens, profile.getSensitivity());
        assertEquals(carbratio, profile.getCarbratio());
        assertEquals(zone, profile.getTimezone());
        assertEquals(1, profile.getBasalProfiles().size());
        assertEquals(basalTime.get(0).getStart(), profile.getBasalProfiles().get(0).getStart());
        assertEquals(basalTime.get(0).getValue(),profile.getBasalProfiles().get(0).getValue());
        profile.toZulu();

        //after
        assertEquals(sens,profile.getSensitivity());
        assertEquals(carbratio, profile.getCarbratio());
        assertEquals(ZoneId.of("Zulu"),profile.getTimezone());
        assertEquals(1, profile.getBasalProfiles().size());
        assertEquals(basalTime.get(0).getStart(), profile.getBasalProfiles().get(0).getStart());
        assertEquals(basalTime.get(0).getValue(),profile.getBasalProfiles().get(0).getValue());
    }

    @Test
    public void testToZulu(){
        //TODO
    }
}
