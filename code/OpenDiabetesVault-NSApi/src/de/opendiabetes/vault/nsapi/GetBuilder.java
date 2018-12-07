package de.opendiabetes.vault.nsapi;

import de.opendiabetes.parser.VaultEntryParser;
import de.opendiabetes.vault.engine.container.VaultEntry;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.util.List;

public class GetBuilder {
    private WebTarget target;

    public GetBuilder(Client client, String target, String path) {
        this.target = client.target(target).path(path);
    }

    public GetBuilder.Operator find(String field) {
        StringBuilder findQuery = new StringBuilder();
        findQuery.append("find[").append(field).append("]");
        return new Operator(this, findQuery);
    }

    /**
     * Sets the token parameter to use for this request
     *
     * @param token token String
     * @return this
     */
    GetBuilder token(String token) {
        this.target = target.queryParam("token", token);
        return this;
    }

    /**
     * Sets the count parameter for this request
     *
     * @param count Number of entries to return
     * @return this
     */
    public GetBuilder count(int count) {
        this.target = target.queryParam("count", count);
        return this;
    }

    private String get() {
        return target.request(MediaType.APPLICATION_JSON).get(String.class);
    }

    /**
     * Completes the request and parses the results to {@link VaultEntry} with the {@link VaultEntryParser}.
     *
     * @return a list of entries as the result of the generated query.
     */
    public List<VaultEntry> getVaultEnries() {
        String json = get();
        VaultEntryParser parser = new VaultEntryParser();
        return parser.parse(json);
    }

    public class Operator {
        private GetBuilder builder;
        private StringBuilder findPath;

        private Operator(GetBuilder builder, StringBuilder findPath) {
            this.builder = builder;
            this.findPath = findPath;
        }

        public GetBuilder eq(Object value) {
            GetBuilder.this.target = GetBuilder.this.target.queryParam(findPath.toString(), value);
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
            GetBuilder.this.target = GetBuilder.this.target.queryParam(findPath.toString(), value);
            return builder;
        }
    }
}
