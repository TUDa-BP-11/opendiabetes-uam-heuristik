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
import java.util.ArrayList;

import java.util.List;
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
            .setStringParser(new PostParser())
            .setLongFlag("post")
            .setShortFlag('p')
            .setHelp("Uploads data to your Nightscout instance. Specify either cgm, bolus, meal, basal or all");
    private static final Parameter P_GET = new FlaggedOption("get")
            .setStringParser(new PostParser())
            .setLongFlag("get")
            .setShortFlag('g')
            .setHelp("TODO");
    private static final Parameter P_FILE = new FlaggedOption("file")
            .setStringParser(JSAP.STRING_PARSER)
            .setShortFlag('f')
            .setLongFlag("file")
            .setHelp("Loads data from a file");
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
            LOGGER.log(Level.INFO, "Argument summary:\n{0}", jsap.getHelp());
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

        // start
        if (config.contains("host")) {
            if (!config.contains("secret")) {
                LOGGER.log(Level.WARNING, "Please specify an API secret: {0}", P_SECRET.getSyntax());
                return;
            }
            NSApi api = new NSApi(config.getString("host"), config.getString("secret"));
            Status status = api.getStatus();
            if (!status.isStatusOk()) {
                LOGGER.log(Level.SEVERE, "Nightscout server status is not ok:\n{0}", getStatus(status));
                return;
            }
            if (!status.isApiEnabled()) {
                LOGGER.log(Level.SEVERE, "Nightscout api is not enabled:\n{0}", getStatus(status));
                return;
            }

            if (config.getBoolean("status")) {
                LOGGER.log(Level.INFO, "Nightscout server information:\n{0}", getStatus(status));
                return;
            }

            if (config.contains("post")) {
                if (config.contains("get")) {
                    LOGGER.warning("Cannot post and get at the same time!");
                    return;
                }
                if (!config.contains("file")) {
                    LOGGER.log(Level.WARNING, "Please specify a file as your data source: {0}", P_FILE.getSyntax());
                    return;
                }
                VaultEntryType type = (VaultEntryType) config.getObject("post");

                String file = config.getString("file");
                try {
                    data = NSApiTools.loadDataFromFile(file, type, false);
                } catch (NightscoutIOException | InvalidDataException e) {
                    LOGGER.log(Level.SEVERE, e, e::getMessage);
                    return;
                }
                LOGGER.log(Level.INFO, "Loaded {0} entries from file", data.size());

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
                    LOGGER.log(Level.INFO, "Successfully uploaded {0} entries to your Nightscout server.", entries.size());
                } else if (entries.isEmpty()) {
                    LOGGER.log(Level.INFO, "Successfully uploaded {0} treatments to your Nightscout server.", treatments.size());
                } else {
                    LOGGER.log(Level.INFO, "Successfully uploaded {0} treatments and {1} entries to your Nightscout server.", new Object[]{treatments.size(), entries.size()});
                }
            }
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

    private static class PostParser extends StringParser {

        @Override
        public Object parse(String s) throws ParseException {
            switch (s.toLowerCase()) {
                case "cgm":
                    return VaultEntryType.GLUCOSE_CGM;
                case "bolus":
                    return VaultEntryType.BOLUS_NORMAL;
                case "meal":
                    return VaultEntryType.MEAL_MANUAL;
                case "basal":
                    return VaultEntryType.BASAL_MANUAL;
                case "all":
                    return null;
                default:
                    throw new ParseException("Invalid post option " + s);
            }
        }
    }
}
