package de.opendiabetes.parser;

public class MealBolusTreatment extends Treatment {
    private int carbs,absorptionTime;

    public MealBolusTreatment(String timestamp, String id, String type, String author, int carbs, int absorptionTime) {
        super(timestamp, id, type, author);
        this.carbs = carbs;
        this.absorptionTime = absorptionTime;
    }

    @Override
    public String toString() {
        return "MealBolusTreatment{" +
                "carbs=" + carbs +
                ", absorptionTime=" + absorptionTime +
                ", timestamp='" + timestamp + '\'' +
                ", id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", author='" + author + '\'' +
                "}\n";
    }
}
