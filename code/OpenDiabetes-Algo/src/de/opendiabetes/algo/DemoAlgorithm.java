package de.opendiabetes.algo;

import de.opendiabetes.vault.engine.container.VaultEntry;

import java.util.Collections;
import java.util.List;

public class DemoAlgorithm implements Algorithm {
    @Override
    public void setCarbRatio(double carbRatio) {

    }

    @Override
    public void setInsulinSensitivity(double insSensitivity) {

    }

    @Override
    public void setAbsorptionTime(double absorptionTime) {

    }

    @Override
    public void setInsulinDuration(double insulinDuration) {

    }

    @Override
    public void setGlucoseMeasurements(List<VaultEntry> entries) {

    }

    @Override
    public void setBolusTreatments(List<VaultEntry> bolusTreatments) {

    }

    @Override
    public void setBasalTratments(List<TempBasal> basalTratments) {

    }

    @Override
    public List<VaultEntry> calculateMeals() {
        return Collections.emptyList();
    }
}
