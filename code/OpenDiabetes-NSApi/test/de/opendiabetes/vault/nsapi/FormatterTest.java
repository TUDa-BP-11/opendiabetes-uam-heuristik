package de.opendiabetes.vault.nsapi;

import de.opendiabetes.vault.nsapi.logging.DebugFormatter;
import de.opendiabetes.vault.nsapi.logging.DefaultFormatter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FormatterTest {
    private static LogRecord log;

    @BeforeAll
    static void setup() {
        long time = System.currentTimeMillis();
        log = new LogRecord(Level.INFO, "message");
        String className = FormatterTest.class.getName();
        String methodName = className + ".testDebugFormatter";
        log.setSourceClassName(className);
        log.setSourceMethodName(methodName);
        log.setMillis(time);
        log.setThrown(new NullPointerException());
    }

    @Test
    void testDefaultFormatter() {
        DefaultFormatter formatter = new DefaultFormatter();
        String message = formatter.format(log);

        String expected = String.format("%1$tF %1$tT [%2$s] message%n", new Date(log.getMillis()), Level.INFO.getLocalizedName());
        assertEquals(expected, message);
    }

    @Test
    void testDebugFormatter() {
        DefaultFormatter formatter = new DebugFormatter();
        String message = formatter.format(log);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println();
        log.getThrown().printStackTrace(pw);
        pw.close();
        String throwable = sw.toString();

        String expected = String.format("%1$tF %1$tT [%2$s] %3$s %4$s: message %5$s%n", new Date(log.getMillis()),
                Level.INFO.getLocalizedName(), log.getSourceClassName(), log.getSourceMethodName(), throwable);
        assertEquals(expected, message);
    }
}
