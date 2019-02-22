package de.opendiabetes.nsapi.logging;

import java.util.logging.LogRecord;

public class VerboseFormatter extends DefaultFormatter {
    @Override
    public String format(LogRecord record) {
        String message = prepare(record);

        String throwable = "";
        if (record.getThrown() != null) {
            throwable = record.getThrown().getMessage();
        }

        return String.format("%1$tF %1$tT [%2$s] %3$s %4$s%n",
                date,
                record.getLevel().getLocalizedName(),
                message,
                throwable);
    }
}
