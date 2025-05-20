package com.example.lostandfound;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;

import android.util.Base64;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import com.bumptech.glide.Glide;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseAuth fbAuth;
    private FirebaseFirestore db;

    private TextView nameTextView, emailTextView, phoneTextView, locationTextView, noPostText;
    private AppCompatButton editButton;
    private ImageView profileImageView;
    private ImageButton menuIcon, settingsIcon;
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

        settingsIcon = findViewById(R.id.settings_icon);
        settingsIcon.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, SettingsActivity.class));
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

    private Bitmap getRoundedBitmap(Bitmap bitmap) {
        int radius = dpToPx(10);

        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawRoundRect(rectF, radius, radius, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
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

                            String imageData = documentSnapshot.getString("profileImage");
                            if (!TextUtils.isEmpty(imageData)) {
                                if (imageData.startsWith("data:image")) {
                                    imageData = imageData.substring(imageData.indexOf(",") + 1);
                                }

                                try {
                                    byte[] decodedBytes = android.util.Base64.decode(imageData, android.util.Base64.DEFAULT);
                                    android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                                    profileImageView.setImageBitmap(getRoundedBitmap(bitmap));
                                } catch (IllegalArgumentException e) {
                                    Glide.with(this)
                                            .load(imageData)
                                            .transform(new CenterCrop(), new RoundedCorners(20))  // 20px corner radius
                                            .placeholder(R.drawable.profile)
                                            .error(R.drawable.profile)
                                            .into(profileImageView);
                                }
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
        requestContainer.removeAllViews();
        FirebaseUser user = fbAuth.getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        Query query = db.collection("lost_and_found_posts").whereEqualTo("userId", uid);

        if (selectedTab.equals("lost")) {
            query = query.whereEqualTo("type", "Lost");
        } else if (selectedTab.equals("found")) {
            query = query.whereEqualTo("type", "Found");
        }

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                boolean hasPosts = false;
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    hasPosts = true;
                    addPostCard(doc);
                }
                noPostText.setVisibility(hasPosts ? View.GONE : View.VISIBLE);
            }
        });
    }

    private void addPostCard(QueryDocumentSnapshot doc) {
        View card = LayoutInflater.from(this).inflate(R.layout.my_post_item, requestContainer, false);

        TextView itemTitle = card.findViewById(R.id.item_title);
        TextView itemDescription = card.findViewById(R.id.item_description);
        TextView itemDate = card.findViewById(R.id.item_date);
        TextView itemTime = card.findViewById(R.id.item_time);
        ImageView itemImage = card.findViewById(R.id.item_image);
        TextView itemLocation = card.findViewById(R.id.item_location);

        String title = doc.getString("category") + " (" + doc.getString("type") + ")";
        String description = doc.getString("description");
        String imageBase64 = doc.getString("imageFile");
        String timeAndDate = doc.getString("timeAndDate");
        String location = doc.getString("location");

        String date = "", time = "";
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

        itemTitle.setText(title);
        itemDescription.setText(description);
        itemLocation.setText(location);
        itemDate.setText(date);
        itemTime.setText(time);

        if (imageBase64 != null && !imageBase64.isEmpty()) {
            byte[] imageBytes = Base64.decode(imageBase64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            itemImage.setImageBitmap(bitmap);
        }

        LinearLayout btnComplete = card.findViewById(R.id.btn_complete);
        LinearLayout btnDelete = card.findViewById(R.id.btn_delete);
        LinearLayout btnEdit = card.findViewById(R.id.btn_edit);

        boolean isCompleted = doc.contains("complete") && doc.getBoolean("complete");
        if (isCompleted) btnComplete.setEnabled(false);

        btnComplete.setOnClickListener(v -> markAsComplete(doc));
        btnDelete.setOnClickListener(v -> showDeleteConfirmationPopup(doc.getId()));
        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, PostActivity.class);
            intent.putExtra("isEditMode", true);
            intent.putExtra("postId", doc.getId());
            startActivity(intent);
        });

        requestContainer.addView(card);
    }

    private void markAsComplete(QueryDocumentSnapshot doc) {
        String docId = doc.getId();
        db.collection("lost_and_found_posts").document(docId)
                .update("complete", true)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Post marked as complete.", Toast.LENGTH_SHORT).show();
                    loadUserPosts();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to complete post.", Toast.LENGTH_SHORT).show();
                });
    }

    private void showDeleteConfirmationPopup(String docId) {
        View popupView = LayoutInflater.from(this).inflate(R.layout.delete_post, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(popupView);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();

        AppCompatButton deleteBtn = popupView.findViewById(R.id.deletePostButton);
        AppCompatButton cancelBtn = popupView.findViewById(R.id.cancelButton);

        deleteBtn.setOnClickListener(v -> {
            db.collection("lost_and_found_posts").document(docId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Post deleted.", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadUserPosts();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to delete post.", Toast.LENGTH_SHORT).show();
                    });
        });

        cancelBtn.setOnClickListener(v -> dialog.dismiss());
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

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }

}
