package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class DisplayUserInfoActivity extends AppCompatActivity {

    private TextView nameTextView, rollNumberTextView;
    private Button backButton, newOutpassButton, viewStatusButton;
    private FirebaseFirestore db;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_user_info);

        db = FirebaseFirestore.getInstance();
        nameTextView = findViewById(R.id.nameTextView);
        rollNumberTextView = findViewById(R.id.rollNumberTextView);
        backButton = findViewById(R.id.backButton);
        newOutpassButton = findViewById(R.id.newOutpassButton);
        viewStatusButton = findViewById(R.id.viewStatusButton);

        userEmail = getIntent().getStringExtra("EMAIL");
        fetchUserInfo(userEmail);

        backButton.setOnClickListener(v -> {
            startActivity(new Intent(DisplayUserInfoActivity.this, MainActivity.class));
            finish();
        });

        newOutpassButton.setOnClickListener(v -> {
            Intent intent = new Intent(DisplayUserInfoActivity.this, NewOutpassActivity.class);
            intent.putExtra("EMAIL", userEmail);
            startActivity(intent);
        });

        viewStatusButton.setOnClickListener(v -> {
            Intent intent = new Intent(DisplayUserInfoActivity.this, ViewStatusActivity.class);
            intent.putExtra("EMAIL", userEmail);
            startActivity(intent);
        });
    }

    private void fetchUserInfo(String email) {
        db.collection("users")
                .document(email)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String rollNumber = documentSnapshot.getString("rollNumber");

                        nameTextView.setText("Name: " + name);
                        rollNumberTextView.setText("Roll Number: " + rollNumber);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(DisplayUserInfoActivity.this, "Error fetching data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
