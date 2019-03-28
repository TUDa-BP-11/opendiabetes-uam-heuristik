package de.opendiabetes.vault.main.algo;

import com.github.sh0nk.matplotlib4j.PythonExecutionException;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.main.CGMPlotter;
import de.opendiabetes.vault.main.math.BasalCalculatorTools;
import de.opendiabetes.vault.main.math.ErrorCalc;
import de.opendiabetes.vault.main.util.Snippet;
import de.opendiabetes.vault.nsapi.NSApi;
import de.opendiabetes.vault.nsapi.importer.NightscoutImporter;
import de.opendiabetes.vault.parser.Profile;
import de.opendiabetes.vault.parser.ProfileParser;
import de.opendiabetes.vault.util.SortVaultEntryByDate;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class Main {

    private static final int ABSORBTION_TIME = 120;
    private static final int INSULIN_DURATION = 180;
    private static final double INSULIN_PEAK = 55;

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
            NSApi.LOGGER.log(Level.WARNING, null, ex);
        }

        treatments.sort(new SortVaultEntryByDate());
        List<VaultEntry> basalTreatments = new ArrayList<>();
        List<VaultEntry> bolusTreatment = new ArrayList<>();
//        List<VaultEntry> mealTreatment = new ArrayList<>();

        for (VaultEntry treatment : treatments) {
            switch (treatment.getType()) {
                case BASAL_MANUAL:
                    basalTreatments.add(treatment);
                    break;
                case BOLUS_NORMAL:
                    bolusTreatment.add(treatment);
                    break;
                case MEAL_MANUAL:
//                    mealTreatment.add(treatment);
                    NSApi.LOGGER.log(Level.INFO, "Ignoring Meals");
                    break;
                default:
                    NSApi.LOGGER.log(Level.INFO, treatment.getType().name());
                    break;
            }
        }

        List<VaultEntry> basals = BasalCalculatorTools.calcBasalDifference(BasalCalculatorTools.adjustBasalTreatments(basalTreatments), profile);

        List<VaultEntry> entries = new ArrayList<>();
        try (InputStream stream = new FileInputStream(entriesPath)) {
            entries = importer.importData(stream);
        } catch (IOException ex) {
            NSApi.LOGGER.log(Level.SEVERE, null, ex);
        }
        entries.sort(new SortVaultEntryByDate());

//        try (BufferedWriter writer = new BufferedWriter(new FileWriter("./basal.csv"))) {
//            StringBuilder sb = new StringBuilder();
//            sb.append("t,x\n");
//            for (VaultEntry ve : basals) {
//                sb.append(ve.getTimestamp().getTime()).append(',').append(ve.getValue()).append("\n");
//            }
//            writer.write(sb.toString());
//        } catch (IOException ex) {
//            Logger.getLogger(CGMPlotter.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        try (BufferedWriter writer = new BufferedWriter(new FileWriter("./bolus.csv"))) {
//            StringBuilder sb = new StringBuilder();
//            sb.append("t,x\n");
//            for (VaultEntry ve : bolusTreatment) {
//                sb.append(ve.getTimestamp().getTime()).append(',').append(ve.getValue()).append("\n");
//            }
//            writer.write(sb.toString());
//        } catch (IOException ex) {
//            Logger.getLogger(CGMPlotter.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        try (BufferedWriter writer = new BufferedWriter(new FileWriter("./entries.csv"))) {
//            StringBuilder sb = new StringBuilder();
//            sb.append("t,x\n");
//            for (VaultEntry ve : entries) {
//                sb.append(ve.getTimestamp().getTime()).append(',').append(ve.getValue()).append("\n");
//            }
//            writer.write(sb.toString());
//        } catch (IOException ex) {
//            Logger.getLogger(CGMPlotter.class.getName()).log(Level.SEVERE, null, ex);
//        }
        List<Snippet> snippets = Snippet.getSnippets(entries, bolusTreatment, basals, 24 * 60 * 60000, INSULIN_DURATION * 60000, 1); //

