package de.opendiabetes.parser;

public class TempBasalTreatment extends Treatment {
    private  int duration;
    private double rate,absolute;
    private String temp;

    public TempBasalTreatment(String timestamp, String id, String type, String author, int duration, double rate, double absolute, String temp) {
        super(timestamp, id, type, author);
        this.duration = duration;
        this.rate = rate;
        this.absolute = absolute;
        this.temp = temp;
    }

    @Override
    public String toString() {
        return "ManualBasalTreatment{" +
                "duration=" + duration +
                ", rate=" + rate +
                ", absolute=" + absolute +
                ", temp='" + temp + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", author='" + author + '\'' +
                "}\n";
    }
}
