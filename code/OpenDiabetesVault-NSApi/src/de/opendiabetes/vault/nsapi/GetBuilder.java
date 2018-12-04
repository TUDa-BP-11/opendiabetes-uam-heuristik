package de.opendiabetes.vault.nsapi;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

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

    public GetBuilder token(String token) {
        this.target = target.queryParam("token", token);
        return this;
    }

    public GetBuilder count(int count) {
        this.target = target.queryParam("count", count);
        return this;
    }

    public String get() {
        return target.request(MediaType.APPLICATION_JSON).get(String.class);
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
