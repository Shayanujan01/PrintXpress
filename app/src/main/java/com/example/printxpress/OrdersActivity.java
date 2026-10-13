package com.example.printxpress;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class OrdersActivity extends Activity {

    private LinearLayout orderContainer;
    private DatabaseHelper databaseHelper;

    private final int navy = Color.rgb(13, 27, 42);
    private final int blue = Color.rgb(25, 118, 210);
    private final int lightBlue = Color.rgb(227, 242, 253);
    private final int primaryText = Color.rgb(23, 32, 42);
    private final int secondaryText = Color.rgb(117, 117, 117);
    private final int borderColor = Color.rgb(230, 235, 241);
    private final int green = Color.rgb(46, 125, 50);
    private final int red = Color.rgb(198, 40, 40);
    private final int orange = Color.rgb(230, 126, 34);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders);

        orderContainer = findViewById(R.id.orderContainer);
        databaseHelper = new DatabaseHelper(this);

        loadOrders();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (databaseHelper != null && orderContainer != null) {
            loadOrders();
        }
    }

    private void loadOrders() {
        orderContainer.removeAllViews();

        int userId = getSharedPreferences("PrintXpressUser", MODE_PRIVATE)
                .getInt("user_id", -1);

        if (userId <= 0) {
            showEmptyState("Please log in", "Log in to view your orders.");
            return;
        }

        Cursor cursor = databaseHelper.getOrdersForUser(userId);

        if (cursor == null) {
            showEmptyState(
                    "Unable to load orders",
                    "Please try again."
            );
            return;
        }

        boolean hasOrders = false;

        try {
            if (cursor.moveToFirst()) {
                hasOrders = true;

                do {
                    int orderId = cursor.getInt(
                            cursor.getColumnIndexOrThrow("id")
                    );

                    int quantity = cursor.getInt(
                            cursor.getColumnIndexOrThrow("quantity")
                    );

                    String product = getColumnText(cursor, "product");
                    String method = getColumnText(cursor, "order_method");
                    String status = getColumnText(cursor, "status");
                    String file = getColumnText(cursor, "file_name");
                    String schedule = getColumnText(cursor, "schedule");
                    String paperType = getColumnText(cursor, "paper_type");
                    String paperSize = getColumnText(cursor, "paper_size");
                    String printType = getColumnText(cursor, "print_type");
                    String instructions = getColumnText(cursor, "instructions");

                    addOrderCard(
                            orderId, product, quantity, method, status,
                            file, schedule, paperType, paperSize,
                            printType, instructions
                    );

                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            showEmptyState(
                    "Unable to display orders",
                    "Please reopen My Orders and try again."
            );
        } finally {
            cursor.close();
        }

        if (!hasOrders) {
            showEmptyState(
                    "No orders yet",
                    "Your printing orders will appear here.\n\n"
                            + "Choose a service from Browse Products to get started."
            );
        }
    }

    private String getColumnText(Cursor cursor, String columnName) {
        int index = cursor.getColumnIndex(columnName);

        if (index == -1 || cursor.isNull(index)) {
            return "Not specified";
        }

        String value = cursor.getString(index);

        if (value == null || value.trim().isEmpty()) {
            return "Not specified";
        }

        return value;
    }

    private void addOrderCard(
            int orderId,
            String product,
            int quantity,
            String method,
            String status,
            String file,
            String schedule,
            String paperType,
            String paperSize,
            String printType,
            String instructions) {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(createRoundedBackground(
                Color.WHITE, borderColor, 18
        ));
        card.setElevation(dp(2));

        TextView title = new TextView(this);
        title.setText(product);
        title.setTextSize(19);
        title.setTextColor(navy);
        title.setTypeface(null, Typeface.BOLD);
        card.addView(title);

        TextView orderNumber = new TextView(this);
        orderNumber.setText("ORDER #" + orderId);
        orderNumber.setTextSize(12);
        orderNumber.setTextColor(secondaryText);
        orderNumber.setLetterSpacing(0.06f);

        LinearLayout.LayoutParams numberParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
        numberParams.topMargin = dp(5);
        card.addView(orderNumber, numberParams);

        TextView statusLabel = new TextView(this);
        statusLabel.setText(status.toUpperCase(Locale.getDefault()));
        statusLabel.setTextSize(12);
        statusLabel.setTypeface(null, Typeface.BOLD);
        statusLabel.setTextColor(getStatusColor(status));
        statusLabel.setPadding(dp(12), dp(7), dp(12), dp(7));
        statusLabel.setBackground(createRoundedBackground(
                getStatusBackground(status),
                getStatusBackground(status),
                30
        ));

        LinearLayout.LayoutParams statusParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
        statusParams.topMargin = dp(12);
        card.addView(statusLabel, statusParams);

        android.view.View separator = new android.view.View(this);
        separator.setBackgroundColor(borderColor);

        LinearLayout.LayoutParams dividerParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(1)
                );
        dividerParams.topMargin = dp(16);
        dividerParams.bottomMargin = dp(14);
        card.addView(separator, dividerParams);

        addDetail(card, "Quantity", String.valueOf(quantity));
        addDetail(card, "Order method", method);
        addDetail(card, "Paper type", paperType);
        addDetail(card, "Paper size", paperSize);
        addDetail(card, "Print type", printType);
        addDetail(card, "File", file);
        addDetail(card, "Date and time", schedule);
        addDetail(card, "Special instructions", instructions);

        if (canManageOrder(status)) {
            LinearLayout buttons = new LinearLayout(this);
            buttons.setOrientation(LinearLayout.HORIZONTAL);
            buttons.setGravity(Gravity.CENTER_VERTICAL);

            Button cancelButton = createActionButton(
                    "Cancel", Color.WHITE, red
            );

            Button rescheduleButton = createActionButton(
                    "Reschedule", blue, blue
            );

            LinearLayout.LayoutParams cancelParams =
                    new LinearLayout.LayoutParams(
                            0, dp(48), 1
                    );
            cancelParams.rightMargin = dp(8);

            LinearLayout.LayoutParams rescheduleParams =
                    new LinearLayout.LayoutParams(
                            0, dp(48), 1
                    );
            rescheduleParams.leftMargin = dp(8);

            buttons.addView(cancelButton, cancelParams);
            buttons.addView(rescheduleButton, rescheduleParams);

            LinearLayout.LayoutParams buttonsParams =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
            buttonsParams.topMargin = dp(18);
            card.addView(buttons, buttonsParams);

            cancelButton.setOnClickListener(v ->
                    new AlertDialog.Builder(OrdersActivity.this)
                            .setTitle("Cancel order?")
                            .setMessage(
                                    "Are you sure you want to cancel order #"
                                            + orderId + "?"
                            )
                            .setNegativeButton("Keep order", null)
                            .setPositiveButton("Cancel order",
                                    (dialog, which) -> {
                                        int userId = getSharedPreferences(
                                                "PrintXpressUser", MODE_PRIVATE
                                        ).getInt("user_id", -1);
                                        boolean cancelled = userId > 0
                                                && databaseHelper.cancelOrder(orderId, userId);
                                        Toast.makeText(
                                                OrdersActivity.this,
                                                cancelled ? "Order cancelled." : "Unable to cancel this order.",
                                                Toast.LENGTH_SHORT
                                        ).show();
                                        loadOrders();
                                    })
                            .show()
            );

            rescheduleButton.setOnClickListener(
                    v -> showRescheduleDialog(orderId)
            );
        }

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
        cardParams.bottomMargin = dp(16);

        orderContainer.addView(card, cardParams);
    }

    private void addDetail(
            LinearLayout card,
            String label,
            String value) {

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(5), 0, dp(5));

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextSize(12);
        labelView.setTextColor(secondaryText);

        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextSize(14);
        valueView.setTextColor(primaryText);

        LinearLayout.LayoutParams valueParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
        valueParams.topMargin = dp(2);

        row.addView(labelView);
        row.addView(valueView, valueParams);
        card.addView(row);
    }

    private Button createActionButton(
            String text,
            int fillColor,
            int strokeColor) {

        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setTypeface(null, Typeface.BOLD);
        button.setTextColor(
                fillColor == Color.WHITE ? strokeColor : Color.WHITE
        );
        button.setPadding(dp(6), 0, dp(6), 0);
        button.setBackground(createRoundedBackground(
                fillColor, strokeColor, 12
        ));

        return button;
    }

    private void showEmptyState(String titleText, String messageText) {
        orderContainer.removeAllViews();

        LinearLayout emptyCard = new LinearLayout(this);
        emptyCard.setOrientation(LinearLayout.VERTICAL);
        emptyCard.setGravity(Gravity.CENTER);
        emptyCard.setPadding(dp(24), dp(30), dp(24), dp(30));
        emptyCard.setBackground(createRoundedBackground(
                Color.WHITE, borderColor, 18
        ));

        TextView title = new TextView(this);
        title.setText(titleText);
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(navy);
        title.setGravity(Gravity.CENTER);

        TextView message = new TextView(this);
        message.setText(messageText);
        message.setTextSize(14);
        message.setTextColor(secondaryText);
        message.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams messageParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
        messageParams.topMargin = dp(10);

        emptyCard.addView(title);
        emptyCard.addView(message, messageParams);
        orderContainer.addView(emptyCard);
    }

    private boolean canManageOrder(String status) {
        return !status.equalsIgnoreCase("Cancelled")
                && !status.equalsIgnoreCase("Printing")
                && !status.equalsIgnoreCase("Completed");
    }

    private int getStatusColor(String status) {
        if (status.equalsIgnoreCase("Cancelled")) {
            return red;
        }

        if (status.equalsIgnoreCase("Completed")) {
            return green;
        }

        if (status.equalsIgnoreCase("Printing")) {
            return blue;
        }

        return orange;
    }

    private int getStatusBackground(String status) {
        if (status.equalsIgnoreCase("Cancelled")) {
            return Color.rgb(255, 235, 238);
        }

        if (status.equalsIgnoreCase("Completed")) {
            return Color.rgb(232, 245, 233);
        }

        if (status.equalsIgnoreCase("Printing")) {
            return lightBlue;
        }

        return Color.rgb(255, 243, 224);
    }

    private GradientDrawable createRoundedBackground(
            int fillColor,
            int strokeColor,
            int radiusDp) {

        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(1), strokeColor);

        return drawable;
    }

    private int dp(int value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    // Date picker followed by time picker
    private void showRescheduleDialog(int orderId) {

        Calendar now = Calendar.getInstance();
        Calendar selectedDate = Calendar.getInstance();

        // Default to tomorrow.
        selectedDate.add(Calendar.DAY_OF_MONTH, 1);

        DatePickerDialog datePicker = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {

                    selectedDate.set(Calendar.YEAR, year);
                    selectedDate.set(Calendar.MONTH, month);
                    selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    Calendar currentTime = Calendar.getInstance();

                    TimePickerDialog timePicker = new TimePickerDialog(
                            OrdersActivity.this,
                            (timeView, hourOfDay, minute) -> {

                                selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                selectedDate.set(Calendar.MINUTE, minute);
                                selectedDate.set(Calendar.SECOND, 0);
                                selectedDate.set(Calendar.MILLISECOND, 0);

                                Calendar current = Calendar.getInstance();

                                if (selectedDate.before(current)) {
                                    Toast.makeText(
                                            OrdersActivity.this,
                                            "Please choose a future date and time.",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    showRescheduleDialog(orderId);
                                    return;
                                }

                                SimpleDateFormat formatter =
                                        new SimpleDateFormat(
                                                "dd MMM yyyy, hh:mm a",
                                                Locale.getDefault()
                                        );

                                String newSchedule =
                                        formatter.format(selectedDate.getTime());

                                new AlertDialog.Builder(OrdersActivity.this)
                                        .setTitle("Confirm new schedule")
                                        .setMessage(
                                                "Order #" + orderId
                                                        + "\n\nNew date and time:\n"
                                                        + newSchedule
                                        )
                                        .setNegativeButton("Choose again",
                                                (dialog, which) ->
                                                        showRescheduleDialog(orderId))
                                        .setPositiveButton("Save schedule",
                                                (dialog, which) -> {
                                                    int userId = getSharedPreferences(
                                                            "PrintXpressUser", MODE_PRIVATE
                                                    ).getInt("user_id", -1);
                                                    boolean updated = userId > 0
                                                            && databaseHelper.rescheduleOrder(
                                                                    orderId,
                                                                    userId,
                                                                    newSchedule
                                                            );

                                                    Toast.makeText(
                                                            OrdersActivity.this,
                                                            updated ? "Order schedule updated." : "Unable to update this order.",
                                                            Toast.LENGTH_SHORT
                                                    ).show();

                                                    loadOrders();
                                                })
                                        .show();
                            },
                            currentTime.get(Calendar.HOUR_OF_DAY),
                            currentTime.get(Calendar.MINUTE),
                            false
                    );

                    timePicker.setTitle("Choose a time");
                    timePicker.show();
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );

        // Do not allow selecting a date before today.
        datePicker.getDatePicker().setMinDate(now.getTimeInMillis());
        datePicker.setTitle("Choose a new date");
        datePicker.show();
    }
}