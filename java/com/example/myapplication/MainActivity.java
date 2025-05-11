package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private EditText emailEditText, passwordEditText;
    private Button loginButton, forgetPasswordButton;
    private LinearLayout selectionLayout;
    private Button wardenButton, advisorButton, securityButton;
    private String selectedEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        forgetPasswordButton = findViewById(R.id.forgetPasswordButton);
        selectionLayout = findViewById(R.id.selectionLayout);
        wardenButton = findViewById(R.id.wardenButton);
        advisorButton = findViewById(R.id.advisorButton);
        securityButton = findViewById(R.id.securityButton);

        loginButton.setOnClickListener(v -> handleLogin());
        forgetPasswordButton.setOnClickListener(v -> showForgotPasswordDialog());

        wardenButton.setOnClickListener(v -> navigateToActivity("warden", selectedEmail));
        advisorButton.setOnClickListener(v -> navigateToActivity("advisor", selectedEmail));
        securityButton.setOnClickListener(v -> navigateToActivity("security", selectedEmail));

        // Trigger migration
        migrateUsersToFirebaseAuth();
    }

    private void handleLogin() {
        String email = emailEditText.getText().toString().trim().toLowerCase();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showToast("Please enter email and password");
            return;
        }

        if (!email.endsWith("@kongu.edu")) {
            showToast("Invalid email domain. Use @kongu.edu");
            return;
        }

        // ✅ FIRST authenticate the user
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    // ✅ ONLY after successful login, check role
                    checkUserRole(email);
                })
                .addOnFailureListener(e -> {
                    showToast("Invalid username or password");
                });
    }


    private void checkUserRole(String email) {
        db.collection("security")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(securitySnapshot -> {
                    if (!securitySnapshot.isEmpty()) {
                        navigateToActivity("security", email);
                    } else {
                        checkWardenAndAdvisor(email);
                    }
                })
                .addOnFailureListener(e -> logError("Security check failed", e));
    }

    private void checkWardenAndAdvisor(String email) {
        db.collection("warden").whereEqualTo("email", email).get()
                .addOnSuccessListener(wardenSnap -> {
                    boolean isWarden = !wardenSnap.isEmpty();

                    db.collection("advisors").whereEqualTo("email", email).get()
                            .addOnSuccessListener(advisorSnap -> {
                                boolean isAdvisor = !advisorSnap.isEmpty();

                                if (isWarden && isAdvisor) {
                                    showSelectionLayout(email);
                                } else if (isWarden) {
                                    navigateToActivity("warden", email);
                                } else if (isAdvisor) {
                                    navigateToActivity("advisor", email);
                                } else {
                                    checkStudent(email);
                                }
                            });
                });
    }

    private void checkStudent(String email) {
        db.collection("studentemail").whereEqualTo("email", email).get()
                .addOnSuccessListener(studentSnap -> {
                    if (!studentSnap.isEmpty()) {
                        checkUserInfo(email);
                    } else {
                        showToast("Invalid email");
                    }
                });
    }

    private void checkUserInfo(String email) {
        db.collection("users").document(email).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        navigateToActivity("display_user", email);
                    } else {
                        navigateToActivity("user_info", email);
                    }
                });
    }

    private void showSelectionLayout(String email) {
        selectedEmail = email;
        selectionLayout.setVisibility(View.VISIBLE);
        loginButton.setVisibility(View.GONE);
        emailEditText.setVisibility(View.GONE);
        passwordEditText.setVisibility(View.GONE);
        wardenButton.setVisibility(View.VISIBLE);
        advisorButton.setVisibility(View.VISIBLE);
    }

    private void navigateToActivity(String role, String email) {
        Intent intent = null;
        switch (role) {
            case "warden":
                intent = new Intent(MainActivity.this, HostelInfoActivity.class);
                intent.putExtra("WARDEN_EMAIL", email);
                break;
            case "advisor":
                intent = new Intent(MainActivity.this, AdvisorInfoActivity.class);
                intent.putExtra("EMAIL", email);
                break;
            case "security":
                intent = new Intent(MainActivity.this, SecurityDashboardActivity.class);
                intent.putExtra("EMAIL", email);
                break;
            case "user_info":
                intent = new Intent(MainActivity.this, Userinfo.class);
                intent.putExtra("EMAIL", email);
                break;
            case "display_user":
                intent = new Intent(MainActivity.this, DisplayUserInfoActivity.class);
                intent.putExtra("EMAIL", email);
                break;
        }

        if (intent != null) startActivity(intent);
        else showToast("Navigation error");
    }

    private void showForgotPasswordDialog() {
        EditText input = new EditText(this);
        input.setHint("Enter your email");

        new android.app.AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setMessage("Enter your email to receive reset link")
                .setView(input)
                .setPositiveButton("Send", (dialog, which) -> {
                    String email = input.getText().toString().trim();
                    if (!email.isEmpty()) {
                        auth.sendPasswordResetEmail(email)
                                .addOnSuccessListener(unused -> showToast("Reset link sent"))
                                .addOnFailureListener(e -> showToast("Failed: " + e.getMessage()));
                    } else {
                        showToast("Please enter email");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void migrateUsersToFirebaseAuth() {
        migrateFromCollection("advisors");
        migrateFromCollection("warden");
        migrateFromCollection("studentemail");
        migrateFromCollection("security");
    }

    private void migrateFromCollection(String collectionName) {
        db.collection(collectionName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d("Migration", "No users found in " + collectionName);
                        return;
                    }

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String email = doc.getString("email");

                        if (email != null && !email.isEmpty()) {
                            String defaultPassword = "Default@123";

                            auth.createUserWithEmailAndPassword(email, defaultPassword)
                                    .addOnSuccessListener(authResult -> {
                                        Log.d("Migration", "User added from " + collectionName + ": " + email);
                                        removePasswordFromFirestore(collectionName, doc.getId());
                                    })
                                    .addOnFailureListener(e -> {
                                        if (e.getMessage().contains("already in use")) {
                                            Log.w("Migration", "Email already exists: " + email);
                                        } else {
                                            Log.e("Migration", "Error for " + email, e);
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("Migration", "Failed to fetch from " + collectionName, e));
    }

    private void removePasswordFromFirestore(String collection, String userId) {
        db.collection(collection).document(userId)
                .update("password", null)
                .addOnSuccessListener(aVoid -> Log.d("Cleanup", "Removed password for: " + userId))
                .addOnFailureListener(e -> Log.e("Cleanup", "Failed to remove password", e));
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void logError(String tag, Exception e) {
        Log.e("MainActivity", tag, e);
        showToast(tag + ": " + e.getMessage());
    }
}
