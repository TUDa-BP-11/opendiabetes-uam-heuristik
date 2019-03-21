package de.opendiabetes.vault.nsapi;

import com.martiansoftware.jsap.*;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.nsapi.exception.NightscoutDataException;
import de.opendiabetes.vault.nsapi.exception.NightscoutIOException;
import de.opendiabetes.vault.nsapi.exception.NightscoutServerException;
import de.opendiabetes.vault.nsapi.exporter.NightscoutExporter;
import de.opendiabetes.vault.nsapi.exporter.NightscoutExporterOptions;
import de.opendiabetes.vault.nsapi.logging.DebugFormatter;
import de.opendiabetes.vault.parser.Status;
import de.opendiabetes.vault.util.SortVaultEntryByDate;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class Main {
    // All parameters
    // Nightscout
    private static final Parameter P_HOST = new FlaggedOption("host")
            .setStringParser(JSAP.STRING_PARSER)
            .setRequired(true)
            .setShortFlag('h')
            .setLongFlag("host")
            .setHelp("Your Nightscout host URL. Make sure to include the port.");
    private static final Parameter P_SECRET = new FlaggedOption("secret")
            .setStringParser(JSAP.STRING_PARSER)
            .setRequired(true)
            .setShortFlag('s')
            .setLongFlag("secret")
            .setHelp("Your Nightscout API secret.");
    // Actions
    private static final Parameter P_STATUS = new Switch("status")
            .setLongFlag("status")
            .setHelp("Prints the Nightscout server status and exists.");
    private static final Parameter P_POST = new FlaggedOption("post")
            .setStringParser(new TypeSetParser())
            .setShortFlag('p')
            .setLongFlag("post")
            .setHelp("Uploads data to your Nightscout server. Specify one or more of the following: cgm, bolus, meal, basal, all");
    private static final Parameter P_GET = new FlaggedOption("get")
            .setStringParser(new TypeSetParser())
            .setShortFlag('g')
            .setLongFlag("get")
            .setHelp("Downloads data from your Nightscout server. Specify one or more of the following: cgm, bolus, meal, basal, all");
    private static final Parameter P_FILE = new FlaggedOption("file")
            .setStringParser(JSAP.STRING_PARSER)
            .setShortFlag('f')
            .setLongFlag("file")
            .setHelp("Loads data from a file");
    private static final Parameter P_OVERWRITE = new Switch("overwrite")
            .setShortFlag('o')
            .setLongFlag("overwrite")
            .setHelp("Overwrite existing files");
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
    // Tuning
    private static final Parameter P_MERGEWINDOW = new FlaggedOption("mergewindow")
            .setStringParser(JSAP.INTEGER_PARSER)
            .setLongFlag("merge-window")
            .setDefault("60")
            .setHelp("Set the maximum amount of seconds two entries can be apart from one another for them to be considered the same entry.");
    private static final Parameter P_BATCHSIZE = new FlaggedOption("batchsize")
            .setStringParser(JSAP.INTEGER_PARSER)
            .setLongFlag("batch-size")
            .setDefault("100")
            .setHelp("How many entries should be loaded at once.");
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
            // Nightscout server
            jsap.registerParameter(P_HOST);
            jsap.registerParameter(P_SECRET);

            // Actions
            jsap.registerParameter(P_STATUS);
            jsap.registerParameter(P_POST);
            jsap.registerParameter(P_GET);
            jsap.registerParameter(P_FILE);
            jsap.registerParameter(P_OVERWRITE);
            jsap.registerParameter(P_LATEST);
            jsap.registerParameter(P_OLDEST);

            // Tuning
            jsap.registerParameter(P_MERGEWINDOW);
            jsap.registerParameter(P_BATCHSIZE);

            // Debugging
            jsap.registerParameter(P_VERBOSE);
            jsap.registerParameter(P_DEBUG);
        } catch (JSAPException e) {
            NSApi.LOGGER.log(Level.SEVERE, "Exception while registering arguments!", e);
        }
    }

    /**
     * Parses the arguments with the given JSAP configuration. Prints an argument summary if no arguments were supplied.
     *
     * @param jsap the JSAP configuration
     * @param args the arguments that will be parsed
     * @return the result of the parsed arguments or <code>null</code>, if the arguments could not be parsed successfully.
     */
    public static JSAPResult initArguments(JSAP jsap, String[] args) {
        // send help message if executed without arguments
        if (args.length == 0) {
            NSApi.LOGGER.log(Level.INFO, "Argument summary:\n%s", jsap.getHelp());
            return null;
        }

        // parse arguments
        JSAPResult config = jsap.parse(args);
        if (!config.success()) {
            NSApi.LOGGER.log(Level.WARNING, "Invalid arguments:");
            config.getErrorMessageIterator().forEachRemaining(o -> NSApi.LOGGER.warning(o.toString()));
            NSApi.LOGGER.info("For an argument summary execute without arguments.");
            return null;
        }
        return config;
    }

    /**
     * Initializes the logger with correct verbose and debug settings
     *
     * @param config the config
     */
    public static void initLogger(JSAPResult config) {
        Level loglevel = config.getBoolean("verbose") ? Level.ALL : Level.INFO;
        NSApi.LOGGER.setLevel(loglevel);
        NSApi.LOGGER.getHandlers()[0].setLevel(loglevel);
        if (config.getBoolean("debug")) {
            NSApi.LOGGER.getHandlers()[0].setFormatter(new DebugFormatter());
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
        List<VaultEntry> data;

        // start
        NSApi api = new NSApi(config.getString("host"), config.getString("secret"));
        if (!api.checkStatusOk())
            return;

        if (config.getBoolean("status")) {
            NSApi.LOGGER.log(Level.INFO, "Nightscout server information:\n%s", api.printStatus());
            return;
        }

        if (config.contains("post") && config.contains("get")) {
            NSApi.LOGGER.warning("Cannot post and get at the same time!");
            return;
        }

        if (config.contains("post")) {
            if (!config.contains("file")) {
                NSApi.LOGGER.log(Level.WARNING, "Please specify a file as your data source: %s", P_FILE.getSyntax());
                return;
            }

            try {
                data = NSApiTools.loadDataFromFile(config.getString("file"), false);
            } catch (NightscoutIOException | NightscoutDataException e) {
                NSApi.LOGGER.log(Level.SEVERE, e, e::getMessage);
                return;
            }
            data = NSApiTools.filterData(data, (Set<VaultEntryType>) config.getObject("post"));
            Date latest = Date.from(((ZonedDateTime) config.getObject("latest")).toInstant());
            Date oldest = Date.from(((ZonedDateTime) config.getObject("oldest")).toInstant());
            data = data.stream().filter(e -> e.getTimestamp().compareTo(oldest) >= 0 && e.getTimestamp().compareTo(latest) <= 0)
                    .collect(Collectors.toList());
            NSApi.LOGGER.log(Level.INFO, "Loaded %d entries from file", data.size());

            List<VaultEntry> treatments = data.stream()
                    .filter(e -> e.getType().equals(VaultEntryType.MEAL_MANUAL)
                            || e.getType().equals(VaultEntryType.BOLUS_NORMAL)
                            || e.getType().equals(VaultEntryType.BASAL_MANUAL)
                    ).collect(Collectors.toList());
            List<VaultEntry> entries = data.stream()
                    .filter(e -> e.getType().equals(VaultEntryType.GLUCOSE_CGM))
                    .collect(Collectors.toList());

            try {
                if (!treatments.isEmpty()) {
                    api.postTreatments(treatments, config.getInt("batchsize"));
                }
                if (!entries.isEmpty()) {
                    api.postEntries(entries, config.getInt("batchsize"));
                }
            } catch (NightscoutIOException | NightscoutServerException | NightscoutDataException e) {
                NSApi.LOGGER.log(Level.SEVERE, e, e::getMessage);
                return;
            }
            if (treatments.isEmpty()) {
                NSApi.LOGGER.log(Level.INFO, "Successfully uploaded %d entries to your Nightscout server.", entries.size());
            } else if (entries.isEmpty()) {
                NSApi.LOGGER.log(Level.INFO, "Successfully uploaded %d treatments to your Nightscout server.", treatments.size());
            } else {
                NSApi.LOGGER.log(Level.INFO, "Successfully uploaded %d treatments and %d entries to your Nightscout server.", new Object[]{treatments.size(), entries.size()});
            }
            return;
        }

        if (config.contains("get")) {
            if (!config.contains("file")) {
                NSApi.LOGGER.log(Level.WARNING, "Please specify a file as your data target: %s", P_FILE.getSyntax());
                return;
            }

            data = new ArrayList<>();
            Set<VaultEntryType> types = (Set<VaultEntryType>) config.getObject("get");
            TemporalAccessor latest = (TemporalAccessor) config.getObject("latest");
            TemporalAccessor oldest = (TemporalAccessor) config.getObject("oldest");
            try {
                if (types.contains(VaultEntryType.GLUCOSE_CGM))
                    data.addAll(api.getEntries(latest, oldest, config.getInt("batchsize")));
                if (types.contains(VaultEntryType.BOLUS_NORMAL)
                        || types.contains(VaultEntryType.MEAL_MANUAL)
                        || types.contains(VaultEntryType.BASAL_MANUAL))
                    data.addAll(api.getTreatments(latest, oldest, config.getInt("batchsize")));
            } catch (NightscoutIOException | NightscoutServerException | NightscoutDataException e) {
                NSApi.LOGGER.log(Level.SEVERE, e, e::getMessage);
                return;
            }
            data = NSApiTools.filterData(data, types);
            data.sort(new SortVaultEntryByDate().reversed());
            try {
                NSApiTools.writeDataToFile(
                        config.getString("file"),
                        data,
                        config.getBoolean("overwrite"),
                        new NightscoutExporter(new NightscoutExporterOptions(config.getInt("mergewindow"), true))
                );
            } catch (NightscoutIOException | NightscoutDataException e) {
                NSApi.LOGGER.log(Level.SEVERE, e, e::getMessage);
                return;
            }
            NSApi.LOGGER.log(Level.INFO, "Successfully downloaded %d entries from your Nightscout server.", data.size());
        }
    }

    private static String getStatus(Status status) {
        return "name:          " + status.getName() + "\n"
                + "version:       " + status.getVersion() + "\n"
                + "server status: " + status.getStatus() + "\n"
                + "api enabled:   " + status.isApiEnabled() + "\n"
                + "server time:   " + status.getServerTime();
    }

    /**
     * Parses the post and get arguments to a set of {@link VaultEntryType VaultEntryTypes} for filtering
     */
    private static class TypeSetParser extends StringParser {
        @Override
        public Set<VaultEntryType> parse(String s) throws ParseException {
            Set<VaultEntryType> types = new HashSet<>();
            for (String type : s.toLowerCase().split(",")) {
                switch (type.trim()) {
                    case "cgm":
                        types.add(VaultEntryType.GLUCOSE_CGM);
                        break;
                    case "bolus":
                        types.add(VaultEntryType.BOLUS_NORMAL);
                        break;
                    case "meal":
                        types.add(VaultEntryType.MEAL_MANUAL);
                        break;
                    case "basal":
                        types.add(VaultEntryType.BASAL_MANUAL);
                        break;
                    case "all":
                        types.add(VaultEntryType.GLUCOSE_CGM);
                        types.add(VaultEntryType.BOLUS_NORMAL);
                        types.add(VaultEntryType.MEAL_MANUAL);
                        types.add(VaultEntryType.BASAL_MANUAL);
                        break;
                    default:
                        throw new ParseException("Invalid post option " + s);
                }
            }
            return types;
        }
    }

    /**
     * Parses the latest and oldest arguments to ZonedDateTime
     */
    public static class IsoDateTimeParser extends StringParser {
        @Override
        public ZonedDateTime parse(String s) throws ParseException {
            try {
                return NSApiTools.getZonedDateTime(s);
            } catch (NightscoutIOException e) {
                throw new ParseException(e);
            }
        }
    }
}
