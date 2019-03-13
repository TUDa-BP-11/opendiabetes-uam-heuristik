package de.opendiabetes.vault.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class IOStreamUtil {
    /**
     * Reads the stream and returns its content as a string. All newlines are converted to <code>\n</code>.
     *
     * @param stream input stream
     * @return content of the stream
     */
    public static String readInputStream(InputStream stream) {
        return new BufferedReader(new InputStreamReader(stream)).lines().collect(Collectors.joining("\n"));
    }
}
