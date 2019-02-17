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
import de.opendiabetes.vault.engine.container.VaultEntry;
import de.opendiabetes.vault.engine.util.SortVaultEntryByDate;

import java.io.IOException;
import java.util.ArrayList;
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
        /*
        for (TempBasal b:basals){
            System.out.println(b.toString());

        }*/

        String entriesPath = "/home/anna/Daten/Uni/14. Semester/BP/Dataset_Small/00390014/direct-sharing-31/entries_2017-07-10_to_2017-11-08.json";
        List<VaultEntry> entries = parser.parseFile(entriesPath);
        entries.sort(new SortVaultEntryByDate());
        for (VaultEntry e : entries) {
            e.setValue(e.getValue() - 100);
        }

        List<Snippet> snippets = Snippet.getSnippets(entries, bolusTreatment, basals, 24 * 60 * 60 * 1000, 0*insDuration * 60 * 1000, 5);

//        Algorithm algo = new OpenDiabetesAlgo(absorptionTime, insDuration, profile);
        NewAlgo algo2 = new NewAlgo(absorptionTime, insDuration, profile);

//        algo.setBolusTreatments(bolusTreatment);
//        algo.setBasalTreatments(basals);
//        Plot plt2 = Plot.create();

        algo2.setGlucoseMeasurements(entries);
        algo2.setBolusTreatments(bolusTreatment);
        algo2.setBasalTreatments(basals);

        List<VaultEntry> meals;
//      meals= algo2.calculateMeals2();
////            List<VaultEntry> meals2 = algo2.calculateMeals2();
//        System.out.println("Found meals:" + meals.size());
//
//        double firstTime = entries.get(0).getTimestamp().getTime();
        List<Double> basalValues = new ArrayList();
        List<Double> basalTimes = new ArrayList();
//        for (TempBasal a : basals) {
//            if (a.getDate().getTime() - firstTime > 1 * 24 * 60 * 60 * 1000) {
//                break;
//            }
//            basalValues.add(a.getValue());
//            basalTimes.add(a.getDate().getTime() / 1000.0);
//        }
        List<Double> bolusValues = new ArrayList();
        List<Double> bolusTimes = new ArrayList();
//        for (VaultEntry a : bolusTreatment) {
//            if (a.getTimestamp().getTime() - firstTime > 1 * 24 * 60 * 60 * 1000) {
//                break;
//            }
//            bolusValues.add(a.getValue());
//            bolusTimes.add(a.getTimestamp().getTime() / 1000.0);
//        }
//
        List<Double> mealValues = new ArrayList();
        List<Double> mealTimes = new ArrayList();
//        for (VaultEntry a : meals) {
//            if (a.getTimestamp().getTime() - firstTime > 1 * 24 * 60 * 60 * 1000) {
//                break;
//            }
//            mealValues.add(a.getValue());
//            mealTimes.add(a.getTimestamp().getTime() / 1000.0);
//        }

        List<Double> bgTimes = new ArrayList();
        List<Double> bgValues = new ArrayList();
        List<Double> algoValues = new ArrayList();
        List<Double> algo2Values = new ArrayList();
        double startValue = entries.get(0).getValue();
