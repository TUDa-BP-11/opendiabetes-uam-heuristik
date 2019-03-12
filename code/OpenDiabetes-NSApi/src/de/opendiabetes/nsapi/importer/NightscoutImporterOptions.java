package de.opendiabetes.nsapi.importer;

import de.opendiabetes.vault.importer.ImporterOptions;

public class NightscoutImporterOptions extends ImporterOptions {
    public final static boolean DEFAULT_REQUIREVALIDDATA = false;

    private final boolean requireValidData;

    public NightscoutImporterOptions() {
        this(DEFAULT_REQUIREVALIDDATA);
    }

    public NightscoutImporterOptions(boolean requireValidData) {
        this.requireValidData = requireValidData;
    }

    public boolean requireValidData() {
        return requireValidData;
    }
}
