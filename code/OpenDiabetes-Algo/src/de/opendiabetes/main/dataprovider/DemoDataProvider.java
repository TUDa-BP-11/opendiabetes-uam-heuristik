package de.opendiabetes.main.dataprovider;

import de.opendiabetes.algo.TempBasal;
import de.opendiabetes.vault.engine.container.VaultEntry;

import java.util.List;

public class DemoDataProvider implements AlgorithmDataProvider {
    @Override
    public List<VaultEntry> getGlucoseMeasurements() {
        return null;
    }

    @Override
    public List<VaultEntry> getBolusTreatments() {
        return null;
    }

    @Override
    public List<TempBasal> getBasalTratments() {
        return null;
    }
}
