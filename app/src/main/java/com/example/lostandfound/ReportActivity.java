package com.example.lostandfound;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupWindow;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportActivity extends AppCompatActivity {

    private ImageButton menuIcon, settingsIcon;
    private PieChart pieChart;
    private BarChart barChart;
    private FirebaseFirestore db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_report);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.report), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;

        });
        pieChart = findViewById(R.id.pieChart);
        barChart = findViewById(R.id.barChart);
        db = FirebaseFirestore.getInstance();

        setupPieChart();
        setupBarChart();

        menuIcon = findViewById(R.id.menu_icon);
        settingsIcon = findViewById(R.id.settings_icon);
        settingsIcon.setOnClickListener(v -> {
            startActivity(new Intent(ReportActivity.this, SettingsActivity.class));
        });

        menuIcon.setOnClickListener(this::showDropdownMenu);
    }
    private void setupPieChart() {
        db.collection("lost_and_found_posts").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                int lostCount = 0, foundCount = 0;
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    String type = doc.getString("type");
                    if ("Lost".equalsIgnoreCase(type)) lostCount++;
                    else if ("Found".equalsIgnoreCase(type)) foundCount++;
                }

                int total = lostCount + foundCount;
                if (total == 0) return;
                List<PieEntry> entries = new ArrayList<>();
                entries.add(new PieEntry((float) lostCount / total, "Lost"));
                entries.add(new PieEntry((float) foundCount / total, "Found"));

                pieChart.setUsePercentValues(true);
                pieChart.setEntryLabelColor(Color.WHITE);
                pieChart.setEntryLabelTextSize(14f);

                PieDataSet dataSet = new PieDataSet(entries, "Item Types");
                dataSet.setColors(new int[]{Color.RED, Color.GREEN});
                PieData pieData = new PieData(dataSet);
                pieData.setValueTextColor(Color.WHITE);
                pieData.setValueTextSize(14f);
                pieData.setValueFormatter(new PercentFormatter(pieChart));

                pieChart.setData(pieData);
                pieChart.getDescription().setText("");
                pieChart.invalidate();
            }
        });
    }

    private void setupBarChart() {
        db.collection("lost_and_found_posts").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Map<String, Integer> userPostCount = new HashMap<>();
                int totalPosts = 0;

                for (QueryDocumentSnapshot doc : task.getResult()) {
                    String userId = doc.getString("userId");
                    if (userId != null) {
                        userPostCount.put(userId, userPostCount.getOrDefault(userId, 0) + 1);
                        totalPosts++;
                    }
                }

                if (totalPosts == 0) return;

                List<BarEntry> entries = new ArrayList<>();
                List<String> userLabels = new ArrayList<>();
                int index = 0;
                for (Map.Entry<String, Integer> entry : userPostCount.entrySet()) {
                    float percentage = (float) entry.getValue() / totalPosts * 100f;
                    entries.add(new BarEntry(index, percentage));
                    userLabels.add(entry.getKey().substring(0, Math.min(6, entry.getKey().length())));
                    index++;
                }

                BarDataSet barDataSet = new BarDataSet(entries, "User Post %");
                barDataSet.setColor(Color.CYAN);
                barDataSet.setValueTextColor(Color.WHITE);
                barDataSet.setValueTextSize(12f);
                barDataSet.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getBarLabel(BarEntry barEntry) {
                        return String.format("%.1f%%", barEntry.getY());
                    }
                });

                BarData barData = new BarData(barDataSet);
                barChart.setData(barData);

                barChart.getXAxis().setDrawLabels(true);
                barChart.getXAxis().setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        int index = (int) value;
                        if (index >= 0 && index < userLabels.size()) {
                            return userLabels.get(index);
                        } else {
                            return "";
                        }
                    }
                });

                barChart.getDescription().setText("");
                barChart.invalidate();
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