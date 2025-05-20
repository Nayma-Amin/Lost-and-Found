package com.example.lostandfound;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomePage extends AppCompatActivity {
    Button btnSignUp, btnLogin, btnGuest;

    TextView termsText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            startActivity(new Intent(HomePage.this, MainActivity.class));
                            finish();
                        } else {
                           }
                    });
            return;
        }

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "default_channel_id",
                    "General Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Used for general notifications");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

    }

}