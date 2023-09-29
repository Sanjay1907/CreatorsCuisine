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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import com.google.firebase.database.DataSnapshot;
import android.widget.RadioGroup;
import android.widget.RadioButton;

public class EditRecommendationActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int LOCATION_SELECTION_REQUEST = 2;
    private ImageView profileImage;
    private Button btnChooseImage, btnAddLocation, btnUpdateRecommendation, btnAddFoodItem;
    private EditText etName, etLink, etAddress, etContactNumber, etFood, ettimings;
    private RatingBar ratingBar;
    private Button btnChooseTimings;
    private int startHour, startMinute, endHour, endMinute;
    private SimpleDateFormat timeFormat;
    private Calendar calendar;
    private boolean isChoosingStartTime = true;
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
    private RadioGroup radioGroupFoodType;
    private RadioButton radioButtonVeg, radioButtonNonVeg;
    private String selectedFoodType = "";

    private Geocoder geocoder;
    private String placeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_recommendation);

        etName = findViewById(R.id.etName);
        etLink = findViewById(R.id.etlink);
        etAddress = findViewById(R.id.etaddress);
        etContactNumber = findViewById(R.id.etContactNumber);
        etFood = findViewById(R.id.etfood);
        ettimings = findViewById(R.id.etTimings);
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
        btnChooseTimings = findViewById(R.id.btnChooseTimings); // Initialize the btnChooseTimings Button
        timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        calendar = Calendar.getInstance();
        ettimings.setEnabled(false);

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        foodItemsBuilder = new StringBuilder();
        radioGroupFoodType = findViewById(R.id.radioGroupFoodType);
        radioButtonVeg = findViewById(R.id.radioButtonVeg);
        radioButtonNonVeg = findViewById(R.id.radioButtonNonVeg);

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
            String timings = extras.getString("timings");
            String foodType = extras.getString("foodType");

            etName.setText(name);
            etLink.setText(link);
            etAddress.setText(address);
            etContactNumber.setText(contactNumber);
            ettimings.setText(timings);
            ratingBar.setRating(rating);
            if (foodType != null) {
                RadioButton radioButton;
                if (foodType.equalsIgnoreCase("Veg")) {
                    radioButton = findViewById(R.id.radioButtonVeg);
                } else {
                    radioButton = findViewById(R.id.radioButtonNonVeg);
                }
                radioButton.setChecked(true);
            }

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
        btnChooseTimings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePickerDialog();
            }
        });
        btnAddLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectLocation();
            }
        });
        radioButtonVeg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedFoodType = "Veg";
            }
        });

        radioButtonNonVeg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedFoodType = "Non-Veg";
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
                            ettimings.setText(formatTime(startHour, startMinute) + " - ");
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
                            ettimings.append(formatTime(endHour, endMinute));
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
            // Get the current food items text
            String currentFoodItems = tvAddedFoodItems.getText().toString().trim();

            // Check if "Must Try Food Items:" is already in the text
            boolean mustTryItemsExist = currentFoodItems.startsWith("Must Try Food Items:\n");

            // Split the text into lines
            String[] lines = currentFoodItems.split("\n");

            // Calculate the next serial number
            int nextSno = mustTryItemsExist ? lines.length : lines.length + 1;

            // Create the new food item with the serial number
            String newFoodItem = nextSno + ". " + foodItem;

            // Append the new food item to the existing text with a new line
            String updatedFoodItems = currentFoodItems.isEmpty() ? newFoodItem : currentFoodItems + "\n" + newFoodItem;

            // Set the updated food items text
            if (!mustTryItemsExist) {
                updatedFoodItems = "" + updatedFoodItems;
            }
            tvAddedFoodItems.setText(updatedFoodItems);

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

                String displayName = data.getStringExtra("displayName"); // Get the display name
                placeId = data.getStringExtra("placeId");


                // Set the display name in the hotelName EditText
                etName.setText(displayName);

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
        String updatedtimings = ettimings.getText().toString().trim();
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
        recommendationRef.child("timings").setValue(updatedtimings);
        recommendationRef.child("food").setValue(updatedFood);
        recommendationRef.child("rating").setValue(updatedRating);
        if (!selectedFoodType.isEmpty()) {
            recommendationRef.child("foodType").setValue(selectedFoodType);
        }
        if (selectedLatitude != 0.0 && selectedLongitude != 0.0) {
            String updatedLocation = "Latitude: " + selectedLatitude + ", Longitude: " + selectedLongitude;
            recommendationRef.child("location").setValue(updatedLocation);
            if (placeId != null) {
                recommendationRef.child("placeId").setValue(placeId);
            }
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
