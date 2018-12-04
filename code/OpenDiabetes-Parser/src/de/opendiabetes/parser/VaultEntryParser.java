package de.opendiabetes.parser;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;

import de.opendiabetes.vault.engine.container.VaultEntry;
import de.opendiabetes.vault.engine.container.VaultEntryType;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;



public class VaultEntryParser {

    //GMT +/-
    long timezone;
    private static final long ONE_HOUR = 3600000;

    public VaultEntryParser(){
        timezone = 1;

    }

    public VaultEntryParser(long timezone){
        this.timezone = timezone;

    }

    public long getTimezone() {
        return timezone;
    }

    public List<VaultEntry> parse(String vaultEntries){

        ArrayList<VaultEntry> result = new ArrayList<>();
        JsonParser parser = new JsonParser();
        JsonElement jsonArray = parser.parse(vaultEntries);
        for (JsonElement element : jsonArray.getAsJsonArray()) {
            JsonObject o = element.getAsJsonObject();

            Date date;
            JsonElement field = o.get("type");
            if(field!=null&&field.getAsString().equals("sgv")){
                VaultEntryType entryType = VaultEntryType.GLUCOSE_CGM;
                date = makeDate(o.get("date").getAsLong(),o.get("dateString").getAsString());
                result.add(new VaultEntry(entryType,date,o.get("sgv").getAsDouble()));
                
            }
            field = o.get("insulin");
            if(field!=null&& !field.isJsonNull()){
                VaultEntryType entryType = VaultEntryType.BOLUS_NORMAL;
                date = makeDateWithTimeZone(o.get("timestamp").getAsString());
                result.add(new VaultEntry(entryType,date,field.getAsDouble()));
            }
            field = o.get("carbs");
            if(field!=null&& !field.isJsonNull()){
                VaultEntryType entryType = VaultEntryType.MEAL_MANUAL;
                date = makeDateWithTimeZone(o.get("timestamp").getAsString());
                result.add(new VaultEntry(entryType,date,field.getAsDouble()));
            }
            field = o.get("eventType");
            if(field!=null&&field.getAsString().equals("Temp Basal")){
                VaultEntryType entryType = VaultEntryType.BASAL_MANUAL;
                date = makeDateWithTimeZone(o.get("timestamp").getAsString());
                result.add(new VaultEntry(entryType,date,o.get("absolute").getAsDouble(),o.get("duration").getAsDouble()));
            }

        }

        return result;

    }

    private void analyseTimezone(long epoch, long time){

        timezone = ((epoch - 3600000 - time)/3600000);

    }

    private Date makeDate(long epoch, String dateString){
        Date date = new Date(epoch);
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        Date tmp = new Date(0);
        try{
            tmp = formatter.parse(dateString);

        }catch (ParseException e){
            System.out.println(e.getMessage());

        }
        timezone = ((date.getTime() - ONE_HOUR - tmp.getTime())/ONE_HOUR);
        return date;
    }

    private Date makeDateWithTimeZone(String dateString){
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        Date tmp = new Date(0);
        try{
            tmp = formatter.parse(dateString);

        }catch (ParseException e){
            System.out.println(e.getMessage());

        }
        Date date = new Date(tmp.getTime()+((1-timezone)*ONE_HOUR));
        return date;
    }


 }
