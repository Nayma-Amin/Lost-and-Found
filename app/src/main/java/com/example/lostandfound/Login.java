package com.example.lostandfound;

import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.text.TextUtils;
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

import java.util.Objects;

public class Login extends AppCompatActivity {
    EditText etEmail, etPassword;
    Button btnLogin;
    ProgressBar pbIcon;

    TextView tvNewHere;

    ImageView ivGoogleLogin;
    private static final int RC_SIGN_IN = 1001;
    private GoogleSignInClient googleSignInClient;

    FirebaseAuth fbAuth;




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
            checkIfInBinAndRestore(user.getUid());
        }

        btnLogin = findViewById(R.id.btnLogin);

        pbIcon = findViewById(R.id.pbIcon);

        tvNewHere = findViewById(R.id.loginTextView);

        ivGoogleLogin = findViewById(R.id.ivGoogleLogin);


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

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // from google-services.json
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        ivGoogleLogin.setOnClickListener(v -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Google Sign-in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        fbAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = fbAuth.getCurrentUser();
                        if (user != null) {
                            // Optionally restore from bin if you want
                            checkIfInBinAndRestore(user.getUid());

                            // Create or update user in Firestore
                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            String userId = user.getUid();
                            String username = user.getDisplayName();

                            db.collection("users").document(userId)
                                    .set(new User(username, user.getEmail()))
                                    .addOnSuccessListener(unused -> {
                                        Toast.makeText(this, "Welcome " + username, Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(this, MainActivity.class));
                                        finish();
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(this, "Firestore Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        Toast.makeText(this, "Firebase Authentication failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }



    private void checkIfInBinAndRestore(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("bin").document(userId).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                db.collection("users").document(userId).set(snapshot.getData()).addOnSuccessListener(unused -> {
                    db.collection("bin").document(userId).delete();
                });
            }
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

                            com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                                    .addOnCompleteListener(tokenTask -> {
                                        if (tokenTask.isSuccessful()) {
                                            String fcmToken = tokenTask.getResult();

                                            db.collection("users").document(userId)
                                                    .update("fcmToken", fcmToken)
                                                    .addOnSuccessListener(aVoid -> {
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Toast.makeText(Login.this, "Failed to store FCM token: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                    });
                                        } else {
                                            Toast.makeText(Login.this, "Fetching FCM token failed", Toast.LENGTH_SHORT).show();
                                        }
                                    });

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
