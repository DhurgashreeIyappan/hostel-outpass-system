package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Userinfo extends AppCompatActivity {

    private EditText nameEditText, rollNumberEditText, classEditText, roomNumberEditText;
    private Spinner advisorSpinner, wardenSpinner;
    private Button saveButton;
    private FirebaseFirestore db;
    private String userEmail;
    private List<String> advisorEmails, wardenEmails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_userinfo);

        db = FirebaseFirestore.getInstance();
        nameEditText = findViewById(R.id.nameEditText);
        rollNumberEditText = findViewById(R.id.rollNumberEditText);
        classEditText = findViewById(R.id.classEditText);
        roomNumberEditText = findViewById(R.id.roomNumberEditText);
        advisorSpinner = findViewById(R.id.advisorSpinner);
        wardenSpinner = findViewById(R.id.wardenSpinner);
        saveButton = findViewById(R.id.saveButton);

        userEmail = getIntent().getStringExtra("EMAIL");
        advisorEmails = new ArrayList<>();
        wardenEmails = new ArrayList<>();

        fetchAdvisors();
        fetchWardens();

        saveButton.setOnClickListener(v -> saveUserInfo());
    }

    private void fetchAdvisors() {
        advisorEmails.add("Select advisor name and email");
        db.collection("advisors")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String name = document.getString("name");
                        String email = document.getString("email");
                        if (name != null && email != null) {
                            String formattedAdvisor = name + " (" + email + ")";
                            advisorEmails.add(formattedAdvisor);
                        }
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_item, advisorEmails);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    advisorSpinner.setAdapter(adapter);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Userinfo.this, "Error fetching advisors: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchWardens() {
        wardenEmails.add("Select warden name and email");
        db.collection("warden")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String name = document.getString("name");
                        String email = document.getString("email");
                        if (name != null && email != null) {
                            String formattedWarden = name + " (" + email + ")";
                            wardenEmails.add(formattedWarden);
                        }
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_item, wardenEmails);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    wardenSpinner.setAdapter(adapter);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Userinfo.this, "Error fetching wardens: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveUserInfo() {
        String name = nameEditText.getText().toString().trim();
        String rollNumber = rollNumberEditText.getText().toString().trim();
        String userClass = classEditText.getText().toString().trim();
        String roomNumber = roomNumberEditText.getText().toString().trim();
        String selectedAdvisorEmail = advisorSpinner.getSelectedItem().toString();
        String selectedWardenEmail = wardenSpinner.getSelectedItem().toString();

        if (name.isEmpty() || rollNumber.isEmpty() || userClass.isEmpty() || roomNumber.isEmpty() ||
                selectedAdvisorEmail.equals("Select advisor name and email") ||
                selectedWardenEmail.equals("Select warden name and email")) {
            Toast.makeText(Userinfo.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Extract emails
        String selectedEmail = selectedAdvisorEmail.substring(selectedAdvisorEmail.indexOf("(") + 1, selectedAdvisorEmail.indexOf(")"));
        String selectedWarden = selectedWardenEmail.substring(selectedWardenEmail.indexOf("(") + 1, selectedWardenEmail.indexOf(")"));


        // Create a map to hold user data
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("name", name);
        userInfo.put("userClass", userClass);
        userInfo.put("rollNumber", rollNumber);
        userInfo.put("Hostel name and roomNumber", roomNumber);
        userInfo.put("email", userEmail);
        userInfo.put("advisorEmail", selectedEmail);
        userInfo.put("wardenEmail", selectedWarden); // Store the selected warden's email

        db.collection("users").document(userEmail)
                .set(userInfo)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(Userinfo.this, "User info saved successfully", Toast.LENGTH_SHORT).show();
                    navigateToDisplayUserInfo();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Userinfo.this, "Error saving user info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToDisplayUserInfo() {
        Intent intent = new Intent(Userinfo.this, DisplayUserInfoActivity.class);
        intent.putExtra("EMAIL", userEmail);
        startActivity(intent);
        finish();
    }
}
