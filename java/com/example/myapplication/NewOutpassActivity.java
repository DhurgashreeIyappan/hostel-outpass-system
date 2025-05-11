package com.example.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;

public class NewOutpassActivity extends AppCompatActivity {

    private static final String TAG = "NewOutpassActivity";

    // Firestore database instance
    private FirebaseFirestore db;

    // UI components
    private EditText reasonEditText, dateFromEditText, dateToEditText, outTimeEditText, inTimeEditText;
    private TextView nameTextView, rollNumberTextView;
    private Button submitButton;

    // User email
    private String userEmail;

    // Advisor and warden emails
    private String advisorEmail = "";
    private String wardenEmail = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_outpass);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        reasonEditText = findViewById(R.id.reasonEditText);
        dateFromEditText = findViewById(R.id.dateFromEditText);
        dateToEditText = findViewById(R.id.dateToEditText);
        outTimeEditText = findViewById(R.id.outTimeEditText);
        inTimeEditText = findViewById(R.id.inTimeEditText);
        nameTextView = findViewById(R.id.nameTextView);
        rollNumberTextView = findViewById(R.id.rollNumberTextView);
        submitButton = findViewById(R.id.submitButton);

        // Get user email from intent
        userEmail = getIntent().getStringExtra("EMAIL");
        if (userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(this, "User email not provided", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "User email: " + userEmail);

        // Fetch and display user details
        fetchUserDetails(userEmail);

        // Set click listener for the submit button
        submitButton.setOnClickListener(view -> {
            if (advisorEmail.isEmpty() || wardenEmail.isEmpty()) {
                Toast.makeText(this, "Advisor or Warden email not available", Toast.LENGTH_SHORT).show();
                return;
            }

            String name = nameTextView.getText().toString().replace("Name: ", "");
            String rollNumber = rollNumberTextView.getText().toString().replace("Roll Number: ", "");

            // Submit the outpass request
            submitOutpassRequest(name, rollNumber);
        });
    }

    // Fetch user details from Firestore
    private void fetchUserDetails(String email) {
        db.collection("users").document(email)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        String name = userDoc.getString("name");
                        String rollNumber = userDoc.getString("rollNumber");
                        advisorEmail = userDoc.getString("advisorEmail");
                        wardenEmail = userDoc.getString("wardenEmail");

                        // Display fetched details
                        nameTextView.setText("Name: " + name);
                        rollNumberTextView.setText("Roll Number: " + rollNumber);

                        Log.d(TAG, "User details fetched successfully");
                        Log.d(TAG, "Advisor Email: " + advisorEmail);
                        Log.d(TAG, "Warden Email: " + wardenEmail);
                    } else {
                        Log.e(TAG, "User document does not exist");
                        Toast.makeText(NewOutpassActivity.this, "User not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user details: ", e);
                    Toast.makeText(NewOutpassActivity.this, "Error fetching user details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Submit the outpass request
    private void submitOutpassRequest(String name, String rollNumber) {
        String reason = reasonEditText.getText().toString().trim();
        String dateFrom = dateFromEditText.getText().toString().trim();
        String dateTo = dateToEditText.getText().toString().trim();
        String outTime = outTimeEditText.getText().toString().trim();
        String inTime = inTimeEditText.getText().toString().trim();

        // Validate input fields
        if (reason.isEmpty() || dateFrom.isEmpty() || dateTo.isEmpty() || outTime.isEmpty() || inTime.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generate a unique request ID
        String requestId = userEmail + "_" + System.currentTimeMillis();

        // Create an OutpassRequest object
        OutpassRequest outpassRequest = new OutpassRequest();
        outpassRequest.setRequestId(requestId);
        outpassRequest.setUserEmail(userEmail);
        outpassRequest.setReason(reason);
        outpassRequest.setDateFrom(dateFrom);
        outpassRequest.setDateTo(dateTo);
        outpassRequest.setOutTime(outTime);
        outpassRequest.setInTime(inTime);
        outpassRequest.setAdvisorEmail(advisorEmail);
        outpassRequest.setWardenEmail(wardenEmail);
        outpassRequest.setStatus("pending");
        outpassRequest.setName(name);
        outpassRequest.setRollNumber(rollNumber);
        outpassRequest.setTimestamp(System.currentTimeMillis());
        outpassRequest.setWstatus("pending");

        // Save the request to Firestore
        db.collection("outpassRequests").document(requestId).set(outpassRequest)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Outpass request submitted successfully", Toast.LENGTH_SHORT).show();
                    finish(); // Close the activity
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error submitting request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error submitting request: ", e);
                });
    }
}
