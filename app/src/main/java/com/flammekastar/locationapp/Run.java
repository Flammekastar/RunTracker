package com.flammekastar.locationapp;

/**
 * Run just to make an object out of Runs. This is not hugely important for the app in its current form,
 * but could save me time down the line if I ever expand.
 *
 * @author  Alexander Maaby
 * @version 1.0
 * @since   22-09-2015
 */

public class Run {

    private int time;
    private int distance;
    private String date;

    public Run(int dist, int t, String d) {
        super();
        time = t;
        distance = dist;
        date = d;
    }

    public int getTime() {
        return time;
    }

    public int getDistance() {
        return distance;
    }

    public String getDate() { return date; }
}
