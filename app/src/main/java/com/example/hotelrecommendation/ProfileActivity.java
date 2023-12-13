package com.example.hotelrecommendation;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
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
import java.util.Random;
import android.Manifest;
public class ProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String TAG = "ProfileActivity";

    private EditText etName, etEmail, etContactNumber, etChannelname, etChannellink, etinstaid, etName2;
    private ImageView profileImage, ivverifiedphone;
    private Button btnChooseImage, btnSaveProfile, btngetverified, btnverify;
    private Uri imageUri;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;
    private ProgressDialog progressDialog;
    private String generatedOTP;
    private static final int PERMISSION_REQUEST_SMS = 100;
    private Dialog dialog;
    private AlertDialog otpDialog;
    private boolean isPhoneNumberVerified = false;
    private boolean isProfileImageAvailable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        Log.d(TAG, "onCreate: ProfileActivity started");

        etName = findViewById(R.id.etName);
        etName2 = findViewById(R.id.etName2);
        etEmail = findViewById(R.id.etEmail);
        etContactNumber = findViewById(R.id.etContactNumber);
        profileImage = findViewById(R.id.profileImage);
        btnChooseImage = findViewById(R.id.btnChooseImage);
        btnSaveProfile = findViewById(R.id.btnUpdateProfile);
        btngetverified = findViewById(R.id.btngetverified);
        etChannelname = findViewById(R.id.etChannelname);
        etChannellink = findViewById(R.id.etChannellink);
        etinstaid = findViewById(R.id.etinstaid);
        btnverify = findViewById(R.id.ivVerified);
        ivverifiedphone = findViewById(R.id.ivVerifiedphone);

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("Creators")
                .child(mAuth.getCurrentUser().getUid()); // Get current user's ID
        storageReference = FirebaseStorage.getInstance().getReference("ProfileImages");

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Fetching user data...");
        progressDialog.setCancelable(false);
        dialog = new Dialog(ProfileActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_wait1);
        dialog.setCanceledOnTouchOutside(false);

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                progressDialog.dismiss(); // Dismiss the progress dialog when data is fetched
                if (dataSnapshot.exists()) {
                    Log.d(TAG, "DataSnapshot exists");
                    String nameFromDb = dataSnapshot.child("name").getValue(String.class);
                    String name2FromDb = dataSnapshot.child("name2").getValue(String.class);
                    String contactnumberFromDb = dataSnapshot.child("phoneNumber").getValue(String.class);
                    String chname = dataSnapshot.child("Youtube Channel name").getValue(String.class);
                    String chlink = dataSnapshot.child("Youtube Channel Link").getValue(String.class);
                    String insta = dataSnapshot.child("Instagram id").getValue(String.class);
                    String imageUrlFromDb = dataSnapshot.child("profileImage").getValue(String.class);

                    // Autofill the fields
                    etName.setText(nameFromDb);
                    etName2.setText(name2FromDb);
                    etContactNumber.setText(contactnumberFromDb);
                    etChannelname.setText(chname);
                    etChannellink.setText(chlink);
                    etinstaid.setText(insta);

                    // Load profile image if available using Glide
                    if (imageUrlFromDb != null && !imageUrlFromDb.isEmpty()) {
                        Glide.with(ProfileActivity.this).load(imageUrlFromDb).into(profileImage);
                        isProfileImageAvailable = true;
                    }else{
                        Log.w(TAG, "Image URL not found in database");
                        isProfileImageAvailable = false;
                    }
                }else{
                    Log.d(TAG, "DataSnapshot does not exist");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressDialog.dismiss(); // Dismiss the progress dialog on database error
                Log.e(TAG, "Database error: " + databaseError.getMessage());
            }
        });

        progressDialog.show(); // Show the progress dialog before starting the data fetch

        // Disable editing of the contact number field and autofill it from the database
        etEmail.setEnabled(false);
        DatabaseReference userReference = databaseReference.child("email");
        userReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String email = dataSnapshot.getValue(String.class);
                    etEmail.setText(email);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Database error: " + databaseError.getMessage());
            }
        });
        etContactNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                String phoneNumber = etContactNumber.getText().toString().trim();
                if (phoneNumber.length() == 10 && !isPhoneNumberVerified) {
                    btnverify.setVisibility(View.VISIBLE);
                } else {
                    btnverify.setVisibility(View.GONE);
                }
            }
        });
        DatabaseReference phoneVerificationReference = databaseReference.child("phoneNumberVerified");
        phoneVerificationReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    isPhoneNumberVerified = dataSnapshot.getValue(Boolean.class);
                    if (isPhoneNumberVerified) {
                        btnverify.setVisibility(View.GONE);
                        ivverifiedphone.setVisibility(View.VISIBLE);
                        etContactNumber.setEnabled(false);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Database error: " + databaseError.getMessage());
            }
        });
        btnverify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkPermission()) {
                    String phoneNumber = etContactNumber.getText().toString().trim();

                    if (TextUtils.isEmpty(phoneNumber)) {
                        Toast.makeText(ProfileActivity.this, "Please enter a phone number.", Toast.LENGTH_SHORT).show();
                    } else {
                        dialog.show();
                        generatedOTP = generateOTP();
                        sendOTPviaSMS(phoneNumber, generatedOTP);
                    }
                } else {
                    requestPermission();
                }
            }
        });

        DatabaseReference verificationReference = databaseReference.child("request_verification")
                .child(mAuth.getCurrentUser().getUid()) // Assuming the user ID matches the key in request_verification
                .child("verification");

        verificationReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String verificationValue = dataSnapshot.getValue(String.class); // Assuming verification is stored as a String
                    if ("1".equals(verificationValue)) { // Check if it equals "1" (verified)
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
                Log.e(TAG, "Database error: " + databaseError.getMessage());
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
    private String generateOTP() {
        Random random = new Random();
        int otpNumber = 100000 + random.nextInt(900000); // Generate a 6-digit random number
        return String.valueOf(otpNumber);
    }

    private void sendOTPviaSMS(String phoneNumber, String otp) {
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phoneNumber, null, "Code for Creators Cuisine: " + otp, null, null);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                dialog.dismiss();
                Toast.makeText(ProfileActivity.this, "OTP sent successfully.", Toast.LENGTH_SHORT).show();
                showOTPDialog();
            }
        }, 3000);
    }
    private void showOTPDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_otp, null);
        builder.setView(dialogView);

        final EditText otpEditText = dialogView.findViewById(R.id.edit_text_otp);
        Button submitButton = dialogView.findViewById(R.id.btn_submit);

        otpDialog = builder.create();
        otpDialog.setCanceledOnTouchOutside(false);
        otpDialog.setCancelable(false);
        otpDialog.show();
        otpDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String enteredOTP = otpEditText.getText().toString().trim();
                if (!enteredOTP.isEmpty()) {
                    if (enteredOTP.equals(generatedOTP)) {
                        isPhoneNumberVerified = true;
                        updatePhoneNumberInDatabase(etContactNumber.getText().toString().trim());
                        dialog.dismiss();
                        otpDialog.dismiss();
                        Toast.makeText(getApplicationContext(), "Phone number verified successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Entered Verification Code is incorrect. Please re-enter the Code", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Please enter the Verification Code", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private void updatePhoneNumberInDatabase(String phoneNumber) {
        // Update the phone number and its verification status in the database
        databaseReference.child("phoneNumber").setValue(phoneNumber)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            // Update phoneNumberVerified flag in the database
                            databaseReference.child("phoneNumberVerified").setValue(true)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            ivverifiedphone.setVisibility(View.VISIBLE);
                                            etContactNumber.setEnabled(false);
                                            btnverify.setVisibility(View.GONE);
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(ProfileActivity.this, "Failed to update phone number verification status.", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Toast.makeText(ProfileActivity.this, "Failed to update phone number.", Toast.LENGTH_SHORT).show();
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

    private void saveProfile() {
        final String newName = etName.getText().toString().trim();
        final String name2 = etName2.getText().toString().trim();
        final String contactnumber = etContactNumber.getText().toString().trim();
        final String channelname = etChannelname.getText().toString().trim();
        final String channellink = etChannellink.getText().toString().trim();
        final String instaid = etinstaid.getText().toString().trim();

        // Check if newName is empty
        if (TextUtils.isEmpty(newName)) {
            Toast.makeText(this, "UserName is required.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(name2)) {
            Toast.makeText(this, "Name is required.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(contactnumber)) {
            Toast.makeText(this, "Contact Number is required.", Toast.LENGTH_SHORT).show();
            return;
        }
        // Check if at least one of YouTube channel link or Instagram ID is provided
        if (TextUtils.isEmpty(channellink) && TextUtils.isEmpty(instaid)) {
            Toast.makeText(this, "Either YouTube channel link or Instagram ID is mandatory.", Toast.LENGTH_SHORT).show();
            return;
        }
        // Check if email is in a valid format
        if (!isValidUrl(channellink)) {
            Toast.makeText(this, "Please enter a valid Youtube channel link.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!TextUtils.isEmpty(channellink) && TextUtils.isEmpty(channelname)) {
            Toast.makeText(this, "YouTube channel name is required as you provided a YouTube channel link.", Toast.LENGTH_SHORT).show();
            return;
        }
        if(!isPhoneNumberVerified){
            Toast.makeText(this, "Please verify your Phone Number to proceed further. ", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isProfileImageAvailable && imageUri == null) {
            Toast.makeText(this, "Profile image is mandatory.", Toast.LENGTH_SHORT).show();
            return;
        }
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Updating Profile...");
        progressDialog.show();

        if (imageUri != null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Updating Profile...");
            progressDialog.show();

            final String imageFileName = mAuth.getCurrentUser().getUid() + "_" + newName + ".jpg";

            uploadNewProfileImage(imageFileName, newName, name2, contactnumber, channelname, channellink, instaid);
        } else {
            updateUserProfile(newName, name2, contactnumber, channelname, channellink, instaid, "");
        }
    }
    private boolean isValidUrl(String url) {
        // Define a regular expression for a valid URL
        String urlRegex = "^(http(s)?://)?[\\w.-]+\\.[a-zA-Z]{2,4}(/\\S*)?$";

        return url.matches(urlRegex);
    }
    private void uploadNewProfileImage(final String imageFileName, final String newName, final String name2,
                                       final String contactnumber, final String channelname, final String channellink,
                                       final String instaid) {
        // Create a reference to the storage location where the image will be uploaded
        final StorageReference imageReference = storageReference.child(imageFileName);

        // Upload the image to the storage location
        UploadTask uploadTask = imageReference.putFile(imageUri);

        uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if (task.isSuccessful()) {
                    // Image uploaded successfully, get the download URL
                    imageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri downloadUrl) {
                            // Update the user's profile with the new image URL
                            updateUserProfile(newName, name2, contactnumber, channelname, channellink, instaid,
                                    downloadUrl.toString());
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(ProfileActivity.this, "Failed to upload image.", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(ProfileActivity.this, "Failed to upload image.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateUserProfile(final String name, String name2, String contactnumber, String channelname, String channellink, String instaid, String imageUrl) {
        // Create a user object
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("name2", name2);
        user.put("phoneNumber", contactnumber);
        user.put("Youtube Channel name", channelname);
        user.put("Youtube Channel Link", channellink);
        user.put("Instagram id", instaid);

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
    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }
    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, PERMISSION_REQUEST_SMS);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with sending OTP
                Toast.makeText(this, "SMS permission granted. You can send OTP now.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "SMS permission denied. Cannot send OTP.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    @Override
    public void onBackPressed() {
        if (otpDialog != null && otpDialog.isShowing()) {
            Toast.makeText(this, "OTP verification is mandatory.", Toast.LENGTH_SHORT).show();
        } else {
            super.onBackPressed();
        }
    }
}