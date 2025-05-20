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
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

public class PostActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private EditText insertCategory, insertDescription, editTextTime, insertContact, insertLocation, insertGpsLink;
    private ImageView previewImage;
    private LinearLayout uploadImageLayout;
    private Button postButton, cancelButton, buttonLost, buttonFound;

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
        buttonLost = findViewById(R.id.buttonLost);
        buttonFound = findViewById(R.id.buttonFound);

        String categoryTag = getIntent().getStringExtra("categoryTag");
        boolean isEditable = getIntent().getBooleanExtra("isEditable", false);

        if (isEditable) {
            insertCategory.setText("");
            insertCategory.setEnabled(true);
        } else {
            insertCategory.setText(categoryTag);
            insertCategory.setEnabled(false);
        }

        uploadImageLayout.setOnClickListener(v -> pickImageFromGallery());
        editTextTime.setOnClickListener(v -> openDateTimePicker());

        insertGpsLink.setOnClickListener(v -> {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=")));
            Toast.makeText(this, "Please copy and insert the location link", Toast.LENGTH_LONG).show();
        });

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

        postButton.setOnClickListener(v -> submitPost());
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
                previewImage.setImageBitmap(bitmap);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 30, baos);
                base64Image = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
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
        if (description.split("\\s+").length < 10 || description.split("\\s+").length > 20) {
            Toast.makeText(this, "Description must be 10–20 words", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String category = insertCategory.getText().toString();
        String location = insertLocation.getText().toString();
        String contact = insertContact.getText().toString();
        long timestamp = System.currentTimeMillis();

        Map<String, Object> post = new HashMap<>();
        post.put("userId", userId);
        post.put("category", category);
        post.put("type", selectedType);
        post.put("description", description);
        post.put("imageFile", base64Image);
        post.put("contact", contact);
        post.put("location", location);
        post.put("gpsLink", insertGpsLink.getText().toString());
        post.put("timestamp", timestamp);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("lost_and_found_posts").add(post).addOnSuccessListener(docRef -> {
            String postId = docRef.getId();

            Map<String, Object> notif = new HashMap<>();
            notif.put("postId", postId);
            notif.put("userId", userId);
            notif.put("location", location);
            notif.put("type", selectedType);
            notif.put("timestamp", timestamp);
            notif.put("category", category);

            db.collection("notifications").add(notif);

            TokenUtil.fetchAccessToken(PostActivity.this, new TokenUtil.AccessTokenCallback() {
                @Override
                public void onTokenReceived(String accessToken) {
                    db.collection("users").get().addOnSuccessListener(querySnapshots -> {
                        for (DocumentSnapshot doc : querySnapshots) {
                            if (!doc.getId().equals(userId)) {
                                String targetToken = doc.getString("fcmToken");
                                String userLoc = doc.getString("location");

                                if (targetToken != null && !targetToken.isEmpty()) {
                                    String title = selectedType + " Item Alert";
                                    String body = location.equalsIgnoreCase(userLoc)
                                            ? "New " + selectedType + " post in your area: " + location
                                            : "New " + selectedType + " post! Check it out: " + location;

                                    sendFCM(targetToken, title, body, postId, accessToken);
                                }
                            }
                        }
                    });
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(PostActivity.this, "Token error", Toast.LENGTH_SHORT).show();
                    Log.e("TokenUtil", "Token error", e);
                }
            });

            Toast.makeText(this, "Post submitted!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(PostActivity.this, ProfileActivity.class));
            finish();

        }).addOnFailureListener(e ->
                Toast.makeText(this, "Failed to submit post", Toast.LENGTH_SHORT).show()
        );
    }

    private void sendFCM(String token, String title, String message, String postId, String accessToken) {
        try {
            JSONObject notification = new JSONObject();
            notification.put("title", title);
            notification.put("body", message);

            JSONObject data = new JSONObject();
            data.put("postId", postId);
            data.put("click_action", "OPEN_LOST_FOUND_POST");

            JSONObject messageObj = new JSONObject();
            messageObj.put("token", token);
            messageObj.put("notification", notification);
            messageObj.put("data", data);

            JSONObject main = new JSONObject();
            main.put("message", messageObj);

            String url = "https://fcm.googleapis.com/v1/projects/lost-and-found-7d95d/messages:send";

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, main,
                    response -> Log.d("FCM", "Notification sent"),
                    error -> Log.e("FCM", "Error sending FCM", error)) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Bearer " + accessToken);
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            request.setRetryPolicy(new DefaultRetryPolicy(10000, 1, 1f));
            Volley.newRequestQueue(getApplicationContext()).add(request);

        } catch (Exception e) {
            Log.e("FCM", "Failed to construct FCM payload", e);
        }
    }
}