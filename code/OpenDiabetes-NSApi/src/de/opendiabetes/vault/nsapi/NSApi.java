package de.opendiabetes.vault.nsapi;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.HttpRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.importer.Importer;
import de.opendiabetes.vault.nsapi.exception.NightscoutDataException;
import de.opendiabetes.vault.nsapi.exception.NightscoutIOException;
import de.opendiabetes.vault.nsapi.exception.NightscoutServerException;
import de.opendiabetes.vault.nsapi.exporter.NightscoutExporter;
import de.opendiabetes.vault.nsapi.exporter.UnannouncedMealExporter;
import de.opendiabetes.vault.nsapi.importer.NightscoutImporter;
import de.opendiabetes.vault.nsapi.importer.UnannouncedMealImporter;
import de.opendiabetes.vault.nsapi.logging.DefaultFormatter;
import de.opendiabetes.vault.parser.Profile;
import de.opendiabetes.vault.parser.ProfileParser;
import de.opendiabetes.vault.parser.Status;
import de.opendiabetes.vault.parser.StatusParser;
import de.opendiabetes.vault.util.IOStreamUtil;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Implementation of the REST API provided by Nightscout
 */
public class NSApi {
    public final static Logger LOGGER;
    /**
     * {@link DateTimeFormatter Pattern} used to format dates in entries.
     */
    public final static String DATETIME_PATTERN_ENTRY = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
    /**
     * Used to format {@link TemporalAccessor} objects in entries.
     */
    public final static DateTimeFormatter DATETIME_FORMATTER_ENTRY = DateTimeFormatter.ofPattern(DATETIME_PATTERN_ENTRY);

    /**
     * @return a SimpleDateFormat using {@link NSApi#DATETIME_PATTERN_ENTRY} with timezone UTC
     */
    public static SimpleDateFormat createSimpleDateFormatEntry() {
        SimpleDateFormat format = new SimpleDateFormat(DATETIME_PATTERN_ENTRY);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format;
    }

    /**
     * {@link DateTimeFormatter Pattern} used to format dates in treatments.
     */
    public final static String DATETIME_PATTERN_TREATMENT = "yyyy-MM-dd'T'HH:mm:ssX";
    /**
     * Used to format {@link TemporalAccessor} objects in treatments.
     */
    public final static DateTimeFormatter DATETIME_FORMATTER_TREATMENT = DateTimeFormatter.ofPattern(DATETIME_PATTERN_TREATMENT);

    /**
     * @return a SimpleDateFormat using {@link NSApi#DATETIME_PATTERN_TREATMENT} with timezone UTC
     */
    public static SimpleDateFormat createSimpleDateFormatTreatment() {
        SimpleDateFormat format = new SimpleDateFormat(DATETIME_PATTERN_TREATMENT);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format;
    }

    /**
     * The used rest api creates a background event loop and the application won't be able to exit until all threads
     * are closed using this method.
     *
     * @throws NightscoutIOException if an exception occurs while closing the event loop.
     */
    public static void shutdown() throws NightscoutIOException {
        try {
            Unirest.shutdown();
        } catch (IOException e) {
            throw new NightscoutIOException("Could not shut down api", e);
        }
    }

    static {
        LOGGER = Logger.getLogger(NSApi.class.getName());
        Handler handler = new ConsoleHandler();
        handler.setFormatter(new DefaultFormatter());
        LOGGER.addHandler(handler);
        LOGGER.setUseParentHandlers(false);

        Unirest.setDefaultHeader("accept", "application/json");
        Unirest.setDefaultHeader("content-type", "application/json");
    }

    private String host;
    private String secret;

    /**
     * Constructs a new NighScout API instance. Connects to the specified host and port without an api secret.
     *
     * @param host Host to connect to
     */
    public NSApi(String host) {
        this(host, null);
    }

    /**
     * Constructs a new NighScout API instance. Connects to the specified host and port, using the given api secret for all queries
     *
     * @param host   Host to connect to
     * @param secret Your API-Secret
     */
    public NSApi(String host, String secret) {
        this.host = host + "/api/v1/";
        if (secret != null)
            this.secret = DigestUtils.sha1Hex(secret);
    }

