package de.opendiabetes.vault.main.dataprovider;

import de.opendiabetes.vault.nsapi.exporter.NightscoutExporter;
import de.opendiabetes.vault.nsapi.exporter.NightscoutExporterOptions;
import de.opendiabetes.vault.parser.Profile;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.util.SortVaultEntryByDate;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * A demo data provider.
 * Generates the data that would result from 4 U insulin input and 60 gr carb input now,
 * based on the Glucodyn model http://perceptus.org/about/glucodyn.
 * 10 glucose measurements are given (every 5 minutes from 0 to 45)
 */
public class DemoDataProvider implements AlgorithmDataProvider {
    private Date start = new Date(System.currentTimeMillis() - 45 * 60 * 1000);

    @Override
    public List<VaultEntry> getGlucoseMeasurements() {
        return Arrays.asList(
                new VaultEntry(VaultEntryType.GLUCOSE_CGM, start, 100),
                new VaultEntry(VaultEntryType.GLUCOSE_CGM, getLater(start, 5), 100.1),
                new VaultEntry(VaultEntryType.GLUCOSE_CGM, getLater(start, 10), 103.5),
                new VaultEntry(VaultEntryType.GLUCOSE_CGM, getLater(start, 15), 107.3),
                new VaultEntry(VaultEntryType.GLUCOSE_CGM, getLater(start, 20), 111),
                new VaultEntry(VaultEntryType.GLUCOSE_CGM, getLater(start, 25), 116),
                new VaultEntry(VaultEntryType.GLUCOSE_CGM, getLater(start, 30), 122),
                new VaultEntry(VaultEntryType.GLUCOSE_CGM, getLater(start, 35), 132.5),
                new VaultEntry(VaultEntryType.GLUCOSE_CGM, getLater(start, 40), 144),
                new VaultEntry(VaultEntryType.GLUCOSE_CGM, getLater(start, 45), 156)
        );
    }

    private Date getLater(Date start, int minutes) {
        return new Date(start.getTime() + minutes * 60 * 1000);
    }

    @Override
    public List<VaultEntry> getBolusTreatments() {
        return Collections.singletonList(new VaultEntry(VaultEntryType.BOLUS_NORMAL, start, 4));
    }

    @Override
    public List<VaultEntry> getRawBasalTreatments() {
        return Collections.emptyList();
    }

    @Override
    public List<VaultEntry> getBasalDifferences() {
        return Collections.emptyList();
    }

    @Override
    public Profile getProfile() {
        return new Profile(ZoneId.systemDefault(), 35, 10, Collections.singletonList(new Profile.BasalProfile(LocalTime.of(0, 0), 1)));
    }

    public static void main(String[] args) {
        NightscoutExporter exporter = new NightscoutExporter(new NightscoutExporterOptions(60, true));
        DemoDataProvider provider = new DemoDataProvider();
        List<VaultEntry> entries = provider.getGlucoseMeasurements();
        entries.sort(new SortVaultEntryByDate().reversed());
        exporter.exportData(System.out, entries);
        exporter.exportData(System.out, provider.getBolusTreatments());
    }
}
