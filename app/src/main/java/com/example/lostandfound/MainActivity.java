package com.example.lostandfound;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ImageView menuIcon, notificationIcon, settingsIcon;
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

        settingsIcon = findViewById(R.id.settings_icon);
        settingsIcon.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        });

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

        loadLostFoundPosts();
    }

    private void loadLostFoundPosts() {
        LinearLayout postContainer = findViewById(R.id.post_container);
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("lost_and_found_posts")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    postContainer.removeAllViews();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String postUserId = doc.getString("userId");
                        if (postUserId != null && postUserId.equals(currentUserId)) {
                            continue;
                        }

                        String title = doc.getString("category") + " (" + doc.getString("type") + ")";
                        String description = doc.getString("description");
                        String imageBase64 = doc.getString("imageFile");
                        String contact = doc.getString("contact");
                        String location = doc.getString("location");
                        String gpsLink = doc.getString("gpsLink");
                        String date = "";
                        String time = "";

                        String timeAndDate = doc.getString("timeAndDate");
                        if (timeAndDate != null && !timeAndDate.isEmpty()) {
                            try {
                                java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                                java.util.Date parsedDate = inputFormat.parse(timeAndDate);

                                java.text.SimpleDateFormat sdfDate = new java.text.SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                                java.text.SimpleDateFormat sdfTime = new java.text.SimpleDateFormat("hh:mm a", Locale.getDefault());

                                date = sdfDate.format(parsedDate);
                                time = sdfTime.format(parsedDate);
                            } catch (java.text.ParseException e) {
                                e.printStackTrace();
                            }
                        }

                        View postView = LayoutInflater.from(this).inflate(R.layout.post_item, postContainer, false);

                        ImageView itemImage = postView.findViewById(R.id.item_image);
                        TextView itemTitle = postView.findViewById(R.id.item_title);
                        TextView itemDescription = postView.findViewById(R.id.item_description);
                        TextView itemDate = postView.findViewById(R.id.item_date);
                        TextView itemTime = postView.findViewById(R.id.item_time);
                        TextView itemLocation = postView.findViewById(R.id.item_location);

                        itemTitle.setText(title);
                        itemDescription.setText(description);
                        itemDate.setText(date);
                        itemTime.setText(time);
                        itemLocation.setText(location);

                        if (imageBase64 != null && !imageBase64.isEmpty()) {
                            byte[] imageBytes = Base64.decode(imageBase64, Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                            itemImage.setImageBitmap(bitmap);
                        }

                        LinearLayout btnContact = postView.findViewById(R.id.btn_edit);
                        LinearLayout btnLocation = postView.findViewById(R.id.btn_delete);
                        LinearLayout btnShare = postView.findViewById(R.id.btn_found);

                        btnContact.setOnClickListener(v -> {
                            Intent callIntent = new Intent(Intent.ACTION_DIAL);
                            callIntent.setData(Uri.parse("tel:" + contact));
                            startActivity(callIntent);
                        });

                        btnLocation.setOnClickListener(v -> {
                            if (gpsLink != null && !gpsLink.isEmpty()) {
                                Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(gpsLink));
                                startActivity(mapIntent);
                            } else {
                                Toast.makeText(this, "No GPS link provided", Toast.LENGTH_SHORT).show();
                            }
                        });

                        btnShare.setOnClickListener(v -> {
                            String shareText = "Lost & Found: " + title + "\n" + description + "\nLocation: " + location + "\nContact: " + contact;
                            Intent shareIntent = new Intent(Intent.ACTION_SEND);
                            shareIntent.setType("text/plain");
                            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                            startActivity(Intent.createChooser(shareIntent, "Share via"));
                        });

                        postContainer.addView(postView);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load posts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    private void toggleFilter() {
        filterLay.setVisibility(filterLay.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }

    private void showLoginPrompt() {
        Toast.makeText(this, "Please create an account to enjoy all the features!", Toast.LENGTH_SHORT).show();
    }

}
