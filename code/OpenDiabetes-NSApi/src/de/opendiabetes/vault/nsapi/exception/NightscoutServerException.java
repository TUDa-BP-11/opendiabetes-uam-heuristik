package de.opendiabetes.vault.nsapi.exception;

import com.mashape.unirest.http.HttpResponse;
import de.opendiabetes.vault.util.IOStreamUtil;

import java.io.IOException;
import java.io.InputStream;

/**
 * Represents a bad HTTP response status from the Nightscout server.
 * Any HTTP status other than 200 is treated as a bad status.
 */
public class NightscoutServerException extends Exception {
    private HttpResponse<InputStream> response;
    private String body;

    public NightscoutServerException(HttpResponse<InputStream> response) {
        super("Nightscout server returned HTTP status code " + response.getStatus() + ": " + response.getStatusText());
        this.response = response;
    }

    /**
     * @return the body of this response
     * @throws IOException if an I/O error occurs while reading the body
     */
    public String getResponseBody() throws IOException {
        if (this.body == null) {
            InputStream stream = response.getBody();
            this.body = IOStreamUtil.readInputStream(stream);
            stream.close();
        }
        return this.body;
    }
}
