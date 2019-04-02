package de.opendiabetes.vault.main;

import com.github.sh0nk.matplotlib4j.PythonExecutionException;
import com.martiansoftware.jsap.*;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.main.algo.Algorithm;
import de.opendiabetes.vault.main.algo.LMAlgo;
import de.opendiabetes.vault.main.algo.QRAlgo;
import de.opendiabetes.vault.main.dataprovider.DataProvider;
import de.opendiabetes.vault.main.dataprovider.FileDataProvider;
import de.opendiabetes.vault.main.dataprovider.NightscoutDataProvider;
import de.opendiabetes.vault.main.exception.DataProviderException;
import de.opendiabetes.vault.main.math.ErrorCalc;
import de.opendiabetes.vault.nsapi.NSApi;
import de.opendiabetes.vault.nsapi.NSApiTools;
import de.opendiabetes.vault.nsapi.exception.NightscoutIOException;
import de.opendiabetes.vault.nsapi.exception.NightscoutServerException;
import de.opendiabetes.vault.nsapi.exporter.NightscoutExporter;
import de.opendiabetes.vault.parser.Profile;
import de.opendiabetes.vault.util.SortVaultEntryByDate;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import static de.opendiabetes.vault.nsapi.Main.*;

public class Main {

    // DataProvider parameters
    private static final Parameter P_DATAPROVIDER = new FlaggedOption("dataprovider")
            .setStringParser(JSAP.STRING_PARSER)
            .setShortFlag('p')
            .setRequired(true)
            .setLongFlag("dataprovider")
            .setHelp("Data provider that should be used");

    // NightscoutDataProvider
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

    // FileDataProvider
    private static final Parameter P_ENTRIES_FILE = new FlaggedOption("entries")
            .setStringParser(JSAP.STRING_PARSER)
            .setShortFlag('E')
            .setLongFlag("entries")
            .setHelp("Path to file with the blood glucose values");
    private static final Parameter P_TREATMENTS_FILE = new FlaggedOption("treatments")
            .setStringParser(JSAP.STRING_PARSER)
            .setShortFlag('T')
            .setLongFlag("treatments")
            .setHelp("File with the basal and bolus insulin treatments");
    private static final Parameter P_PROFILE_FILE = new FlaggedOption("profile")
            .setStringParser(JSAP.STRING_PARSER)
            .setShortFlag('P')
            .setLongFlag("profile")
            .setHelp("Path to a nightscout profile");

    // Algorithm parameters
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
    private static final Parameter P_INSULIN_PEAK = new FlaggedOption("peak")
            .setStringParser(JSAP.DOUBLE_PARSER)
            .setLongFlag("peak")
            .setRequired(true)
            .setDefault("55")
            .setHelp("Duration in minutes until insulin action reaches it’s peak activity level");

    // Output
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

    // Options and tuning
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
    private static final Map<String, Class<? extends DataProvider>> dataproviders = new HashMap<>();

    /**
     * Registers all arguments to the given JSAP instance
     *
     * @param jsap your JSAP instance
     */
    private static void registerArguments(JSAP jsap) {
        try {
            jsap.registerParameter(P_DATAPROVIDER);
            jsap.registerParameter(P_HOST);
            jsap.registerParameter(P_SECRET);

            jsap.registerParameter(P_ENTRIES_FILE);
            jsap.registerParameter(P_TREATMENTS_FILE);
            jsap.registerParameter(P_PROFILE_FILE);

            jsap.registerParameter(P_ALGO);
            jsap.registerParameter(P_ABSORPTION_TIME);
            jsap.registerParameter(P_INSULIN_DURATION);
            jsap.registerParameter(P_INSULIN_PEAK);

            jsap.registerParameter(P_TARGET_HOST);
            jsap.registerParameter(P_TARGET_SECRET);
            jsap.registerParameter(P_OUTPUT_FILE);
            jsap.registerParameter(P_PLOT);

            jsap.registerParameter(P_OVERWRITE_OUTPUT);
            jsap.registerParameter(P_UPLOAD_ALL);
            jsap.registerParameter(P_BATCHSIZE);
            jsap.registerParameter(P_LATEST);
            jsap.registerParameter(P_OLDEST);

            jsap.registerParameter(P_VERBOSE);
            jsap.registerParameter(P_DEBUG);
        } catch (JSAPException e) {
            NSApi.LOGGER.log(Level.SEVERE, "Exception while registering arguments!", e);
        }
    }

