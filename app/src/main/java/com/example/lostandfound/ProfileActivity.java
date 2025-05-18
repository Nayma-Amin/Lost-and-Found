package com.example.lostandfound;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseAuth fbAuth;
    private FirebaseFirestore db;

    private TextView nameTextView, emailTextView, phoneTextView, locationTextView, noPostText;
    private AppCompatButton editButton;
    private ImageView profileImageView;
    private ImageButton btnMore;
    private LinearLayout requestContainer;
    private Button tabAll, tabLost, tabFound;
    private String selectedTab = "all";

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
        profileImageView = findViewById(R.id.image);
        editButton = findViewById(R.id.editButton);
        btnMore = findViewById(R.id.btnMore);
        noPostText = findViewById(R.id.noPostText);
        requestContainer = findViewById(R.id.request_container);
        tabAll = findViewById(R.id.tabAll);
        tabLost = findViewById(R.id.tabLost);
        tabFound = findViewById(R.id.tabFound);

        editButton.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, EditProfileActivity.class));
        });

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

        tabAll.setOnClickListener(v -> switchTab("all"));
        tabLost.setOnClickListener(v -> switchTab("lost"));
        tabFound.setOnClickListener(v -> switchTab("found"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserData();
        switchTab("all");
    }

    private void switchTab(String tab) {
        selectedTab = tab;

        tabAll.setBackgroundTintList(getColorStateList(tab.equals("all") ? R.color.teal_700 : R.color.grey));
        tabLost.setBackgroundTintList(getColorStateList(tab.equals("lost") ? R.color.teal_700 : R.color.grey));
        tabFound.setBackgroundTintList(getColorStateList(tab.equals("found") ? R.color.teal_700 : R.color.grey));

        tabAll.setTextColor(getColor(tab.equals("all") ? R.color.white : R.color.black));
        tabLost.setTextColor(getColor(tab.equals("lost") ? R.color.white : R.color.black));
        tabFound.setTextColor(getColor(tab.equals("found") ? R.color.white : R.color.black));

        loadUserPosts();
    }

    private void loadUserData() {
        FirebaseUser user = fbAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            setTextOrNoData(nameTextView, documentSnapshot.getString("username"));
                            setTextOrNoData(emailTextView, documentSnapshot.getString("email"));
                            setTextOrNoData(phoneTextView, documentSnapshot.getString("phone"));
                            setTextOrNoData(locationTextView, documentSnapshot.getString("location"));

                            String imageUrl = documentSnapshot.getString("profileImage");
                            if (!TextUtils.isEmpty(imageUrl)) {
                                Glide.with(this)
                                        .load(imageUrl)
                                        .apply(new RequestOptions().centerCrop())
                                        .placeholder(R.drawable.profile)
                                        .error(R.drawable.profile)
                                        .into(profileImageView);
                            } else {
                                profileImageView.setImageResource(R.drawable.profile);
                            }
                        } else {
                            showNoDataOnAllFields();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error loading profile: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        }
    }

    private void loadUserPosts() {
        FirebaseUser user = fbAuth.getCurrentUser();
        if (user == null) return;

        requestContainer.removeAllViews();
        noPostText.setVisibility(View.GONE);

        Query query = db.collection("posts").whereEqualTo("userId", user.getUid());
        if (selectedTab.equals("lost") || selectedTab.equals("found")) {
            query = query.whereEqualTo("type", selectedTab);
        }

        query.get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        noPostText.setVisibility(View.VISIBLE);
                    } else {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            String title = doc.getString("title");
                            if (!TextUtils.isEmpty(title)) {
                                TextView postView = new TextView(this);
                                postView.setText("- " + title);
                                postView.setPadding(12, 8, 12, 8);
                                postView.setTextSize(16);
                                postView.setTextColor(getColor(R.color.black));
                                postView.setGravity(Gravity.START);
                                requestContainer.addView(postView);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load posts", Toast.LENGTH_SHORT).show();
                    noPostText.setVisibility(View.VISIBLE);
                });
    }

    private void setTextOrNoData(TextView textView, String value) {
        if (TextUtils.isEmpty(value)) {
            textView.setText("No data");
        } else {
            textView.setText(value);
        }
    }

    private void showNoDataOnAllFields() {
        nameTextView.setText("No data");
        emailTextView.setText("No data");
        phoneTextView.setText("No data");
        locationTextView.setText("No data");
        profileImageView.setImageResource(R.drawable.profile);
    }

    private void logout() {
        fbAuth.signOut();
        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(ProfileActivity.this, Login.class));
        finish();
    }
}
