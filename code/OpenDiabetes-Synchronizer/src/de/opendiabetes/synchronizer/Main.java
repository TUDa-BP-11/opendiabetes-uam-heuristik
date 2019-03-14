package de.opendiabetes.synchronizer;

import com.martiansoftware.jsap.*;
import de.opendiabetes.vault.nsapi.NSApi;
import de.opendiabetes.vault.nsapi.exception.NightscoutIOException;
import de.opendiabetes.vault.nsapi.exception.NightscoutServerException;

import java.io.IOException;
import java.time.temporal.TemporalAccessor;
import java.util.logging.Level;
import java.util.logging.Logger;

import static de.opendiabetes.vault.nsapi.Main.*;
import static de.opendiabetes.vault.nsapi.NSApi.LOGGER;

public class Main {
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
    private static final Parameter P_DATA = new FlaggedOption("data")
            .setStringParser(new SychronizableParser())
            .setLongFlag("data")
            .setAllowMultipleDeclarations(true)
            .setDefault(new String[]{
                    "entries:dateString",
                    "treatments:created_at",
                    "devicestatus:created_at"
            })
            .setUsageName("apiPath[:dateField]")
            .setHelp("Define what data will be synchronized. Can be declared multiple times. Default date field is 'created_at'.");
    private static final Parameter P_WITH_IDS = new Switch("with-ids")
            .setLongFlag("with-ids")
            .setHelp("Set this to keep the '_id' fields of objects when uploading them to the write server.");

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
            jsap.registerParameter(P_DATA);
            jsap.registerParameter(P_LATEST);
            jsap.registerParameter(P_OLDEST);

            // Tuning
            jsap.registerParameter(P_BATCHSIZE);
            jsap.registerParameter(P_WITH_IDS);

            // Debugging
            jsap.registerParameter(P_VERBOSE);
            jsap.registerParameter(P_DEBUG);
        } catch (JSAPException e) {
            LOGGER.log(Level.SEVERE, "Exception while registering arguments!", e);
        }
    }

    public static void main(String[] args) {
        // setup arguments
        JSAP jsap = new JSAP();
        registerArguments(jsap);
        JSAPResult config = de.opendiabetes.vault.nsapi.Main.initArguments(jsap, args);
        if (config == null)
            return;

        // init
        de.opendiabetes.vault.nsapi.Main.initLogger(config);
        NSApi read = new NSApi(config.getString("host-read"), config.getString("secret-read"));
        if (!read.checkStatusOk())
            return;
        NSApi write = new NSApi(config.getString("host-write"), config.getString("secret-write"));
        if (!write.checkStatusOk())
            return;

        Synchronizer synchronizer = new Synchronizer(
                read, write,
                (TemporalAccessor) config.getObject("oldest"),
                (TemporalAccessor) config.getObject("latest"),
                config.getInt("batchsize"));
        for (Object object : config.getObjectArray("data")) {
            Synchronizable sync = (Synchronizable) object;
            try {
                synchronizer.findMissing(sync);
                LOGGER.log(Level.INFO, "Found %d objects in /%s of which %d are missing in the target instance.",
                        new Object[]{sync.getFindCount(), sync.getApiPath(), sync.getMissingCount()});
                if (sync.getMissingCount() > 0) {
                    LOGGER.log(Level.INFO, "Uploading %d objects...", sync.getMissingCount());
                    synchronizer.postMissing(sync, !config.getBoolean("with-ids"));
                }
            } catch (NightscoutIOException | NightscoutServerException e) {
                LOGGER.log(Level.SEVERE, e, e::getMessage);
            }
        }
        try {
            synchronizer.close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e, e::getMessage);
        }
        LOGGER.log(Level.INFO, "Done!");
    }

    public static Logger logger() {
        return LOGGER;
    }

    /**
     * Parses what should be synchronized
     */
    private static class SychronizableParser extends StringParser {
        @Override
        public Synchronizable parse(String s) throws ParseException {
            if (!s.matches("^.+(:[a-z_]+)?$"))
                throw new ParseException("Invalid api path or date field, Syntax is <apiPath>[:dateField]");
            String[] parts = s.split(":");
            return new Synchronizable(parts[0], parts.length == 1 ? "created_at" : parts[1]);
        }
    }
}
