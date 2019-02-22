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

import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Main {
    private static Logger logger;

    // All parameters
    // Nightscout
    private static Parameter pHost = new FlaggedOption("host")
            .setStringParser(JSAP.STRING_PARSER)
            .setShortFlag('h')
            .setLongFlag("host")
            .setHelp("Your Nightscout host URL. Make shure to include the port.");
    private static Parameter pSecret = new FlaggedOption("secret")
            .setStringParser(JSAP.STRING_PARSER)
            .setShortFlag('s')
            .setLongFlag("secret")
            .setHelp("Your Nightscout API secret.");
    // Actions
    private static Parameter pStatus = new Switch("status")
            .setLongFlag("status")
            .setHelp("Prints the Nightscout server status and exists.");
    private static Parameter pPost = new FlaggedOption("post")
            .setStringParser(new PostParser())
            .setLongFlag("post")
            .setShortFlag('p')
            .setHelp("Uploads data to your Nightscout instance. Specify either cgm, bolus, meal, basal or all");
    private static Parameter pGet = new FlaggedOption("get")
            .setStringParser(new PostParser())
            .setLongFlag("get")
            .setShortFlag('g')
            .setHelp("TODO");
    private static Parameter pFile = new FlaggedOption("file")
            .setStringParser(JSAP.STRING_PARSER)
            .setShortFlag('f')
            .setLongFlag("file")
            .setHelp("Loads data from a file");
    // Debugging
    private static Parameter pVerbose = new Switch("verbose")
            .setShortFlag('v')
            .setHelp("Sets logging to verbose");
    private static Parameter pDebug = new Switch("debug")
            .setShortFlag('d')
            .setHelp("Enables debug mode. Prints stack traces to STDERR and more.");

    static {
        logger = Logger.getLogger(NSApi.class.getName());
        Handler handler = new ConsoleHandler();
        handler.setFormatter(new DefaultFormatter());
        logger.addHandler(handler);
        logger.setUseParentHandlers(false);
    }

    /**
     * Registers all arguments to the given JSAP instance
     *
     * @param jsap your JSAP instance
     */
    public static void registerArguments(JSAP jsap) {
        try {
            // Nightscout server
            jsap.registerParameter(pHost);
            jsap.registerParameter(pSecret);

            // Actions
            jsap.registerParameter(pStatus);
            jsap.registerParameter(pPost);
            jsap.registerParameter(pGet);
            jsap.registerParameter(pFile);

            // Debugging
            jsap.registerParameter(pVerbose);
            jsap.registerParameter(pDebug);
        } catch (JSAPException e) {
            logger.severe("Exception while registering arguments!");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws UnirestException {
        // setup arguments
        JSAP jsap = new JSAP();
        registerArguments(jsap);

        // send help message if executed without arguments
        if (args.length == 0) {
            logger.info("Argument summary:\n" + jsap.getHelp());
            return;
        }

        // parse arguments
        JSAPResult config = jsap.parse(args);
        if (!config.success()) {
            logger.warning("Invalid arguments:");
            config.getErrorMessageIterator().forEachRemaining(o -> logger.warning(o.toString()));
            logger.info("For an argument explanation execute without arguments.");
            return;
        }

        // init
        Level loglevel = config.getBoolean("verbose") ? Level.ALL : Level.INFO;
        logger.setLevel(loglevel);
        logger.getHandlers()[0].setLevel(loglevel);
        if (config.getBoolean("debug"))
            logger.getHandlers()[0].setFormatter(new DebugFormatter());
        List<VaultEntry> data;

        // start
        if (config.contains("host")) {
            if (!config.contains("secret")) {
                logger.warning("Please specify an API secret: " + pSecret.getSyntax());
                return;
            }
            NSApi api = new NSApi(config.getString("host"), config.getString("secret"));
            Status status = api.getStatus();
            if (!status.isStatusOk()) {
                logger.severe("Nightscout server status is not ok:\n" + getStatus(status));
                return;
            }
            if (!status.isApiEnabled()) {
                logger.severe("Nightscout api is not enabled:\n" + getStatus(status));
                return;
            }

            if (config.getBoolean("status")) {
                logger.info("Nightscout server information:\n" + getStatus(status));
                return;
            }

            if (config.contains("post")) {
                if (config.contains("get")) {
                    logger.warning("Cannot post and get at the same time!");
                    return;
                }
                if (!config.contains("file")) {
                    logger.warning("Please specify a file as your data source: " + pFile.getSyntax());
                    return;
                }
                VaultEntryType type = (VaultEntryType) config.getObject("post");

                String file = config.getString("file");
                try {
                    data = NSApiTools.loadDataFromFile(file, type, false);
                } catch (NightscoutIOException | InvalidDataException e) {
                    logger.log(Level.SEVERE, e, e::getMessage);
                    return;
                }
                logger.info("Loaded " + data.size() + " entries from file");

                List<VaultEntry> treatments = data.stream()
                        .filter(e -> e.getType().equals(VaultEntryType.MEAL_MANUAL) ||
                                e.getType().equals(VaultEntryType.BOLUS_NORMAL) ||
                                e.getType().equals(VaultEntryType.BASAL_MANUAL)
                        ).collect(Collectors.toList());
                List<VaultEntry> entries = data.stream()
                        .filter(e -> e.getType().equals(VaultEntryType.GLUCOSE_CGM))
                        .collect(Collectors.toList());

                try {
                    if (!treatments.isEmpty())
                        api.postTreatments(treatments);
                    if (!entries.isEmpty())
                        api.postEntries(entries);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, e, e::getMessage);
                    return;
                }
                if (treatments.isEmpty())
                    logger.info("Successfully uploaded " + entries.size() + " entries to your Nightscout server.");
                else if (entries.isEmpty())
                    logger.info("Successfully uploaded " + treatments.size() + " treatments to your Nightscout server.");
                else
                    logger.info("Successfully uploaded " + treatments.size() + " treatments and " + entries.size() + " entries to your Nightscout server.");
            }
        }
    }

    private static String getStatus(Status status) {
        return "name:          " + status.getName() + "\n" +
                "version:       " + status.getVersion() + "\n" +
                "server status: " + status.getStatus() + "\n" +
                "api enabled:   " + status.isApiEnabled() + "\n" +
                "server time:   " + status.getServerTime();
    }

    public static Logger logger() {
        return logger;
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
