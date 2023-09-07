package com.example.hotelrecommendation;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private EditText etName, etEmail, etContactNumber;
    private ImageView profileImage;
    private Button btnChooseImage, btnUpdateProfile;
    private Uri imageUri;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etContactNumber = findViewById(R.id.etContactNumber);
        profileImage = findViewById(R.id.profileImage);
        btnChooseImage = findViewById(R.id.btnChooseImage);
        btnUpdateProfile = findViewById(R.id.btnUpdateProfile);

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");
        storageReference = FirebaseStorage.getInstance().getReference("ProfileImages");

        btnChooseImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImage();
            }
        });

        btnUpdateProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateProfile();
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

    private void updateProfile() {
        final String name = etName.getText().toString().trim();
        final String email = etEmail.getText().toString().trim();
        final String contactNumber = etContactNumber.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || contactNumber.isEmpty() || imageUri == null) {
            Toast.makeText(this, "Please fill in all fields and select a profile picture", Toast.LENGTH_SHORT).show();
            return;
        }

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Updating Profile...");
        progressDialog.show();

        // Upload profile image to Firebase Storage
        // Use the user's name as the image name
        final StorageReference imageReference = storageReference.child(name + ".jpg");
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

                                        // Create a user object
                                        Map<String, Object> user = new HashMap<>();
                                        user.put("name", name);
                                        user.put("email", email);
                                        user.put("contactNumber", contactNumber);
                                        user.put("profileImage", imageUrl);

                                        // Update user profile in Firebase Realtime Database
                                        String userId = mAuth.getCurrentUser().getUid();
                                        databaseReference.child(userId).updateChildren(user)
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> updateTask) {
                                                        progressDialog.dismiss();
                                                        if (updateTask.isSuccessful()) {
                                                            Toast.makeText(ProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                                                            finish();
                                                        } else {
                                                            Toast.makeText(ProfileActivity.this, "Profile update failed", Toast.LENGTH_SHORT).show();
                                                        }
                                                    }
                                                });
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
