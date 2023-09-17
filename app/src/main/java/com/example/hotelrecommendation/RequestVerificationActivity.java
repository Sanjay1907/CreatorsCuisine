package com.example.hotelrecommendation;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.io.File;
import java.util.Objects;



public class RequestVerificationActivity extends AppCompatActivity {

    private static final int PICK_PDF_REQUEST = 2;
    private Spinner spinnerDocumentType;
    private Button btnChooseDocument, btnRequestVerification;
    private TextView tvSelectedDocument;
    private Uri pdfUri;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;
    private boolean formSubmitted = false; // To track if the form has been submitted
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_verification);

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("Creators")
                .child(Objects.requireNonNull(mAuth.getCurrentUser()).getUid())
                .child("request_verification");
        storageReference = FirebaseStorage.getInstance().getReference("RequestVerification");

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Submitting Request...");
        progressDialog.setCancelable(false); // Prevent users from dismissing the dialog

        spinnerDocumentType = findViewById(R.id.spinnerDocumentType);
        btnChooseDocument = findViewById(R.id.btnChooseDocument);
        btnRequestVerification = findViewById(R.id.btnRequestVerification);
        tvSelectedDocument = findViewById(R.id.tvSelectedDocument);

        // Check if the form has already been submitted
        checkFormSubmission();

        // If the form has been submitted, disable form fields
        if (formSubmitted) {
            spinnerDocumentType.setEnabled(false);
            btnChooseDocument.setEnabled(false);
            btnRequestVerification.setEnabled(false);
        } else {
            // If the form has not been submitted, enable form fields
            btnChooseDocument.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectPDF();
                }
            });

            btnRequestVerification.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    uploadPDF();
                }
            });
        }

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.document_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDocumentType.setAdapter(adapter);

        spinnerDocumentType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // Handle document type selection
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Do nothing
            }
        });
    }

    // Check if the form has already been submitted
    private void checkFormSubmission() {
        final String userId = mAuth.getCurrentUser().getUid();
        DatabaseReference userReference = databaseReference.child(userId);

        userReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // The "request_verification" child node exists, indicating the form has been submitted
                    formSubmitted = true;
                    spinnerDocumentType.setEnabled(false);
                    btnChooseDocument.setEnabled(false);
                    btnRequestVerification.setEnabled(false);

                    // Retrieve and display details from the child node
                    String documentFileName = dataSnapshot.child("documentFileName").getValue(String.class);
                    String documentFileType = dataSnapshot.child("documentType").getValue(String.class);

                    // Update the TextView with the submitted document details
                    tvSelectedDocument.setText("Already Submitted the Request.\nUploaded Document Type: " + documentFileType + "\nUploaded Document Name: " + documentFileName);
                } else {
                    // The "request_verification" child node doesn't exist, indicating the form has not been submitted
                    formSubmitted = false;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle database error
            }
        });
    }

    private void selectPDF() {
        Intent intent = new Intent();
        intent.setType("application/pdf");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select PDF Document"), PICK_PDF_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_PDF_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            pdfUri = data.getData();
            String documentFileName = getFileNameFromUri(pdfUri);

            // Update the TextView with the selected document's file name
            tvSelectedDocument.setText("Selected Document: " + documentFileName);
        }
    }

    // Get the file name from the Uri
    private String getFileNameFromUri(Uri uri) {
        String fileName = "";
        if (uri.getScheme().equals("content")) {
            String[] projection = {android.provider.MediaStore.Images.ImageColumns.DISPLAY_NAME};
            Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(android.provider.MediaStore.Images.ImageColumns.DISPLAY_NAME);
                fileName = cursor.getString(columnIndex);
                cursor.close();
            }
        } else if (uri.getScheme().equals("file")) {
            fileName = new File(uri.getPath()).getName();
        }
        return fileName;
    }

    private void uploadPDF() {
        if (pdfUri != null) {
            long maxFileSize = 2 * 1024 * 1024; // 2MB in bytes

            // Calculate the file size
            long fileSize = 0;
            try {
                Cursor cursor = getContentResolver().query(pdfUri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    fileSize = cursor.getLong(sizeIndex);
                    cursor.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (fileSize <= maxFileSize) { // Check if the file size is within the limit
                progressDialog.show(); // Show the progress dialog

                final String documentType = spinnerDocumentType.getSelectedItem().toString();
                final String userId = mAuth.getCurrentUser().getUid();
                final String fileName = userId + "_" + documentType + ".pdf";

                StorageReference fileReference = storageReference.child(fileName);

                fileReference.putFile(pdfUri)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                // Document upload successful, update database
                                fileReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri downloadUri) {
                                        String documentUrl = downloadUri.toString();
                                        saveDocumentData(userId, documentType, documentUrl, fileName);
                                        progressDialog.dismiss(); // Dismiss the progress dialog
                                    }
                                });
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                progressDialog.dismiss(); // Dismiss the progress dialog
                                Toast.makeText(RequestVerificationActivity.this,
                                        "Document upload failed", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                // File size exceeds the limit
                Toast.makeText(RequestVerificationActivity.this,
                        "Please upload a PDF document of size less than or equal to 2MB", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(RequestVerificationActivity.this,
                    "Please select a PDF document", Toast.LENGTH_SHORT).show();
        }
    }



    private void saveDocumentData(String userId, String documentType, String documentUrl, String documentFileName) {

        DatabaseReference userReference = databaseReference.child(userId);
        userReference.child("documentType").setValue(documentType);
        userReference.child("documentFileName").setValue(documentFileName);
        userReference.child("documentUrl").setValue(documentUrl);
        userReference.child("verification").setValue("0");

        progressDialog.dismiss(); // Dismiss the progress dialog

        // Show the verification request submitted message
        Toast.makeText(RequestVerificationActivity.this,
                "Verification Request Submitted", Toast.LENGTH_SHORT).show();
        finish();
    }
}
