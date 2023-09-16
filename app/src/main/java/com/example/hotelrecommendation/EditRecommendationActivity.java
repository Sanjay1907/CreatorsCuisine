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
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import com.google.firebase.database.DataSnapshot;

public class EditRecommendationActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int LOCATION_SELECTION_REQUEST = 2;
    private ImageView profileImage;
    private Button btnChooseImage, btnAddLocation, btnUpdateRecommendation, btnAddFoodItem;
    private EditText etName, etLink, etAddress, etContactNumber, etFood;
    private RatingBar ratingBar;
    private Uri imageUri;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;
    private TextView txtLocation, tvAddedFoodItems;
    private MapView mapView;
    private GoogleMap googleMap;
    private double selectedLatitude;
    private double selectedLongitude;
    private String selectedLocationName;
    private StringBuilder foodItemsBuilder; // To store food items.
    private String recommendationKey;
    private DatabaseReference recommendationRef;

    private Geocoder geocoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_recommendation);

        etName = findViewById(R.id.etName);
        etLink = findViewById(R.id.etlink);
        etAddress = findViewById(R.id.etaddress);
        etContactNumber = findViewById(R.id.etContactNumber);
        etFood = findViewById(R.id.etfood);
        ratingBar = findViewById(R.id.ratingBar);
        btnChooseImage = findViewById(R.id.btnChooseImage);
        btnAddLocation = findViewById(R.id.btnlocation);
        btnUpdateRecommendation = findViewById(R.id.btnUpdateRecommendation);
        txtLocation = findViewById(R.id.txtLocation);
        profileImage = findViewById(R.id.profileImage);
        mapView = findViewById(R.id.mapView);
        tvAddedFoodItems = findViewById(R.id.tvAddedFoodItems);
        btnAddFoodItem = findViewById(R.id.btnAddFoodItem);
        geocoder = new Geocoder(this, Locale.getDefault());

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        foodItemsBuilder = new StringBuilder();

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
            String imageUrl = extras.getString("imageUrl");
            String location = extras.getString("location");

            etName.setText(name);
            etLink.setText(link);
            etAddress.setText(address);
            etContactNumber.setText(contactNumber);
            ratingBar.setRating(rating);

            Glide.with(this).load(imageUrl).placeholder(R.drawable.default_hotel_img).into(profileImage);

            txtLocation.setText(location);

            if (googleMap != null) {
                String[] latLngParts = location.split(", ");
                double latitude = Double.parseDouble(latLngParts[0].substring("Latitude: ".length()));
                double longitude = Double.parseDouble(latLngParts[1].substring("Longitude: ".length()));

                LatLng locationLatLng = new LatLng(latitude, longitude);
                googleMap.addMarker(new MarkerOptions().position(locationLatLng).title("Hotel Location"));
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locationLatLng, 15));
            }

            // Display food items in TextView
            if (food != null && !food.isEmpty()) {
                tvAddedFoodItems.setVisibility(View.VISIBLE);
                tvAddedFoodItems.setText("Must Try Food Items: " + food);
            }
        }

        btnChooseImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImage();
            }
        });

        btnAddLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectLocation();
            }
        });

        btnAddFoodItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addFoodItem();
            }
        });

        btnUpdateRecommendation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateRecommendation();
            }
        });

        recommendationRef = FirebaseDatabase.getInstance().getReference("Creators")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child("recommendation")
                .child(recommendationKey);

        addRecommendationValueListener();
    }

    private void addRecommendationValueListener() {
        recommendationRef.child("location").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String locationString = dataSnapshot.getValue(String.class);
                    if (locationString != null) {
                        String[] latLngParts = locationString.split(", ");
                        double latitude = Double.parseDouble(latLngParts[0].substring("Latitude: ".length()));
                        double longitude = Double.parseDouble(latLngParts[1].substring("Longitude: ".length()));

                        LatLng locationLatLng = new LatLng(latitude, longitude);
                        if (googleMap != null) {
                            googleMap.addMarker(new MarkerOptions().position(locationLatLng).title("Hotel Location"));
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locationLatLng, 15));
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle database errors if needed
            }
        });
    }

    private void addFoodItem() {
        String foodItem = etFood.getText().toString().trim();

        if (!foodItem.isEmpty()) {
            // Append the food item to the TextView with a comma if it's not the first item
            if (tvAddedFoodItems.getVisibility() == View.GONE) {
                tvAddedFoodItems.setVisibility(View.VISIBLE);
            } else {
                tvAddedFoodItems.append(", ");
            }
            tvAddedFoodItems.append(foodItem);

            // Clear the EditText
            etFood.setText("");
        }
    }

    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void selectLocation() {
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
            if (data != null) {
                double latitude = data.getDoubleExtra("latitude", 0.0);
                double longitude = data.getDoubleExtra("longitude", 0.0);

                String address = getAddressFromCoordinates(latitude, longitude);
                etAddress.setText(address);

                String selectedLocation = "Latitude: " + latitude + ", Longitude: " + longitude;
                txtLocation.setText(selectedLocation);

                if (googleMap != null) {
                    selectedLatitude = latitude;
                    selectedLongitude = longitude;
                    LatLng locationLatLng = new LatLng(latitude, longitude);
                    googleMap.clear();
                    googleMap.addMarker(new MarkerOptions().position(locationLatLng).title("Hotel Location"));
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locationLatLng, 15));
                }
            }
        }
    }

    private String getAddressFromCoordinates(double latitude, double longitude) {
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && addresses.size() > 0) {
                Address address = addresses.get(0);
                StringBuilder addressText = new StringBuilder();
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    addressText.append(address.getAddressLine(i)).append(" ");
                }
                return addressText.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Address not found";
    }

    private void updateRecommendation() {
        String updatedName = etName.getText().toString().trim();
        String updatedLink = etLink.getText().toString().trim();
        String updatedAddress = etAddress.getText().toString().trim();
        String updatedContactNumber = etContactNumber.getText().toString().trim();
        String updatedFood = tvAddedFoodItems.getText().toString().trim().replace("Must Try Food Items: ", "");
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

        if (selectedLatitude != 0.0 && selectedLongitude != 0.0) {
            String updatedLocation = "Latitude: " + selectedLatitude + ", Longitude: " + selectedLongitude;
            recommendationRef.child("location").setValue(updatedLocation);
        }

        if (imageUri != null) {
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Updating Recommendation...");
            progressDialog.show();

            final String currentUserId = mAuth.getCurrentUser().getUid();

            final StorageReference imageReference = storageReference.child(updatedName + "_" + currentUserId + ".jpg");

            DatabaseReference oldImageUrlRef = recommendationRef.child("imageUrl");
            oldImageUrlRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DataSnapshot> task) {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String oldImageUrl = task.getResult().getValue(String.class);
                        if (oldImageUrl != null) {
                            StorageReference oldImageRef = FirebaseStorage.getInstance().getReferenceFromUrl(oldImageUrl);
                            oldImageRef.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> deleteTask) {
                                    if (deleteTask.isSuccessful()) {
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
            Toast.makeText(EditRecommendationActivity.this, "Recommendation updated successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(EditRecommendationActivity.this, ViewRecommendationActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }
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

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setAllGesturesEnabled(false);
    }
}
