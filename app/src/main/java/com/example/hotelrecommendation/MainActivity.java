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

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final String VERIFY_ID_APPROVED = "2";
    private static final String VERIFY_ID_REJECTED = "1";
    private static final String VERIFY_ID_CANCELLED = "3";

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
                .child(mAuth.getCurrentUser().getUid());
        btnProfile = findViewById(R.id.btnProfile);
        btnAddRecommendation = findViewById(R.id.btnAddRecommendation);
        btnLogout = findViewById(R.id.btnLogout);
        btnViewRecommendation = findViewById(R.id.btnViewRecommendation);

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.hasChild("name")) {
                    Intent profileIntent = new Intent(MainActivity.this, ProfileActivity.class);
                    startActivity(profileIntent);
                    finish();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle database read error, if any.
            }
        });

        if (mAuth == null) {
            Intent intent = new Intent(getApplicationContext(), SendOTPActivity.class);
            startActivity(intent);
            finish();
        } else {
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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

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
                View dialogView = getLayoutInflater().inflate(R.layout.custom_dialog, null);

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setView(dialogView);
                AlertDialog alertDialog = builder.create();

                TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
                TextView dialogMessage = dialogView.findViewById(R.id.dialogMessage);
                Button dialogButton = dialogView.findViewById(R.id.dialogButton);

                dialogTitle.setText("Logout");
                dialogMessage.setText("Are you sure you want to logout?");

                dialogButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
                        progressDialog.setMessage("Logging out...");
                        progressDialog.setCancelable(false);
                        progressDialog.show();

                        mAuth.signOut();
                        Intent intent = new Intent(getApplicationContext(), SendOTPActivity.class);
                        startActivity(intent);
                        progressDialog.dismiss();
                        finish();

                        alertDialog.dismiss();
                    }
                });

                alertDialog.show();
            }
        });

        // Check for verification status when the activity is created
        checkVerificationStatus();
    }

    // Check for verification status and show dialog
    private void checkVerificationStatus() {
        DatabaseReference verifyIdRef = FirebaseDatabase.getInstance().getReference()
                .child("Creators")
                .child(mAuth.getCurrentUser().getUid())
                .child("verifyid");

        verifyIdRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String verifyIdValue = dataSnapshot.getValue(String.class);
                String title = "";
                String message = "";

                if (VERIFY_ID_APPROVED.equals(verifyIdValue)) {
                    title = "Verification Approved";
                    message = "Congratulations! Your verification request has been approved, and your profile is now verified.";
                } else if (VERIFY_ID_REJECTED.equals(verifyIdValue)) {
                    title = "Verification Rejected";
                    message = "We regret to inform you that your verification request has been rejected. Please resubmit with correct documents for verification.";
                } else if (VERIFY_ID_CANCELLED.equals(verifyIdValue)) {
                    title = "Verified Profile Cancelled";
                    message = "We regret to inform you that your verified profile has been cancelled. Your profile has been changed to a normal profile. Please resubmit the request for verification.";
                }

                if (!title.isEmpty()) {
                    showCustomVerificationStatusDialog(title, message);
                    // Remove the "verifyid" child (optional)
                    dataSnapshot.getRef().removeValue();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle database read error, if any.
            }
        });
    }

    private void showCustomVerificationStatusDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        View dialogView = getLayoutInflater().inflate(R.layout.custom_dialog, null);
        builder.setView(dialogView);

        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        TextView dialogMessage = dialogView.findViewById(R.id.dialogMessage);
        Button dialogButton = dialogView.findViewById(R.id.dialogButton);

        dialogMessage.setText(message);

        // Set the title text
        dialogTitle.setText(title);

        // Set the color of the title based on verification status
        if (title.equals("Verification Approved")) {
            dialogTitle.setTextColor(ContextCompat.getColor(this, R.color.green)); // Green color
        } else {
            dialogTitle.setTextColor(ContextCompat.getColor(this, R.color.red)); // Red color for other cases
        }

        AlertDialog alertDialog = builder.create();

        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
            }
        });

        alertDialog.show();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Location permission granted, you can proceed with your app logic
            } else {
                // Location permission denied, handle it as needed
            }
        }
    }
}
