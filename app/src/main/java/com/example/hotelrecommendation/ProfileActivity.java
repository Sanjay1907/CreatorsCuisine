package com.example.hotelrecommendation;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private EditText etName, etEmail, etContactNumber;
    private ImageView profileImage;
    private Button btnChooseImage, btnSaveProfile, btngetverified;
    private Uri imageUri;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etContactNumber = findViewById(R.id.etContactNumber);
        profileImage = findViewById(R.id.profileImage);
        btnChooseImage = findViewById(R.id.btnChooseImage);
        btnSaveProfile = findViewById(R.id.btnUpdateProfile);
        btngetverified = findViewById(R.id.btngetverified);

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("Creators")
                .child(mAuth.getCurrentUser().getUid()); // Get current user's ID
        storageReference = FirebaseStorage.getInstance().getReference("ProfileImages");

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Updating Profile...");

        // Fetch user data from the database and autofill the fields if available
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String nameFromDb = dataSnapshot.child("name").getValue(String.class);
                    String emailFromDb = dataSnapshot.child("email").getValue(String.class);
                    String imageUrlFromDb = dataSnapshot.child("profileImage").getValue(String.class);

                    // Autofill the fields
                    etName.setText(nameFromDb);
                    etEmail.setText(emailFromDb);

                    // Load profile image if available using Glide
                    if (imageUrlFromDb != null && !imageUrlFromDb.isEmpty()) {
                        Glide.with(ProfileActivity.this).load(imageUrlFromDb).into(profileImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle the error, if any.
            }
        });

        // Disable editing of the contact number field and autofill it from the database
        etContactNumber.setEnabled(false);
        DatabaseReference userReference = databaseReference.child("phoneNumber");
        userReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String phoneNumber = dataSnapshot.getValue(String.class);
                    etContactNumber.setText(phoneNumber);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle the error, if any.
            }
        });

        // Check the value of "verification" child and update the button text
        DatabaseReference verificationReference = databaseReference.child("verification");
        verificationReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    int verificationValue = dataSnapshot.getValue(Integer.class);
                    if (verificationValue == 1) {
                        btngetverified.setText("Verified");
                        int greenColor = ContextCompat.getColor(ProfileActivity.this, R.color.green);
                        btngetverified.setBackgroundColor(greenColor);
                    } else {
                        btngetverified.setText("Get verified");
                    }
                } else {
                    btngetverified.setText("Get verified");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle the error, if any.
            }
        });


        btnChooseImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImage();
            }
        });

        btnSaveProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProfile();
            }
        });
        btngetverified.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check the text on the button
                if (btngetverified.getText().toString().equals("Get verified")) {
                    // If it's "Get verified," navigate to RequestVerificationActivity
                    Intent requestVerificationIntent = new Intent(ProfileActivity.this, RequestVerificationActivity.class);
                    startActivity(requestVerificationIntent);
                } else if (btngetverified.getText().toString().equals("Verified")) {
                    // If it's "Verified," do nothing
                    // You can optionally show a message to inform the user they are already verified.
                    Toast.makeText(ProfileActivity.this, "Your Profile has been already verified.", Toast.LENGTH_SHORT).show();
                }
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
        }
    }

    // Inside the saveProfile method
    private void saveProfile() {
        final String name = etName.getText().toString().trim();
        final String email = etEmail.getText().toString().trim();

        // Check if name and email are not empty
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Name and email are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();

        // Generate a unique image filename using "userid_name" format
        final String imageFileName = mAuth.getCurrentUser().getUid() + "_" + name + ".jpg";

        // Check if there's a change in the profile image URL
        if (imageUri != null) {
            // An image is selected, proceed with image upload

            // Upload profile image to Firebase Storage with the generated filename
            uploadNewProfileImage(imageFileName);
        } else {
            // No image selected, update the profile without changing the image

            // Fetch the current image URL from the database
            databaseReference.child("profileImage").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    String currentImageUrl = dataSnapshot.getValue(String.class);

                    if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
                        // There's a current image URL, update the profile without changing the image
                        updateUserProfile(name, email, currentImageUrl);
                    } else {
                        // There's no current image URL, update the profile without an image
                        updateUserProfile(name, email, "");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    progressDialog.dismiss();
                    Toast.makeText(ProfileActivity.this, "Profile update failed", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }


    private void updateUserProfile(final String name, String email, String imageUrl) {
        // Create a user object
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);

        // Check if there's a change in the profile image URL
        if (!imageUrl.isEmpty()) {
            user.put("profileImage", imageUrl);
            // Delete the old image if it exists
            deleteOldProfileImage(imageUrl);
        }

        // Update user profile in Firebase Realtime Database under the current user's ID
        databaseReference.updateChildren(user)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> updateTask) {
                        progressDialog.dismiss();
                        if (updateTask.isSuccessful()) {
                            Toast.makeText(ProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();

                            // Navigate to MainActivity after updating the profile
                            Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // Clear the back stack
                            startActivity(intent);
                            finish(); // Close the current activity
                        } else {
                            Toast.makeText(ProfileActivity.this, "Profile update failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void deleteOldProfileImage(String imageUrl) {
        // Delete the old image with the generated filename
        StorageReference oldImageReference = storageReference.child(imageUrl);
        oldImageReference.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Old image deleted successfully (if it existed)
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Handle any errors that may occur while deleting the old image
                    }
                });
    }

    private void uploadNewProfileImage(final String imageFileName) {
        // Upload the new profile image with the generated filename
        final StorageReference imageReference = storageReference.child(imageFileName);
        imageReference.putFile(imageUri)
                .addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()) {
                            // Image upload successful
                            imageReference.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                @Override
                                public void onComplete(@NonNull Task<Uri> downloadTask) {
                                    if (downloadTask.isSuccessful()) {
                                        String imageUrl = downloadTask.getResult().toString();
                                        // Update user profile with the new image URL
                                        updateUserProfile(etName.getText().toString().trim(), etEmail.getText().toString().trim(), imageUrl);
                                    } else {
                                        progressDialog.dismiss();
                                        Toast.makeText(ProfileActivity.this, "Image upload failed", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        } else {
                            progressDialog.dismiss();
                            Toast.makeText(ProfileActivity.this, "Image upload failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
