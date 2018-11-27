package de.opendiabetes.parser;


public class CorrectionBolusTreatment extends Treatment{
    private String subtype;
    private double insulin, programmed;
    private int unabsorbed, duration;

    public CorrectionBolusTreatment(String timestamp, String id, String type, String author, String subtype, double insulin, double programmed, int unabsorbed, int duration) {
        super(timestamp, id, type, author);
        this.subtype = subtype;
        this.insulin = insulin;
        this.programmed = programmed;
        this.unabsorbed = unabsorbed;
        this.duration = duration;
    }

    @Override
    public String toString() {
        return "CorrectionBolusTreatment{" +
                "subtype='" + subtype + '\'' +
                ", insulin=" + insulin +
                ", programmed=" + programmed +
                ", unabsorbed=" + unabsorbed +
                ", duration=" + duration +
                ", timestamp='" + timestamp + '\'' +
                ", id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", author='" + author + '\'' +
                "}\n";
    }
}
