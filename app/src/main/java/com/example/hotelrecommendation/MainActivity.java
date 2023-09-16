package com.example.hotelrecommendation;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1; // Use a different code from the previous activity

    private CardView btnProfile, btnAddRecommendation, btnLogout, btnViewRecommendation;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private TextView welcome, name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        welcome=findViewById(R.id.welcome);
        name=findViewById(R.id.name);
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("Creators")
                .child(mAuth.getCurrentUser().getUid()); // Get current user's ID
        btnProfile = findViewById(R.id.btnProfile);
        btnAddRecommendation = findViewById(R.id.btnAddRecommendation);
        btnLogout = findViewById(R.id.btnLogout);
        btnViewRecommendation = findViewById(R.id.btnViewRecommendation);

        // Check if the "name" field exists in the Realtime Database
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.hasChild("name")) {
                    // The "name" field does not exist, open ProfileActivity
                    Intent profileIntent = new Intent(MainActivity.this, ProfileActivity.class);
                    startActivity(profileIntent);
                    finish(); // Finish the MainActivity to prevent going back to it
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle database read error, if any.
            }
        });

        if(mAuth == null){
            Intent intent = new Intent(getApplicationContext(),SendOTPActivity.class);
            startActivity(intent);
            finish();
        }
        else{
            String userId = mAuth.getCurrentUser().getUid();
            DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference().child("Creators").child(userId);

            usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String displayName = dataSnapshot.child("name").getValue(String.class);
                        name.setText(displayName);
                    } else {
                        Toast.makeText(MainActivity.this, "Creators data does not exist in the database", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Handle the error
                    Toast.makeText(MainActivity.this, "Failed to retrieve creators data", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Request location permission if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        // Set click listeners for the buttons
        btnProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                startActivity(intent);
            }
        });

        btnAddRecommendation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, recommendation.class);
                startActivity(intent);
            }
        });

        btnViewRecommendation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ViewRecommendationActivity.class);
                startActivity(intent);
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Inflate the custom logout dialog layout
                View dialogView = getLayoutInflater().inflate(R.layout.dialog_custom_logout, null);

                // Initialize the custom dialog components
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setView(dialogView);
                AlertDialog alertDialog = builder.create();

                // Find the buttons in the custom dialog layout
                Button btnCancel = dialogView.findViewById(R.id.btnCancel);
                Button btnLogout = dialogView.findViewById(R.id.btnLogout);

                // Set click listeners for the custom dialog buttons
                btnCancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Dismiss the dialog if "Cancel" is clicked
                        alertDialog.dismiss();
                    }
                });

                btnLogout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // User confirmed logout, show a progress dialog
                        ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
                        progressDialog.setMessage("Logging out...");
                        progressDialog.setCancelable(false);
                        progressDialog.show();

                        // Perform the logout operation
                        mAuth.signOut();
                        Intent intent = new Intent(getApplicationContext(), SendOTPActivity.class);
                        startActivity(intent);
                        progressDialog.dismiss(); // Dismiss the progress dialog
                        finish();

                        // Dismiss the custom dialog
                        alertDialog.dismiss();
                    }
                });

                // Show the custom dialog
                alertDialog.show();
            }
        });

    }

    // Handle the result of the permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Location permission granted, you can proceed with your app logic
            } else {
                // Location permission denied, handle it as needed
                // You can show a message to the user or disable location-related features
            }
        }
    }
}
