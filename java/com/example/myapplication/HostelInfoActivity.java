package com.example.myapplication;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HostelInfoActivity extends AppCompatActivity implements AcceptedRequestAdapter.RequestActionCallback {

    private ListView requestListView;
    private FirebaseFirestore db;
    private String wardenEmail;
    private List<OutpassRequest> requestList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hostel_info);

        requestListView = findViewById(R.id.acceptedRequestListView);  // Update to match the XML ID

        db = FirebaseFirestore.getInstance();

        // Get the warden's email from the Intent
        wardenEmail = getIntent().getStringExtra("WARDEN_EMAIL"); // Use the correct key
        if (wardenEmail == null) {
            Toast.makeText(this, "Invalid warden email", Toast.LENGTH_SHORT).show();
            finish(); // End activity if email is invalid
            return;
        }

        requestList = new ArrayList<>();
        fetchPendingRequests(); // Fetch requests for the warden
    }

    private void fetchPendingRequests() {
        db.collection("outpassRequests")
                .whereEqualTo("wstatus", "pending")  // Fetch only requests that the warden needs to review
                .whereEqualTo("status", "accepted")  // Ensure the request was accepted by the advisor
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        requestList.clear(); // Clear previous requests
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            OutpassRequest request = document.toObject(OutpassRequest.class);
                            requestList.add(request); // Add fetched request to the list
                        }
                        updateRequestList(); // Update the ListView
                    } else {
                        Toast.makeText(HostelInfoActivity.this, "No pending requests", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(HostelInfoActivity.this, "Error fetching requests: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateRequestList() {
        AcceptedRequestAdapter adapter = new AcceptedRequestAdapter(this, requestList, this);
        requestListView.setAdapter(adapter); // Set the adapter to the ListView
    }

    @Override
    public void onRequestAction(String requestId, String wstatus) {
        // Update the request status in Firestore
        db.collection("outpassRequests").document(requestId)
                .update("wstatus", wstatus)  // Update warden's decision (accepted/rejected)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(HostelInfoActivity.this, "Request " + wstatus, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(HostelInfoActivity.this, "Error updating request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void updateRequestStatus(String requestId, String wstatus) {
        // This is an additional method to update the request in Firestore as needed.
        // You can decide whether you want to merge this functionality with `onRequestAction` or keep it separate.
    }
}
