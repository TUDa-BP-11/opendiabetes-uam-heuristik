package de.opendiabetes.vault.main;

import com.martiansoftware.jsap.*;
import de.opendiabetes.vault.main.algo.Algorithm;
import de.opendiabetes.vault.main.algo.MinimumAlgo;
import de.opendiabetes.vault.main.dataprovider.AlgorithmDataProvider;
import de.opendiabetes.vault.main.dataprovider.DemoDataProvider;
import de.opendiabetes.vault.main.dataprovider.FileDataProvider;
import de.opendiabetes.vault.main.dataprovider.NightscoutDataProvider;
import de.opendiabetes.vault.main.exception.DataProviderException;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.nsapi.NSApi;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import static de.opendiabetes.vault.nsapi.Main.*;
import static de.opendiabetes.vault.nsapi.NSApi.LOGGER;

public class Main {
    // All parameters

    public static final Parameter P_ALGO = new FlaggedOption("algorithm")
            .setStringParser(JSAP.STRING_PARSER)
            .setShortFlag('a')
            .setLongFlag("algorithm")
            .setHelp("Algorithm that should be used");
    public static final Parameter P_TARGET = new FlaggedOption("target")
            .setStringParser(JSAP.STRING_PARSER)
            .setShortFlag('t')
            .setLongFlag("target")
            .setHelp("Target Nightscout server URL. Make sure to include the port.");
    public static final Parameter P_TARGET_SECRET = new FlaggedOption("targetsecret")
            .setStringParser(JSAP.STRING_PARSER)
            .setRequired(true)
            .setShortFlag('z')
            .setLongFlag("targetsecret")
            .setHelp("target Nightscout server API secret.");
    public static final Parameter P_OUTPUT_FILE = new FlaggedOption("output")
            .setStringParser(JSAP.STRING_PARSER)
            .setRequired(true)
            .setShortFlag('o')
            .setLongFlag("output")
            .setHelp("file where the meals should saved in");
    public static final Parameter P_INPUT_ENTRIES = new FlaggedOption("entries")
            .setStringParser(JSAP.STRING_PARSER)
            .setRequired(true)
            .setShortFlag('e')
            .setLongFlag("entries")
            .setHelp("path to file of blood glucose values");
    public static final Parameter P_INPUT_TREATMENTS = new FlaggedOption("treatments")
            .setStringParser(JSAP.STRING_PARSER)
            .setRequired(true)
            .setShortFlag('t')
            .setLongFlag("treatments")
            .setHelp("path to file of treatments");


