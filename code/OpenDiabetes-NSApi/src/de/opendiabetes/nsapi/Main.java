package de.opendiabetes.nsapi;

import com.martiansoftware.jsap.*;
import com.mashape.unirest.http.exceptions.UnirestException;
import de.opendiabetes.nsapi.exception.InvalidDataException;
import de.opendiabetes.nsapi.exception.NightscoutIOException;
import de.opendiabetes.nsapi.logging.DebugFormatter;
import de.opendiabetes.nsapi.logging.DefaultFormatter;
import de.opendiabetes.parser.Status;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.util.SortVaultEntryByDate;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Main {
    private static final Logger LOGGER;

    // All parameters
    // Nightscout
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
    // Debugging
    private static final Parameter P_VERBOSE = new Switch("verbose")
            .setShortFlag('v')
            .setHelp("Sets logging to verbose");
    private static final Parameter P_DEBUG = new Switch("debug")
            .setShortFlag('d')
            .setHelp("Enables debug mode. Prints stack traces to STDERR and more.");

    static {
        LOGGER = Logger.getLogger(NSApi.class.getName());
        Handler handler = new ConsoleHandler();
        handler.setFormatter(new DefaultFormatter());
        LOGGER.addHandler(handler);
        LOGGER.setUseParentHandlers(false);
    }

    /**
     * Registers all arguments to the given JSAP instance
     *
     * @param jsap your JSAP instance
     */
    public static void registerArguments(JSAP jsap) {
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

            // Debugging
            jsap.registerParameter(P_VERBOSE);
            jsap.registerParameter(P_DEBUG);
        } catch (JSAPException e) {
            LOGGER.log(Level.SEVERE, "Exception while registering arguments!", e);
        }
    }

    public static void main(String[] args) throws UnirestException {
        // setup arguments
        JSAP jsap = new JSAP();
        registerArguments(jsap);

        // send help message if executed without arguments
        if (args.length == 0) {
            LOGGER.log(Level.INFO, "Argument summary:\n%s", jsap.getHelp());
            return;
        }

        // parse arguments
        JSAPResult config = jsap.parse(args);
        if (!config.success()) {
            LOGGER.log(Level.WARNING, "Invalid arguments:");
            config.getErrorMessageIterator().forEachRemaining(o -> LOGGER.warning(o.toString()));
            LOGGER.info("For an argument explanation execute without arguments.");
            return;
        }

        // init
        Level loglevel = config.getBoolean("verbose") ? Level.ALL : Level.INFO;
        LOGGER.setLevel(loglevel);
        LOGGER.getHandlers()[0].setLevel(loglevel);
        if (config.getBoolean("debug")) {
            LOGGER.getHandlers()[0].setFormatter(new DebugFormatter());
        }
        List<VaultEntry> data;

        if (!config.contains("host") || !config.contains("secret")) {
            LOGGER.log(Level.WARNING, "Please specify your Nigthscout host and API secret:\n%s\n%s", new Object[]{P_HOST.getSyntax(), P_SECRET.getSyntax()});
            return;
        }

        // start
        NSApi api = new NSApi(config.getString("host"), config.getString("secret"));
        Status status = api.getStatus();
        if (!status.isStatusOk()) {
            LOGGER.log(Level.SEVERE, "Nightscout server status is not ok:\n%s", getStatus(status));
            return;
        }
        if (!status.isApiEnabled()) {
            LOGGER.log(Level.SEVERE, "Nightscout api is not enabled:\n%s", getStatus(status));
            return;
        }

        if (config.getBoolean("status")) {
            LOGGER.log(Level.INFO, "Nightscout server information:\n%s", getStatus(status));
            return;
        }

        if (config.contains("post") && config.contains("get")) {
            LOGGER.warning("Cannot post and get at the same time!");
            return;
        }

        if (config.contains("post")) {
            if (!config.contains("file")) {
                LOGGER.log(Level.WARNING, "Please specify a file as your data source: %s", P_FILE.getSyntax());
                return;
            }

            try {
                data = NSApiTools.loadDataFromFile(config.getString("file"), false);
            } catch (NightscoutIOException | InvalidDataException e) {
                LOGGER.log(Level.SEVERE, e, e::getMessage);
                return;
            }
            data = NSApiTools.filterData(data, (Set<VaultEntryType>) config.getObject("post"));
            LOGGER.log(Level.INFO, "Loaded %d entries from file", data.size());

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
                    api.postTreatments(treatments);
                }
                if (!entries.isEmpty()) {
                    api.postEntries(entries);
                }
            } catch (NightscoutIOException | InvalidDataException e) {
                LOGGER.log(Level.SEVERE, e, e::getMessage);
                return;
            }
            if (treatments.isEmpty()) {
                LOGGER.log(Level.INFO, "Successfully uploaded %d entries to your Nightscout server.", entries.size());
            } else if (entries.isEmpty()) {
                LOGGER.log(Level.INFO, "Successfully uploaded %d treatments to your Nightscout server.", treatments.size());
            } else {
                LOGGER.log(Level.INFO, "Successfully uploaded %d treatments and %d entries to your Nightscout server.", new Object[]{treatments.size(), entries.size()});
            }
            return;
        }

        if (config.contains("get")) {
            if (!config.contains("file")) {
                LOGGER.log(Level.WARNING, "Please specify a file as your data target: %s", P_FILE.getSyntax());
                return;
            }

            data = new ArrayList<>();
            Set<VaultEntryType> types = (Set<VaultEntryType>) config.getObject("get");
            TemporalAccessor latest = (TemporalAccessor) config.getObject("latest");
            TemporalAccessor oldest = (TemporalAccessor) config.getObject("oldest");
            try {
                if (types.contains(VaultEntryType.GLUCOSE_CGM))
                    data.addAll(api.getEntries(latest, oldest, 100));
                if (types.contains(VaultEntryType.BOLUS_NORMAL)
                        || types.contains(VaultEntryType.MEAL_MANUAL)
                        || types.contains(VaultEntryType.BASAL_MANUAL))
                    data.addAll(api.getTreatments(latest, oldest, 100));
            } catch (NightscoutIOException e) {
                LOGGER.log(Level.SEVERE, e, e::getMessage);
                return;
            }
            data = NSApiTools.filterData(data, types);
            data.sort(new SortVaultEntryByDate().reversed());
            try {
                NSApiTools.writeDataToFile(config.getString("file"), data, config.getBoolean("overwrite"));
            } catch (NightscoutIOException e) {
                LOGGER.log(Level.SEVERE, e, e::getMessage);
                return;
            }
            LOGGER.log(Level.INFO, "Successfully downloaded %d entries from your Nightscout server.", data.size());
        }
    }

    private static String getStatus(Status status) {
        return "name:          " + status.getName() + "\n"
                + "version:       " + status.getVersion() + "\n"
                + "server status: " + status.getStatus() + "\n"
                + "api enabled:   " + status.isApiEnabled() + "\n"
                + "server time:   " + status.getServerTime();
    }

    public static Logger logger() {
        return LOGGER;
    }

    private static class TypeSetParser extends StringParser {
        @Override
        public Object parse(String s) throws ParseException {
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

    private static class IsoDateTimeParser extends StringParser {
        @Override
        public Object parse(String s) throws ParseException {
            try {
                return DateTimeFormatter.ISO_DATE_TIME.parse(s);
            } catch (DateTimeParseException e) {
                throw new ParseException(e);
            }
        }
    }
}