    /**
     * Use this method to register more algorithms
     */
    private static void registerAlgorithms() {
        algorithms.put("lm", LMAlgo.class);
        algorithms.put("qr", QRAlgo.class);
    }

    /**
     * Use this method to register more data providers
     */
    private static void registerDataproviders() {
        dataproviders.put("nightscout", NightscoutDataProvider.class);
        dataproviders.put("file", FileDataProvider.class);
    }

    public static void main(String[] args) {

        // setup arguments
        JSAP jsap = new JSAP();
        registerArguments(jsap);
        registerAlgorithms();
        registerDataproviders();
        JSAPResult config = initArguments(jsap, args);
        if (config == null) {
            NSApi.LOGGER.log(Level.INFO, "DataProvider summary:\n%s", dataproviders.keySet());
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
        if (!dataproviders.containsKey(config.getString("dataprovider"))) {
            NSApi.LOGGER.log(Level.INFO, "There is no DataProvider with the name: %s", config.getString("dataprovider"));
            NSApi.LOGGER.log(Level.INFO, "For an argument summary execute without arguments.");
            return;
        }

        if (config.contains("target-host") && !config.contains("target-secret")) {
            NSApi.LOGGER.warning("Please specify the API secret of the target Nightscout server.");
            return;
        }

        if (config.getDouble("peak") <= 0 || config.getDouble("peak") >= config.getInt("insDuration")) {
            NSApi.LOGGER.warning("Peak can not be less than zero or greater than the duration of the insulin used");
            return;
        }

        if ((int) config.getDouble("peak") == config.getInt("insDuration") / 2) {
            NSApi.LOGGER.warning("Peak can not be exactly half the duration of the insulin used");
            return;
        }

        if (LocalDateTime.from((ZonedDateTime) config.getObject("latest")).isBefore(LocalDateTime.from((ZonedDateTime) config.getObject("oldest")))) {
            NSApi.LOGGER.warning("Oldest cannot be after latest");
            return;
        }

        if (!config.contains("target-host") && !config.getBoolean("plot") && !config.contains("output-file")) {
            NSApi.LOGGER.log(Level.WARNING, "The calculated meals will only be logged and not saved anywhere else");
        }

        //init DataProvider
        DataProvider dataProvider;
        try {
            dataProvider = dataproviders.get(config.getString("dataprovider")).getConstructor().newInstance();
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            NSApi.LOGGER.log(Level.SEVERE, e, e::getMessage);
            return;
        }

        try {
            dataProvider.setConfig(config);
        } catch (DataProviderException e) {
            NSApi.LOGGER.log(Level.SEVERE, e, e::getMessage);
            return;
        }

        Profile profile;
        List<VaultEntry> glucoseMeasurements, bolusTreatments, basalTreatments;
        try {
            profile = dataProvider.getProfile();
            profile.toZulu();
            glucoseMeasurements = dataProvider.getGlucoseMeasurements();
            bolusTreatments = dataProvider.getBolusTreatments();
            basalTreatments = dataProvider.getBasalTreatments();
        } catch (DataProviderException e) {
            NSApi.LOGGER.log(Level.SEVERE, e, e::getMessage);
            return;
        }

        //init Algorithm
        int absorptionTime = config.getInt("absorptionTime");
        int insulinDuration = config.getInt("insDuration");
        double peak = config.getDouble("peak");
        Algorithm algorithm;
        try {
            algorithm = algorithms.get(config.getString("algorithm"))
                    .getConstructor(long.class, long.class, double.class, Profile.class, List.class, List.class, List.class)
                    .newInstance(absorptionTime, insulinDuration, peak, profile, glucoseMeasurements, bolusTreatments, basalTreatments);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | IllegalArgumentException e) {
            NSApi.LOGGER.log(Level.SEVERE, e, e::getMessage);
            return;
        }

        List<VaultEntry> meals = algorithm.calculateMeals();

        //Logging
        long maxTimeGap = getMaxTimeGap(glucoseMeasurements);
        if (maxTimeGap < MAX_TIME_GAP) {
            NSApi.LOGGER.log(Level.INFO, "The maximum gap in the blood glucose data is %d min.", maxTimeGap);
        } else {
            NSApi.LOGGER.log(Level.WARNING, "The maximum gap in the blood glucose data is %d min.", maxTimeGap);
        }

        ErrorCalc errorCalc = new ErrorCalc();
        errorCalc.calculateError(algorithm);
        NSApi.LOGGER.log(Level.INFO, "The maximum error between the data and the prediction is %.0f mg/dl.", errorCalc.getMaxError());
        NSApi.LOGGER.log(Level.INFO, "The maximum error in percent is %.1f%%.", errorCalc.getMaxErrorPercent());
        NSApi.LOGGER.log(Level.INFO, "The root mean square error is %.1f mg/dl.", errorCalc.getRootMeanSquareError());
        NSApi.LOGGER.log(Level.INFO, "The standard deviation is %.1f mg/dl.", errorCalc.getStdDeviation());
        NSApi.LOGGER.log(Level.INFO, "The bias is %.1f mg/dl.", errorCalc.getMeanError());

        meals.sort(new SortVaultEntryByDate().reversed());
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
                    nsApi.postUnannouncedMeals(meals, algorithm.getClass().getName(), batchsize);
                    if (config.getBoolean("upload-all")) {
                        bolusTreatments.sort(new SortVaultEntryByDate().reversed());
                        basalTreatments.sort(new SortVaultEntryByDate().reversed());
                        glucoseMeasurements.sort(new SortVaultEntryByDate().reversed());
                        nsApi.postTreatments(bolusTreatments, batchsize);
                        nsApi.postTreatments(basalTreatments, batchsize);
                        nsApi.postEntries(glucoseMeasurements, batchsize);
                        bolusTreatments.sort(new SortVaultEntryByDate());
                        basalTreatments.sort(new SortVaultEntryByDate());
                        glucoseMeasurements.sort(new SortVaultEntryByDate());
                    }
                } catch (NightscoutIOException | NightscoutServerException e) {
                    NSApi.LOGGER.log(Level.SEVERE, e, e::getMessage);
                }
            }
        }

        meals.sort(new SortVaultEntryByDate());

        if (meals.size() > 0) {
            NSApi.LOGGER.log(Level.INFO, "The predicted meals are:");
        } else {
            NSApi.LOGGER.log(Level.INFO, "No meals were predicted");
        }
        meals.forEach((meal) -> NSApi.LOGGER.log(Level.INFO, meal.toString()));

        if (config.getBoolean("plot")) {
            CGMPlotter cgpm = new CGMPlotter(false, true, true, profile.getSensitivity(), insulinDuration,
                    profile.getCarbratio(), absorptionTime, peak);
            cgpm.add(algorithm);
            cgpm.addError(errorCalc.getErrorPercent(), errorCalc.getErrorDates());
            try {
                cgpm.showAll();
            } catch (IOException | PythonExecutionException e) {
                NSApi.LOGGER.log(Level.SEVERE, e, e::getMessage);
            }
        }
        dataProvider.close();
        try {
            NSApi.shutdown();
        } catch (NightscoutIOException e) {
            NSApi.LOGGER.log(Level.SEVERE, e, e::getMessage);
        }
    }

    /**
     * Calculates the max time gap between two neighbors in the given list.
     *
     * @param   list List of Vault Entries
     * @return  max time gap between two neighbors
     */
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
}
