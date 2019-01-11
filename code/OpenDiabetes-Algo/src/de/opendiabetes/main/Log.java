package de.opendiabetes.main;

import java.text.SimpleDateFormat;

public class Log {
    public final static int ERROR = 3;
    public final static int INFO = 2;
    public final static int DEBUG = 1;
    public final static int TRACE = 0;

    private static int level = INFO;

    public static void setLevel(int level) {
        Log.level = level;
    }

    public static void logError(String error, Object... args) {
        log(ERROR, error, "[ERROR] ", args);
    }

    public static void logInfo(String info, Object... args) {
        log(INFO, info, "[INFO] ", args);
    }

    public static void logDebug(String debug, Object... args) {
        log(DEBUG, debug, "[DEBUG] ", args);
    }

    public static void logTrace(String trace, Object... args) {
        log(TRACE, trace, "[TRACE] ", args);
    }

    private final static SimpleDateFormat time = new SimpleDateFormat("HH:mm:ss");

    private static void log(int level, String text, String prefix, Object... args) {
        if (level < Log.level)
            return;
        System.out.println("[" + time.format(System.currentTimeMillis()) + "]" + prefix + String.format(text, args));
    }
}
