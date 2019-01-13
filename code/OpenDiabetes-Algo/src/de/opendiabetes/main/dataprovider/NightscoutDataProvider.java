package de.opendiabetes.main.dataprovider;

import com.mashape.unirest.http.exceptions.UnirestException;
import de.opendiabetes.main.algo.TempBasal;
import de.opendiabetes.main.exception.DataProviderException;
import de.opendiabetes.main.math.BasalCalc;
import de.opendiabetes.nsapi.GetBuilder;
import de.opendiabetes.nsapi.NSApi;
import de.opendiabetes.parser.Profile;
import de.opendiabetes.parser.Status;
import de.opendiabetes.vault.engine.container.VaultEntry;
import de.opendiabetes.vault.engine.container.VaultEntryType;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class NightscoutDataProvider implements AlgorithmDataProvider {
    private String host;
    private String apiSecret;
    private TemporalAccessor latest;
    private TemporalAccessor oldest;
    private int batchSize;

    private NSApi api;
    private List<VaultEntry> entries;
    private List<VaultEntry> treatments;
    private List<TempBasal> basals;
    private Profile profile;

    /**
     * Constructs a Nightscout data provider that fetches all entries in the given time span with the given batch size.
     * All dates are given in ISO-8601 representation. Default values are assumed if arguments are null.
     *
     * @param host      host of the Nightscoout instance
     * @param apiSecret api secret of the Nightscout instance
     * @param latest    latest point in time. If null, the current time is used
     * @param oldest    oldest point in time. If null, this is set 30 minutes before latest
     * @param batchSize amount of entries to fetch at once. If null, a batch size of 100 is used.
     * @throws DataProviderException if host or apiSecret are null or if oldest is after latest
     */
    public NightscoutDataProvider(String host, String apiSecret, TemporalAccessor latest, TemporalAccessor oldest, Integer batchSize) {
        if (host == null)
            throw new DataProviderException(this, "No nightscout host specified!");
        this.host = host;

        if (apiSecret == null)
            throw new DataProviderException(this, "No nightscout api secret specified!");
        this.apiSecret = apiSecret;

        if (latest == null)
            this.latest = LocalDateTime.now();
        else this.latest = latest;

        if (oldest == null)
            this.oldest = LocalDateTime.from(this.latest).minus(30, ChronoUnit.MINUTES);
        else this.oldest = oldest;

        if (LocalDateTime.from(this.oldest).isAfter(LocalDateTime.from(this.latest)))
            throw new DataProviderException(this, "Invalid arguments: oldest cannot be after latest");

        if (batchSize == null)
            this.batchSize = 100;
        else this.batchSize = batchSize;
        if (this.batchSize < 1)
            throw new DataProviderException(this, "Invalid argument: batch size has to be a positive number");

        this.api = new NSApi(host, apiSecret);

        try {
            Status status = api.getStatus();
            if (!status.isStatusOk())
                throw new DataProviderException(this, "Nightscout server returned status " + status.getStatus());
            if (!status.isApiEnabled())
                throw new DataProviderException(this, "Nightscout server returned api is not enabled");
        } catch (UnirestException e) {
            throw new DataProviderException(this, "Exception while reading status from Nightscout: " + e.getMessage(), e);
        }
    }

    private void fetchEntries() {
        entries = new ArrayList<>();
        List<VaultEntry> fetched = null;
        GetBuilder getBuilder;
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        String latest = formatter.format(this.latest);
        String oldest = formatter.format(this.oldest);
        try {
            do {
                getBuilder = api.getEntries().count(batchSize).find("dateString").gte(oldest);
                if (fetched == null)    // first fetch: lte, following fetches: lt
                    getBuilder.find("dateString").lte(latest);
                else getBuilder.find("dateString").lt(latest);
                fetched = getBuilder.getVaultEntries();
                if (!fetched.isEmpty()) {
                    entries.addAll(fetched);
                    latest = fetched.get(fetched.size() - 1).getTimestamp().toInstant().toString();
                }
            } while (!fetched.isEmpty());
        } catch (UnirestException e) {
            throw new DataProviderException(this, "Exception while reading entries from Nightscout: " + e.getMessage(), e);
        }
        if (entries.isEmpty())
            throw new DataProviderException(this, "No entries found in Nightscout instance");
    }

    private void fetchTreatments() {
        treatments = new ArrayList<>();
        List<VaultEntry> fetched = null;
        GetBuilder getBuilder;
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        String latest = formatter.format(this.latest);
        String oldest = formatter.format(this.oldest);
        try {
            do {
                getBuilder = api.getTreatments().count(batchSize).find("created_at").gte(oldest);
                if (fetched == null)    // first fetch: lte, following fetches: lt
                    getBuilder.find("created_at").lte(latest);
                else getBuilder.find("created_at").lt(latest);
                fetched = getBuilder.getVaultEntries();
                if (!fetched.isEmpty()) {
                    treatments.addAll(fetched);
                    latest = fetched.get(fetched.size() - 1).getTimestamp().toInstant().toString();
                }
            } while (!fetched.isEmpty());
        } catch (UnirestException e) {
            throw new DataProviderException(this, "Exception while reading treatments from Nightscout: " + e.getMessage(), e);
        }
        if (treatments.isEmpty())
            throw new DataProviderException(this, "No treatments found in Nightscout instance");
    }

    @Override
    public List<VaultEntry> getGlucoseMeasurements() {
        if (entries == null)
            fetchEntries();
        return entries.stream()
                .filter(e -> e.getType().equals(VaultEntryType.GLUCOSE_CGM))
                .sorted(Comparator.comparing(VaultEntry::getTimestamp))
                .collect(Collectors.toList());
    }

    @Override
    public List<VaultEntry> getBolusTreatments() {
        if (treatments == null)
            fetchTreatments();

        return treatments.stream()
                .filter(e -> e.getType().equals(VaultEntryType.BOLUS_NORMAL))
                .sorted(Comparator.comparing(VaultEntry::getTimestamp))
                .collect(Collectors.toList());
    }

    @Override
    public List<TempBasal> getBasalTratments() {
        if (treatments == null)
            fetchTreatments();
        if (basals == null) {
            BasalCalc calc = new BasalCalc(getProfile());
            List<VaultEntry> list = treatments.stream()
                    .filter(e -> e.getType().equals(VaultEntryType.BASAL_MANUAL))
                    .sorted(Comparator.comparing(VaultEntry::getTimestamp))
                    .collect(Collectors.toList());
            basals = calc.calculateBasal(list);
        }
        return basals;
    }

    @Override
    public Profile getProfile() {
        if (profile == null) {
            try {
                profile = api.getProfile();
            } catch (UnirestException e) {
                throw new DataProviderException(this, "Exception while reading profile from Nightscout: " + e.getMessage(), e);
            }
        }
        return profile;
    }

    @Override
    public void close() {
        try {
            api.close();
        } catch (IOException e) {
            throw new DataProviderException(this, "IOException while closing connection: " + e.getMessage(), e);
        }
    }

    public String getHost() {
        return host;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public TemporalAccessor getLatest() {
        return latest;
    }

    public TemporalAccessor getOldest() {
        return oldest;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public NSApi getApi() {
        return api;
    }
}
