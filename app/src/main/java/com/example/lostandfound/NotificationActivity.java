package com.example.lostandfound;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
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

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class NotificationActivity extends AppCompatActivity {

    private LinearLayout notificationContainer;
    private TextView noNotificationText;
    private ImageButton menuIcon, settingsIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notification);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        notificationContainer = findViewById(R.id.notification_container);
        noNotificationText = findViewById(R.id.no_notification_text);

        menuIcon = findViewById(R.id.menu_icon);
        settingsIcon = findViewById(R.id.settings_icon);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            loadNotifications(user.getUid());
        }

        settingsIcon.setOnClickListener(v -> {
            startActivity(new Intent(NotificationActivity.this, SettingsActivity.class));
        });

        menuIcon.setOnClickListener(this::showDropdownMenu);

    }

    private void loadNotifications(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    boolean hasNotification = false;

                    for (QueryDocumentSnapshot doc : snapshot) {
                        String notificationUserId = doc.getString("userId");

                        if (notificationUserId != null && notificationUserId.equals(userId)) {
                            continue;
                        }

                        hasNotification = true;

                        String type = doc.getString("type");
                        String category = doc.getString("category");
                        String postId = doc.getString("postId");
                        Timestamp timestamp = doc.getTimestamp("timestamp");

                        db.collection("lost_and_found_posts")
                                .document(postId)
                                .get()
                                .addOnSuccessListener(postDoc -> {
                                    String base64Image = postDoc.getString("imageFile");

                                    View notifView = LayoutInflater.from(this).inflate(R.layout.notifications, notificationContainer, false);
                                    ImageView itemImage = notifView.findViewById(R.id.item_image);
                                    TextView titleText = notifView.findViewById(R.id.notification_text);
                                    TextView timeText = notifView.findViewById(R.id.time_text);

                                    ImageView btnOption = notifView.findViewById(R.id.btnOption);

                                    btnOption.setOnClickListener(view -> {
                                        View popupView = LayoutInflater.from(NotificationActivity.this)
                                                .inflate(R.layout.notification_menu, null);

                                        PopupWindow popupWindow = new PopupWindow(popupView,
                                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                                true);

                                        popupWindow.setElevation(10);

                                        popupWindow.showAsDropDown(btnOption, -150, 0);

                                        TextView optionMarkRead = popupView.findViewById(R.id.option_hide);
                                        TextView optionDelete = popupView.findViewById(R.id.option_show);

                                        optionMarkRead.setOnClickListener(v -> {
                                            popupWindow.dismiss();
                                            doc.getReference().update("read", true)
                                                    .addOnSuccessListener(aVoid -> {
                                                        notifView.setBackgroundColor(Color.WHITE);
                                                    });
                                        });

                                        optionDelete.setOnClickListener(v -> {
                                            popupWindow.dismiss();
                                            doc.getReference().delete()
                                                    .addOnSuccessListener(aVoid -> {
                                                        notificationContainer.removeView(notifView);
                                                    });
                                        });
                                    });

                                    String title = "New " + type + " item: " + category + ". Check out!";
                                    titleText.setText(title);

                                    if (timestamp != null) {
                                        timeText.setText(getTimeAgo(timestamp.toDate().getTime()));
                                    }

                                    if (base64Image != null && !base64Image.isEmpty()) {
                                        byte[] decoded = Base64.decode(base64Image, Base64.DEFAULT);
                                        Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                                        itemImage.setImageBitmap(bitmap);
                                    }

                                    notifView.setOnClickListener(v -> {
                                        Intent intent = new Intent(NotificationActivity.this, MainActivity.class);
                                        intent.putExtra("postId", postId);
                                        startActivity(intent);
                                    });

                                    notificationContainer.addView(notifView);
                                });
                    }

                    if (!hasNotification) {
                        noNotificationText.setVisibility(View.VISIBLE);
                    } else {
                        noNotificationText.setVisibility(View.GONE);
                    }
                });
    }

    private String getTimeAgo(long time) {
        long now = System.currentTimeMillis();
        long diff = now - time;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 60) return "Just now";
        else if (minutes < 60) return minutes + " minutes ago";
        else if (hours < 24) return hours + " hours ago";
        else return days + " days ago";
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
