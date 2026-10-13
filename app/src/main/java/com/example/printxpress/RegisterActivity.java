package com.example.printxpress;

import android.app.Activity;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Locale;

public class RegisterActivity extends Activity {

    EditText nameInput;
    EditText emailInput;
    EditText passwordInput;
    Button registerButton;

    DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        registerButton = findViewById(R.id.registerButton);

        databaseHelper = new DatabaseHelper(this);

        registerButton.setOnClickListener(v -> {

            String name = nameInput.getText().toString().trim();
            String email = emailInput.getText().toString()
                    .trim()
                    .toLowerCase(Locale.ROOT);
            String password = passwordInput.getText().toString();

            if (name.isEmpty()) {
                nameInput.setError("Enter your name");
                nameInput.requestFocus();
                return;
            }

            if (email.isEmpty()) {
                emailInput.setError("Enter your email");
                emailInput.requestFocus();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInput.setError("Enter a valid email address");
                emailInput.requestFocus();
                return;
            }

            if (password.trim().isEmpty()) {
                passwordInput.setError("Enter your password");
                passwordInput.requestFocus();
                return;
            }

            // Check for an existing email, ignoring letter case.
            if (databaseHelper.emailExists(email)) {
                emailInput.setError("This email is already registered");
                emailInput.requestFocus();

                Toast.makeText(
                        this,
                        "An account with this email already exists.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            boolean registered = databaseHelper.addUser(
                    name,
                    email,
                    password
            );

            if (registered) {
                Toast.makeText(
                        this,
                        "Account created successfully!",
                        Toast.LENGTH_SHORT
                ).show();

                finish();
            } else {
                Toast.makeText(
                        this,
                        "Registration failed. This email may already exist.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }
}