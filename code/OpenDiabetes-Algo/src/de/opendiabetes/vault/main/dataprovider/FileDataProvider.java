package de.opendiabetes.vault.main.dataprovider;

import com.martiansoftware.jsap.JSAPResult;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.main.exception.DataProviderException;
import de.opendiabetes.vault.nsapi.NSApi;
import de.opendiabetes.vault.nsapi.importer.NightscoutImporter;
import de.opendiabetes.vault.parser.Profile;
import de.opendiabetes.vault.parser.ProfileParser;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class FileDataProvider implements DataProvider {

    private Path entriesPath;
    private Path treatmentsPath;
    private Path profilePath;
    private TemporalAccessor latest;
    private TemporalAccessor oldest;

    private List<VaultEntry> entries;
    private List<VaultEntry> treatments;
    private List<VaultEntry> bolusTreatments;
    private List<VaultEntry> rawBasals;
    private Profile profile;
    private NightscoutImporter importer;

    @Override
    public void setConfig(JSAPResult config) throws DataProviderException {
        if (!config.contains("entries") || !config.contains("treatments") || !config.contains("profile"))
            throw new DataProviderException(this, "Please specify paths to your files of blood glucose values treatments and your profile");

        importer = new NightscoutImporter();

        entriesPath = Paths.get(config.getString("entries"));
        if (!Files.isRegularFile(entriesPath)) {
            throw new DataProviderException(this, "No file found at entries path " + entriesPath.toAbsolutePath().toString());
        }

        treatmentsPath = Paths.get(config.getString("treatments"));
        if (!Files.isRegularFile(treatmentsPath)) {
            throw new DataProviderException(this, "No file found at treatments path " + treatmentsPath.toAbsolutePath().toString());
        }

        profilePath = Paths.get(config.getString("profile"));
        if (!Files.isRegularFile(profilePath)) {
            throw new DataProviderException(this, "No file found at profile path " + profilePath.toAbsolutePath().toString());
        }

        this.latest = (ZonedDateTime) config.getObject("latest");
        this.oldest = (ZonedDateTime) config.getObject("oldest");
    }

    @Override
    public List<VaultEntry> getBasalTreatments() {
        if (treatments == null) {
            readTreatments();
        }
        if (rawBasals == null) {
            rawBasals = treatments.stream()
                    .filter(e -> e.getType().equals(VaultEntryType.BASAL_MANUAL))
                    .sorted(Comparator.comparing(VaultEntry::getTimestamp))
                    .filter(e -> e.getTimestamp().toInstant().isAfter(Instant.from(oldest)))
                    .filter(e -> e.getTimestamp().toInstant().isBefore(Instant.from(latest)))
                    .collect(Collectors.toList());
        }

        return rawBasals;
    }

    @Override
    public List<VaultEntry> getGlucoseMeasurements() {
        if (entries == null) {
            List<VaultEntry> list = new ArrayList<>();
            try (InputStream stream = new FileInputStream(entriesPath.toString())) {
                list = importer.importData(stream);
            } catch (IOException ex) {
                NSApi.LOGGER.log(Level.SEVERE, null, ex);
            }
            entries = list.stream()
                    .filter(e -> e.getType().equals(VaultEntryType.GLUCOSE_CGM))
                    .filter(e -> e.getTimestamp().toInstant().isAfter(Instant.from(oldest)))
                    .filter(e -> e.getTimestamp().toInstant().isBefore(Instant.from(latest)))
                    .sorted(Comparator.comparing(VaultEntry::getTimestamp))
                    .collect(Collectors.toList());
        }
        return entries;
    }

    private void readTreatments() {
        try (InputStream stream = new FileInputStream(treatmentsPath.toString())) {
            treatments = importer.importData(stream);
        } catch (IOException ex) {
            NSApi.LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public List<VaultEntry> getBolusTreatments() {
        if (treatments == null) {
            readTreatments();
        }
        if (bolusTreatments == null) {
            bolusTreatments = treatments.stream()
                    .filter(e -> e.getType().equals(VaultEntryType.BOLUS_NORMAL))
                    .filter(e -> e.getTimestamp().toInstant().isAfter(Instant.from(oldest)))
                    .filter(e -> e.getTimestamp().toInstant().isBefore(Instant.from(latest)))
                    .sorted(Comparator.comparing(VaultEntry::getTimestamp))
                    .collect(Collectors.toList());
        }
        return bolusTreatments;
    }

    @Override
    public Profile getProfile() {
        if (profile == null) {
            ProfileParser parser = new ProfileParser();
            profile = parser.parseFile(profilePath);
        }
        return profile;
    }
}