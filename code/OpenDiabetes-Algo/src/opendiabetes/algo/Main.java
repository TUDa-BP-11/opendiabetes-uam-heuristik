package opendiabetes.algo;

import de.opendiabetes.parser.VaultEntryParser;
import de.opendiabetes.vault.engine.container.VaultEntry;
import de.opendiabetes.vault.engine.util.SortVaultEntryByDate;

import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        VaultEntryParser parser = new VaultEntryParser();


        String path = "";
        List<VaultEntry> entries = parser.parseFile(path);

        entries.sort(new SortVaultEntryByDate());

        for (VaultEntry entry : entries) {
            System.out.println(entry.toString());
        }
        OpenDiabetesAlgo algo = new OpenDiabetesAlgo();
        algo.setGlucose(entries);
        algo.setBolusTreatments(new ArrayList<VaultEntry>());
        System.out.println("calc :");
        List<VaultEntry> meals = algo.calc();
        for (VaultEntry meal : meals) {
            System.out.println(meal.toString());
        }

    }
}
