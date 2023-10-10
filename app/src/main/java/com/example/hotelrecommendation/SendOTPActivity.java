package com.example.hotelrecommendation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import java.util.concurrent.TimeUnit;

public class SendOTPActivity extends AppCompatActivity {
    private static final String TAG = "SendOTPActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Check if user is already signed in
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // User is already signed in, open the dashboard activity
            Log.d(TAG, "User is already signed in. Redirecting to MainActivity.");
            startActivity(new Intent(SendOTPActivity.this, MainActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_send_otpactivity);

        final EditText inputmobile = findViewById(R.id.inputmobile);
        Button getotp = findViewById(R.id.buttonGetOTP);
        final ProgressBar progressBar = findViewById(R.id.progressbar);

        getotp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (inputmobile.getText().toString().isEmpty()) {
                    Log.d(TAG, "Mobile number input is empty.");
                    Toast.makeText(SendOTPActivity.this, "Enter Mobile Number", Toast.LENGTH_SHORT).show();
                } else if (inputmobile.getText().toString().length() != 10) {
                    Log.d(TAG, "Invalid mobile number entered.");
                    Toast.makeText(SendOTPActivity.this, "Enter a valid 10-digit Mobile Number", Toast.LENGTH_SHORT).show();
                } else {
                    Log.d(TAG, "Starting phone number verification...");
                    progressBar.setVisibility(View.VISIBLE);
                    getotp.setVisibility(View.INVISIBLE);

                    PhoneAuthProvider.getInstance().verifyPhoneNumber(
                            "+91" + inputmobile.getText().toString(),
                            60,
                            TimeUnit.SECONDS,
                            SendOTPActivity.this,
                            new PhoneAuthProvider.OnVerificationStateChangedCallbacks(){
                                @Override
                                public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                                    Log.d(TAG, "Verification completed successfully.");
                                    progressBar.setVisibility(View.GONE);
                                    getotp.setVisibility(View.VISIBLE);
                                }

                                @Override
                                public void onVerificationFailed(@NonNull FirebaseException e) {
                                    Log.e(TAG, "Verification failed: " + e.getMessage());
                                    progressBar.setVisibility(View.GONE);
                                    getotp.setVisibility(View.VISIBLE);
                                    Toast.makeText(SendOTPActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                                    Log.d(TAG, "Verification code sent successfully.");
                                    progressBar.setVisibility(View.GONE);
                                    getotp.setVisibility(View.VISIBLE);
                                    Intent intent = new Intent(getApplicationContext(), VerifyOTPActivity.class);
                                    intent.putExtra("mobile", inputmobile.getText().toString());
                                    intent.putExtra("verificationId", verificationId);
                                    startActivity(intent);

                                }
                            }
                    );
                }
            }
        });
    }
}