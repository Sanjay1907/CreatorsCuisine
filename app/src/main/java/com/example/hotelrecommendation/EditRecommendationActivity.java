package com.example.hotelrecommendation;

import android.app.ProgressDialog;
import android.content.Intent;
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
import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class EditRecommendationActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int LOCATION_SELECTION_REQUEST = 2;

    private EditText etName, etLink, etAddress, etContactNumber, etFood;
    private RatingBar ratingBar;
    private TextView txtLocation;
    private MapView mapView;
    private GoogleMap googleMap;
    private String recommendationKey;
    private ImageView profileImage;

    private Uri imageUri;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;
    private double selectedLatitude = 0.0;
    private double selectedLongitude = 0.0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_recommendation);

        etName = findViewById(R.id.etName);
        etLink = findViewById(R.id.etlink);
        etAddress = findViewById(R.id.etaddress);
        etContactNumber = findViewById(R.id.etContactNumber);
        etFood = findViewById(R.id.etfood);
        ratingBar = findViewById(R.id.ratingBar);
        txtLocation = findViewById(R.id.txtLocation);
        mapView = findViewById(R.id.mapView);
        profileImage = findViewById(R.id.profileImage);
        Button btnChooseImage = findViewById(R.id.btnChooseImage);

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("Creators");
        storageReference = FirebaseStorage.getInstance().getReference("hotel_images");

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            recommendationKey = extras.getString("recommendationKey");
            String name = extras.getString("name");
            String link = extras.getString("link");
            String address = extras.getString("address");
            String contactNumber = extras.getString("contactNumber");
            String food = extras.getString("food");
            float rating = extras.getFloat("rating");
            String imageUrl = extras.getString("imageUrl"); // Get the image URL
            String location = extras.getString("location");

            etName.setText(name);
            etLink.setText(link);
            etAddress.setText(address);
            etContactNumber.setText(contactNumber);
            etFood.setText(food);
            ratingBar.setRating(rating);

            // Load profile image using Glide
            Glide.with(this).load(imageUrl).placeholder(R.drawable.default_profile_image).into(profileImage);

            // Display the location in the TextView
            txtLocation.setText(location);

            if (googleMap != null) {
                // Extract latitude and longitude from the location string
                String[] latLngParts = location.split(", ");
                double latitude = Double.parseDouble(latLngParts[0].substring("Latitude: ".length()));
                double longitude = Double.parseDouble(latLngParts[1].substring("Longitude: ".length()));

                LatLng locationLatLng = new LatLng(latitude, longitude);
                googleMap.addMarker(new MarkerOptions().position(locationLatLng).title("Hotel Location"));
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locationLatLng, 15));
            }
        }

        Button btnUpdateRecommendation = findViewById(R.id.btnUpdateRecommendation);
        btnUpdateRecommendation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateRecommendation();
            }
        });

        btnChooseImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImage();
            }
        });

        Button btnSelectLocation = findViewById(R.id.btnlocation);
        btnSelectLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectLocation();
            }
        });
    }

    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void selectLocation() {
        // Open LocationSelectionActivity to select a new location
        Intent locationIntent = new Intent(EditRecommendationActivity.this, LocationSelectionActivity.class);
        startActivityForResult(locationIntent, LOCATION_SELECTION_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            profileImage.setImageURI(imageUri);
        } else if (requestCode == LOCATION_SELECTION_REQUEST && resultCode == RESULT_OK) {
            // Handle location selection result here
            if (data != null) {
                double latitude = data.getDoubleExtra("latitude", 0.0);
                double longitude = data.getDoubleExtra("longitude", 0.0);
                String selectedLocation = "Latitude: " + latitude + ", Longitude: " + longitude;
                txtLocation.setText(selectedLocation);

                // Update the map
                if (googleMap != null) {
                    selectedLatitude = latitude;
                    selectedLongitude = longitude;
                    LatLng locationLatLng = new LatLng(latitude, longitude);
                    googleMap.clear(); // Clear existing markers
                    googleMap.addMarker(new MarkerOptions().position(locationLatLng).title("Hotel Location"));
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locationLatLng, 15));
                }
            }
        }
    }

    private void updateRecommendation() {
        String updatedName = etName.getText().toString().trim();
        String updatedLink = etLink.getText().toString().trim();
        String updatedAddress = etAddress.getText().toString().trim();
        String updatedContactNumber = etContactNumber.getText().toString().trim();
        String updatedFood = etFood.getText().toString().trim();
        float updatedRating = ratingBar.getRating();

        DatabaseReference recommendationRef = FirebaseDatabase.getInstance().getReference("Creators")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child("recommendation")
                .child(recommendationKey);

        recommendationRef.child("name").setValue(updatedName);
        recommendationRef.child("link").setValue(updatedLink);
        recommendationRef.child("address").setValue(updatedAddress);
        recommendationRef.child("contactNumber").setValue(updatedContactNumber);
        recommendationRef.child("food").setValue(updatedFood);
        recommendationRef.child("rating").setValue(updatedRating);

        // Update location if latitude and longitude are available
        if (selectedLatitude != 0.0 && selectedLongitude != 0.0) {
            String updatedLocation = "Latitude: " + selectedLatitude + ", Longitude: " + selectedLongitude;
            recommendationRef.child("location").setValue(updatedLocation);
        }

        if (imageUri != null) {
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Updating Recommendation...");
            progressDialog.show();

            final String currentUserId = mAuth.getCurrentUser().getUid();

            // Use the hotel name as the image name with the user's UID appended
            final StorageReference imageReference = storageReference.child(updatedName + "_" + currentUserId + ".jpg");

            // Delete old image if it exists
            DatabaseReference oldImageUrlRef = recommendationRef.child("imageUrl");
            oldImageUrlRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DataSnapshot> task) {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String oldImageUrl = task.getResult().getValue(String.class);
                        if (oldImageUrl != null) {
                            // Delete the old image from Firebase Storage
                            StorageReference oldImageRef = FirebaseStorage.getInstance().getReferenceFromUrl(oldImageUrl);
                            oldImageRef.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> deleteTask) {
                                    if (deleteTask.isSuccessful()) {
                                        // Upload the new image
                                        imageReference.putFile(imageUri)
                                                .addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> uploadTask) {
                                                        if (uploadTask.isSuccessful()) {
                                                            imageReference.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Uri> downloadTask) {
                                                                    if (downloadTask.isSuccessful()) {
                                                                        String updatedImageUrl = downloadTask.getResult().toString();

                                                                        // Update imageUrl in the database
                                                                        recommendationRef.child("imageUrl").setValue(updatedImageUrl);
                                                                        progressDialog.dismiss();
                                                                        Toast.makeText(EditRecommendationActivity.this, "Recommendation updated successfully", Toast.LENGTH_SHORT).show();
                                                                        finish();
                                                                    } else {
                                                                        progressDialog.dismiss();
                                                                        Toast.makeText(EditRecommendationActivity.this, "Image upload failed", Toast.LENGTH_SHORT).show();
                                                                    }
                                                                }
                                                            });
                                                        } else {
                                                            progressDialog.dismiss();
                                                            Toast.makeText(EditRecommendationActivity.this, "Image upload failed", Toast.LENGTH_SHORT).show();
                                                        }
                                                    }
                                                });
                                    } else {
                                        progressDialog.dismiss();
                                        Toast.makeText(EditRecommendationActivity.this, "Failed to delete old image", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(EditRecommendationActivity.this, "Error retrieving old image URL", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            // If no new image was selected, just update the text fields
            Toast.makeText(EditRecommendationActivity.this, "Recommendation updated successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(EditRecommendationActivity.this, ViewRecommendationActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // Clear the back stack
            startActivity(intent);
            finish();
        }
    }

    // ... (other methods)

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

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setAllGesturesEnabled(false); // Disable map interactions
    }
}
