package de.opendiabetes.parser;

public abstract class Treatment {

    String timestamp;
    String id;
    String type;
    String author;

    public Treatment(String timestamp, String id, String type, String author) {
        this.timestamp = timestamp;
        this.id = id;
        this.type = type;
        this.author = author;
    }
}
