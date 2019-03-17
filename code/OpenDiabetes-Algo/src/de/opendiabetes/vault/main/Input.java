package de.opendiabetes.vault.main;

import de.opendiabetes.vault.main.exception.CommandUsageException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Input extends Thread {
    private boolean active = true;

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String input;
            loop:
            while (active) {
                while (!reader.ready() && active) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        // do nothing
                    }
                }
                if (!active)
                    break;
                input = reader.readLine();
                try {
                    String[] split = input.split(" ");
                    switch (split[0].toLowerCase()) {
                        case "stop":
                            //TODO
                            shutdown();
                            break loop;
                        case "abort":
                            //TODO
                            shutdown();
                            break loop;
                        default:
                            Log.logError("Unknown command: " + split[0]);
                    }
                } catch (Exception e) {
                    if (e instanceof CommandUsageException) {
                        Log.logError(e.getMessage());
                    } else e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        active = false;
    }
}
