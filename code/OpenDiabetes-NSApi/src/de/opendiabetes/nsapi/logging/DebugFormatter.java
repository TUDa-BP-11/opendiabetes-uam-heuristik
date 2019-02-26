package de.opendiabetes.nsapi.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.LogRecord;

/**
 * Formats the log records for debugging purposes.
 * Prints stack traces of throwables attached to log records.
 */
public class DebugFormatter extends DefaultFormatter {
    @Override
    public String format(LogRecord record) {
        String message = prepare(record);

        String source;
        if (record.getSourceClassName() != null) {
            source = record.getSourceClassName();
            if (record.getSourceMethodName() != null) {
                source += " " + record.getSourceMethodName();
            }
        } else {
            source = record.getLoggerName();
        }

        String throwable = "";
        if (record.getThrown() != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.println();
            record.getThrown().printStackTrace(pw);
            pw.close();
            throwable = sw.toString();
        }

        return String.format("%1$tF %1$tT [%2$s] %3$s: %4$s %5$s%n",
                date,
                record.getLevel().getLocalizedName(),
                source,
                message,
                throwable);
    }
}
