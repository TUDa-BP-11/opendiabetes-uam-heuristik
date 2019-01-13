package de.opendiabetes.nsapi;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequest;
import de.opendiabetes.parser.Profile;
import de.opendiabetes.parser.ProfileParser;
import de.opendiabetes.parser.Status;
import de.opendiabetes.parser.StatusParser;
import de.opendiabetes.vault.engine.container.VaultEntry;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;


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
     * @throws UnirestException if an exception occurs during the request
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
     * Fetches all entries from Nightscout that are in between the given latest and oldest time (inclusive).
     * Entries are fetched in batches with the given batch size until Nightscout returns no more results.
     *
     * @param latest    latest point in time, has to be formatable with {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME}
     * @param oldest    oldest point in time, has to be formatable with {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME}
     * @param batchSize amount of entries fetched at once
     * @return list of fetched entries
     * @throws UnirestException            if an exception occurs during the request
     * @throws java.time.DateTimeException if an error occurs while formatting latest or oldest
     */
    public List<VaultEntry> getEntries(TemporalAccessor latest, TemporalAccessor oldest, int batchSize) throws UnirestException {
        return getVaultEntries(latest, oldest, batchSize, "entries", "dateString");
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
     * @throws UnirestException if an exception occurs during the request
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
     * Fetches all treatments from Nightscout that are in between the given latest and oldest time (inclusive).
     * Treatments are fetched in batches with the given batch size until Nightscout returns no more results.
     *
     * @param latest    latest point in time, has to be formatable with {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME}
     * @param oldest    oldest point in time, has to be formatable with {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME}
     * @param batchSize amount of entries fetched at once
     * @return list of fetched entries
     * @throws UnirestException            if an exception occurs during the request
     * @throws java.time.DateTimeException if an error occurs while formatting latest or oldest
     */
    public List<VaultEntry> getTreatments(TemporalAccessor latest, TemporalAccessor oldest, int batchSize) throws UnirestException {
        return getVaultEntries(latest, oldest, batchSize, "treatments", "created_at");
    }

    /**
     * Sends a POST request with the given treatments as its payload. Inserts all entries into the database
     *
     * @param treatments treatments as JSON String.
     * @return whatever the NightScout API returns, as JSON String
     * @throws UnirestException if an exception occurs during the request
     */
    public JsonNode postTreatments(String treatments) throws UnirestException {
        return Unirest.post(host + "treatments")
                .body(treatments)
                .asJson().getBody();
    }

    /**
     * Fetches and parses the profile from Nightscout
     *
     * @return the fetched Profile
     * @throws UnirestException if an exception occurs during the request
     */
    public Profile getProfile() throws UnirestException {
        String profile = get("profile").asString().getBody();
        ProfileParser parser = new ProfileParser();
        return parser.parse(profile);
    }

    private List<VaultEntry> getVaultEntries(TemporalAccessor latest, TemporalAccessor oldest, int batchSize, String path, String dateField) throws UnirestException {
        List<VaultEntry> entries = new ArrayList<>();
        List<VaultEntry> fetched = null;
        GetBuilder getBuilder;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        String latestString = formatter.format(latest);
        String oldestString = formatter.format(oldest);
        do {
            getBuilder = new GetBuilder(get(path)).count(batchSize).find(dateField).gte(oldestString);
            if (fetched == null)    // first fetch: lte, following fetches: lt
                getBuilder.find(dateField).lte(latestString);
            else getBuilder.find(dateField).lt(latestString);
            fetched = getBuilder.getVaultEntries();
            if (!fetched.isEmpty()) {
                entries.addAll(fetched);
                latestString = fetched.get(fetched.size() - 1).getTimestamp().toInstant().toString();
            }
        } while (!fetched.isEmpty());
        return entries;
    }

    /**
     * Closes the connection to the NightScout API
     *
     * @throws IOException if an exception occurs while closing the connection
     */
    public void close() throws IOException {
        Unirest.shutdown();
    }

    public static void main(String[] args) {
        System.out.println("System.currentTimeMillis()  " + System.currentTimeMillis());
        System.out.println("Instant.now()               " + Instant.now().toString());
        System.out.println("LocalDateTime.now()         " + LocalDateTime.now().toString());
        System.out.println("ZonedDateTime.now()         " + ZonedDateTime.now().toString());
    }
}
