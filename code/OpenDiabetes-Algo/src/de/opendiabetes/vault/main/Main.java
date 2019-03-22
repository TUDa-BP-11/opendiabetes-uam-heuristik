package de.opendiabetes.vault.main;

import com.martiansoftware.jsap.*;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.main.algo.*;
import de.opendiabetes.vault.main.dataprovider.AlgorithmDataProvider;
import de.opendiabetes.vault.main.dataprovider.FileDataProvider;
import de.opendiabetes.vault.main.dataprovider.NightscoutDataProvider;
import de.opendiabetes.vault.main.exception.DataProviderException;
import de.opendiabetes.vault.nsapi.NSApi;
import de.opendiabetes.vault.nsapi.NSApiTools;
import de.opendiabetes.vault.nsapi.exception.NightscoutIOException;
import de.opendiabetes.vault.nsapi.exception.NightscoutServerException;
import de.opendiabetes.vault.nsapi.exporter.NightscoutExporter;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;

import static de.opendiabetes.vault.nsapi.Main.*;

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
            .setDefault("LM")
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
    private static final Parameter P_CONSOLE = new Switch("console")
            .setShortFlag('c')
            .setLongFlag("console")
            .setHelp("Prints the result on the console");
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
            jsap.registerParameter(P_CONSOLE);
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

    public static void main(String[] args) {

        // setup arguments
        JSAP jsap = new JSAP();
        registerArguments(jsap);
        JSAPResult config = initArguments(jsap, args);
        if (config == null)
            return;

        // init
        initLogger(config);


        if (someFilesSet(config) && !allFilesSet(config)) {
            NSApi.LOGGER.warning("Please specify paths to your files of blood glucose values treatments and your profile");
            return;
        }

        if (config.contains("host") && allFilesSet(config)) {
            NSApi.LOGGER.warning("Cannot get input from files and server at the same time!");
            return;
        }

        if (!config.contains("host") && !allFilesSet(config)) {
            NSApi.LOGGER.warning("Please specify paths to your files of blood glucose values treatments and your profile\n" +
                    "or a Nightscout server URL. Make sure to include the port if needed");
            return;
        }

        if (config.contains("target-host") && !config.contains("target-secret")) {
            NSApi.LOGGER.warning("Please specify the API secret of the target Nightscout server.");
            return;
        }

        if (!config.contains("target-host") && !config.getBoolean("console") && !config.getBoolean("plot") && config.contains("output-file")) {
            NSApi.LOGGER.warning("Please specify at least one output for the results.");
            return;
        }

        AlgorithmDataProvider dataProvider;
        try {
            dataProvider = chooseDataProvider(config);
        } catch (DataProviderException e) {
            NSApi.LOGGER.log(Level.SEVERE, e, e::getMessage);
            return;
        }

        Algorithm algorithm = chooseAlgorithm(config, dataProvider);

        if (algorithm == null) {
            return;
        }

        List<VaultEntry> meals = algorithm.calculateMeals();

        int absorptionTime = config.getInt("absorptionTime");
        int insulinDuration = config.getInt("insDuration");

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

        if (config.getBoolean("console")) {
            for (VaultEntry meal : meals) {
                System.out.println(meal.toString());
            }
        }

        if (config.getBoolean("plot")) {
            //TODO
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

    private static Algorithm chooseAlgorithm(JSAPResult config, AlgorithmDataProvider dataProvider) {

        int absorptionTime = config.getInt("absorptionTime");
        int insulinDuration = config.getInt("insDuration");

        switch (config.getString("algorithm").toLowerCase()) {
            //TODO
            case "lm":
                return new LMAlgo(absorptionTime, insulinDuration, dataProvider);
            case "min":
                return new MinimumAlgo(absorptionTime, insulinDuration, dataProvider);
            case "filter":
                return new FilterAlgo(absorptionTime, insulinDuration, dataProvider);
            case "poly":
                return new PolyCurveFitterAlgo(absorptionTime, insulinDuration, dataProvider);
            case "qr":
                return new QRAlgo(absorptionTime, insulinDuration, dataProvider);
            case "qrdiff":
                return new QRDiffAlgo(absorptionTime, insulinDuration, dataProvider);
            default:
                NSApi.LOGGER.warning("There is no Algorithm with the name: " + config.getString("algorithm"));
        }
        return null;
    }

    private static boolean allFilesSet(JSAPResult config) {
        return (config.contains("entries") && config.contains("treatments") && config.contains("profile"));
    }

    private static boolean someFilesSet(JSAPResult config) {
        return (config.contains("entries") || config.contains("treatments") || config.contains("profile"));
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