//        snippets = snippets.subList(snippets.size()-1, snippets.size());
        List<Algorithm> algoList = new ArrayList<>();
        List<CGMPlotter> cgpmList = new ArrayList<>();
        Algorithm algo;
        CGMPlotter cgpm;

//        algo = new MinimumAlgo(ABSORBTION_TIME, INSULIN_DURATION, profile);
//        cgpm = new CGMPlotter(true, true, true, profile.getSensitivity(), INSULIN_DURATION,
//                profile.getCarbratio(), ABSORBTION_TIME);
//        cgpm.title("MinimumAlgo");
//        algoList.add(algo);
//        cgpmList.add(cgpm);
//     
//        algo = new PolyCurveFitterAlgo(ABSORBTION_TIME, INSULIN_DURATION, profile);
//        cgpm = new CGMPlotter(true, true, true, profile.getSensitivity(), INSULIN_DURATION,
//                profile.getCarbratio(), ABSORBTION_TIME);
//        cgpm.title("PolyCurveFitterAlgo");
//        algoList.add(algo);
//        cgpmList.add(cgpm);
//
//        algo = new QRAlgo(ABSORBTION_TIME, INSULIN_DURATION, profile);
//        cgpm = new CGMPlotter(true, true, true, profile.getSensitivity(), INSULIN_DURATION,
//                profile.getCarbratio(), ABSORBTION_TIME);
//        cgpm.title("QRAlgo");
//        algoList.add(algo);
//        cgpmList.add(cgpm);

        algo = new OldLMAlgo(ABSORBTION_TIME, INSULIN_DURATION, INSULIN_PEAK, profile, entries, bolusTreatment, basalTreatments);
        cgpm = new CGMPlotter(true, true, true, profile.getSensitivity(), INSULIN_DURATION,
                profile.getCarbratio(), ABSORBTION_TIME, INSULIN_PEAK);
        cgpm.title("OldLMAlgo");
        algoList.add(algo);
        cgpmList.add(cgpm);
        algo = new LMAlgo(ABSORBTION_TIME, INSULIN_DURATION, INSULIN_PEAK, profile, entries, bolusTreatment, basalTreatments);
        cgpm = new CGMPlotter(true, true, true, profile.getSensitivity(), INSULIN_DURATION,
                profile.getCarbratio(), ABSORBTION_TIME, INSULIN_PEAK);
        cgpm.title("LMAlgo");
        algoList.add(algo);
        cgpmList.add(cgpm);
        ErrorCalc errorCalc = new ErrorCalc();

        Snippet s;
        for (int i = 0; i < snippets.size(); i++) {
            s = snippets.get(i);

            NSApi.LOGGER.log(Level.INFO, "calc :%d with %d entries, %d basals, %d bolus", new Object[]{++i, s.getEntries().size(), s.getBasals().size(), s.getBoli().size()});
            for (int jj = 0; jj < algoList.size() && jj < cgpmList.size(); jj++) {
                algo = algoList.get(jj);
                algo.setGlucoseMeasurements(s.getEntries());
                algo.setBolusTreatments(s.getBoli());
                algo.setBasalTreatments(s.getBasals());
                List<VaultEntry> meals = algo.calculateMeals();
                errorCalc.calculateError(algo);
                cgpmList.get(jj).add(s.getEntries(), s.getBasals(), s.getBoli(), meals, algo.getStartIndex(), algo.getStartValue());
                cgpmList.get(jj).addError(errorCalc.getErrorPercent(), errorCalc.getErrorDates());
            }
        }

        for (int jj = 0; jj < algoList.size(); jj++) {
            try {
                cgpmList.get(jj).showAll();

            } catch (IOException | PythonExecutionException ex) {
                NSApi.LOGGER.log(Level.SEVERE, null, ex);
            }
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
