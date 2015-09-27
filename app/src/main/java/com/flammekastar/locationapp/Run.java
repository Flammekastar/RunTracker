package com.flammekastar.locationapp;

/**
 * Created by Flammekastaren on 27/09/2015.
 */
public class Run {

    private int id;
    private int time;
    private int distance;

    public Run() {}

    public Run(int dist, int t) {
        super();
        time = t;
        distance = dist;
    }

    public void setTime(int t) {
        time = t;
    }

    public void setDistance(int dist) {
        distance = dist;
    }

    public int getTime() {
        return time;
    }

    public int getDistance() {
        return distance;
    }
}
