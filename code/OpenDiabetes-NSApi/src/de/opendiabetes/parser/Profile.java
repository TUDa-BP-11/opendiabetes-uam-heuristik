package de.opendiabetes.parser;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Profile {
    private ZoneId timezone;
    private final double sensitivity;
    private final double carbratio;
    private List<BasalProfile> basalProfiles;

    public Profile(ZoneId timezone, double sensitivity, double carbratio, List<BasalProfile> basalProfiles) {
        this.timezone = timezone;
        this.sensitivity = sensitivity;
        this.carbratio = carbratio;
        this.basalProfiles = basalProfiles;
    }

    public ZoneId getTimezone() {
        return timezone;
    }

    public double getSensitivity() {
        return sensitivity;
    }

    public double getCarbratio() {
        return carbratio;
    }

    public List<BasalProfile> getBasalProfiles() {
        return basalProfiles;
    }

    public void setBasalProfiles(List<BasalProfile> basalProfiles) {
        this.basalProfiles = basalProfiles;
    }

    public void adjustProfile() {

        int offset = -timezone.getRules().getOffset(Instant.EPOCH).getTotalSeconds() / 60;
        List<Profile.BasalProfile> list = new ArrayList<>();

        for (BasalProfile basalProfile : basalProfiles) {

            // time in minutes
            int time = (basalProfile.getStart().getMinute() + basalProfile.getStart().getHour() * 60 + offset);
            if (time < 0) {
                time += (24 * 60);
            } else {
                time %= (24 * 60);
            }

            list.add(new Profile.BasalProfile(LocalTime.of(time / 60, time % 60), basalProfile.getValue()));
        }
        list.sort(new BasalProfileComparator());

        // add new 00:00
        if (list.get(0).getStart().getHour() * 60 + list.get(0).getStart().getMinute() > 0) {
            list.add(0, new Profile.BasalProfile(LocalTime.of(0, 0), list.get(list.size() - 1).getValue()));
        }

        //remove duplicates
        for (int i = 1; i < list.size(); i++) {
            if (list.get(i - 1).getValue() == list.get(i).getValue()) {
                list.remove(i);
                i--;
            }
        }

        timezone = ZoneId.of("Zulu");
        basalProfiles = list;
    }


    public static class BasalProfile {
        private final LocalTime start;
        private final double value;

        public BasalProfile(LocalTime start, double value) {
            this.start = start;
            this.value = value;
        }

        public LocalTime getStart() {
            return start;
        }

        public double getValue() {
            return value;
        }
    }


    private static class BasalProfileComparator implements Comparator<BasalProfile> {

        @Override
        public int compare(Profile.BasalProfile profile1, Profile.BasalProfile profile2) {
            return (profile1.getStart().getHour() * 60 + profile1.getStart().getMinute())
                    - (profile2.getStart().getHour() * 60 + profile2.getStart().getMinute());
        }

    }

}