    /**
     * The used rest API creates a background event loop and the application won't be able to exit until all threads
     * are closed using this method.
     *
     * @throws IOException if an exception occurs while closing the connection
     * @deprecated use {@link NSApi#shutdown()} instead.
     */
    @Deprecated
    public void close() throws IOException {
        Unirest.shutdown();
    }

    // general

    /**
     * Reads the stream using {@link IOStreamUtil#readInputStream(InputStream)}. Closes the stream after reading it.
     *
     * @param stream stream
     * @return contents of the stream
     * @throws NightscoutIOException if an I/O error occurs during the request
     */
    private String readInputStream(InputStream stream) throws NightscoutIOException {
        String content = IOStreamUtil.readInputStream(stream);
        try {
            stream.close();
        } catch (IOException e) {
            throw new NightscoutIOException("Exception while closing stream", e);
        }
        return content;
    }

    /**
     * Sends the request to the server and gets the body of the response as an {@link InputStream}.
     *
     * @return the body of the response
     * @throws NightscoutIOException     if an I/O error occurs during the request
     * @throws NightscoutServerException if the Nightscout server returns a bad response status
     */
    InputStream send(HttpRequest request) throws NightscoutIOException, NightscoutServerException {
        LOGGER.log(Level.FINE, "Sending %s request to %s", new Object[]{request.getHttpMethod(), request.getUrl()});
        HttpResponse<InputStream> response;
        try {
            response = request.asBinary();
        } catch (Exception e) {
            throw new NightscoutIOException(e.getMessage());
        }
        if (response.getStatus() != 200)
            throw new NightscoutServerException(response);
        return response.getBody();
    }

