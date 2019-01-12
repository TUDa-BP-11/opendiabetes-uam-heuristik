package de.opendiabetes.main;

import de.opendiabetes.main.dataprovider.NightscoutDataProvider;
import de.opendiabetes.main.exception.DataProviderException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestNightscoutDataProvider {
    @Test
    public void testArguments() {
        // latest is before oldest
        assertThrows(DataProviderException.class, () ->
                new NightscoutDataProvider("", "",
                        LocalDateTime.now().minus(1, ChronoUnit.MINUTES),
                        LocalDateTime.now(),
                        1));
        // batch size is 0
        assertThrows(DataProviderException.class, () ->
                new NightscoutDataProvider("", "",
                        LocalDateTime.now(),
                        LocalDateTime.now().minus(1, ChronoUnit.MINUTES),
                        0));
        // host not available
        assertThrows(DataProviderException.class, () ->
                new NightscoutDataProvider("http://localhost", "mysecret",
                        LocalDateTime.now(),
                        LocalDateTime.now().minus(1, ChronoUnit.MINUTES),
                        1));
    }
}
