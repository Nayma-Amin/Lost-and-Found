package com.example.lostandfound;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private EditText nameEditText, emailEditText, phoneEditText, locationEditText;
    private RadioButton maleButton, femaleButton;
    private Button saveButton, cancelButton, selectPictureBtn;
    private ImageButton imagePickerButton;
    private ImageView profileImageView;
    private Uri imageUri;
    private FirebaseAuth fbAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private String gender = "";  // holds selected gender

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_edit_profile);

        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        locationEditText = findViewById(R.id.locationEditText);
        maleButton = findViewById(R.id.maleButton);
        femaleButton = findViewById(R.id.femaleButton);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);
        profileImageView = findViewById(R.id.image);

        imagePickerButton = findViewById(R.id.select_newpicture);
        selectPictureBtn = findViewById(R.id.select_picture);

        fbAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        loadUserData();

        // Cancel just finishes the activity
        cancelButton.setOnClickListener(v -> finish());

        // Open gallery from both image buttons
        imagePickerButton.setOnClickListener(v -> openImagePicker());
        selectPictureBtn.setOnClickListener(v -> openImagePicker());

        saveButton.setOnClickListener(v -> {
            if (maleButton.isChecked()) {
                gender = "Male";
            } else if (femaleButton.isChecked()) {
                gender = "Female";
            }

            updateUserData();
        });
    }

    private void loadUserData() {
        FirebaseUser user = fbAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            setTextOrHint(nameEditText, document.getString("username"));
                            setTextOrHint(emailEditText, document.getString("email"));
                            setTextOrHint(phoneEditText, document.getString("phone"));
                            setTextOrHint(locationEditText, document.getString("location"));

                            String savedGender = document.getString("gender");
                            if ("Male".equalsIgnoreCase(savedGender)) {
                                maleButton.setChecked(true);
                            } else if ("Female".equalsIgnoreCase(savedGender)) {
                                femaleButton.setChecked(true);
                            }

                            String imageUrl = document.getString("profileImage");
                            if (!TextUtils.isEmpty(imageUrl)) {
                                Glide.with(this)
                                        .load(imageUrl)
                                        .apply(new RequestOptions().centerCrop())
                                        .placeholder(R.drawable.profile)
                                        .error(R.drawable.profile)
                                        .into(profileImageView);
                            } else {
                                profileImageView.setImageResource(R.drawable.profile);
                            }
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show()
                    );
        }
    }

    private void setTextOrHint(EditText field, String value) {
        if (TextUtils.isEmpty(value)) {
            field.setText("");
            field.setHint("No data");
        } else {
            field.setText(value);
        }
    }

    private void updateUserData() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String location = locationEditText.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Name and Email cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = fbAuth.getCurrentUser();
        if (user != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("username", name);
            updates.put("email", email);
            updates.put("phone", phone);
            updates.put("location", location);
            updates.put("gender", gender);

            if (imageUri != null) {
                uploadImageAndSave(user.getUid(), updates);
            } else {
                saveToFirestore(user.getUid(), updates);
            }
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Profile Image"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            profileImageView.setImageURI(imageUri); // Show preview
            Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadImageAndSave(String uid, Map<String, Object> updates) {
        StorageReference storageRef = storage.getReference().child("profile_images/" + uid + ".jpg");
        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            updates.put("profileImage", uri.toString());
                            saveToFirestore(uid, updates);
                        })
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show()
                );
    }

    private void saveToFirestore(String uid, Map<String, Object> updates) {
        db.collection("users").document(uid).update(updates)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    finish(); // Go back to profile page
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
