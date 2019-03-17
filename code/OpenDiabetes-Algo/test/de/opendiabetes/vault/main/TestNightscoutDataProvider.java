package de.opendiabetes.vault.main;

import de.opendiabetes.vault.main.dataprovider.NightscoutDataProvider;
import de.opendiabetes.vault.main.exception.DataProviderException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestNightscoutDataProvider {
    @Test
    public void testArguments() {
        // latest is before oldest
        assertThrows(DataProviderException.class, () ->
                new NightscoutDataProvider("", "", 1,
                        LocalDateTime.now().minus(1, ChronoUnit.MINUTES),
                        LocalDateTime.now()));
        // batch size is 0
        assertThrows(DataProviderException.class, () ->
                new NightscoutDataProvider("", "", 0,
                        LocalDateTime.now(),
                        LocalDateTime.now().minus(1, ChronoUnit.MINUTES)));
        // host not available
        assertThrows(DataProviderException.class, () ->
                new NightscoutDataProvider("http://localhost", "mysecret", 1,
                        LocalDateTime.now(),
                        LocalDateTime.now().minus(1, ChronoUnit.MINUTES)));
    }
}
