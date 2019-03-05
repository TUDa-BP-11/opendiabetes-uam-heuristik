package de.opendiabetes.main.math;

import de.opendiabetes.vault.container.VaultEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Filter {
    public static double getAverage(List<VaultEntry> list, int position, int filterSize) {
        double result = 0;
        double amount = filterSize;

        for (int i = position - filterSize / 2; i < position + filterSize / 2; i++) {
            if (i < 0 || i >= list.size()) {
                amount--;
                continue;
            }
            result += list.get(i).getValue();
        }

        return result / amount;
    }

    public static double getAverage(List<VaultEntry> list, int position, int filterSize, int maxTimeDiff) {
        double result = 0;
        double amount = filterSize;

        for (int i = position - filterSize / 2; i < position + filterSize / 2; i++) {
            if (i < 0 || i >= list.size()) {
                amount--;
                continue;
            }
            VaultEntry entry = list.get(i);
            if (Math.abs((entry.getTimestamp().getTime() - list.get(position).getTimestamp().getTime()) / 60000.0) > maxTimeDiff) {
                amount--;
                continue;
            }

            result += entry.getValue();
        }

        return result / amount;
    }

    public static double getMedian(List<VaultEntry> list, int position, int filterSize) {
        List<Double> result = new ArrayList<>();

        for (int i = position - filterSize / 2; i < position + filterSize / 2; i++) {
            if (i < 0 || i >= list.size()) {
                continue;
            }
            result.add(list.get(i).getValue());
        }

        result.sort(Comparator.naturalOrder());
        return result.get(result.size() / 2);
    }

    public static double getMedian(List<VaultEntry> list, int position, int filterSize, int maxTimeDiff) {
        List<Double> result = new ArrayList<>();

        for (int i = position - filterSize / 2; i < position + filterSize / 2; i++) {
            if (i < 0 || i >= list.size()) {
                continue;
            }
            VaultEntry entry = list.get(i);
            if (Math.abs((entry.getTimestamp().getTime() - list.get(position).getTimestamp().getTime()) / 60000.0) > maxTimeDiff) {
                continue;
            }
            result.add(entry.getValue());
        }

        result.sort(Comparator.naturalOrder());
        return result.get(result.size() / 2);
    }

}
