package de.opendiabetes.main.math;

import de.opendiabetes.parser.Profile;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BasalCalculator {

    public static List<VaultEntry> calcBasals(List<VaultEntry> basalTreatments, Profile profile) {
        List<VaultEntry> result = new ArrayList<>();
        if (profile.getBasalProfiles().size() < 1) {
            throw new IllegalArgumentException("profile must have at least 1 entry in basalProfiles.");
        }
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
            return;
        } else {

            long newDuration = secTime - treatmentTime;
            double deltaValue = entry.getValue() * newDuration / entry.getValue2();
            double newValue = deltaValue / newDuration - profileTime.get(profileTime.size() - 1).getValue() / 60;

            list.add(new VaultEntry(VaultEntryType.BASAL_PROFILE, entry.getTimestamp(), newValue, newDuration));
            addBasal(new VaultEntry(VaultEntryType.BASAL_MANUAL, new Date(entry.getTimestamp().getTime() + newDuration * 60000), entry.getValue() - deltaValue, entry.getValue2() - newDuration), list, profile);
        }
    }
}
