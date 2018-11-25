package de.opendiabetes.parser;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;
import java.lang.reflect.Type;


public class JSONParser {

    public JSONParser (){

    }

    public Entry parseEntry(String entryString){
        Gson gson = new Gson();

        return gson.fromJson(entryString,Entry.class);
    }

    public List<Entry> parseEntryArray(String arrayString){
        Gson gson = new Gson();
        Type type = new TypeToken<List<Entry>>(){}.getType();


        return gson.fromJson(arrayString,type);

    }

    public List<Entry> parseEntryFile(String path) throws FileNotFoundException {
        Gson gson = new Gson();
        Type type = new TypeToken<List<Entry>>(){}.getType();
        JsonReader reader = new JsonReader(new FileReader(path));

        return gson.fromJson(reader,type);
    }
 }