    JsonElement sendAndParse(HttpRequest request) throws NightscoutIOException, NightscoutServerException {
        InputStream inputStream = send(request);
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

    // GET

    private HttpRequest get(String path) {
        HttpRequest request = Unirest.get(this.host + path);
        if (this.secret != null)
            request.header("API-SECRET", this.secret);
        return request;
    }

    /**
     * Creates a new {@link GetBuilder} for sending GET requests to the Nightscout server.
     *
     * @param path path of the GET request.
     * @return the constructed builder
     */
    public GetBuilder createGet(String path) {
        return new GetBuilder(this, get(path));
    }

    /**
     * Sends a GET request for the status
     *
     * @return the status as a JSON formatted String
     * @throws NightscoutIOException     if an I/O error occurs during the request
     * @throws NightscoutServerException if the Nightscout server returns a bad response status
     */
    public Status getStatus() throws NightscoutIOException, NightscoutServerException {
        InputStream stream = send(get("status"));
        StatusParser parser = new StatusParser();
        return parser.parse(readInputStream(stream));
    }

    /**
     * Fetches and parses the profile from Nightscout
     *
     * @return the fetched Profile
     * @throws NightscoutIOException     if an I/O error occurs during the request
     * @throws NightscoutServerException if the Nightscout server returns a bad response status
     */
    public Profile getProfile() throws NightscoutIOException, NightscoutServerException {
        InputStream stream = send(get("profile"));
        ProfileParser parser = new ProfileParser();
        return parser.parse(readInputStream(stream));
    }

    /**
     * Creates a {@link GetBuilder} for entries
     *
     * @return a {@link GetBuilder} with <code>"entries"</code> as its path
     */
    public GetBuilder getEntries() {
        return new GetBuilder(this, get("entries"));
    }

    /**
     * Fetches all entries from Nightscout that are in between the given latest and oldest time (inclusive).
     * Entries are fetched in batches with the given batch size until Nightscout returns no more results.
     *
     * @param latest    latest point in time, has to be formatable with {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME}
     * @param oldest    oldest point in time, has to be formatable with {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME}
     * @param batchSize amount of entries fetched at once
     * @return list of fetched entries
     * @throws NightscoutIOException       if an I/O error occurs during the request
     * @throws NightscoutServerException   if the Nightscout server returns a bad response status
     * @throws java.time.DateTimeException if an error occurs while formatting latest or oldest
     */
    public List<VaultEntry> getEntries(TemporalAccessor latest, TemporalAccessor oldest, int batchSize) throws NightscoutIOException, NightscoutServerException {
        return getVaultEntries(latest, oldest, batchSize, "entries", "dateString", DATETIME_FORMATTER_ENTRY, new NightscoutImporter());
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
        return new GetBuilder(this, get("slice/" + storage + "/" + field + "/" + type + "/" + prefix + "/" + regex));
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
        return new GetBuilder(this, get("echo/" + storage + "/" + spec));
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
        return new GetBuilder(this, get("times/echo/" + prefix + "/" + regex));
    }

    /**
     * The Entries endpoint returns information about the Nightscout entries.
     *
     * @param prefix Prefix to use in constructing a prefix-based regex.
     * @param regex  Tail part of regexp to use in expanding/construccting a query object.
     *               Regexp also has bash-style brace and glob expansion applied to it,
     *               creating ways to search for modal times of day, perhaps using something like this syntax:
     *               T{15..17}:.*, this would search for all records from 3pm to 5pm.
     * @return a {@link GetBuilder} with the given parameters as its path
     */
    public GetBuilder getTimes(String prefix, String regex) {
        return new GetBuilder(this, get("times/" + prefix + "/" + regex));
    }

    /**
     * The Treatments endpoint returns information about the Nightscout treatments.
     *
     * @return a {@link GetBuilder} with treatments as its path
     */
    public GetBuilder getTreatments() {
        return new GetBuilder(this, get("treatments"));
    }

    /**
     * Fetches all treatments from Nightscout that are in between the given latest and oldest time (inclusive).
     * Treatments are fetched in batches with the given batch size until Nightscout returns no more results.
     *
     * @param latest    latest point in time, has to be formatable with {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME}
     * @param oldest    oldest point in time, has to be formatable with {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME}
     * @param batchSize amount of treatments fetched at once
     * @return list of fetched treatments
     * @throws NightscoutIOException       if an I/O error occurs during the request
     * @throws NightscoutServerException   if the Nightscout server returns a bad response status
     * @throws java.time.DateTimeException if an error occurs while formatting latest or oldest
     */
    public List<VaultEntry> getTreatments(TemporalAccessor latest, TemporalAccessor oldest, int batchSize) throws NightscoutIOException, NightscoutServerException {
        return getVaultEntries(latest, oldest, batchSize, "treatments", "created_at", DATETIME_FORMATTER_TREATMENT, new NightscoutImporter());
    }

    /**
     * The uam endpoint returns information about unannounced meals.
     *
     * @return a {@link GetBuilder} with uam as its path
     */
    public GetBuilder getUnannouncedMeals() {
        return new GetBuilder(this, get("uam"));
    }

    /**
     * Fetches all unannounced meals from Nightscout that are in between the given latest and oldest time (inclusive).
     * UAMs are fetched in batches with the given batch size until Nightscout returns no more results.
     *
     * @param latest    latest point in time, has to be formatable with {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME}
     * @param oldest    oldest point in time, has to be formatable with {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME}
     * @param batchSize amount of uams fetched at once
     * @return list of fetched uams
     * @throws NightscoutIOException       if an I/O error occurs during the request
     * @throws NightscoutServerException   if the Nightscout server returns a bad response status
     * @throws java.time.DateTimeException if an error occurs while formatting latest or oldest
     */
    public List<VaultEntry> getUnannouncedMeals(TemporalAccessor latest, TemporalAccessor oldest, int batchSize) throws NightscoutIOException, NightscoutServerException {
        return getVaultEntries(latest, oldest, batchSize, "uam", "created_at", DATETIME_FORMATTER_TREATMENT, new UnannouncedMealImporter());
    }

    private List<VaultEntry> getVaultEntries(TemporalAccessor latest, TemporalAccessor oldest, int batchSize, String path, String dateField, DateTimeFormatter formatter, Importer importer) throws NightscoutIOException, NightscoutServerException {
        List<VaultEntry> entries = new ArrayList<>();
        List<VaultEntry> fetched = null;
        GetBuilder getBuilder;
        String latestString = formatter.format(latest);
        String oldestString = formatter.format(oldest);
        do {
            getBuilder = new GetBuilder(this, get(path)).count(batchSize).find(dateField).gte(oldestString);
            if (fetched == null)    // first fetch: lte, following fetches: lt
                getBuilder.find(dateField).lte(latestString);
            else getBuilder.find(dateField).lt(latestString);
            fetched = getBuilder.getVaultEntries(importer);
            if (!fetched.isEmpty()) {
                entries.addAll(fetched);
                Date ld = fetched.get(fetched.size() - 1).getTimestamp();
                latestString = formatter.format(ld.toInstant().atZone(ZoneId.of("UTC")));
            }
        } while (!fetched.isEmpty());
        return entries;
    }

    // POST

    private HttpRequestWithBody post(String path) {
        HttpRequestWithBody request = Unirest.post(this.host + path);
        if (this.secret != null)
            request.header("API-SECRET", this.secret);
        return request;
    }

    /**
     * Creates a {@link PostBuilder} for sending POST requests to the Nightscout server.
     *
     * @param path the API path used for this request.
     * @return a new {@link PostBuilder}
     */
    public PostBuilder createPost(String path) {
        return new PostBuilder(this, post(path));
    }

    /**
     * Sends one or more POST requests with the given entries as their payloads. Splits the list into smaller batches to upload individually.
     * Uses {@link NightscoutExporter} to export the data.
     *
     * @param entries   data
     * @param batchSize maximum amount of entries to send in one POST request
     * @throws NightscoutIOException     if an I/O error occurs during the request
     * @throws NightscoutServerException if the Nightscout server returns a bad response status
     * @throws NightscoutDataException   if an exception occurs while exporting the data
     */
    public void postEntries(List<VaultEntry> entries, int batchSize) throws NightscoutIOException, NightscoutServerException, NightscoutDataException {
        for (List<VaultEntry> vaultEntries : NSApiTools.split(entries, batchSize)) {
            postEntries(vaultEntries);
        }
    }

    /**
     * Sends a POST request with the given entries as its payload.
     * Uses {@link NightscoutExporter} to export the data.
     *
     * @param entries data
     * @throws NightscoutIOException     if an I/O error occurs during the request
     * @throws NightscoutServerException if the Nightscout server returns a bad response status
     * @throws NightscoutDataException   if an exception occurs while exporting the data
     */
    public void postEntries(List<VaultEntry> entries) throws NightscoutIOException, NightscoutServerException, NightscoutDataException {
        NightscoutExporter exporter = new NightscoutExporter();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        exporter.exportData(stream, entries);
        String body = new String(stream.toByteArray());
        createPost("entries").setBody(body).send();
    }


    /**
     * Sends one or more POST requests with the given treatments as their payloads. Splits the list into smaller batches to upload individually.
     * Uses {@link NightscoutExporter} to export the data.
     *
     * @param treatments data
     * @param batchSize  maximum amount of treatments to send in one POST request
     * @throws NightscoutIOException     if an I/O error occurs during the request
     * @throws NightscoutServerException if the Nightscout server returns a bad response status
     * @throws NightscoutDataException   if an exception occurs while exporting the data
     */
    public void postTreatments(List<VaultEntry> treatments, int batchSize) throws NightscoutIOException, NightscoutServerException, NightscoutDataException {
        for (List<VaultEntry> vaultEntries : NSApiTools.split(treatments, batchSize)) {
            postTreatments(vaultEntries);
        }
    }

    /**
     * Sends a POST request with the given treatments as its payload.
     * Uses {@link NightscoutExporter} to export the data.
     *
     * @param treatments data
     * @throws NightscoutIOException     if an I/O error occurs during the request
     * @throws NightscoutServerException if the Nightscout server returns a bad response status
     * @throws NightscoutDataException   if an exception occurs while exporting the data
     */
    public void postTreatments(List<VaultEntry> treatments) throws NightscoutIOException, NightscoutServerException, NightscoutDataException {
        NightscoutExporter exporter = new NightscoutExporter();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        exporter.exportData(stream, treatments);
        createPost("treatments").setBody(stream.toByteArray()).send();
    }

    /**
     * Sends one or more POST requests with the given unannounced meals as their payloads. Splits the list into smaller batches to upload individually.
     * Uses {@link UnannouncedMealExporter} to export the data.
     *
     * @param uams      data
     * @param algorithm the algorithm used to calculate the meals
     * @param batchSize maximum amount of treatments to send in one POST request
     * @throws NightscoutIOException     if an I/O error occurs during the request
     * @throws NightscoutServerException if the Nightscout server returns a bad response status
     * @throws NightscoutDataException   if an exception occurs while exporting the data
     */
    public void postUnannouncedMeals(List<VaultEntry> uams, String algorithm, int batchSize) throws NightscoutIOException, NightscoutServerException, NightscoutDataException {
        for (List<VaultEntry> vaultEntries : NSApiTools.split(uams, batchSize)) {
            postUnannouncedMeals(vaultEntries, algorithm);
        }
    }

    /**
     * Sends a POST request with the given unannounced meals as its payload.
     * Uses {@link UnannouncedMealExporter} to export the data.
     *
     * @param uams data
     * @throws NightscoutIOException     if an I/O error occurs during the request
     * @throws NightscoutServerException if the Nightscout server returns a bad response status
     * @throws NightscoutDataException   if an exception occurs while exporting the data
     */
    public void postUnannouncedMeals(List<VaultEntry> uams, String algorithm) throws NightscoutIOException, NightscoutServerException, NightscoutDataException {
        UnannouncedMealExporter exporter = new UnannouncedMealExporter(algorithm);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        exporter.exportData(stream, uams);
        createPost("uam").setBody(stream.toByteArray()).send();
    }

    // DELETE

    private HttpRequest delete(String path, String id) {
        HttpRequest request = Unirest.delete(this.host + path + "/" + id);
        if (this.secret != null)
            request.header("API-SECRET", this.secret);
        return request;
    }

    /**
     * Creates a {@link GetBuilder} for sending DELETE requests to the Nightscout server.
     * <br><br>
     * <b style="color: red">WARNING EXPERIMENTAL!</b>
     * <br><br>
     * Use the {@link GetBuilder#getRaw() getRaw()} method of the returned builder to send the request and get the response as a JSON object.
     *
     * @param path the API path used for this request.
     * @return a new {@link PostBuilder}
     */
    public GetBuilder createDelete(String path, String id) {
        return new GetBuilder(this, delete(path, id));
    }

    /**
     * Deletes the given VaultEntry by first sending a GET request to the Nightscout server to find the id of the entry.
     * Then sends a DELETE request with the id of the entry.
     *
     * @param entry entry to delete
     * @throws NightscoutIOException     if an I/O error occurs during the request
     * @throws NightscoutServerException if the Nightscout server returns a bad response status
     * @deprecated This method is currently broken: Nightscout seems to not actually delete any entries even if a valid ID is provided.
     * Thus this method will always throw an exception.
     */
    @Deprecated
    public void deleteEntry(VaultEntry entry) throws NightscoutIOException, NightscoutServerException {
        deleteVaultEntry(entry, "entries", "dateString", createSimpleDateFormatEntry());
    }

    /**
     * Deletes the given VaultEntry by first sending a GET request to the Nightscout server to find the id of the treatment.
     * Then sends a DELETE request with the id of the treatment.
     *
     * @param treatment treatment to delete
     * @throws NightscoutIOException     if an I/O error occurs during the request
     * @throws NightscoutServerException if the Nightscout server returns a bad response status
     * @deprecated This method is currently broken: Nightscout seems to return inconsistent responses even if it successfully deletes the treatment.
     * Thus this method may throw unexpected exceptions.
     */
    @Deprecated
    public void deleteTreatment(VaultEntry treatment) throws NightscoutIOException, NightscoutServerException {
        deleteVaultEntry(treatment, "treatments", "created_at", createSimpleDateFormatTreatment());
    }

    private void deleteVaultEntry(VaultEntry entry, String path, String dateField, SimpleDateFormat formatter) throws NightscoutIOException, NightscoutServerException {
        JsonObject fetched;
        try {
            fetched = createGet(path)
                    .find(dateField).eq(formatter.format(entry.getTimestamp()))
                    .getRaw()
                    .getAsJsonArray().get(0).getAsJsonObject();
        } catch (IllegalStateException | IndexOutOfBoundsException e) {
            throw new NightscoutIOException("Invalid response from Nightscout server, expected JSON Array containing one JSON Object!", e);
        }
        String id;
        try {
            id = fetched.get("_id").getAsString();
        } catch (NullPointerException | IllegalStateException | ClassCastException e) {
            throw new NightscoutIOException("Could not find _id field for this treatment!");
        }
        JsonElement response = sendAndParse(delete(path, id));
        boolean ok;
        int n;
        try {
            JsonObject o = response.getAsJsonObject();
            ok = o.get("ok").getAsInt() == 1;
            n = o.get("n").getAsInt();
        } catch (NullPointerException | ClassCastException e) {
            throw new NightscoutIOException("Invalid response from Nightscout server.");
        }
        if (!ok || n != 1)
            throw new NightscoutDataException("Nightscout server returned 'ok': " + ok + ", 'n': " + n);
    }

    // util

    /**
     * Checks that the Nightscout server is reachable and that the status is ok and the api is enabled.
     * Logs exceptions and problems with {@link NSApi#LOGGER}.
     *
     * @return true if everything is ok, false otherwise
     */
    public boolean checkStatusOk() {
        Status status;
        try {
            status = getStatus();
        } catch (NightscoutIOException | NightscoutServerException e) {
            LOGGER.log(Level.SEVERE, e, e::getMessage);
            return false;
        }
        if (!status.isStatusOk()) {
            LOGGER.log(Level.SEVERE, "Nightscout server status is not ok:\n%s", printStatus(status));
            return false;
        }
        if (!status.isApiEnabled()) {
            LOGGER.log(Level.SEVERE, "Nightscout api is not enabled:\n%s", printStatus(status));
            return false;
        }
        Duration timeDistance = Duration.between(Instant.parse(status.getServerTime()), Instant.now());
        if (timeDistance.abs().getSeconds() > 1) {
            if (timeDistance.isNegative())
                LOGGER.log(Level.WARNING, "Nightscout server time is %d.%d second in the future!", new Object[]{-timeDistance.getSeconds() - 1, timeDistance.getNano() / 1000000});
            else
                LOGGER.log(Level.WARNING, "Nightscout server time is %d.%d seconds behind!", new Object[]{timeDistance.getSeconds(), timeDistance.getNano() / 1000000});
        }
        return true;
    }

    /**
     * Gets the status from the Nightscout server and checks if the given plugin is enabled.
     *
     * @param plugin name of the plugin
     * @return true if the plugin is enabled
     * @throws NightscoutIOException     if an I/O error occurs during the request
     * @throws NightscoutServerException if the Nightscout server returns a bad response status
     */
    public boolean isPluginEnabled(String plugin) throws NightscoutIOException, NightscoutServerException {
        Status status = getStatus();
        return Stream.of(status.getPlugins()).anyMatch(plugin::equalsIgnoreCase);
    }

    /**
     * Fetches the status and formats it for logging.
     *
     * @return formatted String containing status information
     */
    public String printStatus() {
        try {
            return printStatus(getStatus());
        } catch (NightscoutIOException | NightscoutServerException e) {
            return e.getMessage();
        }
    }

    /**
     * Formats the status for logging.
     *
     * @param status Status of the Nightscout server
     * @return formatted String containing status information
     */
    public String printStatus(Status status) {
        return "name:          " + status.getName() + "\n"
                + "version:       " + status.getVersion() + "\n"
                + "server status: " + status.getStatus() + "\n"
                + "api enabled:   " + status.isApiEnabled() + "\n"
                + "server time:   " + status.getServerTime() + "\n"
                + "plugins:       " + String.join(", ", status.getPlugins()) + "\n";
    }
}
