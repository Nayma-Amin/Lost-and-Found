package com.example.lostandfound;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class Login extends AppCompatActivity {
    EditText etEmail, etPassword;
    Button btnLogin;
    ProgressBar pbIcon;

    TextView tvNewHere;

    FirebaseAuth fbAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

       fbAuth = FirebaseAuth.getInstance();

        btnLogin = findViewById(R.id.btnLogin);

        pbIcon = findViewById(R.id.pbIcon);

        tvNewHere = findViewById(R.id.loginTextView);

        btnLogin.setOnClickListener(v -> {

            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(Login.this, "Email or Password cannot be empty.", Toast.LENGTH_SHORT).show();

            } else {
                pbIcon.setVisibility(View.VISIBLE);
                loginUser(email, password);
            }
        });

        tvNewHere.setOnClickListener(v -> {
            Intent intent = new Intent(Login.this, SignUp.class);
            startActivity(intent);
        });

    }


    private void loginUser(String email, String password) {
        fbAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(Login.this, task -> {
                    pbIcon.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        FirebaseUser user = fbAuth.getCurrentUser();
                        if (user != null) {
                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            String userId = user.getUid();

                            db.collection("users").document(userId).get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        if (documentSnapshot.exists()) {
                                            String username = documentSnapshot.getString("username");

                                            Toast.makeText(Login.this, "Welcome " + username, Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(Login.this, MainActivity.class);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            Toast.makeText(Login.this, "User profile not found in Firestore.", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(Login.this, "Firestore error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    } else {
                        Toast.makeText(Login.this, "Login failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    }
