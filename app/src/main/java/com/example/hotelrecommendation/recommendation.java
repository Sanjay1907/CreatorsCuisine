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
    private TextView txtLocation; // TextView for displaying the selected location
    private MapView mapView;
    private GoogleMap googleMap;

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
        txtLocation = findViewById(R.id.txtLocation); // Initialize the TextView
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
                // Create an intent to open the LocationSelectionActivity
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
                // Handle the selected location data here
                if (data != null) {
                    // You can retrieve the selected location information from the data Intent
                    double latitude = data.getDoubleExtra("latitude", 0.0);
                    double longitude = data.getDoubleExtra("longitude", 0.0);

                    // Display the selected location in the TextView
                    txtLocation.setText("Latitude: " + latitude + ", Longitude: " + longitude);

                    // Update the map marker
                    if (googleMap != null) {
                        googleMap.clear();
                        LatLng locationLatLng = new LatLng(latitude, longitude);
                        googleMap.addMarker(new MarkerOptions().position(locationLatLng).title("Hotel Location"));
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locationLatLng, 15));
                    }
                }
            } else if (resultCode == RESULT_CANCELED) {
                // Handle the case where the user canceled the location selection
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
        final String location = txtLocation.getText().toString().trim(); // Get the location from the TextView
        final float rating = ratingBar.getRating();

        if (name.isEmpty() || link.isEmpty() || address.isEmpty() || contactNumber.isEmpty() || food.isEmpty() || location.isEmpty() || imageUri == null) {
            Toast.makeText(this, "Please fill in all fields, select a location, and choose an image", Toast.LENGTH_SHORT).show();
            return;
        }

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Adding Recommendation...");
        progressDialog.show();

        final String currentUserId = mAuth.getCurrentUser().getUid();

        // Use the hotel name as the image name with the user's UID appended
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

                                        // Create a recommendation object
                                        Recommendation1 recommendation = new Recommendation1(name, link, address, contactNumber, food, location, rating, imageUrl);

                                        // Push the recommendation to Firebase Realtime Database under the "recommendation" child node of the user's ID
                                        databaseReference.child(currentUserId).child("recommendation").push().setValue(recommendation)
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> addTask) {
                                                        progressDialog.dismiss();
                                                        if (addTask.isSuccessful()) {
                                                            Toast.makeText(recommendation.this, "Recommendation added successfully", Toast.LENGTH_SHORT).show();
                                                            clearFields();
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

    private void clearFields() {
        etName.setText("");
        etLink.setText("");
        etAddress.setText("");
        etContactNumber.setText("");
        etFood.setText("");
        txtLocation.setText(""); // Clear the location TextView
        ratingBar.setRating(0);
        profileImage.setImageResource(R.drawable.default_profile_image);
        googleMap.clear(); // Clear the map marker
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setAllGesturesEnabled(false); // Disable map interactions
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
