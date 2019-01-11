package de.opendiabetes.nsapi;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequest;
import de.opendiabetes.parser.Status;
import de.opendiabetes.parser.StatusParser;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;


public class NSApi {
    private String host;

    /**
     * Constructs a new NighScout API instance. Connects to the specified host and port, using the given token for all queries
     *
     * @param host   Host to connect to
     * @param secret Your API-Secret
     */
    public NSApi(String host, String secret) {
        this.host = host + "/api/v1/";
        Unirest.setDefaultHeader("accept", "application/json");
        Unirest.setDefaultHeader("content-type", "application/json");
        Unirest.setDefaultHeader("API-SECRET", DigestUtils.sha1Hex(secret));
    }

    private HttpRequest get(String path) {
        return Unirest.get(host + path);
    }

    /**
     * Sends a GET request for the status
     *
     * @return the status as a JSON formatted String
     */
    public Status getStatus() throws UnirestException {
        HttpResponse<String> response = get("status").asString();
        StatusParser parser = new StatusParser();
        return parser.parse(response.getBody());
    }

    /**
     * Creates a {@link GetBuilder} for entries
     *
     * @return a {@link GetBuilder} with "entries" as its path
     */
    public GetBuilder getEntries() {
        return new GetBuilder(get("entries"));
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
        return new GetBuilder(get("slice/" + storage + "/" + field + "/" + type + "/" + prefix + "/" + regex));
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
        return new GetBuilder(get("echo/" + storage + "/" + spec));
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
        return new GetBuilder(get("times/echo/" + prefix + "/" + regex));
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
        return new GetBuilder(get("times/" + prefix + "/" + regex));
    }

    /**
     * Sends a POST request with the given entries as its payload. Inserts all entries into the database
     *
     * @param entries entries as JSON String.
     * @return whatever the NightScout API returns, as JSON String
     */
    public JsonNode postEntries(String entries) throws UnirestException {
        return Unirest.post(host + "entries")
                .body(entries)
                .asJson().getBody();
    }

    /**
     * The Treatments endpoint returns information about the Nightscout treatments.
     *
     * @return a {@link GetBuilder} with treatments as its path
     */
    public GetBuilder getTreatments() {
        return new GetBuilder(get("treatments"));
    }

    /**
     * Sends a POST request with the given treatments as its payload. Inserts all entries into the database
     *
     * @param treatments treatments as JSON String.
     * @return whatever the NightScout API returns, as JSON String
     */
    public JsonNode postTreatments(String treatments) throws UnirestException {
        return Unirest.post(host + "treatments")
                .body(treatments)
                .asJson().getBody();
    }


    /**
     * Closes the connection to the NightScout API
     */
    public void close() {
        try {
            Unirest.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
