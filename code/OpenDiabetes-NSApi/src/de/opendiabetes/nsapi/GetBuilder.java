package de.opendiabetes.nsapi;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mashape.unirest.request.HttpRequest;
import de.opendiabetes.nsapi.exception.NightscoutDataException;
import de.opendiabetes.nsapi.exception.NightscoutIOException;
import de.opendiabetes.nsapi.exception.NightscoutServerException;
import de.opendiabetes.nsapi.importer.NightscoutImporter;
import de.opendiabetes.vault.container.VaultEntry;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Constructs a GET request to the Nightscout server.
 */
public class GetBuilder {
    private final static NightscoutImporter IMPORTER = new NightscoutImporter();

    private NSApi api;
    private HttpRequest request;

    GetBuilder(NSApi api, HttpRequest request) {
        this.api = api;
        this.request = request;
    }

    /**
     * Creates a find parameter for this request.
     *
     * @param field the field to find
     * @return Operator to pass actual operation
     */
    public GetBuilder.Operator find(String field) {
        StringBuilder findQuery = new StringBuilder();
        findQuery.append("find[").append(field).append("]");
        return new Operator(this, findQuery);
    }

    /**
     * Sets the count parameter for this request.
     *
     * @param count Number of entries to return
     * @return this builder
     */
    public GetBuilder count(int count) {
        this.request = this.request.queryString("count", count);
        return this;
    }

    /**
     * Sends the request to the server and gets the raw response as some kind of JSON Element.
     *
     * @return the result of the request represented as some kind of JSON Element
     * @throws NightscoutIOException     if an I/O error occurs during the request, or the response is not valid
     * @throws NightscoutServerException if the Nightscout server returns a bad response status
     */
    public JsonElement getRaw() throws NightscoutIOException, NightscoutServerException {
        InputStream inputStream = api.send(request);
        InputStreamReader reader = new InputStreamReader(inputStream);
        JsonElement element;
        try {
            JsonParser parser = new JsonParser();
            element = parser.parse(reader);
        } catch (JsonParseException e) {
            throw new NightscoutDataException("Exception while parsing response from server", e);
        }
        try {
            reader.close();
        } catch (IOException e) {
            throw new NightscoutIOException("Exception while closing stream", e);
        }
        return element;
    }

    /**
     * Sends the request to the server and passes the result to {@link NightscoutImporter} to be parsed as a list of VaultEntries.
     *
     * @return a List of VaultEntries corresponding to the result of the request
     * @throws NightscoutIOException     if an I/O error occurs during the request, or the response is not valid
     * @throws NightscoutServerException if the Nightscout server returns a bad response status
     */
    public List<VaultEntry> getVaultEntries() throws NightscoutIOException, NightscoutServerException {
        InputStream stream = api.send(request);
        return IMPORTER.importData(stream);
    }

    /**
     * Represents an operator for the {@link #find(String)} parameter.
     */
    public class Operator {
        private GetBuilder builder;
        private StringBuilder findPath;

        private Operator(GetBuilder builder, StringBuilder findPath) {
            this.builder = builder;
            this.findPath = findPath;
        }

        /**
         * equals operator
         *
         * @param value value
         * @return the underlying builder
         */
        public GetBuilder eq(Object value) {
            GetBuilder.this.request = GetBuilder.this.request.queryString(findPath.toString(), value);
            return builder;
        }

        /**
         * greater then operator
         *
         * @param value value
         * @return the underlying builder
         */
        public GetBuilder gt(Object value) {
            return op("gt", value);
        }

        /**
         * greater then or equal operator
         *
         * @param value value
         * @return the underlying builder
         */
        public GetBuilder gte(Object value) {
            return op("gte", value);
        }

        /**
         * less then operator
         *
         * @param value value
         * @return the underlying builder
         */
        public GetBuilder lt(Object value) {
            return op("lt", value);
        }

        /**
         * less then or equal operator
         *
         * @param value value
         * @return the underlying builder
         */
        public GetBuilder lte(Object value) {
            return op("lte", value);
        }

        private GetBuilder op(String op, Object value) {
            findPath.append("[$").append(op).append("]");
            GetBuilder.this.request = GetBuilder.this.request.queryString(findPath.toString(), value);
            return builder;
        }
    }
}
