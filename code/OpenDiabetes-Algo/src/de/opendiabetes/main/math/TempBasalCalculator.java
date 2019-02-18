package de.opendiabetes.main.math;

import de.opendiabetes.main.algo.TempBasal;
import de.opendiabetes.parser.Profile;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TempBasalCalculator {

    public static List<TempBasal> calcTemp(List<VaultEntry> basalTreatments, Profile profile) {
        List<TempBasal> result = new ArrayList<>();
        if (profile.getBasalProfiles().size() < 1) {
            throw new IllegalArgumentException("profile must have at least 1 entry in basalProfiles.");
        }
        if (profile.getBasalProfiles().size() == 1) {
            double basalRate = profile.getBasalProfiles().get(0).getValue();
            for (VaultEntry entry : basalTreatments) {
                if (!entry.getType().equals(VaultEntryType.BASAL_MANUAL)){
                    throw new IllegalArgumentException("VaultEntryType should be BASAL_MANUAL");
                }
                if (entry.getValue2() <= 0) continue;
                double value = (entry.getValue() / entry.getValue2()) - (basalRate / 60);
                result.add(new TempBasal(value, entry.getValue2(), entry.getTimestamp()));
            }
        } else {
            for (VaultEntry entry : basalTreatments) {
                if (!entry.getType().equals(VaultEntryType.BASAL_MANUAL)){
                    throw new IllegalArgumentException("VaultEntryType should be BASAL_MANUAL");
                }
                addTemp(entry, result, profile);
            }
        }
        return result;
    }

    private static void addTemp(VaultEntry entry, List<TempBasal> list, Profile profile) {
        if (entry.getValue2() <= 0) return;

        List<Profile.BasalProfile> profileTime = profile.getBasalProfiles();
        long treatmentTime = (entry.getTimestamp().getTime() / 60000) % (60 * 24); //Time in min from 00:00

        for (int i = 0; i < profileTime.size() - 1; i++) {
            long firstTime = profileTime.get(i).getStart().getHour() * 60 + profileTime.get(i).getStart().getMinute(); //Time in min from 00:00
            long secTime = profileTime.get(i + 1).getStart().getHour() * 60 + profileTime.get(i + 1).getStart().getMinute();

            if (firstTime <= treatmentTime && treatmentTime < secTime) {
                if (treatmentTime + entry.getValue2() <= secTime) {
                    double value = (entry.getValue() / entry.getValue2()) - (profileTime.get(i).getValue() / 60);
                    list.add(new TempBasal(value, entry.getValue2(), entry.getTimestamp()));
                    return;
                } else {

                    long deltadur = secTime - treatmentTime;
                    double deltaValue = entry.getValue() * deltadur / entry.getValue2();
                    double newTempValue = (deltaValue / deltadur) - (profileTime.get(i).getValue() / 60);

                    list.add(new TempBasal(newTempValue, deltadur, entry.getTimestamp()));
                    addTemp(new VaultEntry(VaultEntryType.BASAL_MANUAL, new Date(entry.getTimestamp().getTime() + deltadur * 60000), entry.getValue() - deltaValue, entry.getValue2() - deltadur), list, profile);
                    return;
                }
            }
        }
        //last case 24:00
        long secTime = 24 * 60;

        if (treatmentTime + entry.getValue2() <= secTime) {
            double value = (entry.getValue() / entry.getValue2()) - (profileTime.get(profileTime.size() - 1).getValue() / 60);
            list.add(new TempBasal(value, entry.getValue2(), entry.getTimestamp()));
            return;
        } else {

            long deltadur = secTime - treatmentTime;
            double deltaValue = entry.getValue() * deltadur / entry.getValue2();
            double newTempValue = (deltaValue / deltadur) - (profileTime.get(profileTime.size() - 1).getValue() / 60);

            list.add(new TempBasal(newTempValue, deltadur, entry.getTimestamp()));
            addTemp(new VaultEntry(VaultEntryType.BASAL_MANUAL, new Date(entry.getTimestamp().getTime() + deltadur * 60000), entry.getValue() - deltaValue, entry.getValue2() - deltadur), list, profile);
            return;
        }
    }
}

