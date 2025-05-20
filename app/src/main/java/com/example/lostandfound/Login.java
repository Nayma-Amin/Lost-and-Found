package com.example.lostandfound;

import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Login extends AppCompatActivity {
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private ProgressBar pbIcon;

    private TextView tvNewHere;

    private FirebaseAuth fbAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        fbAuth = FirebaseAuth.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            startActivity(new Intent(Login.this, MainActivity.class));
                            finish();
                        } else {
                            checkIfInBinAndRestore(user.getUid());
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(Login.this, "Failed to check user data", Toast.LENGTH_SHORT).show();
                    });
        }


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
                    if (task.isSuccessful()) {
                        pbIcon.setVisibility(View.VISIBLE);

                        FirebaseUser user = fbAuth.getCurrentUser();
                        if (user == null) {
                            Toast.makeText(Login.this, "Login failed: user is null", Toast.LENGTH_SHORT).show();
                            pbIcon.setVisibility(View.GONE);
                            return;
                        }

                        String userId = user.getUid();
                        FirebaseFirestore db = FirebaseFirestore.getInstance();

                        db.collection("users").document(userId).get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    if (documentSnapshot.exists()) {
                                        com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                                                .addOnCompleteListener(tokenTask -> {
                                                    if (tokenTask.isSuccessful()) {
                                                        String fcmToken = tokenTask.getResult();
                                                        db.collection("users").document(userId)
                                                                .update("fcmToken", fcmToken)
                                                                .addOnFailureListener(e -> Toast.makeText(Login.this, "Failed to store FCM token", Toast.LENGTH_SHORT).show());
                                                    }
                                                });

                                        pbIcon.setVisibility(View.GONE);
                                        String username = documentSnapshot.getString("username");
                                        Toast.makeText(Login.this, "Welcome " + username, Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(Login.this, MainActivity.class));
                                        finish();

                                    } else {
                                        checkIfInBinAndRestore(userId);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    pbIcon.setVisibility(View.GONE);
                                    Toast.makeText(Login.this, "Firestore error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });

                    } else {
                        pbIcon.setVisibility(View.GONE);
                        Toast.makeText(Login.this, "Login failed: " +
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkIfInBinAndRestore(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("bin").document(userId).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        db.collection("users").document(userId)
                                .set(snapshot.getData())
                                .addOnSuccessListener(aVoid -> {
                                    db.collection("bin").document(userId).delete();
                                    Log.d("Login", "User restored from bin");

                                    Toast.makeText(Login.this, "Welcome back!", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(Login.this, MainActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("Login", "Failed to restore user from bin", e);
                                    Toast.makeText(Login.this, "Error restoring user", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(Login.this, "User not found", Toast.LENGTH_SHORT).show();
                        Log.d("Login", "User not found in 'users' or 'bin'");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Login", "Failed to check 'bin' collection", e);
                });
    }
}