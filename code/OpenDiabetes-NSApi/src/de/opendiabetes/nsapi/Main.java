package de.opendiabetes.nsapi;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.mashape.unirest.http.exceptions.UnirestException;
import de.opendiabetes.parser.Status;

import java.util.logging.Logger;

public class Main {
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

        JSAPResult config = jsap.parse(args);
        Logger logger = Logger.getLogger("de.opendiabetes.nsapi");
        if (!config.success()) {
            logger.severe("Invalid arguments! Usage: \n" + jsap.getUsage());
            logger.info("NSApi help:\n" + jsap.getHelp());
            return;
        }

        NSApi api = new NSApi(config.getString("host"), config.getString("secret"));
        Status status = api.getStatus();
        logger.info("Nightscout server information:\n" +
                "name:          " + status.getName() + "\n" +
                "version:       " + status.getVersion() + "\n" +
                "server status: " + status.getStatus() + "\n" +
                "api enabled:   " + status.isApiEnabled() + "\n" +
                "server time:   " + status.getServerTime());
    }
}