    /**
     * Registers all arguments to the given JSAP instance
     *
     * @param jsap your JSAP instance
     */
    private static void registerArguments(JSAP jsap) {
        try {
            jsap.registerParameter(P_ALGO);
            jsap.registerParameter(P_HOST);
            jsap.registerParameter(P_SECRET);
            jsap.registerParameter(P_TARGET);
            jsap.registerParameter(P_TARGET_SECRET);
            jsap.registerParameter(P_OUTPUT_FILE);
            jsap.registerParameter(P_INPUT_ENTRIES);
            jsap.registerParameter(P_INPUT_TREATMENTS);

            jsap.registerParameter(P_LATEST);
            jsap.registerParameter(P_OLDEST);
            jsap.registerParameter(P_OVERWRITE);

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

        AlgorithmDataProvider dataProvider = null;
        if (config.contains("host")){
            if (config.contains("entries") || config.contains("treatments")){
                NSApi.LOGGER.warning("Cannot get input from files and server at the same time!");
                return;
            }
            //dataProvider = new NightscoutDataProvider(); //TODO

        }
        if (config.contains("entries") ^ config.contains("treatments")){
            NSApi.LOGGER.warning("Please specify a path to your file of blood glucose values and your file of treatments");
            return;
        }

        if (config.contains("entries") && config.contains("treatments")){
            //dataProvider = new FileDataProvider(); //TODO
        }

        if(!config.contains("host") && !config.contains("entries") && !config.contains("treatments")){
            NSApi.LOGGER.warning("Please specify the input source (files or server)"); //TODO
            return;
        }

        /*
        // Main control
        Properties config = null;
        String dataProviderName = "demo";
        String algorithmName = "demo";

        // Nightscout data provider
        String host = null;
        String secret = null;
        Integer batchSize = null;

        // Files data provider
        String base = null;
        String entries = null;
        String treatments = null;
        String profile = null;

        // General
        TemporalAccessor lastest = null;
        TemporalAccessor oldest = null;
        long absorptionTime = 120;
        long insulinDuration = 180;

        boolean debug = false;

        // Read startup arguments
        try {
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.startsWith("-")) {
                    arg = arg.substring(1);
                    switch (arg.toLowerCase()) {
                        case "config":
                            config = getPropertiesFileValue(arg, args, i);
                            i++;
                            break;
                        case "data":
                        case "provider":
                        case "dataprovider":
                            dataProviderName = getValue(arg, args, i);
                            i++;
                            break;
                        case "algo":
                        case "algorithm":
                            algorithmName = getValue(arg, args, i);
                            i++;
                            break;

                        // Nightscout data provider
                        case "host":
                            host = getValue(arg, args, i);
                            i++;
                            break;
                        case "secret":
                        case "apisecret":
                            secret = getValue(arg, args, i);
                            i++;
                            break;
                        case "batch":
                        case "batchsize":
                            batchSize = getIntValue(arg, args, i, 1);
                            i++;
                            break;

                        // Files data provider
                        case "base":
                            base = getValue(arg, args, i);
                            i++;
                            break;
                        case "entries":
                            entries = getValue(arg, args, i);
                            i++;
                            break;
                        case "treatments":
                            treatments = getValue(arg, args, i);
                            i++;
                            break;
                        case "profile":
                            profile = getValue(arg, args, i);
                            i++;
                            break;

                        // General
                        case "latest":
                            lastest = getDateTimeValue(arg, args, i);
                            i++;
                            break;
                        case "oldest":
                            oldest = getDateTimeValue(arg, args, i);
                            i++;
                            break;
                        case "absorptiontime":
                            absorptionTime = getLongValue(arg, args, i);
                            i++;
                            break;
                        case "insduration":
                        case "insulinduration":
                            insulinDuration = getLongValue(arg, args, i);
                            i++;
                            break;

                        case "debug":
                            debug = true;
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown argument " + arg);
                    }
                } else {
                    throw new IllegalArgumentException("Unknown argument " + arg);
                }
            }
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.SEVERE, e, e::getMessage);
            return;
        }

        // Set up data provider
        if (config != null) {
            if (lastest == null && config.getProperty("latest") != null) {
                lastest = parseDateTime("latest", config.getProperty("latest"));
            }
            if (oldest == null && config.getProperty("oldest") != null) {
                oldest = parseDateTime("oldest", config.getProperty("oldest"));
            }
        }

        AlgorithmDataProvider dataProvider;
        try {
            switch (dataProviderName.toLowerCase()) {
                case "demo":
                    dataProvider = new DemoDataProvider();
                    break;
                case "file":
                case "files":
                    if (config != null) {
                        if (base == null && config.getProperty("base") != null) {
                            base = config.getProperty("base");
                        }
                        if (entries == null && config.getProperty("entries") != null) {
                            entries = config.getProperty("entries");
                        }
                        if (treatments == null && config.getProperty("treatments") != null) {
                            treatments = config.getProperty("treatments");
                        }
                        if (profile == null && config.getProperty("profile") != null) {
                            profile = config.getProperty("profile");
                        }
                    }
                    dataProvider = new FileDataProvider(base, entries, treatments, profile, lastest, oldest);
                    break;
                case "ns":
                case "nightscout":
                    if (config != null) {
                        if (host == null && config.getProperty("host") != null) {
                            host = config.getProperty("host");
                        }
                        if (secret == null && config.getProperty("secret") != null) {
                            secret = config.getProperty("secret");
                        }
                        if (batchSize == null && config.getProperty("batchsize") != null) {
                            batchSize = parseInt("batchsize", config.getProperty("batchsize"), 1);
                        }
                    }
                    dataProvider = new NightscoutDataProvider(host, secret, batchSize, lastest, oldest);
                    break;
                default:
                    LOGGER.log(Level.WARNING, "Unknown dataprovider " + dataProviderName);
                    return;
            }
        } catch (DataProviderException | IllegalArgumentException e) {
            LOGGER.log(Level.SEVERE, e, e::getMessage);
            return;
        }

        // Set up algorithm
        Algorithm algorithm;
        switch (algorithmName.toLowerCase()) {
            case "demo":
                algorithm = new MinimumAlgo(absorptionTime, insulinDuration, dataProvider);
                break;
            default:
                LOGGER.log(Level.WARNING, "Unknown algorithm " + algorithmName);
                return;
        }


        algorithm.setAbsorptionTime(absorptionTime);
        algorithm.setInsulinDuration(insulinDuration);
        algorithm.setDataProvider(dataProvider);


        List<VaultEntry> data = new ArrayList<>();
        data.addAll(new ArrayList<>(dataProvider.getBolusTreatments()));
        data.addAll(new ArrayList<>(dataProvider.getGlucoseMeasurements()));

        // Start
        List<VaultEntry> meals = algorithm.calculateMeals();
        LOGGER.log(Level.INFO, "Calculated %d meals:", meals.size());

        // export as csv
        // data.addAll(meals);
        // data.sort(Comparator.comparing(VaultEntry::getTimestamp));
        // data.forEach(e -> Log.logInfo("%s", e.toString()));
        // exportCsv(data);

        try {
            dataProvider.close();
        } catch (DataProviderException e) {
            LOGGER.log(Level.SEVERE, e, e::getMessage);
        }*/
    }

    private static String getValue(String arg, String[] args, int i) {
        if (i < args.length - 1) {
            return args[i + 1];
        }
        throw new IllegalArgumentException("Missing value for argument " + arg);
    }

    private static double getDoubleValue(String arg, String[] args, int i) {
        return parseDouble(arg, getValue(arg, args, i));
    }

    private static double parseDouble(String arg, String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid value for argument " + arg + ", has to be a number", e);
        }
    }

