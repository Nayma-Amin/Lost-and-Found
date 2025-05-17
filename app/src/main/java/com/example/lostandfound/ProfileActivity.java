package com.example.lostandfound;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseAuth fbAuth;
    private FirebaseFirestore db;

    private TextView nameTextView, emailTextView, phoneTextView, locationTextView;
    private AppCompatButton editButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_profile);

        fbAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        nameTextView = findViewById(R.id.nameEditText);
        emailTextView = findViewById(R.id.emailEditText);
        phoneTextView = findViewById(R.id.phoneEditText);
        locationTextView = findViewById(R.id.locationEditText);
        editButton = findViewById(R.id.editButton);

        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });

        ImageButton btnMore = findViewById(R.id.btnMore);
        btnMore.setOnClickListener(view -> {
            PopupMenu popupMenu = new PopupMenu(ProfileActivity.this, view);
            popupMenu.getMenuInflater().inflate(R.menu.menu_profile, popupMenu.getMenu());

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

    @Override
    protected void onResume() {
        super.onResume();
        loadUserData();  // Reload data when returning from EditProfileActivity
    }

    private void loadUserData() {
        FirebaseUser user = fbAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            nameTextView.setText(documentSnapshot.getString("username"));
                            emailTextView.setText(documentSnapshot.getString("email"));
                            phoneTextView.setText(documentSnapshot.getString("phone"));
                            locationTextView.setText(documentSnapshot.getString("location"));
                        } else {
                            Toast.makeText(ProfileActivity.this, "Profile not found.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(ProfileActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        }
    }

    private void logout() {
        fbAuth.signOut();
        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(ProfileActivity.this, Login.class));
        finish();
    }
}
