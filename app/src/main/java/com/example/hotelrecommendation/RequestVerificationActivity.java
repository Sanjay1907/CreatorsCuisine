package com.example.hotelrecommendation;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_verification);

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("Creators")
                .child(Objects.requireNonNull(mAuth.getCurrentUser()).getUid())
                .child("request_verification");
        storageReference = FirebaseStorage.getInstance().getReference("RequestVerification");

        spinnerDocumentType = findViewById(R.id.spinnerDocumentType);
        btnChooseDocument = findViewById(R.id.btnChooseDocument);
        btnRequestVerification = findViewById(R.id.btnRequestVerification);
        tvSelectedDocument = findViewById(R.id.tvSelectedDocument);

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
            tvSelectedDocument.setText("Selected Document: " + pdfUri.getLastPathSegment());
        }
    }

    private void uploadPDF() {
        if (pdfUri != null) {
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
                                    saveDocumentData(userId, documentType, documentUrl);
                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(RequestVerificationActivity.this,
                                    "Document upload failed", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(RequestVerificationActivity.this,
                    "Please select a PDF document", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveDocumentData(String userId, String documentType, String documentUrl) {
        DatabaseReference userReference = databaseReference.child(userId);
        userReference.child("documentType").setValue(documentType);
        userReference.child("documentUrl").setValue(documentUrl);

        Toast.makeText(RequestVerificationActivity.this,
                "Document uploaded and saved successfully", Toast.LENGTH_SHORT).show();
        finish();
    }
}
