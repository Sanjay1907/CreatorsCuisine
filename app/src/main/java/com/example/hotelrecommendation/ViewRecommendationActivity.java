package com.example.hotelrecommendation;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
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
import android.content.DialogInterface;

import java.util.ArrayList;

public class ViewRecommendationActivity extends AppCompatActivity {

    private ListView listView;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> recommendationKeys;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_recommendation);

        listView = findViewById(R.id.recommendationListView);
        recommendationKeys = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, R.layout.list_item_recommendation, R.id.tvHotelName, new ArrayList<String>());
        listView.setAdapter(adapter);

        databaseReference = FirebaseDatabase.getInstance().getReference("Creators")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child("recommendation");

        // Read data from the database and populate the ListView
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                adapter.clear();
                recommendationKeys.clear();

                int sno = 1; // Initialize the incrementing number

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String hotelName = snapshot.child("name").getValue(String.class);

                    // Add the incrementing number to the adapter
                    adapter.add(sno + ". " + hotelName);

                    // Store the recommendation keys for later use (e.g., for editing or deleting)
                    recommendationKeys.add(snapshot.getKey());

                    sno++; // Increment the number
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle the error
            }
        });

        // Set click listeners for list items (to handle editing or deleting)
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Handle item click, e.g., open an editing activity
                String selectedRecommendationKey = recommendationKeys.get(position);
                // Pass the recommendation key to the editing activity
                // You can implement the editing activity based on your requirements
            }
        });
    }

    // Handle the "Edit" button click
    public void editRecommendation(View view) {
        int position = listView.getPositionForView(view);
        if (position != ListView.INVALID_POSITION) {
            String selectedRecommendationKey = recommendationKeys.get(position);

            // Get the recommendation data based on the selected recommendation key
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
                        float rating = dataSnapshot.child("rating").getValue(Float.class);
                        String imageUrl = dataSnapshot.child("imageUrl").getValue(String.class); // Get the image URL

                        // Start the EditRecommendationActivity and pass the data
                        Intent editIntent = new Intent(ViewRecommendationActivity.this, EditRecommendationActivity.class);
                        editIntent.putExtra("recommendationKey", selectedRecommendationKey);
                        editIntent.putExtra("name", name);
                        editIntent.putExtra("link", link);
                        editIntent.putExtra("address", address);
                        editIntent.putExtra("contactNumber", contactNumber);
                        editIntent.putExtra("food", food);
                        editIntent.putExtra("location", location);
                        editIntent.putExtra("rating", rating);
                        editIntent.putExtra("imageUrl", imageUrl); // Pass the image URL

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
                }
            });
        }
    }




    // Handle the "Delete" button click
    public void deleteRecommendation(View view) {
        int position = listView.getPositionForView(view);
        if (position != ListView.INVALID_POSITION) {
            String selectedRecommendationKey = recommendationKeys.get(position);

            // Get a reference to the recommendation to be deleted
            DatabaseReference recommendationToDeleteRef = databaseReference.child(selectedRecommendationKey);

            // Show an alert dialog to confirm the deletion
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Are you sure you want to delete this recommendation?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Perform the deletion
                            recommendationToDeleteRef.removeValue()
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            // Recommendation deleted successfully
                                            // Show a confirmation message
                                            showConfirmationDialog("Recommendation Deleted", "The recommendation has been deleted successfully.");
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            // Failed to delete the recommendation
                                            // Show an error message
                                            showConfirmationDialog("Deletion Failed", "Failed to delete the recommendation. Please try again later.");
                                        }
                                    });
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // User canceled the deletion
                            dialog.dismiss();
                        }
                    })
                    .show();
        }
    }

    // Show a confirmation dialog
    private void showConfirmationDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Close the dialog
                        dialog.dismiss();
                    }
                })
                .show();
    }

}
