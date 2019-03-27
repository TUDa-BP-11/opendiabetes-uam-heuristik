package de.opendiabetes.vault.main;

import com.github.sh0nk.matplotlib4j.PythonExecutionException;
import com.martiansoftware.jsap.*;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.main.algo.*;
import de.opendiabetes.vault.main.dataprovider.AlgorithmDataProvider;
import de.opendiabetes.vault.main.dataprovider.FileDataProvider;
import de.opendiabetes.vault.main.dataprovider.NightscoutDataProvider;
import de.opendiabetes.vault.main.exception.DataProviderException;
import de.opendiabetes.vault.main.math.ErrorCalc;
import de.opendiabetes.vault.nsapi.NSApi;
import de.opendiabetes.vault.nsapi.NSApiTools;
import de.opendiabetes.vault.nsapi.exception.NightscoutIOException;
import de.opendiabetes.vault.nsapi.exception.NightscoutServerException;
import de.opendiabetes.vault.nsapi.exporter.NightscoutExporter;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import static de.opendiabetes.vault.nsapi.Main.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.logging.Logger;

public class Main {

    // All parameters
    //Data Source
    private static final Parameter P_HOST = new FlaggedOption("host")
            .setStringParser(JSAP.STRING_PARSER)
            .setShortFlag('h')
            .setLongFlag("host")
            .setHelp("Your Nightscout host URL. Make sure to include the port.");
    private static final Parameter P_SECRET = new FlaggedOption("secret")
            .setStringParser(JSAP.STRING_PARSER)
            .setShortFlag('s')
            .setLongFlag("secret")
            .setHelp("Your Nightscout API secret.");
    private static final Parameter P_ENTRIES_FILE = new FlaggedOption("entries")
            .setStringParser(JSAP.STRING_PARSER)
            .setShortFlag('e')
            .setLongFlag("entries")
            .setHelp("Path to file with the blood glucose values");
    private static final Parameter P_TREATMENTS_FILE = new FlaggedOption("treatments")
            .setStringParser(JSAP.STRING_PARSER)
            .setShortFlag('f')
            .setLongFlag("treatments")
            .setHelp("File with the basal and bolus insulin treatments");
    private static final Parameter P_PROFILE_FILE = new FlaggedOption("profile")
            .setStringParser(JSAP.STRING_PARSER)
            .setShortFlag('p')
            .setLongFlag("profile")
            .setHelp("Path to a nightscout profile");
    //Algoorithm Parameters
    private static final Parameter P_ALGO = new FlaggedOption("algorithm")
            .setStringParser(JSAP.STRING_PARSER)
            .setShortFlag('a')
            .setDefault("lm")
            .setRequired(true)
            .setLongFlag("algorithm")
            .setHelp("Algorithm that should be used");
    private static final Parameter P_ABSORPTION_TIME = new FlaggedOption("absorptionTime")
            .setStringParser(JSAP.INTEGER_PARSER)
            .setShortFlag('t')
            .setLongFlag("absorption-time")
            .setRequired(true)
            .setDefault("120")
            .setHelp("Time to absorb a meal in minutes");
    private static final Parameter P_INSULIN_DURATION = new FlaggedOption("insDuration")
            .setStringParser(JSAP.INTEGER_PARSER)
            .setShortFlag('i')
            .setLongFlag("insulin-duration")
            .setRequired(true)
            .setDefault("180")
            .setHelp("Duration of used Insulin");
    //Output
    private static final Parameter P_TARGET_HOST = new FlaggedOption("target-host")
            .setStringParser(JSAP.STRING_PARSER)
            .setShortFlag('H')
            .setLongFlag("target-host")
            .setHelp("URL of the Nightscout server that you want to upload meals to. Make sure to include the port.");
    private static final Parameter P_TARGET_SECRET = new FlaggedOption("target-secret")
            .setStringParser(JSAP.STRING_PARSER)
            .setShortFlag('S')
            .setLongFlag("target-secret")
            .setHelp("API secret of the target Nightscout server.");
    private static final Parameter P_OUTPUT_FILE = new FlaggedOption("output-file")
            .setStringParser(JSAP.STRING_PARSER)
            .setShortFlag('o')
            .setLongFlag("output-file")
            .setHelp("File where the meals should saved in");
    private static final Parameter P_PLOT = new Switch("plot")
            .setLongFlag("plot")
            .setHelp("Plot meals, blood glucose and predicted values with pythons matplotlib. Make sure to have python and matplotlib installed.");
    //Options and tuning
    private static final Parameter P_OVERWRITE_OUTPUT = new Switch("overwrite")
            .setLongFlag("overwrite")
            .setHelp("Overwrite existing file");
    private static final Parameter P_UPLOAD_ALL = new Switch("upload-all")
            .setLongFlag("upload-all")
            .setHelp("Upload meals, bg-values, bolus and basal treatments");
    private static final Parameter P_BATCHSIZE = new FlaggedOption("batchsize")
            .setStringParser(JSAP.INTEGER_PARSER)
            .setLongFlag("batch-size")
            .setDefault("100")
            .setHelp("How many entries should be loaded at once.");
    private static final Parameter P_LATEST = new FlaggedOption("latest")
            .setStringParser(new IsoDateTimeParser())
            .setLongFlag("latest")
            .setDefault(ZonedDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
            .setHelp("The latest date and time to load data");
    private static final Parameter P_OLDEST = new FlaggedOption("oldest")
            .setStringParser(new IsoDateTimeParser())
            .setLongFlag("oldest")
            .setDefault("1970-01-01T00:00:00.000Z")
            .setHelp("The oldest date and time to load data");
    // Debugging
    private static final Parameter P_VERBOSE = new Switch("verbose")
            .setShortFlag('v')
            .setHelp("Sets logging to verbose");
    private static final Parameter P_DEBUG = new Switch("debug")
            .setShortFlag('d')
            .setHelp("Enables debug mode. Prints stack traces to STDERR and more.");

    private static final int MAX_TIME_GAP = 15;

    private static final Map<String, Class<? extends Algorithm>> algorithms = new HashMap<>();

    /**
     * Registers all arguments to the given JSAP instance
     *
     * @param jsap your JSAP instance
     */
    private static void registerArguments(JSAP jsap) {
        try {
            jsap.registerParameter(P_HOST);
            jsap.registerParameter(P_SECRET);
            jsap.registerParameter(P_ENTRIES_FILE);
            jsap.registerParameter(P_TREATMENTS_FILE);
            jsap.registerParameter(P_PROFILE_FILE);

            jsap.registerParameter(P_ALGO);
            jsap.registerParameter(P_ABSORPTION_TIME);
            jsap.registerParameter(P_INSULIN_DURATION);

            jsap.registerParameter(P_TARGET_HOST);
            jsap.registerParameter(P_TARGET_SECRET);
            jsap.registerParameter(P_OUTPUT_FILE);
            jsap.registerParameter(P_PLOT);

            jsap.registerParameter(P_OVERWRITE_OUTPUT);
            jsap.registerParameter(P_UPLOAD_ALL);
            jsap.registerParameter(P_LATEST);
            jsap.registerParameter(P_OLDEST);
            jsap.registerParameter(P_BATCHSIZE);

            jsap.registerParameter(P_VERBOSE);
            jsap.registerParameter(P_DEBUG);

        } catch (JSAPException e) {
            NSApi.LOGGER.log(Level.SEVERE, "Exception while registering arguments!", e);
        }
    }

    private static void registerAlgorithms() {
        algorithms.put("lm", LMAlgo.class);
        algorithms.put("min", MinimumAlgo.class);
        algorithms.put("filter", FilterAlgo.class);
        algorithms.put("poly", PolyCurveFitterAlgo.class);
        algorithms.put("qr", QRAlgo.class);
        algorithms.put("qrdiff", QRDiffAlgo.class);
        algorithms.put("oldlm", OldLMAlgo.class);
    }

    public static void main(String[] args) {

        // setup arguments
        JSAP jsap = new JSAP();
        registerArguments(jsap);
        registerAlgorithms();
        JSAPResult config = initArguments(jsap, args);
        if (config == null) {
            NSApi.LOGGER.log(Level.INFO, "Algorithm summary:\n%s", algorithms.keySet());
            return;
        }

        // init
        initLogger(config);

        //checks
        if (!algorithms.containsKey(config.getString("algorithm"))) {
            NSApi.LOGGER.log(Level.INFO, "There is no Algorithm with the name: %s", config.getString("algorithm"));
            NSApi.LOGGER.log(Level.INFO, "For an argument summary execute without arguments.");
            return;
        }
        if (someFilesSet(config) && !allFilesSet(config)) {
            NSApi.LOGGER.warning("Please specify paths to your files of blood glucose values treatments and your profile");
            return;
        }
        if (config.contains("host") && allFilesSet(config)) {
            NSApi.LOGGER.warning("Cannot get input from files and server at the same time!");
            return;
        }

        if (!config.contains("host") && !allFilesSet(config)) {
            NSApi.LOGGER.warning("Please specify paths to your files of blood glucose values treatments and your profile\n"
                    + "or a Nightscout server URL. Make sure to include the port if needed");
            return;
        }

        if (config.contains("target-host") && !config.contains("target-secret")) {
            NSApi.LOGGER.warning("Please specify the API secret of the target Nightscout server.");
            return;
        }

        if (!config.contains("target-host") && !config.getBoolean("plot") && config.contains("output-file")) {
            NSApi.LOGGER.warning("Please specify at least one output for the results.");
            return;
        }

        //init DataProvider
        AlgorithmDataProvider dataProvider;
        try {
            dataProvider = chooseDataProvider(config);
        } catch (DataProviderException e) {
            NSApi.LOGGER.log(Level.SEVERE, e, e::getMessage);
            return;
        }

        //init Algorithm
        int absorptionTime = config.getInt("absorptionTime");
        int insulinDuration = config.getInt("insDuration");
        Algorithm algorithm;
        try {
            algorithm = algorithms.get(config.getString("algorithm")).getConstructor(long.class, long.class, AlgorithmDataProvider.class)
                    .newInstance(absorptionTime, insulinDuration, dataProvider);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            NSApi.LOGGER.log(Level.SEVERE, e, e::getMessage);
            return;
        }

        List<VaultEntry> meals = algorithm.calculateMeals();

        //Logging
        NSApi.LOGGER.log(Level.FINE, "calculated meals");

        long maxTimeGap = getMaxTimeGap(dataProvider.getGlucoseMeasurements());
        if (maxTimeGap < MAX_TIME_GAP) {
            NSApi.LOGGER.log(Level.INFO, "The maximum gap in the blood glucose data is %d min.", maxTimeGap);
        } //TODO warning msg
        else {
            NSApi.LOGGER.log(Level.WARNING, "The maximum gap in the blood glucose data is %d min.", maxTimeGap);
        }

        ErrorCalc errorCalc = new ErrorCalc();
        errorCalc.calculateError(algorithm);
        NSApi.LOGGER.log(Level.INFO, "The maximum error between the data and the prediction is %.0f mg/dl.", errorCalc.getMaxError());
        NSApi.LOGGER.log(Level.INFO, "The maximum error in percent is %.1f%%.", errorCalc.getMaxErrorPercent());
        NSApi.LOGGER.log(Level.INFO, "The root mean square error is %.1f mg/dl.", errorCalc.getRootMeanSquareError());
        NSApi.LOGGER.log(Level.INFO, "The standard deviation is %.1f mg/dl.", errorCalc.getStdDeviation());
        NSApi.LOGGER.log(Level.INFO, "The bias is %.1f mg/dl.", errorCalc.getMeanError());

        //Output
        if (config.contains("output-file")) {
            try {
                NSApiTools.writeDataToFile(config.getString("output-file"), meals, config.getBoolean("overwrite"), new NightscoutExporter());
            } catch (NightscoutIOException e) {
                NSApi.LOGGER.log(Level.SEVERE, e, e::getMessage);
            }
        }

        if (config.contains("target-host")) {
            NSApi nsApi = new NSApi(config.getString("target-host"), config.getString("target-secret"));
            if (nsApi.checkStatusOk()) {
                try {
                    int batchsize = config.getInt("batchsize");
                    nsApi.postTreatments(meals, batchsize);
                    if (config.getBoolean("upload-all")) {
                        nsApi.postTreatments(dataProvider.getBolusTreatments(), batchsize);
                        nsApi.postTreatments(dataProvider.getRawBasalTreatments(), batchsize);
                        nsApi.postEntries(dataProvider.getGlucoseMeasurements(), batchsize);
                    }
                } catch (NightscoutIOException | NightscoutServerException e) {
                    NSApi.LOGGER.log(Level.SEVERE, e, e::getMessage);
                }
            }
        }
//
//        if (meals.size() > 0) {
//            NSApi.LOGGER.log(Level.INFO, "The predicted meals are:");
//        } else {
//            NSApi.LOGGER.log(Level.INFO, "No meals were predicted");
//        }
//        meals.forEach((meal) -> {
//            NSApi.LOGGER.log(Level.INFO, meal.toString());
//        });

        if (config.getBoolean("plot")) {
            CGMPlotter cgpm = new CGMPlotter(true, true, true, dataProvider.getProfile().getSensitivity(), insulinDuration, dataProvider.getProfile().getCarbratio(), absorptionTime);
            cgpm.add(algorithm);
            cgpm.addError(errorCalc.getErrorPercent(), errorCalc.getErrorDates());
            try {
                cgpm.showAll();
//                if (pythonDebug) {
//                    exportPlotScript(cgpm.showAll());
//                }

            } catch (IOException | PythonExecutionException ex) {
                NSApi.LOGGER.log(Level.SEVERE, null, ex);//TODO msg?
            }
        }
        dataProvider.close();
    }

    private static AlgorithmDataProvider chooseDataProvider(JSAPResult config) {
        if (config.contains("host")) {
            return new NightscoutDataProvider(config.getString("host"), config.getString("secret"),
                    config.getInt("batchsize"), (ZonedDateTime) config.getObject("latest"),
                    (ZonedDateTime) config.getObject("oldest"));
        }
        if (allFilesSet(config)) {
            return new FileDataProvider(null, config.getString("entries"), config.getString("treatments"),
                    config.getString("profile"), (ZonedDateTime) config.getObject("latest"),
                    (ZonedDateTime) config.getObject("oldest"));
        }
        return null;
    }

    private static boolean allFilesSet(JSAPResult config) {
        return (config.contains("entries") && config.contains("treatments") && config.contains("profile"));
    }

    private static boolean someFilesSet(JSAPResult config) {
        return (config.contains("entries") || config.contains("treatments") || config.contains("profile"));
    }

    private static long getMaxTimeGap(List<VaultEntry> list) {
        long maxTimeGap = 0;
        if (list.size() < 2) {
            return maxTimeGap;
        }
        VaultEntry current = list.get(0);
        for (int i = 1; i < list.size(); i++) {
            VaultEntry next = list.get(i);
            long timeDiff = (next.getTimestamp().getTime() - current.getTimestamp().getTime()) / 60000;
            if (timeDiff > maxTimeGap) {
                maxTimeGap = timeDiff;
            }
            current = next;
        }
        return maxTimeGap;
    }

    /*
    public static void exportCsv(List<VaultEntry> data) {
        if (data == null || data.isEmpty()) {
            Logger.getLogger(Main.class.getName()).severe("Database empty after processing");
            System.exit(0);
        } else {
            // export Data
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMM-HHmmss");
            String odvExpotFileName = "export-"
                    + VaultCsvEntry.VERSION_STRING
                    + "-"
                    + formatter.format(new Date())
                    + ".csv";

            String path = "./"; //System.getProperty("java.io.tmpdir");
            odvExpotFileName = new File(path).getAbsolutePath()
                    + "/" + odvExpotFileName;

            ExporterOptions eOptions = new ExporterOptions(
                    true, //export all
                    null, //from date
                    null // to date     
            );

            // standard export
            FileExporter exporter = new VaultCsvExporter(eOptions,
                    null,
                    odvExpotFileName);
            int result = exporter.exportDataToFile(data);
            if (result != VaultCsvExporter.RESULT_OK) {
                Logger.getLogger(Main.class.getName()).severe("Export Error");
                System.exit(0);
            }
        }
    }
     */
    private static void exportPlotScript(String scriptLines) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("./plotPlot.py"))) {
            writer.write(scriptLines);
        } catch (IOException ex) {
            NSApi.LOGGER.log(Level.SEVERE, null, ex);
        }
    }
}
