package de.opendiabetes.vault.nsapi.logging;

import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Formats the log records for friendly user output
 */
public class DefaultFormatter extends Formatter {
    protected Date date = new Date();

    @Override
    public String format(LogRecord record) {
        String message = prepare(record);

        return String.format("%1$tF %1$tT [%2$s] %3$s%n",
                date,
                record.getLevel().getLocalizedName(),
                message);
    }

    protected String prepare(LogRecord record) {
        date.setTime(record.getMillis());
        if (record.getParameters() != null)
            return String.format(record.getMessage(), record.getParameters());
        else return record.getMessage();
    }
}
