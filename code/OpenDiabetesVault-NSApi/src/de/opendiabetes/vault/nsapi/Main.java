package de.opendiabetes.vault.nsapi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

public class Main {
    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static JsonParser parser = new JsonParser();

    public static void main(String[] args) {
        NSApi api = new NSApi("https://edgxxar.ns.10be.de:15109");
        printJson(api.getEntries().spec("sgv").find("dateString").gte("2018-11-08T15:05:00").get());
    }

    private static void printJson(String jsonString) {
        System.out.println(gson.toJson(parser.parse(jsonString)));
    }
}
