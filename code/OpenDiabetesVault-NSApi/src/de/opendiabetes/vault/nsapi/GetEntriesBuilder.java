package de.opendiabetes.vault.nsapi;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GetEntriesBuilder {
    private NSApi api;
    private List<String> arguments;
    private StringBuilder path;
    private String spec;

    public GetEntriesBuilder(NSApi api) {
        this.api = api;
        this.arguments = new ArrayList<>();
        this.path = new StringBuilder();
        this.path.append("entries");
    }

    public GetEntriesBuilder.Operator find(String field) {
        StringBuilder findQuery = new StringBuilder();
        findQuery.append("find[").append(field).append("]");
        return new Operator(this, findQuery);
    }

    public GetEntriesBuilder count(int count) {
        arguments.add("count=" + count);
        return this;
    }

    public GetEntriesBuilder spec(String spec) {
        this.spec = spec;
        return this;
    }

    public String get() {
        if (spec != null)
            path.append("/").append(spec);
        if (!arguments.isEmpty())
            path.append("?").append(arguments.stream().collect(Collectors.joining("&")));
        return api.get(path.toString());
    }

    public class Operator {
        private GetEntriesBuilder builder;
        private StringBuilder findPath;

        private Operator(GetEntriesBuilder builder, StringBuilder findPath) {
            this.builder = builder;
            this.findPath = findPath;
        }

        public GetEntriesBuilder eq(Object value) {
            return op("eq", value);
        }

        public GetEntriesBuilder ne(Object value) {
            return op("ne", value);
        }

        public GetEntriesBuilder gt(Object value) {
            return op("gt", value);
        }

        public GetEntriesBuilder gte(Object value) {
            return op("gte", value);
        }

        public GetEntriesBuilder lt(Object value) {
            return op("lt", value);
        }

        public GetEntriesBuilder lte(Object value) {
            return op("lte", value);
        }

        private GetEntriesBuilder op(String op, Object value) {
            findPath.append("[$").append(op).append("]=").append(value);
            builder.arguments.add(findPath.toString());
            return builder;
        }
    }
}
