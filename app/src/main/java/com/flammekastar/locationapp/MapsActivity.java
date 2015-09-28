package com.flammekastar.locationapp;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MapsActivity extends AppCompatActivity implements View.OnClickListener, LocationListener {

    public static final String PREFS_NAME = "MyPrefsFile";
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private LocationManager locationManager;
    private ArrayList<LatLng> coordlist = new ArrayList<>();
    private int totaldistancemeters;
    Handler handler;
    private Timer t;
    private int TimeCounter = 0;
    private SQLiteHelper db;

    public MapsActivity() {
        handler = new Handler();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();
        Button locationButton = (Button)findViewById(R.id.button);
        Button stopButton = (Button)findViewById(R.id.button2);
        locationButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String lastLoc = settings.getString("lastLoc", "unknown");
        Float lastWeather = settings.getFloat("lastTemp",0);
        TextView locText = (TextView)findViewById(R.id.textLocation);
        locText.setText("Last time you were located in " + lastLoc);
        TextView weatherText = (TextView)findViewById(R.id.weatherText);
        weatherText.setText("The temperatur was " + lastWeather + " degrees Celcius.");

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    public void onClick(View view) {
        if (view.getId() == R.id.button) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    30000,   // 30 sec
                    0, //minste distanse som blir registrert
                    this);
            startTimer();
        }
        if (view.getId() == R.id.button2) {
            db = new SQLiteHelper(this);
            Run test = new Run(totaldistancemeters,TimeCounter);
            db.addRun(test);
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
            // action with ID action_refresh was selected
            case R.id.action_list:
                Toast.makeText(this, "Refresh selected", Toast.LENGTH_SHORT)
                        .show();
                Intent listIntent = new Intent(this, RunListActivity.class);
                this.startActivity(listIntent);
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
            locText.setText("You are currently located in " + test.get(0).getAddressLine(0));
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("lastLoc", test.get(0).getAddressLine(0));
            editor.apply();
            updateWeatherData(location.getLatitude(),location.getLongitude()); //EXPERIMENTAL
        } catch (IOException ioException) {
            Log.e("shits","wrong");
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
        Toast.makeText(this, "Enabled new provider " + provider,
                Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(this, "Disabled provider " + provider,
                Toast.LENGTH_SHORT).show();
    }

    public void updateRunCoords(LatLng curloc) {
        coordlist.add(curloc);
        mapMarkers();
    }

    public void mapMarkers() {
        mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        for (int a = 0; a < coordlist.size(); a++) {
            mMap.addPolyline(new PolylineOptions().geodesic(true).addAll(coordlist).color(Color.rgb(0, 164, 143)));
        }
    }

    public void setMapFragment(LatLng cLoc){
        mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cLoc, 17));

        //mMap.addMarker(new MarkerOptions()
          //      .title("Location")
            //    .snippet("You are here!")
              //  .position(cLoc));
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
                            weatherTxt.setText("It is " + String.format("%.2f", main.getDouble("temp"))+ " degrees Celcius.");
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
    private double sumUpRun() {
        double lastlat = 0;
        double lastlng = 0;
        double lat;
        double lng;
        double distance = 0;
        boolean first = true;
        boolean firsttaken = false;
        for (int a = 0; a < coordlist.size(); a++) {
            if (first) {
                lastlat = coordlist.get(a).latitude;
                lastlng = coordlist.get(a).longitude;
                firsttaken = true;
            }
            else {
                lat = coordlist.get(a).latitude;
                lng  = coordlist.get(a).longitude;
                distance += distanceBetweenTwoLocationsInKm(lastlat,lastlng,lat,lng);
                lastlat = lat;
                lastlng = lng;
            }
            if(firsttaken) { first = false; }
        }
        double tempx = distance * 1000;
        totaldistancemeters = (int) tempx;
        return distance;
    }
    public void startTimer() {
        t = new Timer();
        t.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
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
    public static Double distanceBetweenTwoLocationsInKm(Double latitudeOne, Double longitudeOne, Double latitudeTwo, Double longitudeTwo) {
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
