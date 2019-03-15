package de.opendiabetes.vault.parser;

import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;

import java.util.ArrayList;
import java.util.List;

public class TreatmentMapper {

    /**
     * Shortens the duration of all entries to their next treatment
     * and adjusts the values of the entries accordingly.
     *
     * @param basalTreatments sorted list of vault entries with type
     *                        {@link de.opendiabetes.vault.container.VaultEntryType#BASAL_MANUAL}
     * @return list of vault entries whose duration won't exceeds the timestamp of their next entries.
     */
    public static List<VaultEntry> adjustBasalTreatments(List<VaultEntry> basalTreatments) {
        List<VaultEntry> result = new ArrayList<>();
        //VaultEntry current;

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

        return result;
    }
}
