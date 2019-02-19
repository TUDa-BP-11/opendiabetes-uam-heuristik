package de.opendiabetes.main.algo;

import com.github.sh0nk.matplotlib4j.Plot;
import com.github.sh0nk.matplotlib4j.PythonExecutionException;
import de.opendiabetes.main.math.Predictions;
import de.opendiabetes.main.math.TempBasalCalculator;
import de.opendiabetes.main.util.Snippet;
import de.opendiabetes.parser.Profile;
import de.opendiabetes.parser.ProfileParser;
import de.opendiabetes.parser.TreatmentMapper;
import de.opendiabetes.parser.VaultEntryParser;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.util.SortVaultEntryByDate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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

        List<TempBasal> basals = TempBasalCalculator.calcTemp(TreatmentMapper.adjustBasalTreatments(basalTreatments), profile);

        String entriesPath = "/home/anna/Daten/Uni/14. Semester/BP/Dataset_Small/00390014/direct-sharing-31/entries_2017-07-10_to_2017-11-08.json";
        List<VaultEntry> entries = parser.parseFile(entriesPath);
        entries.sort(new SortVaultEntryByDate());
        for (VaultEntry e : entries) {
            e.setValue(e.getValue());
        }

        List<Snippet> snippets = Snippet.getSnippets(entries, bolusTreatment, basals, 4 * 60 * 60 * 1000, 0 * insDuration * 60 * 1000, Integer.MAX_VALUE);

//        Algorithm algo = new OpenDiabetesAlgo(absorptionTime, insDuration, profile);
        NewAlgo algo = new NewAlgo(absorptionTime, insDuration, profile);

        algo.setGlucoseMeasurements(entries);
        algo.setBolusTreatments(bolusTreatment);
        algo.setBasalTreatments(basals);

        List<VaultEntry> meals;
        List<Double> basalValues;
        List<Double> basalTimes;
        List<Double> bolusValues;
        List<Double> bolusTimes;
        List<Double> mealValues;
        List<Double> mealTimes;

        double startTime;

        List<Double> bgTimes;
        List<Double> errorTimes;
        List<Double> bgValues;
        List<Double> errorValues;
        List<Double> algo2Values;
        double startValue;
        Plot plt = Plot.create();
        Plot plt2 = Plot.create();

        int i = 0;
        for (Snippet s : snippets) {

            algo.setGlucoseMeasurements(s.getEntries());
            algo.setBolusTreatments(s.getTreatments());
            algo.setBasalTreatments(s.getBasals());

            System.out.println("calc :" + ++i + " with " + s.getEntries().size() + " entries, " + s.getBasals().size() + " basals, " + s.getTreatments().size() + " bolus");
            meals = algo.calculateMeals2();

            System.out.println("Found meals:" + meals.size());

            basalValues = new ArrayList();
            basalTimes = new ArrayList();
            for (TempBasal a : s.getBasals()) {
                basalValues.add(a.getValue());// - a.getValue() * profile.getSensitivity() * a.getDuration()
                basalTimes.add(a.getDate().getTime() / 1000.0);
            }
            bolusValues = new ArrayList();
            bolusTimes = new ArrayList();
            for (VaultEntry a : s.getTreatments()) {
                bolusValues.add(a.getValue()); // -a.getValue() * profile.getSensitivity()
                bolusTimes.add(a.getTimestamp().getTime() / 1000.0);
            }

            mealValues = new ArrayList();
            mealTimes = new ArrayList();
            for (VaultEntry a : meals) {
                mealValues.add(a.getValue()); // a.getValue() * profile.getSensitivity() / profile.getCarbratio()
                mealTimes.add(a.getTimestamp().getTime() / 1000.0);
            }

            bgTimes = new ArrayList();
            errorTimes = new ArrayList();
            bgValues = new ArrayList();
            errorValues = new ArrayList();
            algo2Values = new ArrayList();
            startValue = s.getEntries().get(0).getValue();
            startTime = s.getEntries().get(0).getTimestamp().getTime();
            for (VaultEntry ve : s.getEntries()) {
                double algoPredict = Predictions.predict(ve.getTimestamp().getTime(),
                        meals, s.getTreatments(), s.getBasals(),
                        profile.getSensitivity(), insDuration,
                        profile.getCarbratio(), absorptionTime);

                errorValues.add((startValue + algoPredict - ve.getValue())/ ve.getValue() * 100); //  
                algo2Values.add(startValue + algoPredict);
                bgTimes.add((ve.getTimestamp().getTime()) / 1000.0);
                errorTimes.add((ve.getTimestamp().getTime() - startTime) / 1000.0);
                bgValues.add(ve.getValue());
            }

            plt.plot().addDates(bgTimes).add(bgValues).color("blue");
            plt.plot().addDates(mealTimes).add(mealValues).color("red").linestyle("").marker("x");
            plt.plot().addDates(bolusTimes).add(bolusValues).color("green").linestyle("").marker("o");
            plt.plot().addDates(basalTimes).add(basalValues).color("cyan").linestyle("").marker("o");
            plt2.plot().addDates(errorTimes).add(errorValues);//.color("magenta").linestyle("--");
            plt2.plot().addDates(errorTimes).add(Collections.nCopies(errorValues.size(), 10)).color("black");//.linestyle("--");
            plt2.plot().addDates(errorTimes).add(Collections.nCopies(errorValues.size(), -10)).color("black");
            plt.plot().addDates(bgTimes).add(algo2Values).linestyle("--");//.color("cyan").linestyle("--");

        }
        try {
            plt.show();
            plt2.show();
        } catch (IOException | PythonExecutionException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
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
