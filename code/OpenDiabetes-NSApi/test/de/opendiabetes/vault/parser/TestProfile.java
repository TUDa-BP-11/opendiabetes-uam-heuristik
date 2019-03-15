package de.opendiabetes.vault.parser;

import de.opendiabetes.vault.parser.Profile;
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
        assertEquals(basalTime.get(0).getValue(), profile.getBasalProfiles().get(0).getValue());
        profile.toZulu();

        //after
        assertEquals(sens, profile.getSensitivity());
        assertEquals(carbratio, profile.getCarbratio());
        assertEquals(ZoneId.of("Zulu"), profile.getTimezone());
        assertEquals(1, profile.getBasalProfiles().size());
        assertEquals(basalTime.get(0).getStart(), profile.getBasalProfiles().get(0).getStart());
        assertEquals(basalTime.get(0).getValue(), profile.getBasalProfiles().get(0).getValue());
    }

    @Test
    public void testToZuluPlus() {
        ZoneId zone = ZoneId.of("GMT+3");
        List<Profile.BasalProfile> basalProfileList = new ArrayList<>();
        basalProfileList.add(new Profile.BasalProfile(LocalTime.of(0, 0), 0.8));
        basalProfileList.add(new Profile.BasalProfile(LocalTime.of(6, 20), 1.0));
        basalProfileList.add(new Profile.BasalProfile(LocalTime.of(20, 0), 0.8));
        Profile profile = new Profile(zone, 0, 0, basalProfileList);
        profile.toZulu();

        assertEquals(ZoneId.of("Zulu"), profile.getTimezone());
        assertEquals(3, profile.getBasalProfiles().size());
        assertEquals(0, profile.getBasalProfiles().get(0).getStart().getHour());
        assertEquals(0.8, profile.getBasalProfiles().get(0).getValue());
        assertEquals(3, profile.getBasalProfiles().get(1).getStart().getHour());
        assertEquals(20, profile.getBasalProfiles().get(1).getStart().getMinute());
        assertEquals(1.0, profile.getBasalProfiles().get(1).getValue());
        assertEquals(17, profile.getBasalProfiles().get(2).getStart().getHour());
        assertEquals(0.8, profile.getBasalProfiles().get(2).getValue());

        zone = ZoneId.of("GMT+12");
        basalProfileList = new ArrayList<>();
        basalProfileList.add(new Profile.BasalProfile(LocalTime.of(0, 0), 0.5));
        basalProfileList.add(new Profile.BasalProfile(LocalTime.of(12, 0), 1.0));
        profile = new Profile(zone, 0, 0, basalProfileList);
        profile.toZulu();

        assertEquals(2, profile.getBasalProfiles().size());
        assertEquals(0, profile.getBasalProfiles().get(0).getStart().getHour());
        assertEquals(1.0, profile.getBasalProfiles().get(0).getValue());
        assertEquals(12, profile.getBasalProfiles().get(1).getStart().getHour());
        assertEquals(0.5, profile.getBasalProfiles().get(1).getValue());


    }

    @Test
    public void testToZuluMinus() {
        ZoneId zone = ZoneId.of("GMT-3");
        List<Profile.BasalProfile> basalProfileList = new ArrayList<>();
        basalProfileList.add(new Profile.BasalProfile(LocalTime.of(0, 0), 0.8));
        basalProfileList.add(new Profile.BasalProfile(LocalTime.of(6, 20), 1.0));
        basalProfileList.add(new Profile.BasalProfile(LocalTime.of(20, 0), 0.8));
        Profile profile = new Profile(zone, 0, 0, basalProfileList);
        profile.toZulu();

        assertEquals(ZoneId.of("Zulu"), profile.getTimezone());
        assertEquals(3, profile.getBasalProfiles().size());
        assertEquals(0, profile.getBasalProfiles().get(0).getStart().getHour());
        assertEquals(0.8, profile.getBasalProfiles().get(0).getValue());
        assertEquals(9, profile.getBasalProfiles().get(1).getStart().getHour());
        assertEquals(20, profile.getBasalProfiles().get(1).getStart().getMinute());
        assertEquals(1.0, profile.getBasalProfiles().get(1).getValue());
        assertEquals(23, profile.getBasalProfiles().get(2).getStart().getHour());
        assertEquals(0.8, profile.getBasalProfiles().get(2).getValue());

        zone = ZoneId.of("GMT-12");
        basalProfileList = new ArrayList<>();
        basalProfileList.add(new Profile.BasalProfile(LocalTime.of(0, 0), 0.5));
        basalProfileList.add(new Profile.BasalProfile(LocalTime.of(12, 0), 1.0));
        profile = new Profile(zone, 0, 0, basalProfileList);
        profile.toZulu();

        assertEquals(2, profile.getBasalProfiles().size());
        assertEquals(0, profile.getBasalProfiles().get(0).getStart().getHour());
        assertEquals(1.0, profile.getBasalProfiles().get(0).getValue());
        assertEquals(12, profile.getBasalProfiles().get(1).getStart().getHour());
        assertEquals(0.5, profile.getBasalProfiles().get(1).getValue());
    }

    @Test
    public void testLessEntries() {
        ZoneId zone = ZoneId.of("GMT+6");
        List<Profile.BasalProfile> basalProfileList = new ArrayList<>();
        basalProfileList.add(new Profile.BasalProfile(LocalTime.of(0, 0), 0.5));
        basalProfileList.add(new Profile.BasalProfile(LocalTime.of(6, 0), 1.0));
        basalProfileList.add(new Profile.BasalProfile(LocalTime.of(20, 0), 0.5));
        Profile profile = new Profile(zone, 0, 0, basalProfileList);
        profile.toZulu();

        assertEquals(ZoneId.of("Zulu"), profile.getTimezone());
        assertEquals(2, profile.getBasalProfiles().size());
        assertEquals(0, profile.getBasalProfiles().get(0).getStart().getHour());
        assertEquals(1.0, profile.getBasalProfiles().get(0).getValue());
        assertEquals(14, profile.getBasalProfiles().get(1).getStart().getHour());
        assertEquals(0.5, profile.getBasalProfiles().get(1).getValue());

        zone = ZoneId.of("GMT-4");
        basalProfileList = new ArrayList<>();
        basalProfileList.add(new Profile.BasalProfile(LocalTime.of(0, 0), 0.5));
        basalProfileList.add(new Profile.BasalProfile(LocalTime.of(6, 0), 1.0));
        basalProfileList.add(new Profile.BasalProfile(LocalTime.of(20, 0), 0.5));
        profile = new Profile(zone, 0, 0, basalProfileList);
        profile.toZulu();

        assertEquals(ZoneId.of("Zulu"), profile.getTimezone());
        assertEquals(2, profile.getBasalProfiles().size());
        assertEquals(0, profile.getBasalProfiles().get(0).getStart().getHour());
        assertEquals(0.5, profile.getBasalProfiles().get(0).getValue());
        assertEquals(10, profile.getBasalProfiles().get(1).getStart().getHour());
        assertEquals(1.0, profile.getBasalProfiles().get(1).getValue());
    }

    @Test
    public void testMoreEntries() {
        ZoneId zone = ZoneId.of("GMT+7");
        List<Profile.BasalProfile> basalProfileList = new ArrayList<>();
        basalProfileList.add(new Profile.BasalProfile(LocalTime.of(0, 0), 0.7));
        basalProfileList.add(new Profile.BasalProfile(LocalTime.of(12, 0), 1.1));
        Profile profile = new Profile(zone, 0, 0, basalProfileList);
        profile.toZulu();

        assertEquals(ZoneId.of("Zulu"), profile.getTimezone());
        assertEquals(3, profile.getBasalProfiles().size());
        assertEquals(0, profile.getBasalProfiles().get(0).getStart().getHour());
        assertEquals(0.7, profile.getBasalProfiles().get(0).getValue());
        assertEquals(5, profile.getBasalProfiles().get(1).getStart().getHour());
        assertEquals(1.1, profile.getBasalProfiles().get(1).getValue());
        assertEquals(17, profile.getBasalProfiles().get(2).getStart().getHour());
        assertEquals(0.7, profile.getBasalProfiles().get(2).getValue());

        zone = ZoneId.of("GMT-9");
        basalProfileList = new ArrayList<>();
        basalProfileList.add(new Profile.BasalProfile(LocalTime.of(0, 0), 0.7));
        basalProfileList.add(new Profile.BasalProfile(LocalTime.of(12, 0), 1.1));
        profile = new Profile(zone, 0, 0, basalProfileList);
        profile.toZulu();

        assertEquals(ZoneId.of("Zulu"), profile.getTimezone());
        assertEquals(3, profile.getBasalProfiles().size());
        assertEquals(0, profile.getBasalProfiles().get(0).getStart().getHour());
        assertEquals(1.1, profile.getBasalProfiles().get(0).getValue());
        assertEquals(9, profile.getBasalProfiles().get(1).getStart().getHour());
        assertEquals(0.7, profile.getBasalProfiles().get(1).getValue());
        assertEquals(21, profile.getBasalProfiles().get(2).getStart().getHour());
        assertEquals(1.1, profile.getBasalProfiles().get(2).getValue());
    }

    @Test
    public void testNoChange() {
        ZoneId zone = ZoneId.of("Zulu");
        List<Profile.BasalProfile> basalProfileList = new ArrayList<>();
        basalProfileList.add(new Profile.BasalProfile(LocalTime.of(0, 0), 0.4));
        basalProfileList.add(new Profile.BasalProfile(LocalTime.of(7, 0), 0.6));
        basalProfileList.add(new Profile.BasalProfile(LocalTime.of(21, 0), 0.4));
        Profile profile = new Profile(zone, 0, 0, basalProfileList);
        profile.toZulu();

        assertEquals(basalProfileList.size(), profile.getBasalProfiles().size());
        for (int i = 0; i < basalProfileList.size(); i++) {
            assertEquals(basalProfileList.get(i).getStart(), profile.getBasalProfiles().get(i).getStart());
            assertEquals(basalProfileList.get(i).getValue(), profile.getBasalProfiles().get(i).getValue());
        }

    }


}
