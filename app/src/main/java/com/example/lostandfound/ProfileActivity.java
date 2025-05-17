package com.example.lostandfound;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseAuth fbAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_profile);

        Log.d("ProfileActivity", "onCreate: Started");

        fbAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = fbAuth.getCurrentUser();
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("username");
                            Toast.makeText(ProfileActivity.this, "Welcome " + name, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ProfileActivity.this, "User info load failed.", Toast.LENGTH_SHORT).show();
                    });
        }

        ImageButton btnMore = findViewById(R.id.btnMore);
        btnMore.setOnClickListener(view -> {
            PopupMenu popupMenu = new PopupMenu(ProfileActivity.this, view);
            popupMenu.getMenuInflater().inflate(R.menu.menu_profile, popupMenu.getMenu());

            // Colorize the menu items (here, only one item: Logout)
            for (int i = 0; i < popupMenu.getMenu().size(); i++) {
                MenuItem menuItem = popupMenu.getMenu().getItem(i);
                SpannableString spanString = new SpannableString(menuItem.getTitle());
                spanString.setSpan(new ForegroundColorSpan(Color.parseColor("#1E82A1")), 0, spanString.length(), 0);
                menuItem.setTitle(spanString);
            }

            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.menu_logout) {
                    logout();
                    return true;
                }
                return false;
            });

            popupMenu.show();
        });
    }

    private void logout() {
        fbAuth.signOut();
        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(ProfileActivity.this, Login.class));
        finish();
    }
}
