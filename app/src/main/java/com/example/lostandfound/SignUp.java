package com.example.lostandfound;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
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
import com.google.firebase.Firebase;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;


public class SignUp extends AppCompatActivity {

    private EditText etName, etEmail, etPassword, etPhone;

    CheckBox cbTerms;
    private Button btnSignUp;
    private ProgressBar pbIcon;

    private TextView tvLAlreadyHaveAccount;

    private FirebaseAuth fbAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_sign_up);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etPhone = findViewById(R.id.etPhone);
        pbIcon = findViewById(R.id.pbIcon);


        cbTerms = findViewById(R.id.cbTerms);

        btnSignUp = findViewById(R.id.btnSignUp);

        tvLAlreadyHaveAccount = findViewById(R.id.tvLAlreadyHaveAccount);

        fbAuth = FirebaseAuth.getInstance();

        tvLAlreadyHaveAccount.setOnClickListener(v -> {
            Intent intent = new Intent(SignUp.this, Login.class);
            startActivity(intent);
        });


        btnSignUp.setOnClickListener(v -> {

            String emailText = etEmail.getText().toString().trim();
            String passwordText = etPassword.getText().toString().trim();

            if (!cbTerms.isChecked()) {
                Toast.makeText(SignUp.this, "You must accept the Terms and Conditions.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(emailText) || TextUtils.isEmpty(passwordText)) {
                Toast.makeText(SignUp.this, "Please fill out Email and Password.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (passwordText.length() < 6) {
                Toast.makeText(SignUp.this, "Password must be at least 6 characters long.", Toast.LENGTH_SHORT).show();
                return;
            }

            pbIcon.setVisibility(View.VISIBLE);

           fbAuth.createUserWithEmailAndPassword(emailText, passwordText)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            pbIcon.setVisibility(View.GONE);
                            if (task.isSuccessful()) {
                                FirebaseUser user = fbAuth.getCurrentUser();

                                if (user != null) {
                                    Toast.makeText(SignUp.this, "Signup is successful, " + user.getEmail(), Toast.LENGTH_SHORT).show();

                                    FirebaseFirestore ub = FirebaseFirestore.getInstance();

                                    Map<String, Object> userMap = new HashMap<>();
                                    userMap.put("username", etName.getText().toString().trim());
                                    userMap.put("email", etEmail.getText().toString().trim());
                                    userMap.put("phone", etPhone.getText().toString().trim());
                                    userMap.put("uid", user.getUid());

                                    ub.collection("users").document(user.getUid()).set(userMap).addOnSuccessListener(aVoid -> {
                                                if (fbAuth.getCurrentUser() != null) {
                                                    Toast.makeText(SignUp.this, "Account created. Please log in.", Toast.LENGTH_SHORT).show();
                                                    startActivity(new Intent(SignUp.this, Login.class));
                                                    finish();
                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(SignUp.this, "Firestore error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });
                                }
                            } else {
                                Toast.makeText(SignUp.this, "Signup failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        });

    }
}