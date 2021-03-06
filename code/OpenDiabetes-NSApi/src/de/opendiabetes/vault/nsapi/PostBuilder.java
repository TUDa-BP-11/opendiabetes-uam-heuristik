package de.opendiabetes.vault.nsapi;

import com.mashape.unirest.request.HttpRequestWithBody;
import de.opendiabetes.vault.nsapi.exception.NightscoutIOException;
import de.opendiabetes.vault.nsapi.exception.NightscoutServerException;

/**
 * Builder for HTTP POST requests
 */
public class PostBuilder {
    private final NSApi api;
    private HttpRequestWithBody request;

    PostBuilder(NSApi api, HttpRequestWithBody request) {
        this.api = api;
        this.request = request;
    }

    /**
     * Sets the body of this request.
     *
     * @param content the content of the body.
     * @return this builder
     */
    public PostBuilder setBody(String content) {
        request.body(content);
        return this;
    }

    /**
     * Sets the body of this request.
     *
     * @param content the content of the body.
     * @return this builder
     */
    public PostBuilder setBody(byte[] content) {
        request.body(content);
        return this;
    }

    /**
     * Sends the request to the server.
     *
     * @throws NightscoutIOException     if an I/O error occurs during the request
     * @throws NightscoutServerException if the Nightscout server returns a bad response status
     */
    public void send() throws NightscoutIOException, NightscoutServerException {
        api.send(request);
    }
}
