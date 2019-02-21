package de.opendiabetes.main.dataprovider;

import de.opendiabetes.main.exception.DataProviderException;
import de.opendiabetes.main.math.BasalCalculator;
import de.opendiabetes.parser.Profile;
import de.opendiabetes.parser.ProfileParser;
import de.opendiabetes.parser.TreatmentMapper;
import de.opendiabetes.parser.VaultEntryParser;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class FileDataProvider implements AlgorithmDataProvider {
    private Path entriesPath;
    private Path treatmentsPath;
    private Path profilePath;
    private TemporalAccessor latest;
    private TemporalAccessor oldest;

    private List<VaultEntry> entries;
    private List<VaultEntry> treatments;
    private List<VaultEntry> basals;
    private Profile profile;

    /**
     * Creates a data provider that reads data from disk. All dates are given in ISO-8601 representation.
     * Default values are assumed if arguments are null.
     *
     * @param base       path to base directory. If null, the current working directory is used
     * @param entries    path to entries file relative to base directory. If null, <code>entries.json</code> is used
     * @param treatments path to treatments file relative to base directory. If null, <code>treatments.json</code> is used
     * @param profile    path to profile file relative to base directory. If null, <code>profile.json</code> is used
     * @param latest     latest point in time. If null, the current time is used
     * @param oldest     oldest point in time. If null, this is set 30 minutes before latest
     * @throws DataProviderException if any path of the given or assumed paths is invalid or a file cannot be found or if oldest is after latest
     */
    public FileDataProvider(String base, String entries, String treatments, String profile, TemporalAccessor latest, TemporalAccessor oldest) {
        Path basePath;
        if (base == null)
            basePath = Paths.get("");
        else basePath = Paths.get(base);
        if (!Files.isDirectory(basePath))
            throw new DataProviderException(this, "No directory found at base path " + basePath.toAbsolutePath().toString());

        if (entries == null)
            entriesPath = basePath.resolve("entries.json");
        else entriesPath = basePath.resolve(entries);
        if (!Files.isRegularFile(entriesPath))
            throw new DataProviderException(this, "No file found at entries path " + entriesPath.toAbsolutePath().toString());

        if (treatments == null)
            treatmentsPath = basePath.resolve("treatments.json");
        else treatmentsPath = basePath.resolve(treatments);
        if (!Files.isRegularFile(treatmentsPath))
            throw new DataProviderException(this, "No file found at treatments path " + treatmentsPath.toAbsolutePath().toString());

        if (profile == null)
            profilePath = basePath.resolve("profile.json");
        else profilePath = basePath.resolve(profile);
        if (!Files.isRegularFile(profilePath))
            throw new DataProviderException(this, "No file found at profile path " + profilePath.toAbsolutePath().toString());

        if (latest == null)
            this.latest = LocalDateTime.now();
        else this.latest = latest;

        if (oldest == null)
            this.oldest = LocalDateTime.from(this.latest).minus(30, ChronoUnit.MINUTES);
        else this.oldest = oldest;

        if (LocalDateTime.from(this.oldest).isAfter(LocalDateTime.from(this.latest)))
            throw new DataProviderException(this, "Invalid arguments: oldest cannot be after latest");
    }

    @Override
    public List<VaultEntry> getGlucoseMeasurements() {
        if (entries == null) {
            VaultEntryParser parser = new VaultEntryParser();
            List<VaultEntry> list = parser.parseFile(entriesPath);
            entries = list.stream()
                    .filter(e -> e.getType().equals(VaultEntryType.GLUCOSE_CGM))
                    .sorted(Comparator.comparing(VaultEntry::getTimestamp))
                    .collect(Collectors.toList());
        }
        return entries;
    }

    private void readTreatments() {
        VaultEntryParser parser = new VaultEntryParser();
        treatments = parser.parseFile(treatmentsPath);
    }

    @Override
    public List<VaultEntry> getBolusTreatments() {
        if (treatments == null)
            readTreatments();
        return treatments.stream()
                .filter(e -> e.getType().equals(VaultEntryType.BOLUS_NORMAL))
                .sorted(Comparator.comparing(VaultEntry::getTimestamp))
                .collect(Collectors.toList());
    }

    @Override
    public List<VaultEntry> getBasalTratments() {
        if (treatments == null)
            readTreatments();
        if (basals == null) {
            List<VaultEntry> list = treatments.stream()
                    .filter(e -> e.getType().equals(VaultEntryType.BASAL_MANUAL))
                    .sorted(Comparator.comparing(VaultEntry::getTimestamp))
                    .collect(Collectors.toList());
            basals = BasalCalculator.calcBasals(TreatmentMapper.adjustBasalTreatments(list), getProfile());
        }
        return basals;
    }

    @Override
    public Profile getProfile() {
        if (profile == null) {
            ProfileParser parser = new ProfileParser();
            profile = parser.parseFile(profilePath);
        }
        return profile;
    }

    public TemporalAccessor getLatest() {
        return latest;
    }

    public TemporalAccessor getOldest() {
        return oldest;
    }
}
