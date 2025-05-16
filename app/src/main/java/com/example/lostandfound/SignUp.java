package com.example.lostandfound;

import android.content.Intent;
import android.os.Bundle;
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
import com.google.firebase.Firebase;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;


public class SignUp extends AppCompatActivity {

    EditText etName, etEmail, etPassword, etPhone;
    Button btnSignUp;
    ProgressBar pbIcon;

    TextView tvLAlreadyHaveAccount;

    FirebaseAuth fbAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_sign_up);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etPhone = findViewById(R.id.etPhone);

        btnSignUp = findViewById(R.id.btnSignUp);

        tvLAlreadyHaveAccount = findViewById(R.id.tvLAlreadyHaveAccount);
        pbIcon = findViewById(R.id.pbIcon);

        fbAuth = FirebaseAuth.getInstance();


        if(fbAuth.getCurrentUser() != null){
            Intent i = new Intent(this, Login.class);
            startActivity(i);
        }

        tvLAlreadyHaveAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(SignUp.this, Login.class);
                startActivity(i);
            }
        });


        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = etName.getText().toString().trim();
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();
                String phone = etPhone.getText().toString().trim();

                if (name.isEmpty()) {
                    etName.setError("Name is required");
                    return;
                }

                if (email.isEmpty()) {
                    etEmail.setError("Email is required");
                    return;
                }

                if (password.isEmpty()) {
                    etPassword.setError("Password is required");
                    return;
                }

                if (password.length() < 6) {
                    etPassword.setError("Password must be at least 6 characters");
                    return;
                }

                if (phone.isEmpty()) {
                    etPhone.setError("Phone number is required");
                    return;
                }

                pbIcon.setVisibility(ProgressBar.VISIBLE);

                fbAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        pbIcon.setVisibility(ProgressBar.GONE);

                        if (task.isSuccessful()) {
                            Toast.makeText(SignUp.this, "SignUp Successfully", Toast.LENGTH_SHORT).show();

                            Intent i = new Intent(SignUp.this, Login.class);
                            startActivity(i);

                        } else {
                            Toast.makeText(SignUp.this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });


            }
        });

    }
}















