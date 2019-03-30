package de.opendiabetes.vault.main.dataprovider;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.main.exception.DataProviderException;
import de.opendiabetes.vault.nsapi.Main;
import de.opendiabetes.vault.nsapi.NSApiTools;
import de.opendiabetes.vault.nsapi.exception.NightscoutIOException;
import de.opendiabetes.vault.parser.Profile;
import de.opendiabetes.vault.parser.ProfileParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestFileDataProvider {
    private static Path entries;
    private static Path treatments;
    private static Path profile;
    private static FileDataProvider dataProvider = new FileDataProvider();
    private static ZonedDateTime latest = ZonedDateTime.now();
    private static ZonedDateTime oldest = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault());

    @BeforeAll
    static void setup() throws JSAPException, DataProviderException {
        Path module = Paths.get("code", "OpenDiabetes-Algo");
        Path testdata;
        if (Files.isDirectory(module)) {
            // Test is executed in global directory
            testdata = Paths.get(module.toString(), "testdata");
        } else {
            // Test is executed in module directory
            testdata = Paths.get("testdata");
        }
        if (!Files.isDirectory(testdata))
            fail("Could not find testdata directory!");
        entries = Paths.get(testdata.toString(), "entries.json").normalize();
        treatments = Paths.get(testdata.toString(), "treatments.json").normalize();
        profile = Paths.get(testdata.toString(), "profile.json").normalize();

        JSAPResult config = getConfig(entries.toString(), treatments.toString(), profile.toString(), latest, oldest);
        dataProvider.setConfig(config);
    }

    @Test
    public void testArguments() {
        FileDataProvider dataProvider = new FileDataProvider();
        assertThrows(DataProviderException.class, () -> dataProvider.setConfig(getConfig(
                "invalid.json", treatments.toString(), profile.toString(), latest, oldest
        )));
        assertThrows(DataProviderException.class, () -> dataProvider.setConfig(getConfig(
                entries.toString(), "invalid.json", profile.toString(), latest, oldest
        )));
        assertThrows(DataProviderException.class, () -> dataProvider.setConfig(getConfig(
                entries.toString(), treatments.toString(), "invalid.json", latest, oldest
        )));
    }

    @Test
    public void testEntries() throws NightscoutIOException {
        List<VaultEntry> expected = NSApiTools.loadDataFromFile(entries.toString(), VaultEntryType.GLUCOSE_CGM, true);

        List<VaultEntry> actual = dataProvider.getGlucoseMeasurements();
        assertIterableEquals(expected, actual);
    }

    @Test
    public void testBolus() throws NightscoutIOException {
        List<VaultEntry> expected = NSApiTools.loadDataFromFile(treatments.toString(), VaultEntryType.BOLUS_NORMAL, true);

        List<VaultEntry> actual = dataProvider.getBolusTreatments();
        assertIterableEquals(expected, actual);
    }

    @Test
    public void testBasal() throws NightscoutIOException {
        List<VaultEntry> expected = NSApiTools.loadDataFromFile(treatments.toString(), VaultEntryType.BASAL_MANUAL, true);

        List<VaultEntry> actual = dataProvider.getBasalTreatments();
        assertIterableEquals(expected, actual);
    }

    @Test
    public void testProfile() {
        Profile expected = new ProfileParser().parseFile(profile);

        Profile actual = dataProvider.getProfile();
        assertEquals(expected.getTimezone(), actual.getTimezone());
        assertIterableEquals(expected.getBasalProfiles(), actual.getBasalProfiles());
        assertEquals(expected.getCarbratio(), actual.getCarbratio());
        assertEquals(expected.getSensitivity(), actual.getSensitivity());
    }

    private static JSAPResult getConfig(String entries, String treatments, String profile, TemporalAccessor latest, TemporalAccessor oldest) throws JSAPException {
        JSAP jsap = new JSAP();
        jsap.registerParameter(
                new FlaggedOption("entries")
                        .setStringParser(JSAP.STRING_PARSER)
                        .setLongFlag("entries")
        );
        jsap.registerParameter(
                new FlaggedOption("treatments")
                        .setStringParser(JSAP.STRING_PARSER)
                        .setLongFlag("treatments")
        );
        jsap.registerParameter(
                new FlaggedOption("profile")
                        .setStringParser(JSAP.STRING_PARSER)
                        .setLongFlag("profile")
        );
        jsap.registerParameter(
                new FlaggedOption("latest")
                        .setStringParser(new Main.IsoDateTimeParser())
                        .setLongFlag("latest")
        );
        jsap.registerParameter(
                new FlaggedOption("oldest")
                        .setStringParser(new Main.IsoDateTimeParser())
                        .setLongFlag("oldest")
        );
        return jsap.parse(new String[]{
                "--entries", entries,
                "--treatments", treatments,
                "--profile", profile,
                "--latest", latest.toString(),
                "--oldest", oldest.toString()
        });
    }
}
