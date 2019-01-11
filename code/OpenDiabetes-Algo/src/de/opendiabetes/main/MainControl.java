package de.opendiabetes.main;

import de.opendiabetes.nsapi.NSApi;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * The main thread. Polls the NightScout API for new entries and passes them to the algorithm
 */
public class MainControl {
    /**
     * Connection to the API used for polling entries
     */
    private NSApi entriesApi;

    /**
     * Connection to the API used for uploading calculated treatments
     */
    private NSApi treatmentsApi;

    private ScheduledExecutorService scheduler;
    private ScheduledFuture mainTask;

    /**
     * Creates a new thread
     *
     * @param entriesApi    API used for polling entries
     * @param treatmentsApi API used for uploading calculated treatments
     */
    public MainControl(NSApi entriesApi, NSApi treatmentsApi) {
        this.entriesApi = entriesApi;
        this.treatmentsApi = treatmentsApi;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    /**
     * Starts the scheduler.
     */
    public void start() {
        if (mainTask != null)
            throw new RuntimeException("You cannot start the scheduler a second time.");
        mainTask = scheduler.scheduleAtFixedRate(() -> {
            //TODO do the work
            Log.logInfo("BEEP");
        }, 0, 10, TimeUnit.SECONDS);
    }

    /**
     * Stops the scheduler. Currently running tasks are allowed to finish.
     */
    public void stop() {
        Log.logInfo("Stopping MainControl...");
        mainTask.cancel(false);
        scheduler.shutdown();
        entriesApi.close();
        treatmentsApi.close();
        Log.logInfo("Goodbye!");
    }

    /**
     * Stops the scheduler. Aborts all currently running tasks.
     */
    public void abort() {
        Log.logInfo("Aborting execution, stopping MainControl...");
        mainTask.cancel(true);
        scheduler.shutdownNow();
        entriesApi.close();
        treatmentsApi.close();
        Log.logInfo("Goodbye!");
    }
}
