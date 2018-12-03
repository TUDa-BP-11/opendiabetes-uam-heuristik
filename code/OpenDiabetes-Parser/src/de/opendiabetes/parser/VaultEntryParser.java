package de.opendiabetes.parser;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;

import de.opendiabetes.vault.engine.container.VaultEntry;
import de.opendiabetes.vault.engine.container.VaultEntryType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class VaultEntryParser {

    public VaultEntryParser(){

    }

    public List<VaultEntry> parse(String vaultEntries){

        ArrayList<VaultEntry> result = new ArrayList<>();
        JsonParser parser = new JsonParser();
        JsonElement jsonArray = parser.parse(vaultEntries);
        for (JsonElement e : jsonArray.getAsJsonArray()) {
            JsonObject o = e.getAsJsonObject();
            int timezone;//TODO Timezone
            Date date;
            JsonElement field = o.get("type");
            if(field!=null&&field.getAsString().equals("sgv")){
                VaultEntryType entryType = VaultEntryType.GLUCOSE_CGM;
                //TODO Timezone + Date
                date = new Date();
                result.add(new VaultEntry(entryType,date,o.get("sgv").getAsDouble()));
                
            }
            field = o.get("insulin");
            if(field!=null&& !field.isJsonNull()){
                VaultEntryType entryType = VaultEntryType.BOLUS_NORMAL;
                //TODO Date
                date = new Date();
                result.add(new VaultEntry(entryType,date,field.getAsDouble()));
            }
            field = o.get("carbs");
            if(field!=null&& !field.isJsonNull()){
                VaultEntryType entryType = VaultEntryType.MEAL_MANUAL;
                //TODO Date
                date = new Date();
                result.add(new VaultEntry(entryType,date,field.getAsDouble()));
            }
            field = o.get("eventType");
            if(field!=null&&field.getAsString().equals("Temp Basal")){
                VaultEntryType entryType = VaultEntryType.BASAL_MANUAL;
                //TODO Date
                date = new Date();
                result.add(new VaultEntry(entryType,date,o.get("absolute").getAsDouble(),o.get("duration").getAsDouble()));
            }

        }
        return result;



    }

 }
