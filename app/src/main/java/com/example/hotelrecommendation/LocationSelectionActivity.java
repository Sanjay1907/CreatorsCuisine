package com.example.hotelrecommendation;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.util.List;


public class LocationSelectionActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Button btnConfirmLocation;
    private Button btnSearch;
    private double selectedLatitude;
    private double selectedLongitude;
    private boolean isLocationConfirmed = false;
    private SupportMapFragment mapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_selection);

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapView);
        mapFragment.getMapAsync(this);

        btnConfirmLocation = findViewById(R.id.btnConfirmLocation);
        btnConfirmLocation.setVisibility(View.GONE); // Hide the "Confirm Location" button initially

        btnSearch = findViewById(R.id.btnSearch);

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchLocation();
            }
        });

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
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Check if a location is already confirmed
        if (!isLocationConfirmed) {
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
        }

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                // Clear previous markers
                mMap.clear();

                // Add a new marker at the selected location
                mMap.addMarker(new MarkerOptions().position(latLng).title("Selected Location"));

                // Set the selected location coordinates
                selectedLatitude = latLng.latitude;
                selectedLongitude = latLng.longitude;

                // Update the flag and show the "Confirm Location" button
                isLocationConfirmed = true;
                btnConfirmLocation.setVisibility(View.VISIBLE);
            }
        });
    }


    public void searchLocation() {
        EditText etLocationQuery = findViewById(R.id.etLocationQuery);
        String locationQuery = etLocationQuery.getText().toString().trim();

        if (!locationQuery.isEmpty()) {
            Geocoder geocoder = new Geocoder(LocationSelectionActivity.this);
            try {
                List<Address> addressList = geocoder.getFromLocationName(locationQuery, 1);
                if (!addressList.isEmpty()) {
                    Address address = addressList.get(0);
                    LatLng searchedLocation = new LatLng(address.getLatitude(), address.getLongitude());

                    // Clear previous markers
                    mMap.clear();

                    // Add a marker at the searched location
                    mMap.addMarker(new MarkerOptions().position(searchedLocation).title("Searched Location"));

                    // Set the selected location coordinates
                    selectedLatitude = searchedLocation.latitude;
                    selectedLongitude = searchedLocation.longitude;

                    // Update the flag and show the "Confirm Location" button
                    isLocationConfirmed = true;
                    btnConfirmLocation.setVisibility(View.VISIBLE);

                    // Set the map's camera position to the searched location
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(searchedLocation, 15));
                } else {
                    Toast.makeText(LocationSelectionActivity.this, "Location not found", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(LocationSelectionActivity.this, "Please enter a location", Toast.LENGTH_SHORT).show();
        }
    }
}
