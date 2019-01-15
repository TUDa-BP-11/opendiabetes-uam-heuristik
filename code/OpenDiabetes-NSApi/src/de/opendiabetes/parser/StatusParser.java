package de.opendiabetes.parser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class StatusParser implements Parser<Status> {
    @Override
    public Status parse(String input) {
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(input, Status.class);
    }
}