package com.example.lostandfound;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;
import com.example.lostandfound.R;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class NotificationActivity extends AppCompatActivity {

    private LinearLayout notificationContainer;

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

        // Example: Add 1 notification for demonstration
        addNotification();
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
                    Toast.makeText(NotificationActivity.this, "Marked as read", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (id == R.id.action_delete) {
                    Toast.makeText(NotificationActivity.this, "Notification deleted", Toast.LENGTH_SHORT).show();
                    notificationContainer.removeView(notificationView);
                    return true;
                }

                return false;
            });

            // Force icon display
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
    }
}
