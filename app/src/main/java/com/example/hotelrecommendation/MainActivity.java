package com.example.hotelrecommendation;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private Button btnProfile, btnAddRecommendation, btnLogout;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("Creators")
                .child(mAuth.getCurrentUser().getUid()); // Get current user's ID
        btnProfile = findViewById(R.id.btnProfile);
        btnAddRecommendation = findViewById(R.id.btnAddRecommendation);
        btnLogout = findViewById(R.id.btnLogout);

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

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAuth.signOut();
                Intent i = new Intent(getApplicationContext(), SendOTPActivity.class);
                startActivity(i);
                finish();
            }
        });
    }
}
