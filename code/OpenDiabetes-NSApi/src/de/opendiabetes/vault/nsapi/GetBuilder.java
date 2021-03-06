package de.opendiabetes.vault.nsapi;

import com.google.gson.JsonElement;
import com.mashape.unirest.request.HttpRequest;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.importer.Importer;
import de.opendiabetes.vault.nsapi.exception.NightscoutIOException;
import de.opendiabetes.vault.nsapi.exception.NightscoutServerException;
import de.opendiabetes.vault.nsapi.importer.NightscoutImporter;
import de.opendiabetes.vault.nsapi.importer.UnannouncedMealImporter;

import java.io.InputStream;
import java.util.List;

/**
 * Builder for HTTP GET requests
 */
public class GetBuilder {
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
        return api.sendAndParse(request);
    }

    /**
     * Sends the request to the server and passes the result to {@link NightscoutImporter} to be parsed as a list of VaultEntries.
     *
     * @return a List of VaultEntries corresponding to the result of the request
     * @throws NightscoutIOException     if an I/O error occurs during the request, or the response is not valid
     * @throws NightscoutServerException if the Nightscout server returns a bad response status
     */
    public List<VaultEntry> getVaultEntries() throws NightscoutIOException, NightscoutServerException {
        return getVaultEntries(new NightscoutImporter());
    }

    /**
     * Sends the request to the server and passes the result to {@link UnannouncedMealImporter} to be parsed as a list of VaultEntries.
     *
     * @return a List of VaultEntries corresponding to the result of the request
     * @throws NightscoutIOException     if an I/O error occurs during the request, or the response is not valid
     * @throws NightscoutServerException if the Nightscout server returns a bad response status
     */
    public List<VaultEntry> getUnannouncedMeals() throws NightscoutIOException, NightscoutServerException {
        return getVaultEntries(new UnannouncedMealImporter());
    }

    /**
     * Sends the request to the server and passes the result to the given importer to be parsed as a list of VaultEntries.
     *
     * @param importer Importer used to format the entries.
     * @return a List of VaultEntries corresponding to the result of the request
     * @throws NightscoutIOException     if an I/O error occurs during the request, or the response is not valid
     * @throws NightscoutServerException if the Nightscout server returns a bad response status
     */
    public List<VaultEntry> getVaultEntries(Importer importer) throws NightscoutIOException, NightscoutServerException {
        InputStream stream = api.send(request);
        return importer.importData(stream);
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
