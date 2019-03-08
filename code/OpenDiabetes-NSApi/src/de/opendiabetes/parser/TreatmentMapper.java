package de.opendiabetes.parser;

import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;

import java.util.ArrayList;
import java.util.List;

public class TreatmentMapper {

    public static List<VaultEntry> adjustBasalTreatments(List<VaultEntry> basalTreatments) {
        List<VaultEntry> result = new ArrayList<>();
        VaultEntry current;

        if (!basalTreatments.isEmpty()) {
            current = basalTreatments.remove(0);

            while (!basalTreatments.isEmpty()) {
                if (!current.getType().equals(VaultEntryType.BASAL_MANUAL)) {
                    throw new IllegalArgumentException("VaultEntryType should be BASAL_MANUAL");
                }
                VaultEntry next = basalTreatments.remove(0);
                long deltaTime = Math.round((next.getTimestamp().getTime() - current.getTimestamp().getTime()) / 60000.0);

                if (deltaTime < 0){
                    throw new IllegalArgumentException("Input have to be sorted by Timestamp");
                }
                if (deltaTime < current.getValue2()) {
                    double value = current.getValue() * deltaTime / current.getValue2();
                    result.add(new VaultEntry(VaultEntryType.BASAL_MANUAL, current.getTimestamp(), value, deltaTime));
                } else {
                    result.add(new VaultEntry(VaultEntryType.BASAL_MANUAL, current.getTimestamp(), current.getValue(), current.getValue2()));
                }

                current = next;
            }
            //last one
            if (!current.getType().equals(VaultEntryType.BASAL_MANUAL)) {
                throw new IllegalArgumentException("VaultEntryType should be BASAL_MANUAL");
            }
            result.add(new VaultEntry(VaultEntryType.BASAL_MANUAL, current.getTimestamp(), current.getValue(), current.getValue2()));
        }
        return result;
    }
}
