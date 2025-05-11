package com.example.myapplication;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class AdvisorInfoActivity extends AppCompatActivity implements OutpassRequestAdapter.UpdateRequestStatusCallback {

    private ListView requestListView;
    private FirebaseFirestore db;
    private String advisorEmail;
    private List<OutpassRequest> requestList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advisor_info);

        requestListView = findViewById(R.id.requestListView);
        db = FirebaseFirestore.getInstance();

        // Get the advisor's email from the Intent
        advisorEmail = getIntent().getStringExtra("EMAIL");
        if (advisorEmail == null) {
            Toast.makeText(this, "Invalid advisor email", Toast.LENGTH_SHORT).show();
            finish(); // End activity if email is invalid
            return;
        }

        requestList = new ArrayList<>();
        fetchPendingRequests(); // Fetch requests for the advisor
    }

    private void fetchPendingRequests() {
        db.collection("outpassRequests")
                .whereEqualTo("advisorEmail", advisorEmail)
                .whereEqualTo("status", "pending")
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
                        // Show a message indicating there are no pending requests
                        Toast.makeText(AdvisorInfoActivity.this, "No pending requests", Toast.LENGTH_SHORT).show();
                        requestList.clear(); // Clear the list to ensure it's empty
                        updateRequestList(); // Update the ListView to reflect no requests
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AdvisorInfoActivity.this, "Error fetching requests: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateRequestList() {
        OutpassRequestAdapter adapter = new OutpassRequestAdapter(this, requestList, this);
        requestListView.setAdapter(adapter); // Set the adapter to the ListView
    }

    @Override
    public void updateRequestStatus(String requestId, String newStatus) {
        db.collection("outpassRequests").document(requestId)
                .update("status", newStatus) // Set status based on advisor's decision
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AdvisorInfoActivity.this, "Request " + newStatus, Toast.LENGTH_SHORT).show();
                    fetchPendingRequests(); // Refresh the list of requests
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AdvisorInfoActivity.this, "Error updating request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}