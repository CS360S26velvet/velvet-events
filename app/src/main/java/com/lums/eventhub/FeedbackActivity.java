package com.lums.eventhub;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * FeedbackActivity.java
 *
 * AT US-19 — Allows attendee to submit a star rating and written
 * feedback for an event they attended.
 *
 * AT US-20 — Provides an anonymous toggle. When checked, userId
 * is omitted from both Firestore writes so the organizer cannot
 * identify the respondent.
 *
 * Firestore writes (on submit):
 * 1. events/{eventId}/feedback/{feedbackId}  ← organizer can read
 * 2. users/{userId}/feedback/{feedbackId}    ← user's own record
 *    (write 2 is skipped entirely if anonymous is checked)
 *
 * Launched from MyRegistrationsActivity — only for past events
 * (event date already passed).
 *
 * Receives via Intent:
 * - userId, eventId, eventTitle, eventOrganizer, eventDate
 */
public class FeedbackActivity extends AppCompatActivity {

    private TextView tvEventTitle;
    private TextView tvEventOrganizer;
    private TextView tvEventDate;
    private LinearLayout starRow;
    private TextView tvRatingLabel;
    private EditText etFeedback;
    private CheckBox cbAnonymous;
    private Button btnSubmit;
    private Button btnBack;

    private FirebaseFirestore db;

    private String userId;
    private String eventId;
    private String eventTitle;
    private String eventOrganizer;
    private String eventDate;

    private int selectedRating = 0; // 1–5, 0 means not yet selected
    private TextView[] stars = new TextView[5];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        Intent intent = getIntent();
        userId        = intent.getStringExtra("userId");
        eventId       = intent.getStringExtra("eventId");
        eventTitle    = intent.getStringExtra("eventTitle");
        eventOrganizer = intent.getStringExtra("eventOrganizer");
        eventDate     = intent.getStringExtra("eventDate");

        tvEventTitle    = findViewById(R.id.tvFeedbackEventTitle);
        tvEventOrganizer = findViewById(R.id.tvFeedbackOrganizer);
        tvEventDate     = findViewById(R.id.tvFeedbackDate);
        starRow         = findViewById(R.id.starRow);
        tvRatingLabel   = findViewById(R.id.tvRatingLabel);
        etFeedback      = findViewById(R.id.etFeedbackText);
        cbAnonymous     = findViewById(R.id.cbAnonymous);
        btnSubmit       = findViewById(R.id.btnSubmitFeedback);
        btnBack         = findViewById(R.id.btnFeedbackBack);

        db = FirebaseFirestore.getInstance();

        etFeedback.setHintTextColor(0xFFCCCCCC);

        populateEventInfo();
        buildStarRating();

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitFeedback();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    private void populateEventInfo() {
        tvEventTitle.setText(eventTitle != null ? eventTitle : "—");
        tvEventOrganizer.setText(eventOrganizer != null ? "by " + eventOrganizer : "");
        tvEventDate.setText(eventDate != null ? eventDate : "");
    }

    /**
     * Builds 5 star TextViews programmatically inside starRow.
     * Tapping a star fills all stars up to and including that one.
     */
    private void buildStarRating() {
        starRow.removeAllViews();

        for (int i = 0; i < 5; i++) {
            final int starIndex = i + 1; // 1-based rating

            TextView star = new TextView(this);
            star.setText("☆");
            star.setTextSize(36);
            star.setTextColor(0xFFCCCCCC);
            star.setTypeface(null, Typeface.BOLD);
            star.setGravity(Gravity.CENTER);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(6, 0, 6, 0);
            star.setLayoutParams(params);

            star.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedRating = starIndex;
                    updateStarDisplay();
                }
            });

            stars[i] = star;
            starRow.addView(star);
        }
    }

    /**
     * Fills stars up to selectedRating, empties the rest.
     * Also updates the rating label text.
     */
    private void updateStarDisplay() {
        String[] labels = {"", "Poor", "Fair", "Good", "Very Good", "Excellent"};

        for (int i = 0; i < 5; i++) {
            if (i < selectedRating) {
                stars[i].setText("★");
                stars[i].setTextColor(0xFFFFC107); // amber filled
            } else {
                stars[i].setText("☆");
                stars[i].setTextColor(0xFFCCCCCC); // grey empty
            }
        }

        if (selectedRating > 0) {
            tvRatingLabel.setText(labels[selectedRating]);
            tvRatingLabel.setVisibility(View.VISIBLE);
        } else {
            tvRatingLabel.setVisibility(View.GONE);
        }
    }

    /**
     * Validates input then writes to Firestore.
     *
     * AT US-19 — writes rating + feedback text to:
     *   events/{eventId}/feedback/{auto-id}
     *   users/{userId}/feedback/{auto-id}  (skipped if anonymous)
     *
     * AT US-20 — if anonymous checkbox is checked, userId is
     * replaced with "anonymous" in the events collection write,
     * and the users collection write is skipped entirely.
     */
    private void submitFeedback() {
        if (selectedRating == 0) {
            Toast.makeText(this, "Please select a star rating", Toast.LENGTH_SHORT).show();
            return;
        }

        String feedbackText = etFeedback.getText().toString().trim();
        boolean isAnonymous = cbAnonymous.isChecked();

        // Build the feedback document
        Map<String, Object> feedbackData = new HashMap<>();
        feedbackData.put("eventId",      eventId);
        feedbackData.put("eventTitle",   eventTitle);
        feedbackData.put("rating",       selectedRating);
        feedbackData.put("feedback",     feedbackText);
        feedbackData.put("anonymous",    isAnonymous);
        feedbackData.put("submittedAt",  FieldValue.serverTimestamp());

        // AT US-20 — omit real userId if anonymous
        feedbackData.put("userId", isAnonymous ? "anonymous" : userId);

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Submitting...");

        // Write 1: events/{eventId}/feedback — organizer can read this
        db.collection("events")
                .document(eventId)
                .collection("feedback")
                .add(feedbackData)
                .addOnSuccessListener(docRef -> {

                    // Write 2: users/{userId}/feedback — skipped if anonymous (AT US-20)
                    if (!isAnonymous) {
                        Map<String, Object> userFeedbackData = new HashMap<>(feedbackData);
                        userFeedbackData.put("feedbackId", docRef.getId()); // link back to event feedback doc

                        db.collection("users")
                                .document(userId)
                                .collection("feedback")
                                .document(docRef.getId()) // same doc ID for easy cross-reference
                                .set(userFeedbackData)
                                .addOnFailureListener(e -> {
                                    // Non-critical — event feedback already saved
                                });
                    }

                    Toast.makeText(this,
                            "Thank you for your feedback!", Toast.LENGTH_SHORT).show();
                    finish(); // go back to MyRegistrationsActivity
                })
                .addOnFailureListener(e -> {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Submit Feedback");
                    Toast.makeText(this,
                            "Failed to submit: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}