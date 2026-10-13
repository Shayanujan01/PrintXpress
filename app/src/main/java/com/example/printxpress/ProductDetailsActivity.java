package com.example.printxpress;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ProductDetailsActivity extends Activity {

    EditText quantityInput;
    EditText instructionsInput;
    RadioGroup orderMethodGroup;
    Button placeOrderButton;
    Button uploadButton;
    TextView fileNameText;
    TextView productName;
    TextView productDescription;
    TextView productPrice;
    Spinner paperTypeSpinner;
    Spinner paperSizeSpinner;
    Spinner printTypeSpinner;
    DatabaseHelper databaseHelper;
    String selectedFileName = "";
    String selectedProduct = "Document Printing";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_details);

        productName = findViewById(R.id.productName);
        productDescription = findViewById(R.id.productDescription);
        productPrice = findViewById(R.id.productPrice);
        quantityInput = findViewById(R.id.quantityInput);
        instructionsInput = findViewById(R.id.instructionsInput);
        orderMethodGroup = findViewById(R.id.orderMethodGroup);
        placeOrderButton = findViewById(R.id.placeOrderButton);
        uploadButton = findViewById(R.id.uploadButton);
        fileNameText = findViewById(R.id.fileNameText);
        paperTypeSpinner = findViewById(R.id.paperTypeSpinner);
        paperSizeSpinner = findViewById(R.id.paperSizeSpinner);
        printTypeSpinner = findViewById(R.id.printTypeSpinner);
        databaseHelper = new DatabaseHelper(this);

        String productFromIntent = getIntent().getStringExtra("productName");
        if (productFromIntent != null) selectedProduct = productFromIntent;
        showProductDetails();

        uploadButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            try {
                startActivityForResult(intent, 100);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "No file manager found", Toast.LENGTH_SHORT).show();
            }
        });

        placeOrderButton.setOnClickListener(v -> placeOrder());
    }

    private void placeOrder() {
        String quantityText = quantityInput.getText().toString().trim();
        if (quantityText.isEmpty()) {
            quantityInput.setError("Enter quantity");
            quantityInput.requestFocus();
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityText);
        } catch (NumberFormatException e) {
            quantityInput.setError("Enter a valid number");
            quantityInput.requestFocus();
            return;
        }

        if (quantity <= 0) {
            quantityInput.setError("Quantity must be greater than 0");
            quantityInput.requestFocus();
            return;
        }
        if (quantity > 1000) {
            quantityInput.setError("Maximum quantity is 1000");
            quantityInput.requestFocus();
            return;
        }

        int selectedOption = orderMethodGroup.getCheckedRadioButtonId();
        if (selectedOption == -1) {
            Toast.makeText(this, "Please choose Pickup or Delivery", Toast.LENGTH_SHORT).show();
            return;
        }

        String orderMethod = selectedOption == R.id.pickupOption ? "Pickup" : "Delivery";
        String paperType = paperTypeSpinner.getSelectedItem().toString();
        String paperSize = paperSizeSpinner.getSelectedItem().toString();
        String printType = printTypeSpinner.getSelectedItem().toString();
        String instructions = instructionsInput.getText().toString().trim();
        if (instructions.isEmpty()) instructions = "No special instructions";
        String fileName = selectedFileName.isEmpty() ? "No file selected" : selectedFileName;

        SharedPreferences userPreferences = getSharedPreferences("PrintXpressUser", MODE_PRIVATE);
        int userId = userPreferences.getInt("user_id", -1);
        if (userId <= 0) {
            Toast.makeText(this, "Please log in again before placing an order.", Toast.LENGTH_LONG).show();
            return;
        }

        boolean inserted = databaseHelper.addOrder(
                userId,
                selectedProduct,
                quantity,
                orderMethod,
                "Pending",
                fileName,
                paperType,
                paperSize,
                printType,
                instructions
        );

        if (inserted) {
            getSharedPreferences("PrintXpressOrders", MODE_PRIVATE).edit()
                    .putString("product", selectedProduct)
                    .putString("quantity", quantityText)
                    .putString("orderMethod", orderMethod)
                    .putString("status", "Pending")
                    .putString("fileName", fileName)
                    .putString("paperType", paperType)
                    .putString("paperSize", paperSize)
                    .putString("printType", printType)
                    .putString("instructions", instructions)
                    .apply();

            Toast.makeText(this, "Order placed successfully!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to place order. Please log in again and retry.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showProductDetails() {
        if (selectedProduct.equals("Document Printing")) {
            productName.setText("Document Printing");
            productDescription.setText("High quality document printing service.");
            productPrice.setText("Price: LKR 20 per page");
        } else if (selectedProduct.equals("Photo Printing")) {
            productName.setText("Photo Printing");
            productDescription.setText("High quality photo printing service.");
            productPrice.setText("Price: LKR 50 per photo");
        } else if (selectedProduct.equals("Poster Printing")) {
            productName.setText("Poster Printing");
            productDescription.setText("High quality poster printing service.");
            productPrice.setText("Price: LKR 500 per poster");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            if (fileUri != null) {
                selectedFileName = fileUri.getLastPathSegment();
                fileNameText.setText("Selected file: " + selectedFileName);
            }
        }
    }
}
