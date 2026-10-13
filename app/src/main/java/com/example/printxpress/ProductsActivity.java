package com.example.printxpress;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class ProductsActivity extends Activity {

    Button documentButton;
    Button photoButton;
    Button posterButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products);

        documentButton = findViewById(R.id.documentButton);
        photoButton = findViewById(R.id.photoButton);
        posterButton = findViewById(R.id.posterButton);

        // Document Printing
        documentButton.setOnClickListener(v -> {

            openProduct("Document Printing");
        });

        // Photo Printing
        photoButton.setOnClickListener(v -> {

            openProduct("Photo Printing");
        });

        // Poster Printing
        posterButton.setOnClickListener(v -> {

            openProduct("Poster Printing");
        });
    }

    private void openProduct(String productName) {

        Intent intent =
                new Intent(
                        ProductsActivity.this,
                        ProductDetailsActivity.class
                );

        intent.putExtra(
                "productName",
                productName
        );

        startActivity(intent);
    }
}