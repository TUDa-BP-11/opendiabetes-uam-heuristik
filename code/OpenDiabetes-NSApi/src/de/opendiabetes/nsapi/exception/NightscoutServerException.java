package de.opendiabetes.nsapi.exception;

import com.mashape.unirest.http.HttpResponse;

/**
 * Represents a bad HTTP response from the Nightscout server
 */
public class NightscoutServerException extends RuntimeException {
    public NightscoutServerException(HttpResponse<String> response) {
        super("Nightscout server returned HTTP status code " + response.getStatus() + ": " + response.getStatusText(), new Body(response));
    }

    private static class Body extends Exception {
        Body(HttpResponse<String> response) {
            super("\n" + response.getBody());
        }
    }
}
