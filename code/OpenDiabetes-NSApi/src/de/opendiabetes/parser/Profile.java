package de.opendiabetes.parser;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

public class Profile {
    private ZoneId timezone;
    private double sensitivity;
    private double carbratio;
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

    public static class BasalProfile {
        private LocalTime start;
        private double value;

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
}
