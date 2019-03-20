package de.opendiabetes.vault.main.algo;

import de.opendiabetes.vault.main.CGMPlotter;
import de.opendiabetes.vault.main.math.BasalCalculatorTools;
import de.opendiabetes.vault.main.util.Snippet;
import de.opendiabetes.vault.parser.Profile;
import de.opendiabetes.vault.parser.ProfileParser;
import de.opendiabetes.vault.nsapi.importer.NightscoutImporter;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.util.SortVaultEntryByDate;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    private static final int absorptionTime = 120;
    private static final int insDuration = 180;

    public static void main(String[] args) {

        ProfileParser profileParser = new ProfileParser();
String profilePath = "/home/anna/Daten/Uni/14. Semester/BP/Dataset_Small/00390014/direct-sharing-31/profile_2017-07-10_to_2017-11-08.json";

String entriesPath = "/home/anna/Daten/Uni/14. Semester/BP/Dataset_Small/00390014/direct-sharing-31/entries_2017-07-10_to_2017-11-08.json";

String treatmentPath = "/home/anna/Daten/Uni/14. Semester/BP/Dataset_Small/00390014/direct-sharing-31/treatments_2017-07-10_to_2017-11-08.json";

        Profile profile = profileParser.parseFile(profilePath);
        profile.toZulu();

        List<VaultEntry> treatments = new ArrayList<>();

        NightscoutImporter importer = new NightscoutImporter();
        try (InputStream stream = new FileInputStream(treatmentPath)) {
            treatments = importer.importData(stream);
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.WARNING, null, ex);
        }

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
                    System.out.println("Main.main() " + treatment.getType());
                    break;
            }

        }

        List<VaultEntry> basals = BasalCalculatorTools.calcBasalDifference(BasalCalculatorTools.adjustBasalTreatments(basalTreatments), profile);

        List<VaultEntry> entries = new ArrayList<>();
        try (InputStream stream = new FileInputStream(entriesPath)) {
            entries = importer.importData(stream);
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        entries.sort(new SortVaultEntryByDate());

        List<Snippet> snippets = Snippet.getSnippets(entries, bolusTreatment, basals, 3 * 60 * 60000, 1 * insDuration * 60000, 4); //Integer.MAX_VALUE

//        Algorithm algo = new FilterAlgo(absorptionTime, insDuration, profile);
//        Algorithm algo = new MinimumAlgo(absorptionTime, insDuration, profile);
//        Algorithm algo = new PolyCurveFitterAlgo(absorptionTime, insDuration, profile);
        Algorithm algo = new LMAlgo(absorptionTime, insDuration, profile);

        int i = 0;

        CGMPlotter cgpm = new CGMPlotter(true);

        for (Snippet s : snippets) {

            algo.setGlucoseMeasurements(s.getEntries());
            algo.setBolusTreatments(s.getBoli());
            algo.setBasalTreatments(s.getBasals());

            System.out.println("calc :" + ++i + " with " + s.getEntries().size() + " entries, " + s.getBasals().size() + " basals, " + s.getBoli().size() + " bolus");
            List<VaultEntry> meals = algo.calculateMeals();

            System.out.println("Found meals:" + meals.size());
           

            cgpm.plot(s.getEntries(), s.getBasals(), s.getBoli(), meals, profile.getSensitivity(), insDuration,
                    profile.getCarbratio(), absorptionTime);
        }

        cgpm.showAll();

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
// QR nochemol 
//Bias: 4.177488722344515
//RootMeanSquareError: 5.72346890825367
//Varianz: 15.30668431843085
//Standardabweichung: 3.9123757895211
// QR gefiltert
//Bias: -0.8028490815722585
//RootMeanSquareError: 9.374997218912611
//Varianz: 87.24600620683776
//Standardabweichung: 9.340557060841594
// QR ungefiltert
//Bias: 0.9211518031116709
//RootMeanSquareError: 8.38324840630532
//Varianz: 69.4303331974448
//Standardabweichung: 8.332486615497489
// PCF time contraint
//Bias: -1.433234557714322
//RootMeanSquareError: 21.317030666853903
//Varianz: 452.36163515416314
//Standardabweichung: 21.268794868401997
// PCF unconstrained
//Bias: 0.79567402861145
//RootMeanSquareError: 41.82931286794456
//Varianz: 1749.0583178445856
//Standardabweichung: 41.82174455764113
// Min positive
//Bias: 118.04564100165312
//RootMeanSquareError: 259.385428933642
//Varianz: 53346.02738359828
//Standardabweichung: 230.96758946570463
