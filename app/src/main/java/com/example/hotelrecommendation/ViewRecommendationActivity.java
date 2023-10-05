package com.example.hotelrecommendation;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.app.AlertDialog;
import android.widget.Toast;

import java.util.ArrayList;
import com.google.firebase.database.Query;
import com.google.firebase.database.ChildEventListener;

public class ViewRecommendationActivity extends AppCompatActivity {

    private LinearLayout recommendationsContainer;
    private DatabaseReference databaseReference;
    private ArrayList<String> recommendationKeys;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_recommendation);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Fetching recommendation history...");
        progressDialog.setCancelable(false);

        recommendationsContainer = findViewById(R.id.recommendationsContainer);
        recommendationKeys = new ArrayList<>();

        databaseReference = FirebaseDatabase.getInstance().getReference("Creators")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child("recommendation");
        Query query = databaseReference.orderByChild("name");

        // Show the progress dialog before starting to fetch data
        progressDialog.show();

        // Read data from the database and populate the recommendationsContainer
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                recommendationsContainer.removeAllViews();
                recommendationKeys.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String hotelName = snapshot.child("name").getValue(String.class);

                    if (hotelName != null) {
                        // Create a new layout for each recommendation
                        View recommendationView = LayoutInflater.from(ViewRecommendationActivity.this)
                                .inflate(R.layout.item_recommendation, recommendationsContainer, false);

                        TextView hotelNameTextView = recommendationView.findViewById(R.id.hotelNameTextView);
                        Button editButton = recommendationView.findViewById(R.id.editButton);

                        hotelNameTextView.setText(hotelName);

                        // Store the recommendation keys for later use (e.g., for editing or deleting)
                        String selectedRecommendationKey = snapshot.getKey();
                        recommendationKeys.add(selectedRecommendationKey);

                        editButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // Handle edit action directly
                                DatabaseReference selectedRecommendationRef = databaseReference.child(selectedRecommendationKey);
                                selectedRecommendationRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.exists()) {
                                            // Extract the recommendation data
                                            String name = dataSnapshot.child("name").getValue(String.class);
                                            String link = dataSnapshot.child("link").getValue(String.class);
                                            String address = dataSnapshot.child("address").getValue(String.class);
                                            String contactNumber = dataSnapshot.child("contactNumber").getValue(String.class);
                                            String food = dataSnapshot.child("food").getValue(String.class);
                                            String location = dataSnapshot.child("location").getValue(String.class);
                                            String timings = dataSnapshot.child("timings").getValue(String.class);
                                            float rating = dataSnapshot.child("rating").getValue(Float.class);
                                            String imageUrl = dataSnapshot.child("imageUrl").getValue(String.class); // Get the image URL
                                            String foodType = dataSnapshot.child("foodType").getValue(String.class);
                                            String city = dataSnapshot.child("city").getValue(String.class);
                                            String specialType = dataSnapshot.child("specialType").getValue(String.class);
                                            String pincode = dataSnapshot.child("pincode").getValue(String.class);

                                            // Start the EditRecommendationActivity and pass the data
                                            Intent editIntent = new Intent(ViewRecommendationActivity.this, EditRecommendationActivity.class);
                                            editIntent.putExtra("recommendationKey", selectedRecommendationKey);
                                            editIntent.putExtra("name", name);
                                            editIntent.putExtra("link", link);
                                            editIntent.putExtra("address", address);
                                            editIntent.putExtra("contactNumber", contactNumber);
                                            editIntent.putExtra("food", food);
                                            editIntent.putExtra("location", location);
                                            editIntent.putExtra("timings", timings);
                                            editIntent.putExtra("rating", rating);
                                            editIntent.putExtra("imageUrl", imageUrl); // Pass the image URL
                                            editIntent.putExtra("foodType", foodType);
                                            editIntent.putExtra("city", city);
                                            editIntent.putExtra("specialType", specialType);
                                            editIntent.putExtra("pincode", pincode);

                                            // Pass the MapView data here
                                            // Extract latitude and longitude from the location string
                                            String[] latLngParts = location.split(", ");
                                            double latitude = Double.parseDouble(latLngParts[0].substring("Latitude: ".length()));
                                            double longitude = Double.parseDouble(latLngParts[1].substring("Longitude: ".length()));
                                            editIntent.putExtra("latitude", latitude);
                                            editIntent.putExtra("longitude", longitude);

                                            startActivity(editIntent);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                        // Handle the error
                                        // Dismiss the progress dialog in case of an error as well
                                        progressDialog.dismiss();
                                    }
                                });
                            }
                        });
                        recommendationsContainer.addView(recommendationView);
                    }
                }

                // Dismiss the progress dialog once all recommendations are loaded
                progressDialog.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle the error
                // Dismiss the progress dialog in case of an error
                progressDialog.dismiss();
            }
        });
    }
}
