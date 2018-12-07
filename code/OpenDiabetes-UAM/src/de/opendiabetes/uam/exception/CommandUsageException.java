package de.opendiabetes.uam.exception;

public class CommandUsageException extends RuntimeException {
    public CommandUsageException(String command, String usage) {
        super("Invalid command usage for " + command + ". Use '" + usage + "'");
    }
}