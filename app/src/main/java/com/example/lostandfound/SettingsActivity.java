package com.example.lostandfound;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

public class SettingsActivity extends AppCompatActivity {

    private ImageButton menuIcon, settingsIcon;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText contactPrivacy = findViewById(R.id.contact_privacy);
        ImageButton dropContact = findViewById(R.id.drop_button);
        EditText locationPrivacy = findViewById(R.id.location_privacy);
        ImageButton dropLocation = findViewById(R.id.dropButton);

        dropContact.setOnClickListener(v -> showPrivacyDropdown(v, contactPrivacy, "SecureContact"));
        contactPrivacy.setOnClickListener(v -> showPrivacyDropdown(v, contactPrivacy, "SecureContact"));

        dropLocation.setOnClickListener(v -> showPrivacyDropdown(v, locationPrivacy, "SecureLocation"));
        locationPrivacy.setOnClickListener(v -> showPrivacyDropdown(v, locationPrivacy, "SecureLocation"));

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore.getInstance().collection("users")
                    .document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String contactSetting = documentSnapshot.getString("SecureContact");
                            String locationSetting = documentSnapshot.getString("SecureLocation");

                            if (contactSetting != null) {
                                contactPrivacy.setText(contactSetting);
                            }

                            if (locationSetting != null) {
                                locationPrivacy.setText(locationSetting);
                            }
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to load privacy settings", Toast.LENGTH_SHORT).show());
        }

        findViewById(R.id.delete_account).setOnClickListener(v -> showDeleteAccountPopup());
        findViewById(R.id.delete_button).setOnClickListener(v -> showDeleteAccountPopup());

        menuIcon = findViewById(R.id.menu_icon);
        menuIcon.setOnClickListener(this::showDropdownMenu);

        settingsIcon = findViewById(R.id.settings_icon);
        settingsIcon.setOnClickListener(v -> {
                Toast.makeText(this, "You are already in Settings!", Toast.LENGTH_SHORT).show();
            });

    }

    private void showPrivacyDropdown(View anchor, EditText targetEditText, String firestoreField) {
        View popupView = LayoutInflater.from(this).inflate(R.layout.privacy_dropmenu, null);
        PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setElevation(30);
        popupWindow.showAsDropDown(anchor);

        TextView optionHide = popupView.findViewById(R.id.option_hide);
        TextView optionShow = popupView.findViewById(R.id.option_show);

        View.OnClickListener optionClickListener = v -> {
            String selected = ((TextView) v).getText().toString();
            targetEditText.setText(selected);

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                FirebaseFirestore.getInstance().collection("users")
                        .document(user.getUid())
                        .update(firestoreField, selected)
                        .addOnSuccessListener(unused ->
                                Toast.makeText(this, firestoreField + " set to " + selected, Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "Failed to update " + firestoreField, Toast.LENGTH_SHORT).show());
            }

            popupWindow.dismiss();
        };

        optionHide.setOnClickListener(optionClickListener);
        optionShow.setOnClickListener(optionClickListener);
    }

    private void showDeleteAccountPopup() {
        View popupView = LayoutInflater.from(this).inflate(R.layout.delete_account, null);

        PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setElevation(20);
        popupWindow.showAtLocation(findViewById(android.R.id.content), android.view.Gravity.CENTER, 0, 0);

        popupView.findViewById(R.id.cancelButton).setOnClickListener(v -> popupWindow.dismiss());

        popupView.findViewById(R.id.deletePostButton).setOnClickListener(v -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                String uid = currentUser.getUid();
                FirebaseFirestore db = FirebaseFirestore.getInstance();

                db.collection("users").document(uid).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                db.collection("bin").document(uid)
                                        .set(documentSnapshot.getData())
                                        .addOnSuccessListener(unused -> {
                                            db.collection("users").document(uid).delete()
                                                    .addOnSuccessListener(aVoid -> {
                                                        FirebaseAuth.getInstance().signOut();
                                                        Toast.makeText(this, "Delete account request has been submitted.", Toast.LENGTH_LONG).show();
                                                        popupWindow.dismiss();
                                                        startActivity(new Intent(this, Login.class));
                                                        finish();
                                                    })
                                                    .addOnFailureListener(e ->
                                                            Toast.makeText(this, "Failed to delete user data.", Toast.LENGTH_SHORT).show());
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(this, "Failed to move user data to bin.", Toast.LENGTH_SHORT).show());
                            } else {
                                Toast.makeText(this, "User data not found.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "Error fetching user data.", Toast.LENGTH_SHORT).show());
            }
        });
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

        popupView.findViewById(R.id.about_us).setOnClickListener(v -> {
            startActivity(new Intent(this, AboutUsActivity.class));
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

        popupView.findViewById(R.id.edit_profile).setOnClickListener(v -> {
            startActivity(new Intent(this, EditProfileActivity.class));
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.share_app).setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            String shareBody = "Check out our app: https://play.google.com/store/apps/details?id=" + getPackageName();
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.logout).setOnClickListener(v -> {
            logoutAndRemoveToken();
            popupWindow.dismiss();
        });
    }

    private void logoutAndRemoveToken() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            FirebaseFirestore.getInstance().collection("users")
                    .document(userId)
                    .update("fcmToken", FieldValue.delete())
                    .addOnSuccessListener(aVoid -> {
                        FirebaseAuth.getInstance().signOut();
                        startActivity(new Intent(this, Login.class));
                        finish();
                    });
        }
    }
}