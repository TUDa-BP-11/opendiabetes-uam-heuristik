package de.opendiabetes.main.algo;

import java.util.Date;

public class TempBasal {
    private double value;
    private long duration;
    private Date date;

    public TempBasal(double value, long duration, Date date) {
        this.value = value;
        this.duration = duration;
        this.date = date;
    }

    public double getValue() {
        return value;
    }

    public long getDuration() {
        return duration;
    }

    public Date getDate() {
        return date;
    }

    @Override
    public String toString() {
        return "TempBasal{" +
                "value=" + value +
                ", duration=" + duration +
                ", date=" + date.toString() +
                '}';
    }

}
