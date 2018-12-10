package de.opendiabetes.parser;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;

import de.opendiabetes.vault.engine.container.VaultEntry;
import de.opendiabetes.vault.engine.container.VaultEntryType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Stream;

public class VaultEntryParser {

    //GMT +/-
    TimeZone timezone;
    TimeZone localeTimezone;

    private static final long ONE_HOUR = 3600000;

    public VaultEntryParser() {

        String[] ids = TimeZone.getAvailableIDs(0);
        System.out.println(ids[0]);
        this.timezone = TimeZone.getTimeZone(ids[0]);
        localeTimezone = TimeZone.getDefault();
    }

    /**
     * long to timezone
     *
     * @param timezone
     */
    public VaultEntryParser(long timezone) {
        String[] ids = TimeZone.getAvailableIDs((int) timezone);
        this.timezone = TimeZone.getTimeZone(ids[0]);
        localeTimezone = TimeZone.getDefault();
    }

    /**
     * long to timezone
     *
     * @param timezone
     * @param localeTimezone
     */
    public VaultEntryParser(long timezone, long localeTimezone) {
        String[] ids = TimeZone.getAvailableIDs((int) timezone);
        this.timezone = TimeZone.getTimeZone(ids[0]);
        ids = TimeZone.getAvailableIDs((int) localeTimezone);
        this.localeTimezone = TimeZone.getTimeZone(ids[0]);
    }

    public long getTimezone() {
        return timezone.getRawOffset();
    }

    public List<VaultEntry> parse(String vaultEntries) {

        ArrayList<VaultEntry> result = new ArrayList<>();
        JsonParser parser = new JsonParser();
        JsonElement jsonArray = parser.parse(vaultEntries);
        for (JsonElement element : jsonArray.getAsJsonArray()) {
            JsonObject o = element.getAsJsonObject();

            Date date;
            JsonElement field = o.get("type");
            if (field != null && field.getAsString().equals("sgv")) {
                VaultEntryType entryType = VaultEntryType.GLUCOSE_CGM;
                date = makeDate(o.get("date").getAsLong());
                result.add(new VaultEntry(entryType, date, o.get("sgv").getAsDouble()));

            }
            field = o.get("insulin");
            if (field != null && !field.isJsonNull()) {
                VaultEntryType entryType = VaultEntryType.BOLUS_NORMAL;
                date = makeDateInLocalTimeZone(o.get("timestamp").getAsString());
                result.add(new VaultEntry(entryType, date, field.getAsDouble()));
            }
            field = o.get("carbs");
            if (field != null && !field.isJsonNull()) {
                VaultEntryType entryType = VaultEntryType.MEAL_MANUAL;
                date = makeDateInLocalTimeZone(o.get("timestamp").getAsString());
                result.add(new VaultEntry(entryType, date, field.getAsDouble()));
            }
            field = o.get("eventType");
            if (field != null && field.getAsString().equals("Temp Basal")) {
                VaultEntryType entryType = VaultEntryType.BASAL_MANUAL;
                date = makeDateInLocalTimeZone(o.get("timestamp").getAsString());
                result.add(new VaultEntry(entryType, date, o.get("absolute").getAsDouble(), o.get("duration").getAsDouble()));
            }

        }

        return result;

    }

    public List<VaultEntry> parseFile(String path) {
        StringBuilder builder = new StringBuilder();

        try (Stream<String> stream = Files.lines(Paths.get(path), StandardCharsets.UTF_8)) {
            stream.forEach(line -> builder.append(line));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return this.parse(builder.toString());
    }

    /**
     * compare epoch in date object with epoch extracted from string. Java
     * interprets dateString as in locale time zone, correction required
     *
     */
    private void getJSONTimeZone(Date date, String dateString) {

        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        Date tmp = new Date(0);
        try {
            tmp = formatter.parse(dateString);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
        }

        long offset = 0;
        if (localeTimezone.inDaylightTime(tmp)) {
            offset = ONE_HOUR;
        }
        long timezoneOffset = (localeTimezone.getRawOffset() + offset - date.getTime() + tmp.getTime());
        String[] ids = TimeZone.getAvailableIDs((int) timezoneOffset);
        timezone = TimeZone.getTimeZone(ids[0]);

        // DEBUG stuff  
//        System.out.println("Local Timezone: " + localeTimezone.getRawOffset() / ONE_HOUR);
//        System.out.println("DST: " + offset / ONE_HOUR);
//        System.out.println("Timezone: " + timezone.getRawOffset() / ONE_HOUR);
//
//        DateFormat formatter2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS ZZZZ");
//
//        System.out.println(formatter2.format(date) + " " + formatter2.getTimeZone().getDisplayName());
//        System.out.println(formatter2.format(tmp) + " " + formatter2.getTimeZone().getDisplayName());
//        formatter2.setTimeZone(timezone);
//
//        System.out.println(formatter2.format(date) + " " + formatter2.getTimeZone().getDisplayName());
//        System.out.println(formatter2.format(tmp) + " " + formatter2.getTimeZone().getDisplayName());
    }

    /**
     * get a date object from epoch number
     */
    private Date makeDate(long epoch) {
        return new Date(epoch);
    }

    /**
     * get a date object from string with time zone information
     */
    private Date makeDateInLocalTimeZone(String dateString) {
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
        Date tmp = new Date(0);
        try {
            tmp = formatter.parse(dateString);

        } catch (ParseException e) {
            System.out.println(e.getMessage());
        }

        return tmp;
    }

}
