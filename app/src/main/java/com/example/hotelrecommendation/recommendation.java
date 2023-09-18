package com.example.hotelrecommendation;

import android.app.ProgressDialog;
import android.app.TimePickerDialog;
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
import android.widget.TimePicker;
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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class recommendation extends AppCompatActivity implements OnMapReadyCallback {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PICK_LOCATION_REQUEST = 2;
    private ImageView profileImage;
    private Button btnChooseImage, btnAddLocation, btnAddRecommendation, btnAddFoodItem;
    private EditText etName, etLink, etAddress, etContactNumber, etFood, etTimings;
    private Button btnChooseTimings;

    private int startHour, startMinute, endHour, endMinute;
    private SimpleDateFormat timeFormat;
    private Calendar calendar;
    private boolean isChoosingStartTime = true;

    private RatingBar ratingBar;
    private Uri imageUri;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;
    private TextView txtLocation, tvAddedFoodItems;
    private MapView mapView;
    private int foodItemNumber = 1;
    private GoogleMap googleMap;
    private double selectedLatitude;
    private double selectedLongitude;
    private String selectedLocationName;
    private StringBuilder foodItemsBuilder; // To store food items.

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
        etTimings = findViewById(R.id.etTimings);
        btnChooseTimings = findViewById(R.id.btnChooseTimings); // Initialize the btnChooseTimings Button
        timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        calendar = Calendar.getInstance();
        etTimings.setEnabled(false);

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("Creators");
        storageReference = FirebaseStorage.getInstance().getReference("hotel_images");

        btnChooseImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImage();
            }
        });

        btnChooseTimings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePickerDialog();
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

        btnAddFoodItem = findViewById(R.id.btnAddFoodItem);
        tvAddedFoodItems = findViewById(R.id.tvAddedFoodItems);
        foodItemsBuilder = new StringBuilder(); // Initialize the StringBuilder
        tvAddedFoodItems.setVisibility(View.GONE);

        btnAddFoodItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addFoodItem();
            }
        });
    }

    private void showTimePickerDialog() {
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = calendar.get(Calendar.MINUTE);

        if (isChoosingStartTime) {
            // User is choosing the start time
            TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                    new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                            startHour = hourOfDay;
                            startMinute = minute;
                            etTimings.setText(formatTime(startHour, startMinute) + " - ");
                            isChoosingStartTime = false; // Now, the user will choose close time
                            btnChooseTimings.setText("Choose Closing Time");
                            showToast("Choose hotel closing time");
                        }
                    }, currentHour, currentMinute, true);
            timePickerDialog.show();
        } else {
            // User is choosing the close time
            TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                    new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                            endHour = hourOfDay;
                            endMinute = minute;
                            etTimings.append(formatTime(endHour, endMinute));
                            isChoosingStartTime = true; // Reset to choose start time next time
                            btnChooseTimings.setText("Choose Timings");
                        }
                    }, currentHour, currentMinute, true);
            timePickerDialog.show();
        }
    }

    private String formatTime(int hourOfDay, int minute) {
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        return timeFormat.format(calendar.getTime());
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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

    private void addFoodItem() {
        String foodItem = etFood.getText().toString().trim();

        if (!foodItem.isEmpty()) {
            // Append the food item with the item number and a period
            if (foodItemsBuilder.length() > 0) {
                foodItemsBuilder.append("\n"); // Add a new line
            }
            foodItemsBuilder.append(foodItemNumber).append(". ").append(foodItem); // Add item number

            // Increment the item number for the next food item
            foodItemNumber++;

            // Update the TextView with the current list of food items
            tvAddedFoodItems.setText("Must Try Food Items:\n" + foodItemsBuilder.toString());

            // Clear the EditText
            etFood.setText("");

            // Set the TextView as visible if it was initially invisible
            if (tvAddedFoodItems.getVisibility() == View.GONE) {
                tvAddedFoodItems.setVisibility(View.VISIBLE);
            }
        }
    }


    private void addRecommendation() {
        final String name = etName.getText().toString().trim();
        final String link = etLink.getText().toString().trim();
        final String address = etAddress.getText().toString().trim();
        final String contactNumber = etContactNumber.getText().toString().trim();
        final String timings = etTimings.getText().toString().trim();
        final String location = txtLocation.getText().toString().trim();
        final float rating = ratingBar.getRating();

        if(imageUri==null){
            Toast.makeText(this,"Hotel Image is Required", Toast.LENGTH_SHORT).show();
            return;
        }
        if(name.isEmpty()){
            Toast.makeText(this,"Hotel Name is Required", Toast.LENGTH_SHORT).show();
            return;
        }
        if(address.isEmpty()){
            Toast.makeText(this,"Hotel Address is Required", Toast.LENGTH_SHORT).show();
            return;
        }
        if(contactNumber.isEmpty()){
            Toast.makeText(this,"Contact Number is Required", Toast.LENGTH_SHORT).show();
            return;
        }
        if(timings.isEmpty()){
            Toast.makeText(this,"Hotel Timings is Required", Toast.LENGTH_SHORT).show();
            return;
        }
        if(location.isEmpty()){
            Toast.makeText(this,"Location is Required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (rating == 0) {
            Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isValidUrl(link)) {
            Toast.makeText(this, "Please enter a valid URL in the Link field", Toast.LENGTH_SHORT).show();
            return;
        }

        if (foodItemsBuilder.length() == 0) {
            Toast.makeText(this, "At least one food item is required", Toast.LENGTH_SHORT).show();
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

                                        Recommendation1 recommendation = new Recommendation1(name, link, address, contactNumber, foodItemsBuilder.toString(), location, rating, imageUrl, timings);

                                        databaseReference.child(currentUserId).child("recommendation").push().setValue(recommendation)
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> addTask) {
                                                        progressDialog.dismiss();
                                                        if (addTask.isSuccessful()) {
                                                            Toast.makeText(recommendation.this, "Recommendation added successfully", Toast.LENGTH_SHORT).show();
                                                            Intent intent = new Intent(recommendation.this, MainActivity.class);
                                                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // Clear the back stack
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

    private boolean isValidUrl(String url) {
        // Define a regular expression for a valid URL
        String urlRegex = "^(http(s)?://)?[\\w.-]+\\.[a-zA-Z]{2,4}(/\\S*)?$";

        // Check if the input matches the regular expression
        return url.matches(urlRegex);
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
        etTimings.setText("");
        etFood.setText("");
        txtLocation.setText("");
        ratingBar.setRating(0);
        profileImage.setImageResource(R.drawable.default_profile_image);
        googleMap.clear();
        tvAddedFoodItems.setText(""); // Clear the added food items TextView
        foodItemsBuilder.setLength(0); // Clear the food items StringBuilder
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
