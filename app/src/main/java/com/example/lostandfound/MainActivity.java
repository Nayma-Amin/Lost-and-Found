package com.example.lostandfound;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Base64;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private ImageView menuIcon, notificationIcon;
    private AppCompatButton filterButton;
    private LinearLayout filterLay, postLayout, reportLayout, profileLayout;
    private TextView loggedInUser;
    private FirebaseUser currentUser;
    private boolean isGuest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        isGuest = (currentUser == null);

        menuIcon = findViewById(R.id.menu_icon);
        notificationIcon = findViewById(R.id.notification_icon);
        filterButton = findViewById(R.id.filter_button);
        filterLay = findViewById(R.id.filter_lay);
        postLayout = findViewById(R.id.post);
        reportLayout = findViewById(R.id.report);
        profileLayout = findViewById(R.id.profile);
        loggedInUser = findViewById(R.id.loggedInUser);

        TextView loggedInUser = findViewById(R.id.loggedInUser);
        TextView guest = findViewById(R.id.guest);

        if (isGuest) {
            loggedInUser.setVisibility(View.GONE);
            guest.setVisibility(View.VISIBLE);
        } else {
            loggedInUser.setVisibility(View.VISIBLE);
            guest.setVisibility(View.GONE);
        }

        menuIcon.setOnClickListener(v -> showDropdownMenu());

        notificationIcon.setOnClickListener(v -> {
            if (isGuest) showLoginPrompt();
            else startActivity(new Intent(this, NotificationActivity.class));
        });

        filterButton.setOnClickListener(v -> {
            if (isGuest) showLoginPrompt();
            else toggleFilter();
        });

        postLayout.setOnClickListener(v -> {
            if (isGuest) showLoginPrompt();
            else startActivity(new Intent(this, CategoryClick.class));
        });

        reportLayout.setOnClickListener(v -> {
            if (isGuest) showLoginPrompt();
            else startActivity(new Intent(this, ReportActivity.class));
        });


        ImageView profileImageView = findViewById(R.id.profileImageView);

        if (!isGuest) {
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseFirestore.getInstance().collection("users")
                    .document(currentUserId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists() && documentSnapshot.contains("profileImage")) {
                            String base64Image = documentSnapshot.getString("profileImage");
                            if (base64Image != null && !base64Image.isEmpty()) {
                                byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
                                Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                                profileImageView.setImageBitmap(decodedBitmap);
                            } else {
                                profileImageView.setImageResource(R.drawable.profile);
                            }
                        } else {
                            profileImageView.setImageResource(R.drawable.profile);
                        }
                    })
                    .addOnFailureListener(e -> {
                        profileImageView.setImageResource(R.drawable.profile);
                    });
        } else {
            profileImageView.setImageResource(R.drawable.profile);
        }

        profileLayout.setOnClickListener(v -> {
            if (isGuest) {
                startActivity(new Intent(this, HomePage.class));
            } else {
                startActivity(new Intent(this, ProfileActivity.class));
            }
        });

    }

    private void showDropdownMenu() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.dropdown_menu, null);

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
        popupWindow.showAsDropDown(menuIcon);

        popupView.findViewById(R.id.home_page).setOnClickListener(v -> popupWindow.dismiss());

        popupView.findViewById(R.id.edit_profile).setOnClickListener(v -> {
            if (!isGuest) {
                startActivity(new Intent(this, EditProfileActivity.class));
                popupWindow.dismiss();
            } else showLoginPrompt();
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
            if (!isGuest) {
                startActivity(new Intent(this, AboutUsActivity.class));
                popupWindow.dismiss();
            } else showLoginPrompt();
        });

        popupView.findViewById(R.id.share_app).setOnClickListener(v -> {
            if (!isGuest) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                String shareBody = "Check out our app: https://play.google.com/store/apps/details?id=" + getPackageName();
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
                startActivity(Intent.createChooser(shareIntent, "Share via"));
                popupWindow.dismiss();
            } else showLoginPrompt();
        });

        popupView.findViewById(R.id.logout).setOnClickListener(v -> {
            if (!isGuest) {
                logoutAndRemoveToken();
                popupWindow.dismiss();
            } else showLoginPrompt();
        });

        popupView.findViewById(R.id.delete_account).setOnClickListener(v -> {
            if (!isGuest) {
                showDeleteAccountPopup();
                popupWindow.dismiss();
            } else showLoginPrompt();
        });
    }

    private void showDeleteAccountPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 40, 60, 10);

        TextView message = new TextView(this);
        message.setText("Are you sure you want to delete your account? This action can be undone if you log in within 7 days.");
        message.setTextSize(16);
        message.setTextColor(Color.BLACK);
        message.setPadding(0, 0, 0, 30);
        layout.addView(message);

        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setGravity(Gravity.END);

        Button confirmButton = new Button(this);
        confirmButton.setText("Delete");
        confirmButton.setTextColor(Color.WHITE);
        confirmButton.setBackgroundColor(Color.RED);
        confirmButton.setPadding(30, 10, 30, 10);

        Button cancelButton = new Button(this);
        cancelButton.setText("Cancel");
        cancelButton.setTextColor(Color.BLACK);
        cancelButton.setBackgroundColor(Color.LTGRAY);
        cancelButton.setPadding(30, 10, 30, 10);

        buttonLayout.addView(cancelButton);
        buttonLayout.addView(confirmButton);
        layout.addView(buttonLayout);

        builder.setView(layout);
        AlertDialog dialog = builder.create();
        dialog.show();

        confirmButton.setOnClickListener(v -> {
            moveToBinAndDeleteUser();
            dialog.dismiss();
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
    }

    private void logoutAndRemoveToken() {
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

    private void moveToBinAndDeleteUser() {
        String userId = currentUser.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(userId).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                db.collection("bin").document(userId).set(snapshot.getData())
                        .addOnSuccessListener(unused -> {
                            db.collection("users").document(userId).delete();
                            FirebaseAuth.getInstance().signOut();
                            startActivity(new Intent(this, Login.class));
                            finish();
                        });
            }
        });
    }

    private void toggleFilter() {
        filterLay.setVisibility(filterLay.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }

    private void showLoginPrompt() {
        Toast.makeText(this, "Please create an account to enjoy all the features.", Toast.LENGTH_SHORT).show();
    }

}
