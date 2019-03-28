package de.opendiabetes.vault.main.dataprovider;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import de.opendiabetes.vault.main.exception.DataProviderException;
import de.opendiabetes.vault.nsapi.Main;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestNightscoutDataProvider {
    @Test
    public void testArguments() {
        NightscoutDataProvider dataProvider = new NightscoutDataProvider();
        // latest is before oldest
        assertThrows(DataProviderException.class, () ->
                dataProvider.setConfig(getConfig("http://localhost", "", 1,
                        LocalDateTime.now().minus(1, ChronoUnit.MINUTES),
                        LocalDateTime.now())));
        // batch size is 0
        assertThrows(DataProviderException.class, () ->
                dataProvider.setConfig(getConfig("http://localhost", "", 0,
                        LocalDateTime.now(),
                        LocalDateTime.now().minus(1, ChronoUnit.MINUTES))));
        // host not available
        assertThrows(DataProviderException.class, () ->
                dataProvider.setConfig(getConfig("http://localhost", "mysecret", 1,
                        LocalDateTime.now(),
                        LocalDateTime.now().minus(1, ChronoUnit.MINUTES))));
    }
    private JSAPResult getConfig(String host, String secret, int batchsize, TemporalAccessor latest, TemporalAccessor oldest) throws JSAPException {
        JSAP jsap = new JSAP();
        jsap.registerParameter(
                new FlaggedOption("host")
                        .setStringParser(JSAP.STRING_PARSER)
                        .setLongFlag("host")
                        .setDefault(host)
        );
        jsap.registerParameter(
                new FlaggedOption("secret")
                        .setStringParser(JSAP.STRING_PARSER)
                        .setLongFlag("secret")
                        .setDefault(secret)
        );
        jsap.registerParameter(
                new FlaggedOption("latest")
                        .setStringParser(new Main.IsoDateTimeParser())
                        .setLongFlag("latest")
                        .setDefault(latest.toString())
        );
        jsap.registerParameter(
                new FlaggedOption("oldest")
                        .setStringParser(new Main.IsoDateTimeParser())
                        .setLongFlag("oldest")
                        .setDefault(oldest.toString())
        );
        jsap.registerParameter(
                new FlaggedOption("batchsize")
                        .setStringParser(JSAP.INTEGER_PARSER)
                        .setLongFlag("batchsize")
                        .setDefault(String.valueOf(batchsize))
        );
        return jsap.parse(new String[0]);
    }
    
}