////            double startTime = s.getEntries().get(0).getTimestamp().getTime();
//        for (VaultEntry ve : entries) {
//            if (ve.getTimestamp().getTime() - firstTime > 1 * 24 * 60 * 60 * 1000) {
//                break;
//            }
////            double algoPredict = Predictions.predict(ve.getTimestamp().getTime(),
////                    meals, bolusTreatment, basals,
////                    profile.getSensitivity(), insDuration,
////                    profile.getCarbratio(), absorptionTime);
//            double algo2Predict = Predictions.predict(ve.getTimestamp().getTime(),
//                    meals, bolusTreatment, basals,
//                    profile.getSensitivity(), insDuration,
//                    profile.getCarbratio(), absorptionTime);
//
//            algoValues.add((startValue + algo2Predict - ve.getValue()) / ve.getValue() * 100);
//            algo2Values.add(startValue + algo2Predict);
//            bgTimes.add((ve.getTimestamp().getTime()) / 1000.0);
//            bgValues.add(ve.getValue());
//
//        }
//
//        plt2.plot().addDates(bgTimes).add(bgValues).color("blue");
//        plt2.plot().addDates(mealTimes).add(mealValues).color("red").linestyle("").marker("x");
//        plt2.plot().addDates(bolusTimes).add(bolusValues).color("green").linestyle("").marker("o");
//        plt2.plot().addDates(basalTimes).add(basalValues).color("cyan").linestyle("").marker("o");
//        plt2.plot().addDates(bgTimes).add(algo2Values).color("magenta").linestyle("--");
////            plt.plot().addDates(bgTimes).add(algo2Values).linestyle("--");//.color("cyan").linestyle("--");

        Plot plt = Plot.create();
        int i = 0;
        for (Snippet s : snippets) {

            if (i > 5) {
                break;
            }
//            algo.setGlucoseMeasurements(s.getEntries());
            algo2.setGlucoseMeasurements(s.getEntries());
            algo2.setBolusTreatments(s.getTreatments());
            algo2.setBasalTreatments(s.getBasals());

            System.out.println("calc :" + ++i + " with " + s.getEntries().size() + " entries, " + s.getBasals().size() + " basals, " + s.getTreatments().size() + " bolus");
            meals = algo2.calculateMeals2();
//            List<VaultEntry> meals2 = algo2.calculateMeals2();
            System.out.println("Found meals:" + meals.size());

            basalValues = new ArrayList();
            basalTimes = new ArrayList();
            for (TempBasal a : s.getBasals()) {
                basalValues.add(a.getValue());
                basalTimes.add(a.getDate().getTime() / 1000.0);
            }
            bolusValues = new ArrayList();
            bolusTimes = new ArrayList();
            for (VaultEntry a : s.getTreatments()) {
                bolusValues.add(a.getValue());
                bolusTimes.add(a.getTimestamp().getTime() / 1000.0);
            }

            mealValues = new ArrayList();
            mealTimes = new ArrayList();
            for (VaultEntry a : meals) {
                mealValues.add(a.getValue());
                mealTimes.add(a.getTimestamp().getTime() / 1000.0);
            }

            bgTimes = new ArrayList();
            bgValues = new ArrayList();
            algoValues = new ArrayList();
            algo2Values = new ArrayList();
            startValue = s.getEntries().get(0).getValue();
//            double startTime = s.getEntries().get(0).getTimestamp().getTime();
            for (VaultEntry ve : s.getEntries()) {
//            double algoPredict = Predictions.predict(ve.getTimestamp().getTime(),
//                    meals, bolusTreatment, basals,
//                    profile.getSensitivity(), insDuration,
//                    profile.getCarbratio(), absorptionTime);
                double algo2Predict = Predictions.predict(ve.getTimestamp().getTime(),
                        meals, s.getTreatments(), s.getBasals(),
                        profile.getSensitivity(), insDuration,
                        profile.getCarbratio(), absorptionTime);

                algoValues.add((startValue + algo2Predict - ve.getValue()) / (ve.getValue()+100) * 100);
                algo2Values.add(startValue + algo2Predict);
                bgTimes.add((ve.getTimestamp().getTime()) / 1000.0);
                bgValues.add(ve.getValue());
            }

            plt.plot().addDates(bgTimes).add(bgValues).color("blue");
            plt.plot().addDates(mealTimes).add(mealValues).color("red").linestyle("").marker("x");
            plt.plot().addDates(bolusTimes).add(bolusValues).color("green").linestyle("").marker("o");
            plt.plot().addDates(basalTimes).add(basalValues).color("cyan").linestyle("").marker("o");
            plt.plot().addDates(bgTimes).add(algoValues).color("magenta").linestyle("--");
            plt.plot().addDates(bgTimes).add(algo2Values).linestyle("--");//.color("cyan").linestyle("--");

        }
        try {
            plt.show();
//            plt2.show();
        } catch (IOException | PythonExecutionException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

//        for (VaultEntry meal : meals) {
//            System.out.println(meal.toString());
//        }
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
