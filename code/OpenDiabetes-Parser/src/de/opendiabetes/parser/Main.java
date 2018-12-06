package de.opendiabetes.parser;

import de.opendiabetes.vault.engine.container.VaultEntry;

import java.util.List;

public class Main {
    public static void main(String[] args){
        VaultEntryParser parser = new VaultEntryParser();

        String path = "";
        List<VaultEntry> entries = parser.parseFile(path);
        System.out.println("Entries:");
        for (VaultEntry entry : entries){
            System.out.println(entry.toString());
        }

        String path2 = "";
        List<VaultEntry> treatments = parser.parseFile(path2);
        System.out.println("Treatments:");
        for (VaultEntry entry : treatments){
            System.out.println(entry.toString());
        }

        System.out.println("Timezone: GMT "+parser.getTimezone());
    }

}
