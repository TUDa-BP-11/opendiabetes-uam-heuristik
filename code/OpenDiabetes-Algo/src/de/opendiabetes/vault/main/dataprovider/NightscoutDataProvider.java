package de.opendiabetes.vault.main.dataprovider;

import com.martiansoftware.jsap.JSAPResult;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.main.exception.DataProviderException;
import de.opendiabetes.vault.nsapi.NSApi;
import de.opendiabetes.vault.nsapi.exception.NightscoutIOException;
import de.opendiabetes.vault.nsapi.exception.NightscoutServerException;
import de.opendiabetes.vault.parser.Profile;
import de.opendiabetes.vault.parser.Status;

import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class NightscoutDataProvider implements DataProvider {
    private int batchSize;
    private TemporalAccessor latest;
    private TemporalAccessor oldest;

    private NSApi api;
    private List<VaultEntry> entries;
    private List<VaultEntry> treatments;
    private List<VaultEntry> bolusTreatments;
    private List<VaultEntry> basalTreatments;
    private Profile profile;

    @Override
    public void setConfig(JSAPResult config) throws DataProviderException {
        if (!config.contains("host"))
            throw new DataProviderException(this, "No nightscout host specified!");

        this.batchSize = config.getInt("batchsize");
        this.latest = (ZonedDateTime) config.getObject("latest");
        this.oldest = (ZonedDateTime) config.getObject("oldest");
        if (this.batchSize < 1)
            throw new DataProviderException(this, "Invalid argument: batch size has to be a positive number");

        this.api = new NSApi(config.getString("host"), config.getString("secret"));

        try {
            Status status = api.getStatus();
            if (!status.isStatusOk())
                throw new DataProviderException(this, "Nightscout server returned status " + status.getStatus());
            if (!status.isApiEnabled())
                throw new DataProviderException(this, "Nightscout server returned api is not enabled");
        } catch (NightscoutIOException | NightscoutServerException e) {
            throw new DataProviderException(this, "Exception while reading status from Nightscout: " + e.getMessage(), e);
        }
    }

    private void fetchEntries() throws DataProviderException {
        try {
            entries = api.getEntries(latest, oldest, batchSize);
        } catch (NightscoutIOException | NightscoutServerException e) {
            throw new DataProviderException(this, "Exception while reading entries from Nightscout: " + e.getMessage(), e);
        }
        if (entries.isEmpty())
            throw new DataProviderException(this, "No entries found in Nightscout instance");
    }

    private void fetchTreatments() throws DataProviderException {
        try {
            treatments = api.getTreatments(latest, oldest, batchSize);
        } catch (NightscoutIOException | NightscoutServerException e) {
            throw new DataProviderException(this, "Exception while reading treatments from Nightscout: " + e.getMessage(), e);
        }
        if (treatments.isEmpty())
            throw new DataProviderException(this, "No treatments found in Nightscout instance");
    }

    @Override
    public List<VaultEntry> getGlucoseMeasurements() throws DataProviderException {
        if (entries == null)
            fetchEntries();
        return entries.stream()
                .filter(e -> e.getType().equals(VaultEntryType.GLUCOSE_CGM))
                .sorted(Comparator.comparing(VaultEntry::getTimestamp))
                .collect(Collectors.toList());
    }

    @Override
    public List<VaultEntry> getBolusTreatments() throws DataProviderException {
        if (treatments == null)
            fetchTreatments();
        if (bolusTreatments == null)
            bolusTreatments = treatments.stream()
                    .filter(e -> e.getType().equals(VaultEntryType.BOLUS_NORMAL))
                    .sorted(Comparator.comparing(VaultEntry::getTimestamp))
                    .collect(Collectors.toList());
        return bolusTreatments;
    }

    @Override
    public List<VaultEntry> getBasalTreatments() throws DataProviderException {
        if (treatments == null)
            fetchTreatments();
        if (basalTreatments == null) {
            basalTreatments = treatments.stream()
                    .filter(e -> e.getType().equals(VaultEntryType.BASAL_MANUAL))
                    .sorted(Comparator.comparing(VaultEntry::getTimestamp))
                    .collect(Collectors.toList());
        }

        return basalTreatments;
    }

    @Override
    public Profile getProfile() throws DataProviderException {
        if (profile == null) {
            try {
                profile = api.getProfile();
            } catch (NightscoutIOException | NightscoutServerException e) {
                throw new DataProviderException(this, "Exception while reading profile from Nightscout: " + e.getMessage(), e);
            }
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
