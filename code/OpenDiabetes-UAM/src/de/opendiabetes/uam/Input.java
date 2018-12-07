package de.opendiabetes.uam;

import de.opendiabetes.uam.exception.CommandUsageException;

import java.util.Scanner;

public class Input extends Thread {

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        String input;
        loop:
        while (true) {
            input = scanner.nextLine();
            try {
                String[] split = input.split(" ");
                switch (split[0].toLowerCase()) {
                    case "stop":
                        Main.getMainControl().stop();
                        break loop;
                    default:
                        Log.logError("Unknown command: ");
                }
            } catch (Exception e) {
                if (e instanceof CommandUsageException) {
                    Log.logError(e.getMessage());
                } else e.printStackTrace();
            }
        }
    }
}
