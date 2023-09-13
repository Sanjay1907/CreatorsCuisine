package com.example.hotelrecommendation;

import android.app.ProgressDialog;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class recommendation extends AppCompatActivity implements OnMapReadyCallback {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PICK_LOCATION_REQUEST = 2;
    private ImageView profileImage;
    private Button btnChooseImage, btnAddLocation, btnAddRecommendation;
    private EditText etName, etLink, etAddress, etContactNumber, etFood;
    private RatingBar ratingBar;
    private Uri imageUri;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;
    private TextView txtLocation;
    private MapView mapView;
    private GoogleMap googleMap;
    private double selectedLatitude;
    private double selectedLongitude;
    private String selectedLocationName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommendation);

        profileImage = findViewById(R.id.profileImage);
        btnChooseImage = findViewById(R.id.btnChooseImage);
        etName = findViewById(R.id.etName);
        etLink = findViewById(R.id.etlink);
        etAddress = findViewById(R.id.etaddress);
        etContactNumber = findViewById(R.id.etContactNumber);
        etFood = findViewById(R.id.etfood);
        ratingBar = findViewById(R.id.ratingBar);
        btnAddLocation = findViewById(R.id.btnlocation);
        btnAddRecommendation = findViewById(R.id.btnadd);
        txtLocation = findViewById(R.id.txtLocation);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("Creators");
        storageReference = FirebaseStorage.getInstance().getReference("hotel_images");

        btnChooseImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImage();
            }
        });

        btnAddLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent locationIntent = new Intent(recommendation.this, LocationSelectionActivity.class);
                startActivityForResult(locationIntent, PICK_LOCATION_REQUEST);
            }
        });

        btnAddRecommendation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addRecommendation();
            }
        });
    }

    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            profileImage.setImageURI(imageUri);
        } else if (requestCode == PICK_LOCATION_REQUEST) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    selectedLatitude = data.getDoubleExtra("latitude", 0.0);
                    selectedLongitude = data.getDoubleExtra("longitude", 0.0);

                    // Use reverse geocoding to get the location name
                    selectedLocationName = getLocationName(selectedLatitude, selectedLongitude);

                    // Fill the location name in the EditText
                    if (selectedLocationName != null) {
                        etAddress.setText(selectedLocationName);
                    }

                    // Display latitude and longitude in the TextView
                    txtLocation.setText("Latitude: " + selectedLatitude + ", Longitude: " + selectedLongitude);

                    if (googleMap != null) {
                        googleMap.clear();
                        LatLng locationLatLng = new LatLng(selectedLatitude, selectedLongitude);
                        googleMap.addMarker(new MarkerOptions().position(locationLatLng).title("Hotel Location"));
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locationLatLng, 15));
                    }
                }
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Location selection canceled", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void addRecommendation() {
        final String name = etName.getText().toString().trim();
        final String link = etLink.getText().toString().trim();
        final String address = etAddress.getText().toString().trim();
        final String contactNumber = etContactNumber.getText().toString().trim();
        final String food = etFood.getText().toString().trim();
        final String location = txtLocation.getText().toString().trim();
        final float rating = ratingBar.getRating();

        if (name.isEmpty() || link.isEmpty() || address.isEmpty() || contactNumber.isEmpty() || food.isEmpty() || location.isEmpty() || imageUri == null) {
            Toast.makeText(this, "Please fill in all fields, select a location, and choose an image", Toast.LENGTH_SHORT).show();
            return;
        }

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Adding Recommendation...");
        progressDialog.show();

        final String currentUserId = mAuth.getCurrentUser().getUid();

        final StorageReference imageReference = storageReference.child(name + "_" + currentUserId + ".jpg");

        imageReference.putFile(imageUri)
                .addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()) {
                            imageReference.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                @Override
                                public void onComplete(@NonNull Task<Uri> downloadTask) {
                                    if (downloadTask.isSuccessful()) {
                                        String imageUrl = downloadTask.getResult().toString();

                                        Recommendation1 recommendation = new Recommendation1(name, link, address, contactNumber, food, location, rating, imageUrl);

                                        databaseReference.child(currentUserId).child("recommendation").push().setValue(recommendation)
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> addTask) {
                                                        progressDialog.dismiss();
                                                        if (addTask.isSuccessful()) {
                                                            Toast.makeText(recommendation.this, "Recommendation added successfully", Toast.LENGTH_SHORT).show();
                                                            Intent intent = new Intent(recommendation.this, MainActivity.class);
                                                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                            startActivity(intent);
                                                            finish();
                                                        } else {
                                                            Toast.makeText(recommendation.this, "Failed to add recommendation", Toast.LENGTH_SHORT).show();
                                                        }
                                                    }
                                                });
                                    } else {
                                        progressDialog.dismiss();
                                        Toast.makeText(recommendation.this, "Image upload failed", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        } else {
                            progressDialog.dismiss();
                            Toast.makeText(recommendation.this, "Image upload failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private String getLocationName(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (!addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder locationName = new StringBuilder();
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    locationName.append(address.getAddressLine(i));
                    if (i < address.getMaxAddressLineIndex()) {
                        locationName.append(", ");
                    }
                }
                return locationName.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void clearFields() {
        etName.setText("");
        etLink.setText("");
        etAddress.setText("");
        etContactNumber.setText("");
        etFood.setText("");
        txtLocation.setText("");
        ratingBar.setRating(0);
        profileImage.setImageResource(R.drawable.default_profile_image);
        googleMap.clear();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setAllGesturesEnabled(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
