package opendiabetes.algo;

import de.opendiabetes.parser.Profile;
import de.opendiabetes.parser.ProfileParser;
import de.opendiabetes.parser.VaultEntryParser;
import de.opendiabetes.vault.engine.container.VaultEntry;
import de.opendiabetes.vault.engine.container.VaultEntryType;
import de.opendiabetes.vault.engine.util.SortVaultEntryByDate;

import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {


        ProfileParser profileParser = new ProfileParser();
        String profilePath = "";
        Profile profile = profileParser.parseFile(profilePath);
        profile.adjustProfile();

        BasalCalc basalCalculator = new BasalCalc(profile);
        VaultEntryParser parser = new VaultEntryParser();

        String treatmentPath = "";
        List<VaultEntry> treatments = parser.parseFile(treatmentPath);
        treatments.sort(new SortVaultEntryByDate());
        List<VaultEntry> basalTreatments = new ArrayList<>();
        List<VaultEntry> bolusTreatment = new ArrayList<>();
        List<VaultEntry> mealTreatment = new ArrayList<>();

        for(VaultEntry treatment : treatments){
            if (treatment.getType().equals(VaultEntryType.BASAL_MANUAL)){
                basalTreatments.add(treatment);
            }else if (treatment.getType().equals(VaultEntryType.BOLUS_NORMAL)){
                bolusTreatment.add(treatment);
            }else if (treatment.getType().equals(VaultEntryType.MEAL_MANUAL)){
                mealTreatment.add(treatment);
            }

        }


        List<tmpBasal> basals = basalCalculator.calculateBasal(basalTreatments);
        /*
        for (tmpBasal b:basals){
            System.out.println(b.toString());

        }*/

        String entriesPath = "";
        List<VaultEntry> entries = parser.parseFile(entriesPath);
        entries.sort(new SortVaultEntryByDate());

        OpenDiabetesAlgo algo = new OpenDiabetesAlgo();
        algo.setGlucose(entries);
        algo.setBolusTreatments(bolusTreatment);
        algo.setBasalTratments(basals);

        System.out.println("calc :");
        List<VaultEntry> meals = algo.calc2();
        for (VaultEntry meal : meals) {
            System.out.println(meal.toString());
        }
    }
}
