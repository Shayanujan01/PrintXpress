package com.example.printxpress;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SupportActivity extends Activity {

    private View emailCard;
    private View phoneCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support);

        emailCard = findViewById(R.id.emailCard);
        phoneCard = findViewById(R.id.phoneCard);

        setupFaq(findViewById(R.id.faqCard1));
        setupFaq(findViewById(R.id.faqCard2));
        setupFaq(findViewById(R.id.faqCard3));

        setupContactActions();
    }

    private void setupFaq(View faqCard) {
        if (!(faqCard instanceof ViewGroup)) {
            return;
        }

        ViewGroup card = (ViewGroup) faqCard;

        if (card.getChildCount() < 2) {
            return;
        }

        View questionView = card.getChildAt(0);
        View answerView = card.getChildAt(1);

        if (!(questionView instanceof TextView)) {
            return;
        }

        if (!(answerView instanceof TextView)) {
            return;
        }

        TextView question = (TextView) questionView;
        TextView answer = (TextView) answerView;

        // Answers start hidden.
        answer.setVisibility(View.GONE);

        card.setClickable(true);
        card.setFocusable(true);

        question.setCompoundDrawablesWithIntrinsicBounds(
                0, 0, android.R.drawable.arrow_down_float, 0
        );

        card.setOnClickListener(v -> {
            boolean isExpanded = answer.getVisibility() == View.VISIBLE;

            if (isExpanded) {
                answer.setVisibility(View.GONE);
                question.setCompoundDrawablesWithIntrinsicBounds(
                        0, 0, android.R.drawable.arrow_down_float, 0
                );
            } else {
                answer.setVisibility(View.VISIBLE);
                question.setCompoundDrawablesWithIntrinsicBounds(
                        0, 0, android.R.drawable.arrow_up_float, 0
                );
            }
        });
    }

    private void setupContactActions() {

        emailCard.setOnClickListener(v -> {
            Intent intent = new Intent(
                    Intent.ACTION_SENDTO,
                    Uri.parse("mailto:printxpress@gmail.com")
            );

            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(
                        this,
                        "No email application is available on this device.",
                        Toast.LENGTH_LONG
                ).show();
            }
        });

        phoneCard.setOnClickListener(v -> {
            Intent intent = new Intent(
                    Intent.ACTION_DIAL,
                    Uri.parse("tel:+947712345678")
            );

            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(
                        this,
                        "Unable to open the phone dialler.",
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }
}