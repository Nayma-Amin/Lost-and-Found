package com.example.lostandfound;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

public class PostActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private EditText insertCategory, insertDescription, editTextTime, insertContact, insertLocation, insertGpsLink;
    private ImageView previewImage;
    private LinearLayout uploadImageLayout;
    private Button postButton, cancelButton;

    private Button buttonLost, buttonFound;
    private String selectedType = "Lost";

    private String base64Image = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        insertCategory = findViewById(R.id.insert_category);
        insertDescription = findViewById(R.id.editTextDescription);
        editTextTime = findViewById(R.id.editTextTime);
        insertContact = findViewById(R.id.editTextContact);
        insertLocation = findViewById(R.id.editTextLocation);
        insertGpsLink = findViewById(R.id.gpsLink);
        uploadImageLayout = findViewById(R.id.uploadImageLayout);
        previewImage = findViewById(R.id.previewImage);
        postButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);

        String categoryTag = getIntent().getStringExtra("categoryTag");
        boolean isEditable = getIntent().getBooleanExtra("isEditable", false);

        if (isEditable) {
            insertCategory.setText("");
            insertCategory.setEnabled(true);
            insertCategory.setFocusable(true);
            insertCategory.setFocusableInTouchMode(true);
        } else {
            insertCategory.setText(categoryTag);
            insertCategory.setEnabled(false);
            insertCategory.setFocusable(false);
            insertCategory.setFocusableInTouchMode(false);
        }


        uploadImageLayout.setOnClickListener(v -> pickImageFromGallery());

        editTextTime.setOnClickListener(v -> openDateTimePicker());

        insertGpsLink.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="));
            startActivity(intent);
            Toast.makeText(this, "Please copy and insert the location link", Toast.LENGTH_LONG).show();
        });

        postButton.setOnClickListener(v -> submitPost());

        buttonLost = findViewById(R.id.buttonLost);
        buttonFound = findViewById(R.id.buttonFound);

        buttonLost.setOnClickListener(v -> {
            selectedType = "Lost";
            buttonLost.setBackgroundColor(Color.parseColor("#081C2B"));
            buttonFound.setBackgroundColor(Color.parseColor("#4C7B9D"));
        });

        buttonFound.setOnClickListener(v -> {
            selectedType = "Found";
            buttonLost.setBackgroundColor(Color.parseColor("#4C7B9D"));
            buttonFound.setBackgroundColor(Color.parseColor("#081C2B"));
        });

        cancelButton.setOnClickListener(v -> {
            Intent intent = new Intent(PostActivity.this, CategoryClick.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

    }
    private void pickImageFromGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Profile Image"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                previewImage.setImageBitmap(bitmap);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 30, baos);
                byte[] imageBytes = baos.toByteArray();
                base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void openDateTimePicker() {
        final Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            new TimePickerDialog(this, (view1, hourOfDay, minute) -> {
                String dateTime = String.format(Locale.getDefault(), "%04d-%02d-%02d %02d:%02d",
                        year, month + 1, dayOfMonth, hourOfDay, minute);
                editTextTime.setText(dateTime);
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void submitPost() {
        String description = insertDescription.getText().toString().trim();
        if (description.split("\\s+").length > 20 || description.split("\\s+").length < 15) {
            Toast.makeText(this, "Description must be 15–20 words", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        long timestamp = System.currentTimeMillis();

        Map<String, Object> post = new HashMap<>();
        post.put("userId", userId);
        post.put("category", insertCategory.getText().toString());
        post.put("type", selectedType);
        post.put("description", description);
        post.put("imageFile", base64Image);
        post.put("contact", insertContact.getText().toString());
        post.put("location", insertLocation.getText().toString());
        post.put("gpsLink", insertGpsLink.getText().toString());
        post.put("timestamp", timestamp);

        db.collection("lost_and_found_posts")
                .add(post)
                .addOnSuccessListener(documentReference -> {
                    String postId = documentReference.getId();

                    Map<String, Object> notification = new HashMap<>();
                    notification.put("postId", postId);
                    notification.put("userId", userId);
                    notification.put("location", insertLocation.getText().toString());
                    notification.put("type", selectedType);
                    notification.put("timestamp", timestamp);
                    notification.put("category", insertCategory.getText().toString());

                    db.collection("notifications").add(notification);

                    Toast.makeText(this, "Post submitted!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(PostActivity.this, ProfileActivity.class));
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to submit post", Toast.LENGTH_SHORT).show());
    }

}