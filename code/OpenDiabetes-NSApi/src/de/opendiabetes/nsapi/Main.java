package de.opendiabetes.nsapi;

import com.martiansoftware.jsap.*;
import com.mashape.unirest.http.exceptions.UnirestException;
import de.opendiabetes.parser.Status;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static Logger logger;

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$s %2$s %n%5$s%6$s%n");
        logger = Logger.getLogger(NSApi.class.getName());
        if (logger.getHandlers().length == 0)
            logger.addHandler(new ConsoleHandler());
    }

    public static void main(String[] args) throws JSAPException, UnirestException {
        JSAP jsap = new JSAP();
        jsap.registerParameter(new FlaggedOption("host")
                .setStringParser(JSAP.STRING_PARSER)
                .setRequired(true)
                .setShortFlag('h')
                .setLongFlag("host")
                .setHelp("Your Nightscout host URL. Make shure to include the port."));
        jsap.registerParameter(new FlaggedOption("secret")
                .setStringParser(JSAP.STRING_PARSER)
                .setRequired(true)
                .setShortFlag('s')
                .setLongFlag("secret")
                .setHelp("Your Nightscout API secret."));
        jsap.registerParameter(new Switch("verbose")
                .setShortFlag('v'));
        jsap.registerParameter(new Switch("status")
                .setLongFlag("status"));

        JSAPResult config = jsap.parse(args);
        if (!config.success()) {
            logger.severe("Invalid arguments! Usage: \n" + jsap.getUsage());
            logger.info("NSApi help:\n" + jsap.getHelp());
            return;
        }
        Level loglevel;
        if (config.getBoolean("verbose"))
            loglevel = Level.ALL;
        else loglevel = Level.INFO;
        logger.setLevel(loglevel);
        logger.getHandlers()[0].setLevel(loglevel);

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

        if (config.getBoolean("status"))
            logger.info("Nightscout server information:\n" + getStatus(status));
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
}
