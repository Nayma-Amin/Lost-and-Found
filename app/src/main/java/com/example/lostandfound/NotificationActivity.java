package com.example.lostandfound;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

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
        settingsIcon.setOnClickListener(v -> {
            startActivity(new Intent(NotificationActivity.this, SettingsActivity.class));
        });

        menuIcon.setOnClickListener(this::showDropdownMenu);

        updateNotificationVisibility();

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
    private void updateNotificationVisibility() {
        if (notificationContainer.getChildCount() == 0) {
            noNotificationText.setVisibility(View.VISIBLE);
        } else {
            noNotificationText.setVisibility(View.GONE);
        }
    }

    private void addNotification() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View notificationView = inflater.inflate(R.layout.notifications, notificationContainer, false);

        ImageButton btnOption = notificationView.findViewById(R.id.btnOption);
        btnOption.setOnClickListener(view -> {
            PopupMenu popup = new PopupMenu(NotificationActivity.this, view);
            MenuInflater inflater1 = popup.getMenuInflater();
            popup.inflate(R.menu.notification_options_menu);

            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.action_mark_read) {
                    Toast.makeText(this, "Marked as read", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (id == R.id.action_delete) {
                    Toast.makeText(this, "Notification deleted", Toast.LENGTH_SHORT).show();
                    notificationContainer.removeView(notificationView);
                    updateNotificationVisibility();
                    return true;
                }
                return false;
            });

            try {
                java.lang.reflect.Field mFieldPopup = popup.getClass().getDeclaredField("mPopup");
                mFieldPopup.setAccessible(true);
                Object mPopup = mFieldPopup.get(popup);
                mPopup.getClass().getDeclaredMethod("setForceShowIcon", boolean.class).invoke(mPopup, true);
            } catch (Exception e) {
                e.printStackTrace();
            }

            popup.show();
        });

        notificationContainer.addView(notificationView);
        updateNotificationVisibility();
    }

}
