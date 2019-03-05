package de.opendiabetes.synchronizer;

import com.martiansoftware.jsap.*;
import de.opendiabetes.nsapi.NSApi;
import de.opendiabetes.nsapi.exception.NightscoutIOException;
import de.opendiabetes.nsapi.exception.NightscoutServerException;

import java.io.IOException;
import java.time.temporal.TemporalAccessor;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private final static Logger LOGGER;

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
        LOGGER = Logger.getLogger(NSApi.class.getName());
    }

    /**
     * Registers all arguments to the given JSAP instance
     *
     * @param jsap your JSAP instance
     */
    private static void registerArguments(JSAP jsap) {
        try {
            // Nightscout server
            jsap.registerParameter(P_HOST_READ);
            jsap.registerParameter(P_SECRET_READ);
            jsap.registerParameter(P_HOST_WRITE);
            jsap.registerParameter(P_SECRET_WRITE);

            // Action
            jsap.registerParameter(de.opendiabetes.nsapi.Main.P_LATEST);
            jsap.registerParameter(de.opendiabetes.nsapi.Main.P_OLDEST);

            // Tuning
            jsap.registerParameter(de.opendiabetes.nsapi.Main.P_BATCHSIZE);

            // Debugging
            jsap.registerParameter(de.opendiabetes.nsapi.Main.P_VERBOSE);
            jsap.registerParameter(de.opendiabetes.nsapi.Main.P_DEBUG);
        } catch (JSAPException e) {
            LOGGER.log(Level.SEVERE, "Exception while registering arguments!", e);
        }
    }

    public static void main(String[] args) {
        // setup arguments
        JSAP jsap = new JSAP();
        registerArguments(jsap);
        JSAPResult config = de.opendiabetes.nsapi.Main.initArguments(jsap, args);
        if (config == null)
            return;

        // init
        de.opendiabetes.nsapi.Main.initLogger(config);
        NSApi read = new NSApi(config.getString("host-read"), config.getString("secret-read"));
        NSApi write = new NSApi(config.getString("host-write"), config.getString("secret-write"));

        Synchronizer synchronizer = new Synchronizer(
                read, write,
                (TemporalAccessor) config.getObject("oldest"),
                (TemporalAccessor) config.getObject("latest"),
                config.getInt("batchsize"));
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
