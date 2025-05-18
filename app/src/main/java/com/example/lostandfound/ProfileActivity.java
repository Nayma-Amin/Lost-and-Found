package com.example.lostandfound;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
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
    private ImageButton menuIcon;
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
        menuIcon = findViewById(R.id.menu_icon);
        noPostText = findViewById(R.id.noPostText);
        requestContainer = findViewById(R.id.request_container);
        tabAll = findViewById(R.id.tabAll);
        tabLost = findViewById(R.id.tabLost);
        tabFound = findViewById(R.id.tabFound);

        editButton.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, EditProfileActivity.class));
        });

        menuIcon.setOnClickListener(this::showDropdownMenu);

        tabAll.setOnClickListener(v -> switchTab("all"));
        tabLost.setOnClickListener(v -> switchTab("lost"));
        tabFound.setOnClickListener(v -> switchTab("found"));
    }

    private void showDropdownMenu(View anchor) {
        View popupView = LayoutInflater.from(this).inflate(R.layout.dropdown_menu, null);
        int width = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                300,
                getResources().getDisplayMetrics()
        );

        PopupWindow popupWindow = new PopupWindow(
                popupView,
                width,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.showAsDropDown(anchor);

        popupView.findViewById(R.id.home_page).setOnClickListener(v -> {
            startActivity(new Intent(this, HomePage.class));
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.edit_profile).setOnClickListener(v -> {
            startActivity(new Intent(this, EditProfileActivity.class));
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.terms).setOnClickListener(v -> {
            startActivity(new Intent(this, TermsAndConditions.class));
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.how_to_use).setOnClickListener(v -> {
            startActivity(new Intent(this, HowToUseApp.class));
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.about_us).setOnClickListener(v -> {
            startActivity(new Intent(this, AboutUsActivity.class));
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.share_app).setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            String body = "Check out the app: https://play.google.com/store/apps/details?id=" + getPackageName();
            shareIntent.putExtra(Intent.EXTRA_TEXT, body);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.logout).setOnClickListener(v -> {
            fbAuth.signOut();
            startActivity(new Intent(this, Login.class));
            finish();
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.delete_account).setOnClickListener(v -> {
            Toast.makeText(this, "Delete account coming soon.", Toast.LENGTH_SHORT).show();
            popupWindow.dismiss();
        });
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
        if (!selectedTab.equals("all")) {
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
        textView.setText(TextUtils.isEmpty(value) ? "No data" : value);
    }

    private void showNoDataOnAllFields() {
        nameTextView.setText("No data");
        emailTextView.setText("No data");
        phoneTextView.setText("No data");
        locationTextView.setText("No data");
        profileImageView.setImageResource(R.drawable.profile);
    }
}