    private static long getLongValue(String arg, String[] args, int i) {
        return parseLong(arg, getValue(arg, args, i));
    }

    private static long parseLong(String arg, String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid value for argument " + arg + ", has to be a number", e);
        }
    }

    private static int getIntValue(String arg, String[] args, int i, int min) {
        return parseInt(arg, getValue(arg, args, i), min);
    }

    private static int parseInt(String arg, String value, int min) {
        try {
            int integer = Integer.parseInt(value);
            if (integer < min) {
                throw new IllegalArgumentException("Invalid value for argument " + arg + ", minimum is " + min);
            }
            return integer;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid value for argument " + arg + ", has to be a number", e);
        }
    }

    private static TemporalAccessor getDateTimeValue(String arg, String[] args, int i) {
        return parseDateTime(arg, getValue(arg, args, i));
    }

    private static DateTimeFormatter parser = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static TemporalAccessor parseDateTime(String arg, String value) {
        try {
            return parser.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid value for argument " + arg + ", has to be in ISO-8601 representation without timezone data", e);
        }
    }

    private static Properties getPropertiesFileValue(String arg, String[] args, int i) {
        String value = getValue(arg, args, i);
        try (InputStream input = new FileInputStream(value)) {
            Properties properties = new Properties();
            properties.load(input);
            return properties;
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Cannot find config file at " + value, e);
        } catch (IOException e) {
            throw new IllegalArgumentException("IOException while reading config file: " + e.getMessage(), e);
        }
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
