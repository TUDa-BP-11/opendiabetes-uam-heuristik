package de.opendiabetes.parser;

import com.google.gson.annotations.SerializedName;

public class Entry {
    private String direction;
    @SerializedName("_id")
    private String id;
    @SerializedName(value="sgv", alternate={"mbg", "cal"})
    private int value;
    private String dateString;
    private long date;
    private String type;
    private int trend;

    private String device;


    public Entry(String direction, String id, int value, String dateString, long date, int trend, String type, String device) {
        this.direction = direction;
        this.id = id;
        this.value = value;
        this.dateString = dateString;
        this.date = date;
        this.trend = trend;
        this.type = type;
        this.device = device;
    }

    public void print(){
         System.out.println("Direction: " + direction +", id: "+ id + ", value: " + value + ", dateString: "+dateString+", date: "+date +", trend: "+ trend+", type: "+ type + ", device: "+device);
    }

}