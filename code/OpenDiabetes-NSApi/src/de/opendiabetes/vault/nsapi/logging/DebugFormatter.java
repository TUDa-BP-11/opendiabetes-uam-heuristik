package de.opendiabetes.vault.nsapi.logging;

import de.opendiabetes.vault.nsapi.exception.NightscoutServerException;

import java.io.IOException;
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
        Throwable thrown = record.getThrown();
        if (record.getThrown() != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.println();
            if (thrown instanceof NightscoutServerException) {
                NightscoutServerException exception = (NightscoutServerException) thrown;
                try {
                    String body = exception.getResponseBody();
                    sw.append(body);
                } catch (IOException e) {
                    sw.append("Exception while trying to handle NightscoutServerException: ");
                    sw.append(e.getMessage());
                    sw.append(System.lineSeparator());
                    e.printStackTrace(pw);
                }
                pw.println();
            }
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
