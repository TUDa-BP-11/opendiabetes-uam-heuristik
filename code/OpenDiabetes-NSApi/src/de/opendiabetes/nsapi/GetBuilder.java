package de.opendiabetes.nsapi;

import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequest;
import de.opendiabetes.parser.VaultEntryParser;
import de.opendiabetes.vault.engine.container.VaultEntry;

import java.util.List;

public class GetBuilder {
    private HttpRequest request;

    public GetBuilder(HttpRequest request) {
        this.request = request;
    }

    /**
     * Creates a find parameter for this request
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
     * Sets the count parameter for this request
     *
     * @param count Number of entries to return
     * @return this
     */
    public GetBuilder count(int count) {
        this.request = this.request.queryString("count", count);
        return this;
    }

    /**
     * Completes the request
     *
     * @return the result of the request represented as some kind of JSON Object
     * @throws UnirestException if an exception occurs during the request
     */
    public JsonNode get() throws UnirestException {
        return request.asJson().getBody();
    }

    /**
     * Completes the request and passes the result to {@link VaultEntryParser} to be parsed as a list of VaultEntries
     *
     * @return a List of VaultEntries corresponding to the result of the request
     * @throws UnirestException if an exception occurs during the request
     */
    public List<VaultEntry> getVaultEntries() throws UnirestException {
        VaultEntryParser parser = new VaultEntryParser();
        return parser.parse(get().getArray());
    }

    public class Operator {
        private GetBuilder builder;
        private StringBuilder findPath;

        private Operator(GetBuilder builder, StringBuilder findPath) {
            this.builder = builder;
            this.findPath = findPath;
        }

        public GetBuilder eq(Object value) {
            GetBuilder.this.request = GetBuilder.this.request.queryString(findPath.toString(), value);
            return builder;
        }

        public GetBuilder gt(Object value) {
            return op("gt", value);
        }

        public GetBuilder gte(Object value) {
            return op("gte", value);
        }

        public GetBuilder lt(Object value) {
            return op("lt", value);
        }

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
