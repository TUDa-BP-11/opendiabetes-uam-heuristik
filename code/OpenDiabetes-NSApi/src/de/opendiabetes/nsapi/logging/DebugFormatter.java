package de.opendiabetes.nsapi.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.LogRecord;

public class DebugFormatter extends DefaultFormatter {
    private final boolean verbose;

    public DebugFormatter(boolean verbose) {
        this.verbose = verbose;
    }

    @Override
    public String format(LogRecord record) {
        String message = prepare(record);

        String throwable = "";
        if (record.getThrown() != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.println();
            record.getThrown().printStackTrace(pw);
            if (verbose && record.getThrown().getCause() != null)
                record.getThrown().getCause().printStackTrace(pw);
            pw.close();
            throwable = sw.toString();
        }

        return String.format("%1$tF %1$tT [%2$s] %3$s %4$s%n",
                date,
                record.getLevel().getLocalizedName(),
                message,
                throwable);
    }
}
