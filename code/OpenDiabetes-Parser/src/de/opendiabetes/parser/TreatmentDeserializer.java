package de.opendiabetes.parser;

import com.google.gson.*;

import java.lang.reflect.Type;

public class TreatmentDeserializer implements JsonDeserializer <Treatment> {

    @Override
    public Treatment deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        String id = jsonObject.get("_id").getAsString();
        String timestamp = jsonObject.get("timestamp").getAsString();
        String eventType = jsonObject.get("eventType").getAsString();
        String author = jsonObject.get("enteredBy").getAsString();

        if(eventType.equals("Temp Basal")){
            return new TempBasalTreatment(timestamp,id,eventType,author,
                    jsonObject.get("duration").getAsInt(),
                    jsonObject.get("rate").getAsDouble(),
                    jsonObject.get("absolute").getAsDouble(),
                    jsonObject.get("temp").getAsString());

        }else if (eventType.equals("Correction Bolus")){
            return new CorrectionBolusTreatment(timestamp,id,eventType,author,
                    jsonObject.get("type").getAsString(),
                    jsonObject.get("insulin").getAsDouble(),
                    jsonObject.get("programmed").getAsDouble(),
                    jsonObject.get("unabsorbed").getAsInt(),
                    jsonObject.get("duration").getAsInt());

        }else if (eventType.equals("Meal Bolus")){
            return new MealBolusTreatment(timestamp,id,eventType,author,
                    jsonObject.get("carbs").getAsInt(),
                    jsonObject.get("absorptionTime").getAsInt());

        } else throw new JsonParseException("unknown eventType: "+eventType);

    }
}
