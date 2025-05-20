package com.example.lostandfound;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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

public class CategoryClick extends AppCompatActivity {

    private ImageButton menuIcon, settingsIcon;
    private TextView itemsTab, personPetTab;
    private GridLayout categoryGrid, personGrid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_category);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.category), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        itemsTab = findViewById(R.id.items_tab);
        personPetTab = findViewById(R.id.person_pet_tab);
        categoryGrid = findViewById(R.id.category_grid);
        personGrid = findViewById(R.id.person_grid);

        selectItemsTab();

        itemsTab.setOnClickListener(v -> selectItemsTab());
        personPetTab.setOnClickListener(v -> selectPersonTab());

        settingsIcon = findViewById(R.id.settings_icon);
        settingsIcon.setOnClickListener(v -> {
            startActivity(new Intent(CategoryClick.this, SettingsActivity.class));
        });

        menuIcon = findViewById(R.id.menu_icon);
        menuIcon.setOnClickListener(this::showDropdownMenu);
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
    private void selectItemsTab() {
        itemsTab.setBackgroundColor(Color.WHITE);
        personPetTab.setBackgroundColor(Color.parseColor("#D9D9D9"));
        categoryGrid.setVisibility(View.VISIBLE);
        personGrid.setVisibility(View.GONE);
    }

    private void selectPersonTab() {
        itemsTab.setBackgroundColor(Color.parseColor("#D9D9D9"));
        personPetTab.setBackgroundColor(Color.WHITE);
        categoryGrid.setVisibility(View.GONE);
        personGrid.setVisibility(View.VISIBLE);
    }

    public void onCategoryClick(View view) {
        String tag = (String) view.getTag();
        if (tag == null) return;

        boolean editable = tag.equalsIgnoreCase("Other") || tag.equalsIgnoreCase("Others");

        Intent intent = new Intent(CategoryClick.this, PostActivity.class);
        intent.putExtra("categoryTag", tag);
        intent.putExtra("isEditable", editable);
        startActivity(intent);
    }


}
