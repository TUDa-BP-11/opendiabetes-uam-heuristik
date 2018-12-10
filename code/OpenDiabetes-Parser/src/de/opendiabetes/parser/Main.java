package de.opendiabetes.parser;

import de.opendiabetes.vault.engine.container.VaultEntry;

import java.util.List;

public class Main {

    private static final long ONE_HOUR = 3600000;

    public static void main(String[] args) {
        VaultEntryParser parser = new VaultEntryParser();

        String path = "/home/anna/Daten/Uni/14. Semester/BP/Dataset_Small/00390014/direct-sharing-31/entries_2017-07-10_to_2017-11-08.json";
        List<VaultEntry> entries = parser.parseFile(path);
        System.out.println("Entries:");
        for (VaultEntry entry : entries) {
            System.out.println(entry.toString());
        }

        String path2 = "/home/anna/Daten/Uni/14. Semester/BP/Dataset_Small/00390014/direct-sharing-31/treatments_2017-07-10_to_2017-11-08.json";
        List<VaultEntry> treatments = parser.parseFile(path2);
        System.out.println("Treatments:");
        for (VaultEntry entry : treatments) {
            System.out.println(entry.toString());
        }

        System.out.println("Timezone: GMT " + parser.getTimezone());
    }
}
