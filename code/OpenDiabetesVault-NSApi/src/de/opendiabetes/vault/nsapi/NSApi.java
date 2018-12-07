package de.opendiabetes.vault.nsapi;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

public class NSApi {
    private Client client;
    private String target;
    private String token;

    /**
     * Constructs a new NighScout API instance. Connects to the specified host and port, using the given token for all queries
     *
     * @param host  Host to connect to
     * @param port  Port to connect to
     * @param token Token to use for all queries
     */
    public NSApi(String host, String port, String token) {
        this.client = ClientBuilder.newClient();
        this.target = "https://" + host + ":" + port + "/api/v1";
        this.token = token;
    }

    /**
     * Sends a GET request for the status
     *
     * @return the status as a JSON formatted String
     */
    public String getStatus() {
        return client.target(target).path("status")
                .queryParam("token", token)
                .request(MediaType.APPLICATION_JSON)
                .get(String.class);
    }

    /**
     * Creates a {@link GetBuilder} for entries
     *
     * @return a {@link GetBuilder} with "entries" as its path
     */
    public GetBuilder getEntries() {
        return new GetBuilder(client, target, "entries").token(token);
    }


    /**
     * Creates a {@link GetBuilder} for slices
     *
     * @param storage Prefix to use in constructing a prefix-based regex, e.g. entries.
     * @param field   Name of the field to use Regex against in query object, e.g. dateString.
     * @param type    The type field to search against, e.g. sgv.
     * @param prefix  Prefix to use in constructing a prefix-based regex.
     * @param regex   Tail part of regexp to use in expanding/construccting a query object.
     *                Regexp also has bash-style brace and glob expansion applied to it,
     *                creating ways to search for modal times of day, perhaps using something like this syntax:
     *                T{15..17}:.*, this would search for all records from 3pm to 5pm.
     * @return a {@link GetBuilder} with the given parameters as its path
     */
    public GetBuilder getSlice(String storage, String field, String type, String prefix, String regex) {
        return new GetBuilder(client, target, "slice/" + storage + "/" + field + "/" + type + "/" + prefix + "/" + regex).token(token);
    }

    /**
     * Information about the mongo query object created by the query.
     *
     * @param storage entries, or treatments to select the storage layer.
     * @param spec    entry id, such as 55cf81bc436037528ec75fa5 or a type filter such as sgv, mbg, etc.
     *                This parameter is optional.
     * @return a {@link GetBuilder} with the given parameters as its path
     */
    public GetBuilder getEcho(String storage, String spec) {
        return new GetBuilder(client, target, "echo/" + storage + "/" + spec).token(token);
    }

    /**
     * Echo debug information about the query object constructed.
     *
     * @param prefix Prefix to use in constructing a prefix-based regex.
     * @param regex  Tail part of regexp to use in expanding/construccting a query object.
     *               Regexp also has bash-style brace and glob expansion applied to it,
     *               creating ways to search for modal times of day, perhaps using something like this syntax:
     *               T{15..17}:.*, this would search for all records from 3pm to 5pm.
     * @return a {@link GetBuilder} with the given parameters as its path
     */
    public GetBuilder getTimesEcho(String prefix, String regex) {
        return new GetBuilder(client, target, "times/echo/" + prefix + "/" + regex).token(token);
    }

    /**
     * The Entries endpoint returns information about the Nightscout entries.
     * //TODO fix this description
     *
     * @param prefix Prefix to use in constructing a prefix-based regex.
     * @param regex  Tail part of regexp to use in expanding/construccting a query object.
     *               Regexp also has bash-style brace and glob expansion applied to it,
     *               creating ways to search for modal times of day, perhaps using something like this syntax:
     *               T{15..17}:.*, this would search for all records from 3pm to 5pm.
     * @return a {@link GetBuilder} with the given parameters as its path
     */
    public GetBuilder getTimes(String prefix, String regex) {
        return new GetBuilder(client, target, "times/" + prefix + "/" + regex).token(token);
    }

    /**
     * Sends a POST request with the given entries as its payload. Inserts all entries into the database
     *
     * @param entries entries as JSON String.
     * @return whatever the NightScout API returns, as JSON String
     */
    public String postEntries(String entries) {
        return client.target(target).path("entries")
                .queryParam("token", token)
                .request(MediaType.APPLICATION_JSON)
                .header("content-type", MediaType.APPLICATION_JSON)
                .post(Entity.json(entries), String.class);
    }

    /**
     * The Treatments endpoint returns information about the Nightscout treatments.
     *
     * @return a {@link GetBuilder} with treatments as its path
     */
    public GetBuilder getTreatments() {
        return new GetBuilder(client, target, "treatments").token(token);
    }

    /**
     * Sends a POST request with the given treatments as its payload. Inserts all entries into the database
     *
     * @param treatments treatments as JSON String.
     * @return whatever the NightScout API returns, as JSON String
     */
    public String postTreatments(String treatments) {
        return client.target(target).path("treatments")
                .queryParam("token", token)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(treatments), String.class);
    }

    /**
     * Closes the connection to the NightScout API
     */
    public void close() {
        this.client.close();
    }
}
