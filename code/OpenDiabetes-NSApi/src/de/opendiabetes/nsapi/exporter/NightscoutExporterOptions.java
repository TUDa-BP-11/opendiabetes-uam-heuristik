package de.opendiabetes.nsapi.exporter;

import de.opendiabetes.vault.exporter.ExporterOptions;

public class NightscoutExporterOptions extends ExporterOptions {
    public final static int DEFAULT_MERGEWINDOW = 60;

    private final int mergeWindow;

    public NightscoutExporterOptions() {
        this(DEFAULT_MERGEWINDOW);
    }

    public NightscoutExporterOptions(int mergeWindow) {
        this.mergeWindow = mergeWindow;
    }

    public int getMergeWindow() {
        return mergeWindow;
    }
}
