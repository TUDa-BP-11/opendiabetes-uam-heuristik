package de.opendiabetes.vault.main.dataprovider;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.main.exception.DataProviderException;
import de.opendiabetes.vault.nsapi.Main;
import de.opendiabetes.vault.nsapi.NSApi;
import de.opendiabetes.vault.nsapi.exception.NightscoutIOException;
import de.opendiabetes.vault.nsapi.exception.NightscoutServerException;
import de.opendiabetes.vault.parser.Profile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class TestNightscoutDataProvider {
    private static NSApi api;
    private static NightscoutDataProvider dataProvider = new NightscoutDataProvider();
    private static ZonedDateTime latest = ZonedDateTime.now();
    private static ZonedDateTime oldest = ZonedDateTime.now().minus(4, ChronoUnit.HOURS);

    @BeforeAll
    static void setUp() throws JSAPException, DataProviderException {
        String host = System.getenv("NS_HOST");
        String secret = System.getenv("NS_APISECRET");
        if (host == null)
            System.err.println("Environment variable NS_HOST not found!");
        if (secret == null)
            System.err.println("Environment variable NS_APISECRET not found!");
        if (host == null || secret == null)
            fail("");

        api = new NSApi(host, secret);
        dataProvider.setConfig(getConfig(host, secret, 100, latest, oldest));
    }

    @Test
    public void testArguments() {
        NightscoutDataProvider dataProvider = new NightscoutDataProvider();
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

    @Test
    public void testEntries() throws DataProviderException, NightscoutIOException, NightscoutServerException {
        List<VaultEntry> expected = api.getEntries(latest, oldest, 50);

        if (expected.isEmpty()) {
            assertThrows(DataProviderException.class, () -> dataProvider.getGlucoseMeasurements());
        } else {
            Collections.reverse(expected);   // dataprovider sorts ascending, api returns descending

            List<VaultEntry> actual = dataProvider.getGlucoseMeasurements();
            assertIterableEquals(expected, actual);
        }
    }

    @Test
    public void testTreatments() throws DataProviderException, NightscoutIOException, NightscoutServerException {
        List<VaultEntry> expected = api.getTreatments(latest, oldest, 50);

        if (expected.isEmpty()) {
            assertThrows(DataProviderException.class, () -> dataProvider.getBolusTreatments());
        } else {
            Collections.reverse(expected);   // dataprovider sorts ascending, api returns descending

            List<VaultEntry> expectedBolus = expected.stream().filter(e -> e.getType().equals(VaultEntryType.BOLUS_NORMAL)).collect(Collectors.toList());
            List<VaultEntry> actualBolus = dataProvider.getBolusTreatments();
            assertIterableEquals(expectedBolus, actualBolus);

            List<VaultEntry> expectedBasal = expected.stream().filter(e -> e.getType().equals(VaultEntryType.BASAL_MANUAL)).collect(Collectors.toList());
            List<VaultEntry> actualBasal = dataProvider.getBasalTreatments();
            assertIterableEquals(expectedBasal, actualBasal);
        }
    }

    @Test
    public void testProfile() throws NightscoutIOException, NightscoutServerException, DataProviderException {
        Profile expected = api.getProfile();

        Profile actual = dataProvider.getProfile();
        assertEquals(expected.getTimezone(), actual.getTimezone());
        assertIterableEquals(expected.getBasalProfiles(), actual.getBasalProfiles());
        assertEquals(expected.getCarbratio(), actual.getCarbratio());
        assertEquals(expected.getSensitivity(), actual.getSensitivity());
    }

    private static JSAPResult getConfig(String host, String secret, int batchsize, TemporalAccessor latest, TemporalAccessor oldest) throws JSAPException {
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
