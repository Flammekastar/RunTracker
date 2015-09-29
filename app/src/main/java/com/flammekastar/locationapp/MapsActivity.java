package com.flammekastar.locationapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;


import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This applications main activity. Contains the users ability to track runs. And the methods to save
 * these to the database. Also contains the apps primary navigation within its actionbar.
 *
 * @author  Alexander Maaby
 * @version 1.0
 * @since   13-09-2015
 */

public class MapsActivity extends AppCompatActivity implements View.OnClickListener, LocationListener {

    private static final String PREFS_NAME = "MyPrefsFile";
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private LocationManager locationManager;
    private final ArrayList<LatLng> coordlist = new ArrayList<>();
    private int totaldistancemeters;
    private final Handler handler;
    private Timer t;
    private int TimeCounter = 0;
    private String strDate;
    private boolean runStarted;

    public MapsActivity() {
        handler = new Handler();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();
        Button locationButton = (Button)findViewById(R.id.button);
        locationButton.setOnClickListener(this);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String lastLoc = settings.getString("lastLoc", "unknown");
        Float lastWeather = settings.getFloat("lastTemp",0);
        TextView locText = (TextView)findViewById(R.id.textLocation);
        locText.setText(lastLoc);
        TextView weatherText = (TextView)findViewById(R.id.weatherText);
        weatherText.setText(lastWeather + "" + getString(R.string.celsius));
        runStarted = false;
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    public void onClick(View view) {
        if (view.getId() == R.id.button) {
            if (!runStarted) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        10000,   // 30 sec
                        0, //Lowest distance that will be recorded.
                        this);
                Calendar c = Calendar.getInstance();
                @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");  //Suppressing this because I prefer to just use this formating for this purpose.
                strDate = sdf.format(c.getTime());
                startTimer();
                runStarted = true;
                Button start = (Button)findViewById(R.id.button);
                start.setText(R.string.stop_run);
            }
            else {
                t.cancel();
                locationManager.removeUpdates(this);
                SQLiteHelper db;
                db = new SQLiteHelper(this);
                Run test = new Run(totaldistancemeters,TimeCounter,strDate);
                db.addRun(test);
                Button start = (Button)findViewById(R.id.button);
                start.setText(R.string.start_run);
                runStarted = false;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // action with ID action_list was selected, then start RunListActivity
            case R.id.action_list:
                Intent listIntent = new Intent(this, RunListActivity.class);
                this.startActivity(listIntent);
                break;
            case R.id.action_settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                this.startActivity(settingsIntent);
                break;
            default:
                break;
        }

        return true;
    }


    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    private void setUpMap() {
        mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
    }

    public void onLocationChanged(Location location) {
        LatLng curloc = new LatLng(location.getLatitude(), location.getLongitude());
        setMapFragment(curloc);
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> test = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 2);
            TextView locText = (TextView)findViewById(R.id.textLocation);
            locText.setText(test.get(0).getAddressLine(0));
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("lastLoc", test.get(0).getAddressLine(0));
            editor.apply();
            updateWeatherData(location.getLatitude(),location.getLongitude()); //EXPERIMENTAL
        } catch (IOException ioException) {
            Log.e("IOException",":o");
        }
        //Update the list of coords that has been recorded. Using the LatLng created earlier.
        updateRunCoords(curloc);
        sumUpRun();
        TextView test = (TextView)findViewById(R.id.distanceText);
        //test.setText(String.format("%.2f", sumUpRun() ) + "km");
        test.setText(totaldistancemeters + "m");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {


    }

    @Override
    public void onProviderEnabled(String provider) {
        Toast.makeText(this, R.string.enabled_gps + provider,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(this, R.string.disabled_gps + provider,
                Toast.LENGTH_SHORT).show();
    }

    private void updateRunCoords(LatLng curloc) {
        coordlist.add(curloc);
        mapMarkers();
    }

    private void mapMarkers() {
        mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        for (int a = 0; a < coordlist.size(); a++) {
            mMap.addPolyline(new PolylineOptions().geodesic(true).addAll(coordlist).color(Color.rgb(0, 164, 143)));
        }
    }

    private void setMapFragment(LatLng cLoc){
        mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cLoc, 17));
    }

    private void updateWeatherData(final double lat, final double lng){
        new Thread(){
            public void run(){
                final JSONObject json = RemoteFetch.getJSON(MapsActivity.this, lat, lng);
                if(json == null){
                    handler.post(new Runnable(){
                        public void run(){
                            Toast.makeText(MapsActivity.this,
                                    MapsActivity.this.getString(R.string.place_not_found),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    handler.post(new Runnable(){
                        public void run(){
                            //renderWeather(json);
                            try {
                            TextView weatherTxt =(TextView)findViewById(R.id.weatherText);
                                JSONObject main = json.getJSONObject("main");
                            weatherTxt.setText(String.format("%.2f", main.getDouble("temp"))+ getString(R.string.celsius));
                                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                                SharedPreferences.Editor editor = settings.edit();
                                editor.putFloat("lastTemp",(float)main.getDouble("temp"));
                                editor.apply();

                            } catch(Exception e){
                                Log.e("SimpleWeather", "One or more fields not found in the JSON data");
                            }
                        }
                    });
                }
            }
        }.start();
    }

    //Gets the total distance between all the coords the user has recorded.
    private void sumUpRun() {
        double lastlat = 0;
        double lastlng = 0;
        double lat;
        double lng;
        double distance = 0;
        boolean first = true;
        for (int a = 0; a < coordlist.size(); a++) {
            if (first) {
                lastlat = coordlist.get(a).latitude;
                lastlng = coordlist.get(a).longitude;
                first = false;
            }
            else {
                lat = coordlist.get(a).latitude;
                lng  = coordlist.get(a).longitude;
                distance += distanceBetweenTwoLocationsInKm(lastlat,lastlng,lat,lng);
                lastlat = lat;
                lastlng = lng;
            }
        }
        double tempx = distance * 1000;
        totaldistancemeters = (int) tempx;
    }
    private void startTimer() {
        t = new Timer();
        t.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        TextView time = (TextView)findViewById(R.id.timeText);
                        time.setText(String.valueOf(TimeCounter)); // you can set it to a textView to show it to the user to see the time passing while he is writing.
                        TimeCounter++;
                    }
                });

            }
        }, 1000, 1000); // 1000 means start from 1 sec, and the second 1000 is do the loop each 1 sec.
    }

    //Method to grab the distance between two locations based on long/lat and math wizardry.
    private static Double distanceBetweenTwoLocationsInKm(Double latitudeOne, Double longitudeOne, Double latitudeTwo, Double longitudeTwo) {
        if (latitudeOne == null || latitudeTwo == null || longitudeOne == null || longitudeTwo == null) {
            return null;
        }
        Double earthRadius = 6371.0;
        Double diffBetweenLatitudeRadians = Math.toRadians(latitudeTwo - latitudeOne);
        Double diffBetweenLongitudeRadians = Math.toRadians(longitudeTwo - longitudeOne);
        Double latitudeOneInRadians = Math.toRadians(latitudeOne);
        Double latitudeTwoInRadians = Math.toRadians(latitudeTwo);
        Double a = Math.sin(diffBetweenLatitudeRadians / 2) * Math.sin(diffBetweenLatitudeRadians / 2) + Math.cos(latitudeOneInRadians) * Math.cos(latitudeTwoInRadians) * Math.sin(diffBetweenLongitudeRadians / 2)
                * Math.sin(diffBetweenLongitudeRadians / 2);
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (earthRadius * c);
    }
}
