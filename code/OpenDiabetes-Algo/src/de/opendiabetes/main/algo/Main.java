package de.opendiabetes.main.algo;

import de.opendiabetes.main.CGMPlotter;
import de.opendiabetes.main.math.BasalCalculator;
import de.opendiabetes.main.util.Snippet;
import de.opendiabetes.parser.Profile;
import de.opendiabetes.parser.ProfileParser;
import de.opendiabetes.parser.TreatmentMapper;
import de.opendiabetes.parser.VaultEntryParser;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.util.SortVaultEntryByDate;

import java.util.ArrayList;
import java.util.List;

public class Main {

    static int absorptionTime = 180;
    static int insDuration = 3 * 60;

    public static void main(String[] args) {

        ProfileParser profileParser = new ProfileParser();

        String profilePath = "/home/anna/Daten/Uni/14. Semester/BP/Dataset_Small/00390014/direct-sharing-31/profile_2017-07-10_to_2017-11-08.json";
        Profile profile = profileParser.parseFile(profilePath);
        profile.toZulu();

        VaultEntryParser parser = new VaultEntryParser();

        String treatmentPath = "/home/anna/Daten/Uni/14. Semester/BP/Dataset_Small/00390014/direct-sharing-31/treatments_2017-07-10_to_2017-11-08.json";
        List<VaultEntry> treatments = parser.parseFile(treatmentPath);
        treatments.sort(new SortVaultEntryByDate());
        List<VaultEntry> basalTreatments = new ArrayList<>();
        List<VaultEntry> bolusTreatment = new ArrayList<>();
        List<VaultEntry> mealTreatment = new ArrayList<>();

        for (VaultEntry treatment : treatments) {
            switch (treatment.getType()) {
                case BASAL_MANUAL:
                    basalTreatments.add(treatment);
                    break;
                case BOLUS_NORMAL:
                    bolusTreatment.add(treatment);
                    break;
                case MEAL_MANUAL:
                    mealTreatment.add(treatment);
                    break;
                default:
                    System.out.println("de.opendiabetes.main.algo.Main.main() " + treatment.getType());
                    break;
            }

        }

        List<VaultEntry> basals = BasalCalculator.calcBasals(TreatmentMapper.adjustBasalTreatments(basalTreatments), profile);

        String entriesPath = "/home/anna/Daten/Uni/14. Semester/BP/Dataset_Small/00390014/direct-sharing-31/entries_2017-07-10_to_2017-11-08.json";
        List<VaultEntry> entries = parser.parseFile(entriesPath);
        entries.sort(new SortVaultEntryByDate());
        for (VaultEntry e : entries) {
            e.setValue(e.getValue());
        }

        List<Snippet> snippets = Snippet.getSnippets(entries, bolusTreatment, basals, 4 * 60 * 60 * 1000, 0 * insDuration * 60 * 1000, 1); //Integer.MAX_VALUE

        //Algorithm algo = new MinimumAlgo(absorptionTime, insDuration, profile);
//        Algorithm algo = new PolyCurveFitterAlgo(absorptionTime, insDuration, profile);
        Algorithm algo = new QRAlgo(absorptionTime, insDuration, profile);


        List<VaultEntry> meals = new ArrayList<>();

        int i = 0;
        for (Snippet s : snippets) {

            algo.setGlucoseMeasurements(s.getEntries());
            algo.setBolusTreatments(s.getTreatments());
            algo.setBasalTreatments(s.getBasals());

            System.out.println("calc :" + ++i + " with " + s.getEntries().size() + " entries, " + s.getBasals().size() + " basals, " + s.getTreatments().size() + " bolus");
            meals = algo.calculateMeals();

            System.out.println("Found meals:" + meals.size());

            CGMPlotter cgpm = new CGMPlotter();
            cgpm.plot(s, meals, profile.getSensitivity(), insDuration,
                    profile.getCarbratio(), absorptionTime);
//            cgpm.plotError(s, meals, profile.getSensitivity(), insDuration,
//                    profile.getCarbratio(), absorptionTime);
        }
//        //FOR LATER USE
//        // query data
//        List<VaultEntry> data = VaultDao.getInstance().queryAllVaultEntries();
//
//        if (data == null || data.isEmpty()) {
//            Logger.getLogger(Main.class.getName()).severe("Database empty after processing");
//            System.exit(0);
//        } else {
//            // export Data
//            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMM-HHmmss");
//            String odvExpotFileName = "export-"
//                    + VaultCsvEntry.VERSION_STRING
//                    + "-"
//                    + formatter.format(new Date())
//                    + ".csv";
//
//            String path = "./"; //System.getProperty("java.io.tmpdir");
//            odvExpotFileName = new File(path).getAbsolutePath()
//                    + "/" + odvExpotFileName;
//
//            ExporterOptions eOptions = new ExporterOptions(
//                    true, //export all
//                    null, //from date
//                    null // to date     
//            );
//
//            // standard export
//            FileExporter exporter = new VaultCsvExporter(eOptions,
//                    VaultDao.getInstance(),
//                    odvExpotFileName);
//            int result = exporter.exportDataToFile(null);
//            if (result != VaultCsvExporter.RESULT_OK) {
//                Logger.getLogger(Main.class.getName()).severe("Export Error");
//                System.exit(0);
//            }
//        }
    }
}
