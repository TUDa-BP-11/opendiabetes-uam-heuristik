package de.opendiabetes.main.algo;

import java.util.Date;

public class TempBasal {
    private double value;
    private double duration;
    private Date date;

    public TempBasal(double unitPerMin, double duration, Date date) {
        this.value = unitPerMin;
        this.duration = duration;
        this.date = date;
    }

    public double getValue() {
        return value;
    }

    public double getDuration() {
        return duration;
    }

    public Date getDate() {
        return date;
    }

    @Override
    public String toString() {
        return "TempBasal{" +
                "value=" + value + " U/min" +
                ", duration=" + duration +
                ", date=" + date.toString() +
                '}';
    }

}
