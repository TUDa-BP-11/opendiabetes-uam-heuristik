package de.opendiabetes.synchronizer;

import com.martiansoftware.jsap.*;
import de.opendiabetes.nsapi.NSApi;
import de.opendiabetes.nsapi.exception.NightscoutIOException;
import de.opendiabetes.nsapi.exception.NightscoutServerException;
import de.opendiabetes.nsapi.logging.DebugFormatter;
import de.opendiabetes.nsapi.logging.DefaultFormatter;
import de.opendiabetes.vault.container.VaultEntry;

import java.io.IOException;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static final Logger LOGGER;

    // All parameters
    // Nightscout
    private static final Parameter P_HOST_READ = new FlaggedOption("host-read")
            .setStringParser(JSAP.STRING_PARSER)
            .setRequired(true)
            .setShortFlag('h')
            .setLongFlag("host-read")
            .setHelp("URL of the Nightscout server that you want to read data from. Make sure to include the port.");
    private static final Parameter P_SECRET_READ = new FlaggedOption("secret-read")
            .setStringParser(JSAP.STRING_PARSER)
            .setRequired(true)
            .setShortFlag('s')
            .setLongFlag("secret-read")
            .setHelp("API secret of the read Nightscout server.");
    private static final Parameter P_HOST_WRITE = new FlaggedOption("host-write")
            .setStringParser(JSAP.STRING_PARSER)
            .setRequired(true)
            .setShortFlag('H')
            .setLongFlag("host-write")
            .setHelp("URL of the Nightscout server that you want to synchronize data to. Make sure to include the port.");
    private static final Parameter P_SECRET_WRITE = new FlaggedOption("secret-write")
            .setStringParser(JSAP.STRING_PARSER)
            .setRequired(true)
            .setShortFlag('S')
            .setLongFlag("secret-write")
            .setHelp("API secret of the synchronized Nightscout server.");

    static {
        LOGGER = Logger.getLogger(Synchronizer.class.getName());
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
            jsap.registerParameter(P_HOST_READ);
            jsap.registerParameter(P_SECRET_READ);
            jsap.registerParameter(P_HOST_WRITE);
            jsap.registerParameter(P_SECRET_WRITE);

            // Action
            jsap.registerParameter(de.opendiabetes.nsapi.Main.P_LATEST);
            jsap.registerParameter(de.opendiabetes.nsapi.Main.P_OLDEST);

            // Debugging
            jsap.registerParameter(de.opendiabetes.nsapi.Main.P_VERBOSE);
            jsap.registerParameter(de.opendiabetes.nsapi.Main.P_DEBUG);
        } catch (JSAPException e) {
            LOGGER.log(Level.SEVERE, "Exception while registering arguments!", e);
        }
    }

    public static void main(String[] args) {
        //TODO: dont duplicate this with NSApi, fix setting LogLevel for NSApi logger and this logger
        
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
            LOGGER.info("For an argument summary execute without arguments.");
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

        NSApi read = new NSApi(config.getString("host-read"), config.getString("secret-read"));
        NSApi write = new NSApi(config.getString("host-write"), config.getString("secret-write"));

        Synchronizer synchronizer = new Synchronizer(
                read, write,
                (TemporalAccessor) config.getObject("oldest"),
                (TemporalAccessor) config.getObject("latest"),
                100);
        Synchronizable entries = new Synchronizable("entries", "dateString");
        Synchronizable treatments = new Synchronizable("treatments", "created_at");
        Synchronizable status = new Synchronizable("devicestatus", "created_at");

        try {
            synchronizer.findMissing(entries);
            System.out.println("Found " + entries.getFindCount() + " entries of which " + entries.getMissingCount() + " are missing in the target instance.");
            if (entries.getMissingCount() > 0)
                synchronizer.postMissing(entries);
            synchronizer.findMissing(treatments);
            System.out.println("Found " + treatments.getFindCount() + " treatments of which " + treatments.getMissingCount() + " are missing in the target instance.");
            if (treatments.getMissingCount() > 0)
                synchronizer.postMissing(treatments);
            synchronizer.findMissing(status);
            System.out.println("Found " + status.getFindCount() + " devicestatus of which " + status.getMissingCount() + " are missing in the target instance.");
            if (status.getMissingCount() > 0)
                synchronizer.postMissing(status);
        } catch (NightscoutIOException | NightscoutServerException e) {
            LOGGER.log(Level.SEVERE, e, e::getMessage);
        }
        try {
            synchronizer.close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e, e::getMessage);
        }
        System.out.println("Done!");
    }

    public static Logger logger() {
        return LOGGER;
    }
}
