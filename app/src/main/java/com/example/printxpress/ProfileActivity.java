package com.example.printxpress;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class ProfileActivity extends Activity {

    private ImageView profileImage;
    private TextView changePhotoButton;
    private TextView profileName;
    private TextView profileEmail;

    private EditText nameInput;
    private EditText emailInput;
    private EditText addressInput;
    private EditText phoneInput;

    private Button editProfileButton;
    private Button saveProfileButton;

    private DatabaseHelper databaseHelper;
    private String currentEmail;

    private static final int PICK_IMAGE = 200;
    private static final String USER_PREFS = "PrintXpressUser";
    private static final String PROFILE_PREFS = "PrintXpressProfile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        profileName = findViewById(R.id.profileName);
        profileImage = findViewById(R.id.profileImage);
        changePhotoButton = findViewById(R.id.changePhotoButton);
        profileEmail = findViewById(R.id.profileEmail);

        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        addressInput = findViewById(R.id.addressInput);
        phoneInput = findViewById(R.id.phoneInput);

        editProfileButton = findViewById(R.id.editProfileButton);
        saveProfileButton = findViewById(R.id.saveProfileButton);

        databaseHelper = new DatabaseHelper(this);

        currentEmail = getSharedPreferences(USER_PREFS, MODE_PRIVATE)
                .getString("email", "");

        if (currentEmail == null || currentEmail.trim().isEmpty()) {
            Toast.makeText(this, "Please log in again.",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentEmail = currentEmail.trim();

        changePhotoButton.setVisibility(View.VISIBLE);
        changePhotoButton.bringToFront();

        loadProfileImage();
        loadProfile();

        changePhotoButton.setOnClickListener(v -> showPhotoOptions());
        profileImage.setOnClickListener(v -> showPhotoOptions());
        editProfileButton.setOnClickListener(v -> showEditSection());
        saveProfileButton.setOnClickListener(v -> saveProfile());
    }

    private String getProfileImageKey(String email) {
        return "profileImage_" + email.trim().toLowerCase(Locale.ROOT);
    }

    // LOAD PROFILE DETAILS

    private void loadProfile() {
        Cursor cursor = databaseHelper.getUser(currentEmail);

        if (cursor == null) {
            return;
        }

        try {
            if (cursor.moveToFirst()) {
                String name = cursor.getString(
                        cursor.getColumnIndexOrThrow("name"));

                String email = cursor.getString(
                        cursor.getColumnIndexOrThrow("email"));

                profileName.setText(name);
                profileEmail.setText(email);
                nameInput.setText(name);
                emailInput.setText(email);

                int addressIndex = cursor.getColumnIndex("address");
                int phoneIndex = cursor.getColumnIndex("phone");

                if (addressIndex >= 0) {
                    String address = cursor.getString(addressIndex);
                    addressInput.setText(address == null ? "" : address);
                }

                if (phoneIndex >= 0) {
                    String phone = cursor.getString(phoneIndex);
                    phoneInput.setText(phone == null ? "" : phone);
                }
            } else {
                Toast.makeText(this, "User profile not found.",
                        Toast.LENGTH_SHORT).show();
            }
        } finally {
            cursor.close();
        }
    }

    // SHOW EDIT SECTION

    private void showEditSection() {
        nameInput.setVisibility(View.VISIBLE);
        emailInput.setVisibility(View.VISIBLE);
        addressInput.setVisibility(View.VISIBLE);
        phoneInput.setVisibility(View.VISIBLE);

        saveProfileButton.setVisibility(View.VISIBLE);
        editProfileButton.setVisibility(View.GONE);

        Animation animation = AnimationUtils.loadAnimation(
                this, android.R.anim.fade_in);

        nameInput.startAnimation(animation);
        emailInput.startAnimation(animation);
        addressInput.startAnimation(animation);
        phoneInput.startAnimation(animation);
        saveProfileButton.startAnimation(animation);
    }

    // SAVE PROFILE DETAILS

    private void saveProfile() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String address = addressInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();

        if (name.isEmpty()) {
            nameInput.setError("Enter your name");
            return;
        }

        if (email.isEmpty()) {
            emailInput.setError("Enter your email");
            return;
        }

        // Accept standard email formats from different providers.
        // Examples: user@gmail.com and user@email.com.
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Enter a valid email address");
            return;
        }

        if (address.isEmpty()) {
            addressInput.setError("Enter your address");
            return;
        }

        if (phone.isEmpty()) {
            phoneInput.setError("Enter your phone number");
            return;
        }

        if (phone.length() < 10) {
            phoneInput.setError("Enter a valid phone number");
            return;
        }

        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        SharedPreferences profilePreferences =
                getSharedPreferences(PROFILE_PREFS, MODE_PRIVATE);

        SharedPreferences userPreferences =
                getSharedPreferences(USER_PREFS, MODE_PRIVATE);

        String oldEmail = currentEmail;
        String oldImageKey = getProfileImageKey(oldEmail);
        String newImageKey = getProfileImageKey(email);

        String savedImageUri =
                profilePreferences.getString(oldImageKey, "");

        // Check whether another account already uses this email.
        Cursor emailCursor = db.query(
                "users",
                new String[]{"email"},
                "LOWER(email) = LOWER(?) AND LOWER(email) != LOWER(?)",
                new String[]{email, oldEmail},
                null,
                null,
                null
        );

        boolean emailAlreadyExists;

        try {
            emailAlreadyExists = emailCursor != null
                    && emailCursor.moveToFirst();
        } finally {
            if (emailCursor != null) {
                emailCursor.close();
            }
        }

        if (emailAlreadyExists) {
            emailInput.setError("This email is already registered");
            Toast.makeText(this,
                    "Please use a different email address.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("email", email);
        values.put("address", address);
        values.put("phone", phone);

        int result = db.update(
                "users",
                values,
                "email = ?",
                new String[]{oldEmail}
        );

        if (result > 0) {

            // Copy the profile picture to the new email key.
            if (!oldImageKey.equals(newImageKey)
                    && savedImageUri != null
                    && !savedImageUri.isEmpty()) {

                boolean imageSaved = profilePreferences.edit()
                        .putString(newImageKey, savedImageUri)
                        .commit();

                if (!imageSaved) {
                    Toast.makeText(this,
                            "Profile updated, but the picture could not be saved.",
                            Toast.LENGTH_LONG).show();
                }
            }

            // Update the current login session.
            userPreferences.edit()
                    .putString("email", email)
                    .apply();

            currentEmail = email;

            profileName.setText(name);
            profileEmail.setText(email);

            loadProfileImage();

            nameInput.setVisibility(View.GONE);
            emailInput.setVisibility(View.GONE);
            addressInput.setVisibility(View.GONE);
            phoneInput.setVisibility(View.GONE);

            saveProfileButton.setVisibility(View.GONE);
            editProfileButton.setVisibility(View.VISIBLE);

            Toast.makeText(this,
                    "Profile updated successfully!",
                    Toast.LENGTH_SHORT).show();

        } else {
            Toast.makeText(this,
                    "Unable to update profile. Please try again.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // PROFILE PHOTO OPTIONS

    private void showPhotoOptions() {
        String[] options = {
                "Add / Change Photo",
                "Remove Photo"
        };

        new AlertDialog.Builder(this)
                .setTitle("Profile Picture")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        chooseProfileImage();
                    } else {
                        removeProfileImage();
                    }
                })
                .show();
    }

    // CHOOSE IMAGE

    private void chooseProfileImage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        );

        startActivityForResult(intent, PICK_IMAGE);
    }

    // IMAGE RESULT

    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != PICK_IMAGE
                || resultCode != RESULT_OK
                || data == null) {
            return;
        }

        Uri imageUri = data.getData();

        if (imageUri == null) {
            return;
        }

        try {
            getContentResolver().takePersistableUriPermission(
                    imageUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (Exception ignored) {
            // Some document providers do not support persistent permission.
        }

        String imageKey = getProfileImageKey(currentEmail);

        getSharedPreferences(PROFILE_PREFS, MODE_PRIVATE)
                .edit()
                .putString(imageKey, imageUri.toString())
                .apply();

        displayProfileImage(imageUri);

        Toast.makeText(this, "Profile picture updated!",
                Toast.LENGTH_SHORT).show();
    }

    // LOAD THE CURRENT ACCOUNT'S IMAGE

    private void loadProfileImage() {
        String imageKey = getProfileImageKey(currentEmail);

        SharedPreferences preferences =
                getSharedPreferences(PROFILE_PREFS, MODE_PRIVATE);

        String imageUri = preferences.getString(imageKey, "");

        if (imageUri == null || imageUri.isEmpty()) {
            profileImage.setImageResource(R.drawable.ic_profile_default);
            return;
        }

        try {
            displayProfileImage(Uri.parse(imageUri));
        } catch (Exception e) {
            profileImage.setImageResource(R.drawable.ic_profile_default);
        }
    }

    private void displayProfileImage(Uri imageUri) {
        try {
            profileImage.setImageURI(null);
            profileImage.setImageURI(imageUri);

            Animation animation = AnimationUtils.loadAnimation(
                    this, android.R.anim.fade_in);

            profileImage.startAnimation(animation);
        } catch (Exception e) {
            profileImage.setImageResource(R.drawable.ic_profile_default);
            Toast.makeText(this,
                    "Unable to load this picture. Please choose another.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // REMOVE ONLY THE CURRENT ACCOUNT'S IMAGE

    private void removeProfileImage() {
        String imageKey = getProfileImageKey(currentEmail);

        getSharedPreferences(PROFILE_PREFS, MODE_PRIVATE)
                .edit()
                .remove(imageKey)
                .apply();

        profileImage.setImageResource(R.drawable.ic_profile_default);

        Toast.makeText(this, "Profile picture removed",
                Toast.LENGTH_SHORT).show();
    }
}