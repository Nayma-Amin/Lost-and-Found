package com.example.lostandfound;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class HomePage extends AppCompatActivity {
    Button btnSignUp, btnLogin, btnGuest;

    TextView termsText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        btnSignUp = findViewById(R.id.btnSignUp);
        btnLogin = findViewById(R.id.btnLogin);
        btnGuest = findViewById(R.id.guestBtn);

        btnSignUp.setOnClickListener(v -> {
            startActivity(new Intent(HomePage.this, SignUp.class));
        });

        btnLogin.setOnClickListener(v -> {
            startActivity(new Intent(HomePage.this, Login.class));
        });

       btnGuest.setOnClickListener(v -> {
           startActivity(new Intent(HomePage.this, MainActivity.class));
       });

        termsText = findViewById(R.id.termsText);
        termsText.setOnClickListener(v -> {
            startActivity(new Intent(HomePage.this, TermsAndConditions.class));
        });

    }
}