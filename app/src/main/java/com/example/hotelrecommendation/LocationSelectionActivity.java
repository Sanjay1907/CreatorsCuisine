package com.example.hotelrecommendation;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;

public class LocationSelectionActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Button btnConfirmLocation;
    private double selectedLatitude;
    private double selectedLongitude;
    private boolean isLocationConfirmed = false;
    private SupportMapFragment mapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_selection);

        // Initialize Places API with your API key
        Places.initialize(getApplicationContext(), "AIzaSyDHoXOg6fB7_Aj9u9hCCkM76W0CzN5pZHE");

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapView);
        mapFragment.getMapAsync(this);

        btnConfirmLocation = findViewById(R.id.btnConfirmLocation);
        btnConfirmLocation.setVisibility(View.GONE); // Hide the "Confirm Location" button initially

        btnConfirmLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if the location is confirmed
                if (isLocationConfirmed) {
                    // Create an intent to send the selected coordinates to the recommendation activity
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("latitude", selectedLatitude);
                    resultIntent.putExtra("longitude", selectedLongitude);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                }
            }
        });

        // Set up Autocomplete Support Fragment
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        // Specify the types of place data to return (remove setTypeFilter)
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));

        // Set a PlaceSelectionListener to handle user selection
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                // Handle the selected place
                LatLng selectedLatLng = place.getLatLng();

                // Clear previous markers
                mMap.clear();

                // Add a marker at the selected location
                mMap.addMarker(new MarkerOptions().position(selectedLatLng).title(place.getName()));

                // Set the selected location coordinates
                selectedLatitude = selectedLatLng.latitude;
                selectedLongitude = selectedLatLng.longitude;

                // Update the flag and show the "Confirm Location" button
                isLocationConfirmed = true;

                // Fetch the place details including the display name using the TextSearch API
                fetchPlaceDetails(place.getId());

                // Set the map's camera position to the selected location
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, 15));
            }

            @Override
            public void onError(@NonNull Status status) {
                // Handle any errors
                Toast.makeText(LocationSelectionActivity.this, "Error: " + status.getStatusMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchPlaceDetails(String placeId) {
        OkHttpClient client = new OkHttpClient();
        String apiKey = "AIzaSyDHoXOg6fB7_Aj9u9hCCkM76W0CzN5pZHE"; // Replace with your API key
        String url = "https://maps.googleapis.com/maps/api/place/details/json" +
                "?place_id=" + placeId +
                "&fields=name" +
                "&key=" + apiKey;

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(responseBody);
                        if (jsonObject.has("result")) {
                            JSONObject result = jsonObject.getJSONObject("result");
                            if (result.has("name")) {
                                final String displayName = result.getString("name");
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        // Set the display name in the recommendation activity
                                        Intent resultIntent = new Intent();
                                        resultIntent.putExtra("latitude", selectedLatitude);
                                        resultIntent.putExtra("longitude", selectedLongitude);
                                        resultIntent.putExtra("displayName", displayName);
                                        resultIntent.putExtra("placeId", placeId);
                                        setResult(RESULT_OK, resultIntent);
                                        finish();
                                    }
                                });
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Enable the My Location layer if it's not already enabled
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

        // Customize the position of the My Location button
        View locationButton = ((View) mapFragment.getView().findViewById(Integer.parseInt("1")).
                getParent()).findViewById(Integer.parseInt("2"));
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)
                locationButton.getLayoutParams();
        // Position the button on the bottom center
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        layoutParams.setMargins(0, 0, 30, 30); // Adjust margins as needed

        // Set a default zoom level for the user's current location
        mMap.moveCamera(CameraUpdateFactory.zoomTo(15));

        // Use FusedLocationProviderClient to get the user's current location
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            // Get the current location coordinates
                            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());

                            // Set the map's camera position to the current location
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
                        }
                    }
                });

        // Set a map click listener to allow the user to select a location by clicking on the map
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                // Clear previous markers
                mMap.clear();

                // Add a marker at the clicked location
                mMap.addMarker(new MarkerOptions().position(latLng));

                // Set the selected location coordinates
                selectedLatitude = latLng.latitude;
                selectedLongitude = latLng.longitude;

                // Update the flag and show the "Confirm Location" button
                isLocationConfirmed = true;
                btnConfirmLocation.setVisibility(View.VISIBLE);
            }
        });
    }
}
