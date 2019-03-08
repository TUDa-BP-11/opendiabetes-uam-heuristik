package de.opendiabetes.nsapi.exporter;

import de.opendiabetes.vault.exporter.ExporterOptions;

public class NightscoutExporterOptions extends ExporterOptions {
    public final static int DEFAULT_MERGEWINDOW = 60;
    public final static boolean DEFAULT_PRETTY = false;

    private final int mergeWindow;
    private final boolean pretty;

    public NightscoutExporterOptions() {
        this(DEFAULT_MERGEWINDOW, DEFAULT_PRETTY);
    }

    public NightscoutExporterOptions(int mergeWindow, boolean pretty) {
        this.mergeWindow = mergeWindow;
        this.pretty = pretty;
    }

    public int getMergeWindow() {
        return mergeWindow;
    }

    public boolean isPretty() {
        return pretty;
    }
}
