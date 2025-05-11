package com.example.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import androidx.core.content.ContextCompat;

public class SecurityDashboardActivity extends AppCompatActivity {

    private Button scanQRButton, acceptButton, rejectButton, submitButton;
    private TextView outpassDetailsTextView;
    private EditText gateNumberEditText;
    private FirebaseFirestore db;
    private String scannedOutpassId;
    private String securityStatus = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security_dashboard);

        scanQRButton = findViewById(R.id.scanQRButton);
        acceptButton = findViewById(R.id.acceptButton);
        rejectButton = findViewById(R.id.rejectButton);
        submitButton = findViewById(R.id.submitButton);
        outpassDetailsTextView = findViewById(R.id.outpassDetailsTextView);
        gateNumberEditText = findViewById(R.id.gateNumberEditText);
        db = FirebaseFirestore.getInstance();

        scanQRButton.setOnClickListener(v -> scanQRCode());
        acceptButton.setOnClickListener(v -> onAcceptClicked());
        rejectButton.setOnClickListener(v -> onRejectClicked());
        submitButton.setOnClickListener(v -> submitOutpassDetails());

        // Initially show only the scan button
        setUIVisibility(false);
    }

    private void scanQRCode() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan a QR Code");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(CustomScannerActivity.class);
        qrCodeLauncher.launch(options);
    }

    private final androidx.activity.result.ActivityResultLauncher<ScanOptions> qrCodeLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    scannedOutpassId = result.getContents();
                    fetchOutpassDetails(scannedOutpassId);
                    setUIVisibility(true);  // Show all fields after scanning
                    scanQRButton.setEnabled(false);
                } else {
                    Toast.makeText(this, "Scan Canceled", Toast.LENGTH_SHORT).show();
                }
            });

    private void fetchOutpassDetails(String outpassId) {
        db.collection("outpassRequests").document(outpassId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String details = formatOutpassDetails(documentSnapshot);
                        outpassDetailsTextView.setText(details);
                    } else {
                        Toast.makeText(this, "No Outpass Found!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Log.e("QRScanner", "Error fetching details", e));
    }

    private String formatOutpassDetails(DocumentSnapshot document) {
        return "Student Name: " + document.getString("name") + "\n" +
                "Roll No: " + document.getString("rollNumber") + "\n" +
                "Reason: " + document.getString("reason") + "\n" +
                "Date From: " + document.getString("dateFrom") + "\n" +
                "Date To: " + document.getString("dateTo") + "\n" +
                "Out Time: " + document.getString("outTime") + "\n" +
                "In Time: " + document.getString("inTime") + "\n" +
                "Advisor Status: " + document.getString("status") + "\n" +
                "Warden Status: " + document.getString("wstatus");
    }

    private void onAcceptClicked() {
        acceptButton.setEnabled(false);
        acceptButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        acceptButton.setTextColor(ContextCompat.getColor(this, android.R.color.black));

        rejectButton.setEnabled(true);
        rejectButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        rejectButton.setTextColor(ContextCompat.getColor(this, android.R.color.white));

        submitButton.setEnabled(true);
        securityStatus = "Accepted";
    }

    private void onRejectClicked() {
        rejectButton.setEnabled(false);
        acceptButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        acceptButton.setTextColor(ContextCompat.getColor(this, android.R.color.black));

        acceptButton.setEnabled(true);
        acceptButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        acceptButton.setTextColor(ContextCompat.getColor(this, android.R.color.white));

        submitButton.setEnabled(true);
        securityStatus = "Rejected";
    }

    private void submitOutpassDetails() {
        if (scannedOutpassId == null || securityStatus.isEmpty()) {
            Toast.makeText(this, "No Outpass scanned or status not selected", Toast.LENGTH_SHORT).show();
            return;
        }

        String gateNumber = gateNumberEditText.getText().toString().trim();
        if (gateNumber.isEmpty()) {
            Toast.makeText(this, "Please enter a gate number", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("outpassRequests").document(scannedOutpassId)
                .update("gateNumber", gateNumber, "securityStatus", securityStatus)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Updated Successfully", Toast.LENGTH_SHORT).show();
                    resetUI();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Update Failed", Toast.LENGTH_SHORT).show());
    }

    private void setUIVisibility(boolean showDetails) {
        if (showDetails) {
            acceptButton.setVisibility(View.VISIBLE);
            rejectButton.setVisibility(View.VISIBLE);
            submitButton.setVisibility(View.VISIBLE);
            outpassDetailsTextView.setVisibility(View.VISIBLE);
            gateNumberEditText.setVisibility(View.VISIBLE);
        } else {
            acceptButton.setVisibility(View.GONE);
            rejectButton.setVisibility(View.GONE);
            submitButton.setVisibility(View.GONE);
            outpassDetailsTextView.setVisibility(View.GONE);
            gateNumberEditText.setVisibility(View.GONE);
            scanQRButton.setEnabled(true);
        }
    }

    private void resetUI() {
        scanQRButton.setEnabled(true);
        scanQRButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark));
        scanQRButton.setTextColor(ContextCompat.getColor(this, android.R.color.white));

        outpassDetailsTextView.setText("");
        gateNumberEditText.setText("");
        securityStatus = "";

        setUIVisibility(false);
    }
}
