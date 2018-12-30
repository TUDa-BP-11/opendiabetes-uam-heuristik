package opendiabetes.algo;

import de.opendiabetes.parser.VaultEntryParser;
import de.opendiabetes.vault.engine.container.VaultEntry;
import de.opendiabetes.vault.engine.util.SortVaultEntryByDate;

import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        VaultEntryParser parser = new VaultEntryParser();


        String path = "C:\\Users\\Jan\\Desktop\\00390014\\direct-sharing-31\\entries_test.json";
        List<VaultEntry> entries = parser.parseFile(path);

        entries.sort(new SortVaultEntryByDate());

        OpenDiabetesAlgo algo = new OpenDiabetesAlgo();
        algo.setGlucose(entries);
        algo.setBolusTreatments(new ArrayList<>());
        System.out.println("calc :");
        List<VaultEntry> meals = algo.calc();
        for (VaultEntry meal : meals) {
            System.out.println(meal.toString());
        }
    }
}
