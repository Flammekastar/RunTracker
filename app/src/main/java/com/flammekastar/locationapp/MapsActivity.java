package com.flammekastar.locationapp;

import android.content.Context;
import android.content.SharedPreferences;

import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
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

public class MapsActivity extends FragmentActivity implements View.OnClickListener, LocationListener {

    public static final String PREFS_NAME = "MyPrefsFile";
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private LocationManager locationManager;
    private ArrayList<LatLng> coordlist = new ArrayList<>();
    Handler handler;

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


        }
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
            locText.setText("You are currently located in " + test.get(0).getAddressLine(0).toString());
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("lastLoc", test.get(0).getAddressLine(0).toString());
            editor.commit();
            updateWeatherData(location.getLatitude(),location.getLongitude()); //EXPERIMENTAL
        } catch (IOException ioException) {
            Log.e("shits","wrong");
        }
        //Update the list of coords that has been recorded. Using the LatLng created earlier.
        updateRunCoords(curloc);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub

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

    public void updateRunCoords(LatLng curloc) {;
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
                                editor.commit();

                            } catch(Exception e){
                                Log.e("SimpleWeather", "One or more fields not found in the JSON data");
                            }
                        }
                    });
                }
            }
        }.start();
    }
}
