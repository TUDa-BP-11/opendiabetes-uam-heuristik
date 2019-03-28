package de.opendiabetes.vault.main.math;

import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.parser.Profile;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BasalCalculatorTools {

    /**
     * Shortens the duration of all entries to their next treatment
     * and adjusts the values of the entries accordingly.
     *
     * @param basalTreatments sorted list of vault entries with type
     *                        {@link VaultEntryType#BASAL_MANUAL}
     * @return list of vault entries whose duration won't exceeds the timestamp of their next entries.
     */
    public static List<VaultEntry> adjustBasalTreatments(List<VaultEntry> basalTreatments) {
        List<VaultEntry> result = new ArrayList<>();
        //VaultEntry current;

        if (basalTreatments.size() > 0) {
            for (int i = 0; i < basalTreatments.size() - 1; i++) {
                VaultEntry current = basalTreatments.get(i);
                VaultEntry next = basalTreatments.get(i + 1);

                if (!current.getType().equals(VaultEntryType.BASAL_MANUAL)) {
                    throw new IllegalArgumentException("VaultEntryType should be BASAL_MANUAL");
                }
                long deltaTime = Math.round((next.getTimestamp().getTime() - current.getTimestamp().getTime()) / 60000.0);

                if (deltaTime < 0) {
                    throw new IllegalArgumentException("Input have to be sorted by Timestamp");
                }

                if (deltaTime < current.getValue2()) {
                    double value = current.getValue() * deltaTime / current.getValue2();
                    result.add(new VaultEntry(VaultEntryType.BASAL_MANUAL, current.getTimestamp(), value, deltaTime));
                } else {
                    result.add(new VaultEntry(VaultEntryType.BASAL_MANUAL, current.getTimestamp(), current.getValue(), current.getValue2()));
                }

            }

            VaultEntry last = basalTreatments.get(basalTreatments.size() - 1);
            if (!last.getType().equals(VaultEntryType.BASAL_MANUAL)) {
                throw new IllegalArgumentException("VaultEntryType should be BASAL_MANUAL");
            }
            result.add(new VaultEntry(VaultEntryType.BASAL_MANUAL, last.getTimestamp(), last.getValue(), last.getValue2()));
        }

        return result;
    }

    /**
     * Calculates the difference between Temp Basal Treatments and the basal rates given in the profile.
     * The values of the resulting List is in units per minute.
     *
     * @param basalTreatments list of VaultEntries with type
     * {@link de.opendiabetes.vault.container.VaultEntryType#BASAL_MANUAL}
     * @param profile nightscout profile with Timezone Zulu
     * @return list of VaultEntries with type
     * {@link de.opendiabetes.vault.container.VaultEntryType#BASAL_PROFILE}
     */
    public static List<VaultEntry> calcBasalDifference(List<VaultEntry> basalTreatments, Profile profile) {
        if (!profile.getTimezone().equals(ZoneId.of("Zulu"))){
            throw new IllegalArgumentException("profile Timezone should be Zulu, make sure to run toZulu() before.");
        }
        if (profile.getBasalProfiles().size() < 1) {
            throw new IllegalArgumentException("profile must have at least 1 entry in basalProfiles.");
        }
        List<VaultEntry> result = new ArrayList<>();
        if (profile.getBasalProfiles().size() == 1) {
            double basalRate = profile.getBasalProfiles().get(0).getValue();
            for (VaultEntry entry : basalTreatments) {
                if (!entry.getType().equals(VaultEntryType.BASAL_MANUAL)) {
                    throw new IllegalArgumentException("VaultEntryType should be BASAL_MANUAL but was " + entry.getType().toString());
                }
                if (entry.getValue2() <= 0) {
                    continue;
                }
                double value = entry.getValue() / entry.getValue2() - basalRate / 60;
                result.add(new VaultEntry(VaultEntryType.BASAL_PROFILE, entry.getTimestamp(), value, entry.getValue2()));
            }
        } else {
            for (VaultEntry entry : basalTreatments) {
                if (!entry.getType().equals(VaultEntryType.BASAL_MANUAL)) {
                    throw new IllegalArgumentException("VaultEntryType should be BASAL_MANUAL but was " + entry.getType().toString());
                }
                addBasal(entry, result, profile);
            }
        }
        return result;
    }

    /**
     * Calculate the difference between Temp Basal Treatments and the basal rates.
     * Is called recursively if one of the given times is exceeded.
     *
     * @param entry entry to add
     * @param list result list
     * @param profile nightscout profile
     */
    private static void addBasal(VaultEntry entry, List<VaultEntry> list, Profile profile) {
        if (entry.getValue2() <= 0) {
            return;
        }

        List<Profile.BasalProfile> profileTime = profile.getBasalProfiles();
        long treatmentTime = (entry.getTimestamp().getTime() / 60000) % (60 * 24); //Time in min from 00:00

        for (int i = 0; i < profileTime.size() - 1; i++) {
            long firstTime = profileTime.get(i).getStart().getHour() * 60 + profileTime.get(i).getStart().getMinute(); //Time in min from 00:00
            long secTime = profileTime.get(i + 1).getStart().getHour() * 60 + profileTime.get(i + 1).getStart().getMinute();

            if (firstTime <= treatmentTime && treatmentTime < secTime) {
                if (treatmentTime + entry.getValue2() <= secTime) {
                    double value = (entry.getValue() / entry.getValue2()) - (profileTime.get(i).getValue() / 60);
                    list.add(new VaultEntry(VaultEntryType.BASAL_PROFILE, entry.getTimestamp(), value, entry.getValue2()));
                    return;
                } else {

                    long newDuration = secTime - treatmentTime;
                    double deltaValue = entry.getValue() * newDuration / entry.getValue2();
                    double newValue = (deltaValue / newDuration) - (profileTime.get(i).getValue() / 60);

                    list.add(new VaultEntry(VaultEntryType.BASAL_PROFILE, entry.getTimestamp(), newValue, newDuration));
                    addBasal(new VaultEntry(VaultEntryType.BASAL_MANUAL, new Date(entry.getTimestamp().getTime() + newDuration * 60000), entry.getValue() - deltaValue, entry.getValue2() - newDuration), list, profile);
                    return;
                }
            }
        }
        //last case 24:00
        long secTime = 24 * 60;

        if (treatmentTime + entry.getValue2() <= secTime) {
            double value = entry.getValue() / entry.getValue2() - profileTime.get(profileTime.size() - 1).getValue() / 60;
            list.add(new VaultEntry(VaultEntryType.BASAL_PROFILE, entry.getTimestamp(), value, entry.getValue2()));
        } else {

            long newDuration = secTime - treatmentTime;
            double deltaValue = entry.getValue() * newDuration / entry.getValue2();
            double newValue = deltaValue / newDuration - profileTime.get(profileTime.size() - 1).getValue() / 60;

            list.add(new VaultEntry(VaultEntryType.BASAL_PROFILE, entry.getTimestamp(), newValue, newDuration));
            addBasal(new VaultEntry(VaultEntryType.BASAL_MANUAL, new Date(entry.getTimestamp().getTime() + newDuration * 60000), entry.getValue() - deltaValue, entry.getValue2() - newDuration), list, profile);
        }
    }
}
