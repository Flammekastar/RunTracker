package com.flammekastar.locationapp;

/**
 * Created by Flammekastaren on 27/09/2015.
 */
public class Run {

    private int id;
    private int time;
    private int distance;
    private String date;

    public Run(int dist, int t, String d) {
        super();
        time = t;
        distance = dist;
        date = d;
    }

    public void setId(int i) {
        id = i;
    }

    public void setTime(int t) {
        time = t;
    }

    public void setDistance(int dist) {
        distance = dist;
    }

    public void setDate(String d) { date = d; }

    public int getID() { return id; }

    public int getTime() {
        return time;
    }

    public int getDistance() {
        return distance;
    }

    public String getDate() { return date; }
}
