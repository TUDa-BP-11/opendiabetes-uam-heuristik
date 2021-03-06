package de.opendiabetes.vault.main.math;

import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.parser.Profile;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class BasalCalcDifferenceTest {

    private static final long ONE_MINUTE = 60 * 1000;
    private static final double DELTA = 1e-15;

    @Test
    public void noBasalProfile() {
        assertThrows(IllegalArgumentException.class, () ->
                BasalCalculatorTools.calcBasalDifference(new ArrayList<>(), new Profile(ZoneId.of("Zulu"), 0, 0, new ArrayList<>())));
    }

    @Test
    public void singleBasalProfile() {
        Random random = new Random();
        List<VaultEntry> testTreatments = new ArrayList<>();
        int size = 1 + random.nextInt(5);
        Date date = new Date(0);
        for (int i = 0; i < size; i++) {
            int duration = (1 + random.nextInt(12)) * 5;
            VaultEntry entry = new VaultEntry(VaultEntryType.BASAL_MANUAL, date, random.nextInt(10), duration);
            testTreatments.add(entry);
            date = new Date(date.getTime() + duration * ONE_MINUTE);
        }

        //This Entry should get removed
        VaultEntry entry = new VaultEntry(VaultEntryType.BASAL_MANUAL, date, random.nextInt(10), 0);
        testTreatments.add(entry);

        List<Profile.BasalProfile> basalProfiles = new ArrayList<>();
        Profile.BasalProfile basalProfile = new Profile.BasalProfile(LocalTime.of(0, 0), random.nextDouble());
        basalProfiles.add(basalProfile);
        Profile profile = new Profile(ZoneId.of("Zulu"), 0, 0, basalProfiles);
        List<VaultEntry> result = BasalCalculatorTools.calcBasalDifference(testTreatments, profile);

        assertEquals(size, result.size());
        for (int i = 0; i < size; i++) {
            assertEquals(testTreatments.get(i).getTimestamp().getTime(), result.get(i).getTimestamp().getTime());
            assertEquals(testTreatments.get(i).getValue2(), result.get(i).getValue2());
            double expected = (testTreatments.get(i).getValue() / testTreatments.get(i).getValue2()) - (profile.getBasalProfiles().get(0).getValue() / 60);
            assertEquals(expected, result.get(i).getValue(), DELTA);
        }
    }

    @Test
    public void testRecursiveCall() {
        List<Profile.BasalProfile> basalProfiles = new ArrayList<>();
        basalProfiles.add(new Profile.BasalProfile(LocalTime.of(0, 0), 0.6));
        basalProfiles.add(new Profile.BasalProfile(LocalTime.of(0, 20), 1.2));
        basalProfiles.add(new Profile.BasalProfile(LocalTime.of(0, 40), 0.6));
        basalProfiles.add(new Profile.BasalProfile(LocalTime.of(1, 0), 0));
        Profile profile = new Profile(ZoneId.of("Zulu"), 0, 0, basalProfiles);

        List<VaultEntry> testTreatments = new ArrayList<>();
        testTreatments.add(new VaultEntry(VaultEntryType.BASAL_MANUAL, new Date(0), 0, 60));
        List<VaultEntry> result = BasalCalculatorTools.calcBasalDifference(testTreatments, profile);

        assertEquals(3, result.size());
        assertEquals(0, result.get(0).getTimestamp().getTime());
        assertEquals(20, result.get(0).getValue2());
        assertEquals(-0.6 / 60, result.get(0).getValue());
        assertEquals(20, result.get(1).getValue2());
        assertEquals(-1.2 / 60, result.get(1).getValue());
        assertEquals(20, result.get(2).getValue2());
        assertEquals(-0.6 / 60, result.get(2).getValue());


        testTreatments = new ArrayList<>();
        testTreatments.add(new VaultEntry(VaultEntryType.BASAL_MANUAL, new Date(0), 6, 60));
        result = BasalCalculatorTools.calcBasalDifference(testTreatments, profile);

        assertEquals(3, result.size());
        assertEquals(0, result.get(0).getTimestamp().getTime());
        assertEquals(20, result.get(0).getValue2());
        assertEquals(0.1 - 0.6 / 60, result.get(0).getValue());
        assertEquals(20, result.get(1).getValue2());
        assertEquals(0.1 - 1.2 / 60, result.get(1).getValue());
        assertEquals(20, result.get(2).getValue2());
        assertEquals(0.1 - 0.6 / 60, result.get(2).getValue());
    }

    @Test
    public void testCalcTemp() {
        Random random = new Random();
        List<Profile.BasalProfile> basalProfiles = new ArrayList<>();
        double profileRate1 = random.nextDouble();
        double profileRate2 = random.nextDouble();
        basalProfiles.add(new Profile.BasalProfile(LocalTime.of(0, 0), profileRate1));
        basalProfiles.add(new Profile.BasalProfile(LocalTime.of(0, 30), profileRate2));
        Profile profile = new Profile(ZoneId.of("Zulu"), 0, 0, basalProfiles);

        List<VaultEntry> testTreatments = new ArrayList<>();
        testTreatments.add(new VaultEntry(VaultEntryType.BASAL_MANUAL, new Date(0), random.nextDouble() * 5, 20));
        testTreatments.add(new VaultEntry(VaultEntryType.BASAL_MANUAL, new Date(20 * ONE_MINUTE), random.nextDouble() * 5, 20));
        testTreatments.add(new VaultEntry(VaultEntryType.BASAL_MANUAL, new Date(40 * ONE_MINUTE), random.nextDouble() * 5, 20));
        List<VaultEntry> result = BasalCalculatorTools.calcBasalDifference(testTreatments, profile);

        assertEquals(4, result.size());
        VaultEntry entry = testTreatments.get(0);
        VaultEntry resBasal = result.get(0);
        assertEquals(entry.getValue2(), resBasal.getValue2());
        assertEquals(entry.getTimestamp().getTime(), resBasal.getTimestamp().getTime());
        assertEquals(entry.getValue() / 20 - profileRate1 / 60, resBasal.getValue(), DELTA);

        entry = testTreatments.get(1);
        resBasal = result.get(1);
        assertEquals(entry.getValue2() / 2, resBasal.getValue2());
        assertEquals(entry.getTimestamp().getTime(), resBasal.getTimestamp().getTime());
        assertEquals(entry.getValue() / 20 - profileRate1 / 60, resBasal.getValue(), DELTA);

        resBasal = result.get(2);
        assertEquals(entry.getValue2() / 2, resBasal.getValue2());
        assertEquals(entry.getTimestamp().getTime() + 10 * ONE_MINUTE, resBasal.getTimestamp().getTime());
        assertEquals(entry.getValue() / 20 - profileRate2 / 60, resBasal.getValue(), DELTA);

        entry = testTreatments.get(2);
        resBasal = result.get(3);
        assertEquals(entry.getValue2(), resBasal.getValue2());
        assertEquals(entry.getTimestamp().getTime(), resBasal.getTimestamp().getTime());
        assertEquals(entry.getValue() / 20 - profileRate2 / 60, resBasal.getValue(), DELTA);

        testTreatments = new ArrayList<>();
        testTreatments.add(new VaultEntry(VaultEntryType.BASAL_MANUAL, new Date(), 0, 0));
        result = BasalCalculatorTools.calcBasalDifference(testTreatments, profile);
        assertEquals(0, result.size());

        testTreatments = new ArrayList<>();
        testTreatments.add(new VaultEntry(VaultEntryType.BASAL_MANUAL, new Date((24 * 60 * ONE_MINUTE) - (10 * ONE_MINUTE)), random.nextDouble(), 20));
        result = BasalCalculatorTools.calcBasalDifference(testTreatments, profile);
        assertEquals(2, result.size());
        entry = testTreatments.get(0);
        resBasal = result.get(0);
        assertEquals(entry.getValue2() / 2, resBasal.getValue2());
        assertEquals(entry.getTimestamp().getTime(), resBasal.getTimestamp().getTime());
        assertEquals(entry.getValue() / 20 - profileRate2 / 60, resBasal.getValue(), DELTA);

        resBasal = result.get(1);
        assertEquals(entry.getValue2() / 2, resBasal.getValue2());
        assertEquals(entry.getTimestamp().getTime() + 10 * ONE_MINUTE, resBasal.getTimestamp().getTime());
        assertEquals(entry.getValue() / 20 - profileRate1 / 60, resBasal.getValue(), DELTA);
    }

    @Test
    public void testExceptions() {
        //No Entries in BasalProfiles
        List<Profile.BasalProfile> basalProfiles = new ArrayList<>();
        Profile profile = new Profile(ZoneId.of("Zulu"), 2, 2, basalProfiles);
        List<VaultEntry> testTreatments = new ArrayList<>();
        testTreatments.add(new VaultEntry(VaultEntryType.BASAL_MANUAL, new Date(0), 2, 30));
        assertThrows(IllegalArgumentException.class, () -> BasalCalculatorTools.calcBasalDifference(testTreatments, profile));

        //WrongTimezone
        basalProfiles.add(new Profile.BasalProfile(LocalTime.of(0, 0), 0.5));
        Profile profile2 = new Profile(ZoneId.of("GMT+1"), 2, 2, basalProfiles);
        assertThrows(IllegalArgumentException.class, () -> BasalCalculatorTools.calcBasalDifference(testTreatments, profile2));

        //Wrong Treatment Type
        testTreatments.add(new VaultEntry(VaultEntryType.BOLUS_NORMAL, new Date(3000000), 2, 30));
        Profile profile3 = new Profile(ZoneId.of("Zulu"), 2, 2, basalProfiles);
        assertThrows(IllegalArgumentException.class, () -> BasalCalculatorTools.calcBasalDifference(testTreatments, profile3));

        basalProfiles.add(new Profile.BasalProfile(LocalTime.of(3, 0), 1));
        Profile profile4 = new Profile(ZoneId.of("Zulu"), 2, 2, basalProfiles);
        assertThrows(IllegalArgumentException.class, () -> BasalCalculatorTools.calcBasalDifference(testTreatments, profile4));


    }
}
