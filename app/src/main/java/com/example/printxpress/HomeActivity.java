package com.example.printxpress;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class HomeActivity extends Activity {

    Button productsButton;
    Button ordersButton;
    Button profileButton;
    Button supportButton;
    Button notificationsButton;
    Button logoutButton;
    TextView orderStatusText;
    DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        productsButton = findViewById(R.id.productsButton);
        ordersButton = findViewById(R.id.ordersButton);
        profileButton = findViewById(R.id.profileButton);
        supportButton = findViewById(R.id.supportButton);
        notificationsButton = findViewById(R.id.notificationsButton);
        logoutButton = findViewById(R.id.logoutButton);
        orderStatusText = findViewById(R.id.orderStatusText);
        databaseHelper = new DatabaseHelper(this);

        productsButton.setOnClickListener(v -> startActivity(new Intent(this, ProductsActivity.class)));
        ordersButton.setOnClickListener(v -> startActivity(new Intent(this, OrdersActivity.class)));
        profileButton.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        supportButton.setOnClickListener(v -> startActivity(new Intent(this, SupportActivity.class)));
        notificationsButton.setOnClickListener(v -> startActivity(new Intent(this, NotificationsActivity.class)));

        logoutButton.setOnClickListener(v -> {
            getSharedPreferences("PrintXpressUser", MODE_PRIVATE).edit().clear().apply();
            Intent intent = new Intent(HomeActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        showLatestOrder();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (databaseHelper != null && orderStatusText != null) showLatestOrder();
    }

    private void showLatestOrder() {
        int userId = getSharedPreferences("PrintXpressUser", MODE_PRIVATE)
                .getInt("user_id", -1);

        if (userId <= 0) {
            orderStatusText.setText("Please log in to view your orders.");
            return;
        }

        Cursor cursor = databaseHelper.getOrdersForUser(userId);
        try {
            if (cursor.moveToFirst()) {
                String product = cursor.getString(cursor.getColumnIndexOrThrow("product"));
                String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
                orderStatusText.setText("Latest Order\n\nProduct: " + product + "\nStatus: " + status);
            } else {
                orderStatusText.setText("No orders yet.");
            }
        } finally {
            cursor.close();
        }
    }
}
