package com.example.lostandfound;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.Toast;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private EditText nameEditText, emailEditText, phoneEditText, locationEditText;
    private RadioButton maleButton, femaleButton;
    private Button saveButton, cancelButton, selectPictureBtn;
    private ImageButton imagePickerButton, menuIcon;
    private ImageView profileImageView;
    private Uri imageUri;

    private FirebaseAuth fbAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private String gender = "";

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
        selectPictureBtn = findViewById(R.id.select_picture);
        imagePickerButton = findViewById(R.id.select_newpicture);
        profileImageView = findViewById(R.id.image);
        menuIcon = findViewById(R.id.menu_icon);

        fbAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        loadUserData();

        cancelButton.setOnClickListener(v -> finish());

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

        menuIcon.setOnClickListener(this::showDropdownMenu);
    }

    private void showDropdownMenu(View anchor) {
        View popupView = LayoutInflater.from(this).inflate(R.layout.dropdown_menu, null);
        int width = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                300,
                getResources().getDisplayMetrics()
        );

        PopupWindow popupWindow = new PopupWindow(popupView, width, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.showAsDropDown(anchor);

        popupView.findViewById(R.id.home_page).setOnClickListener(v -> popupWindow.dismiss());

        popupView.findViewById(R.id.edit_profile).setOnClickListener(v -> {
            popupWindow.dismiss(); // Already here
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
            String shareBody = "Check out the app: https://play.google.com/store/apps/details?id=" + getPackageName();
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.logout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, Login.class));
            finish();
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.delete_account).setOnClickListener(v -> {
            Toast.makeText(this, "Delete account logic coming soon", Toast.LENGTH_SHORT).show();
            popupWindow.dismiss();
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

                            String imageBase64 = document.getString("profileImage");
                            if (!TextUtils.isEmpty(imageBase64)) {
                                byte[] decodedBytes = Base64.decode(imageBase64, Base64.DEFAULT);
                                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                                profileImageView.setImageBitmap(bitmap);
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
                String base64Image = convertImageToBase64(imageUri);
                if (base64Image != null) {
                    updates.put("profileImage", base64Image);
                }
            }
            saveToFirestore(user.getUid(), updates);

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
            profileImageView.setImageURI(imageUri);
            Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show();
        }
    }

    private String convertImageToBase64(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream);
            byte[] byteArray = stream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
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
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
