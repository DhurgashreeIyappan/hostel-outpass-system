package com.example.myapplication;

import android.graphics.Bitmap;
import android.widget.Button;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ViewStatusActivity extends AppCompatActivity {

    private static final String TAG = "ViewStatusActivity";
    private TextView nameTextView, rollNumberTextView, latestStatusTextView;
    private LinearLayout outpassContainer;
    private FirebaseFirestore db;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_status);

        db = FirebaseFirestore.getInstance();

        nameTextView = findViewById(R.id.nameTextView);
        rollNumberTextView = findViewById(R.id.rollNumberTextView);
        outpassContainer = findViewById(R.id.outpassContainer);
        latestStatusTextView = findViewById(R.id.statusTextView);

        userEmail = getIntent().getStringExtra("EMAIL");
        Log.d(TAG, "User email received: " + userEmail);

        if (userEmail != null) {
            fetchStudentDetails(userEmail);
        } else {
            Toast.makeText(this, "No email provided", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchStudentDetails(String email) {
        db.collection("users").document(email)
                .get()
                .addOnCompleteListener(studentTask -> {
                    if (studentTask.isSuccessful() && studentTask.getResult() != null) {
                        DocumentSnapshot studentDoc = studentTask.getResult();
                        if (studentDoc.exists()) {
                            String studentName = studentDoc.getString("name");
                            String rollNumber = studentDoc.getString("rollNumber");

                            nameTextView.setText("Name: " + (studentName != null ? studentName : "N/A"));
                            nameTextView.setTextColor(ContextCompat.getColor(this, android.R.color.black));

                            rollNumberTextView.setText("Roll Number: " + (rollNumber != null ? rollNumber : "N/A"));
                            rollNumberTextView.setTextColor(ContextCompat.getColor(this, android.R.color.black));

                            fetchOutpassRequests(email);
                        } else {
                            Toast.makeText(this, "Student not found.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Error fetching student details.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchOutpassRequests(String email) {
        db.collection("outpassRequests")
                .whereEqualTo("userEmail", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        outpassContainer.removeAllViews();

                        String latestAdvisorStatus = "No records found";
                        String latestWardenStatus = "No records found";

                        for (DocumentSnapshot requestDoc : task.getResult().getDocuments()) {
                            String outpassId = requestDoc.getId();
                            latestAdvisorStatus = requestDoc.getString("status") != null ? requestDoc.getString("status") : "N/A";
                            latestWardenStatus = requestDoc.getString("wstatus") != null ? requestDoc.getString("wstatus") : "N/A";
                            displayOutpassDetails(outpassId, requestDoc);
                        }

                        latestStatusTextView.setText("Advisor Status: " + latestAdvisorStatus + " | Warden Status: " + latestWardenStatus);
                        latestStatusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.black));
                    } else {
                        Toast.makeText(this, "No outpass records found.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void displayOutpassDetails(String outpassId, DocumentSnapshot document) {
        LinearLayout outpassBox = new LinearLayout(this);
        outpassBox.setOrientation(LinearLayout.HORIZONTAL);  // Set horizontal orientation
        outpassBox.setPadding(20, 20, 20, 20);
        outpassBox.setGravity(Gravity.CENTER_HORIZONTAL);

        // Left side for outpass details
        LinearLayout leftLayout = new LinearLayout(this);
        leftLayout.setOrientation(LinearLayout.VERTICAL);
        leftLayout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1)); // Use 1 weight

        String dateFrom = document.getString("dateFrom");
        String inTime = document.getString("inTime");  // Get inTime from document

        TextView detailsTextView = new TextView(this);
        String details = "Reason: " + (document.getString("reason") != null ? document.getString("reason") : "N/A") + "\n" +
                "Date From: " + (dateFrom != null ? dateFrom : "N/A") + "\n" +
                "Date To: " + (document.getString("dateTo") != null ? document.getString("dateTo") : "N/A") + "\n" +
                "Out Time: " + (document.getString("outTime") != null ? document.getString("outTime") : "N/A") + "\n" +
                "In Time: " + (inTime != null ? inTime : "N/A") + "\n" +
                "Advisor Status: " + (document.getString("status") != null ? document.getString("status") : "N/A") + "\n" +
                "Warden Status: " + (document.getString("wstatus") != null ? document.getString("wstatus") : "N/A");

        detailsTextView.setText(details);
        detailsTextView.setPadding(10, 10, 10, 10);
        detailsTextView.setTextColor(ContextCompat.getColor(this, android.R.color.black));

        leftLayout.addView(detailsTextView);

        // Right side for QR and status buttons
        LinearLayout rightLayout = new LinearLayout(this);
        rightLayout.setOrientation(LinearLayout.VERTICAL);
        rightLayout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1)); // Use 1 weight
        rightLayout.setGravity(Gravity.END);  // Align items to the right

        String securityStatus = document.getString("securityStatus");

        // Add security status or QR expired text if applicable
        if (isExpired(dateFrom, inTime)) {
            // Display "QR Expired"
            TextView expiredText = new TextView(this);
            expiredText.setText("QR Expired");
            expiredText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            expiredText.setGravity(Gravity.CENTER);
            rightLayout.addView(expiredText);
        } else if (securityStatus != null) {
            // Display security status
            TextView statusText = new TextView(this);
            statusText.setText("Security Status: " + securityStatus);
            statusText.setTextColor(securityStatus.equals("Accepted") ?
                    ContextCompat.getColor(this, android.R.color.holo_green_dark) :
                    ContextCompat.getColor(this, android.R.color.holo_red_dark));
            statusText.setGravity(Gravity.CENTER);
            rightLayout.addView(statusText);
        } else {
            // Show "View QR" button if unanswered
            Button viewQRButton = new Button(this);
            viewQRButton.setText("View QR");
            viewQRButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_blue_light));
            viewQRButton.setTextColor(ContextCompat.getColor(this, android.R.color.white));

            viewQRButton.setOnClickListener(v -> {
                // Check if QR is already displayed
                if (outpassBox.findViewWithTag("qrImageView") == null) {
                    Bitmap qrBitmap = generateQRCode(outpassId);
                    if (qrBitmap != null) {
                        ImageView qrImageView = new ImageView(this);
                        qrImageView.setImageBitmap(qrBitmap);
                        qrImageView.setTag("qrImageView"); // Set a tag to identify the QR image
                        rightLayout.addView(qrImageView);
                    }
                }
            });

            rightLayout.addView(viewQRButton);
        }

        // Add left and right layouts to the outpassBox
        outpassBox.addView(leftLayout);
        outpassBox.addView(rightLayout);

        // Add the outpass box to the main container
        outpassContainer.addView(outpassBox);
    }


    private Bitmap generateQRCode(String text) {
        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            return barcodeEncoder.encodeBitmap(text, BarcodeFormat.QR_CODE, 400, 400);
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean isExpired(String dateFrom, String inTime) {
        if (dateFrom == null) return false;

        // Normalize the date format to handle both dd-mm-yy and dd/mm/yy formats
        dateFrom = dateFrom.replace("-", "/"); // Convert `-` to `/` if necessary.

        // Try parsing with multiple date formats
        SimpleDateFormat sdf1 = new SimpleDateFormat("dd/MM/yy hh:mm a", Locale.getDefault());  // With time
        SimpleDateFormat sdf2 = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());  // Without time

        Date fromDate = null;

        // First try parsing with time format
        try {
            if (!dateFrom.contains("AM") && !dateFrom.contains("PM")) {
                // If no time is present, append a default time "12:00 AM"
                dateFrom = dateFrom + " 12:00 AM";
            }
            fromDate = sdf1.parse(dateFrom);  // Attempt parsing with time
        } catch (ParseException e) {
            // If parsing with time format fails, try without time
            try {
                fromDate = sdf2.parse(dateFrom);  // Attempt parsing without time
                if (fromDate != null) {
                    // If parsed without time, add time to it (e.g., start of the day)
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(fromDate);
                    calendar.set(Calendar.HOUR_OF_DAY, 0);  // Set time to midnight
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);  // Set milliseconds to 0
                    fromDate = calendar.getTime();
                }
            } catch (ParseException ex) {
                ex.printStackTrace();
                return false;  // Return false if both formats fail
            }
        }

        if (fromDate == null) return false;  // Return false if parsing fails

        // Now check if the parsed date has expired
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(fromDate);
        calendar.add(Calendar.DAY_OF_MONTH, 1);  // Expire after 1 day
        calendar.set(Calendar.HOUR_OF_DAY, 0);  // Set time to midnight
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);  // Zero out milliseconds

        return new Date().after(calendar.getTime());  // If current time is after the expiration time, return true (expired)
    }



}
