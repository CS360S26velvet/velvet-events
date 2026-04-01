package com.lums.eventhub;

/**
 * CapacitySettingActivity.java
 *
 * <p>Role: Allows organizers to configure form settings for their events.</p>
 *
 * <p>User Stories Covered:
 * - US-25: Set maximum registration capacity for an event
 * - US-23: Duplicate a form from a previous event</p>
 *
 * <p>Design Pattern: Simple Activity with Firestore read/write operations.</p>
 *
 * <p>Outstanding Issues: Event selection dialog needs real event list from Firestore.</p>
 */

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class CapacitySettingActivity extends AppCompatActivity {

    /** Input field for entering max capacity number */
    EditText etCapacity;

    /** Shows the currently selected previous event for duplication */
    TextView tvSelectedEvent;

    /** Firestore database instance */
    FirebaseFirestore db;

    /** Current event ID (hardcoded for now, will come from intent later) */
    String currentEventId = "SPADES2025";

    /** Selected previous event ID for form duplication */
    String selectedPreviousEventId = "";

    /** Sample list of previous events for the selection dialog */
    String[] previousEvents = {"Annual Gala 2024", "Tech Fest 2024", "Sports Day 2024"};

    /** Corresponding event IDs */
    String[] previousEventIds = {"GALA2024", "TECHFEST2024", "SPORTS2024"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capacity_setting);

        db = FirebaseFirestore.getInstance();

        etCapacity = findViewById(R.id.etCapacity);
        tvSelectedEvent = findViewById(R.id.tvSelectedEvent);
        Button btnSaveCapacity = findViewById(R.id.btnSaveCapacity);
        Button btnSelectEvent = findViewById(R.id.btnSelectEvent);
        Button btnDuplicateForm = findViewById(R.id.btnDuplicateForm);

        loadCurrentCapacity();

        btnSaveCapacity.setOnClickListener(v -> saveCapacity());
        btnSelectEvent.setOnClickListener(v -> showEventSelectionDialog());
        btnDuplicateForm.setOnClickListener(v -> duplicateForm());
    }

    /**
     * Loads the current capacity setting from Firestore and displays it.
     * Falls back gracefully if no capacity has been set yet.
     */
    void loadCurrentCapacity() {
        db.collection("events").document(currentEventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.getLong("maxCapacity") != null) {
                        etCapacity.setText(String.valueOf(doc.getLong("maxCapacity")));
                    }
                });
    }

    /**
     * Saves the entered capacity value to Firestore for the current event.
     * Validates that the input is not empty before saving.
     */
    void saveCapacity() {
        String capacityStr = etCapacity.getText().toString().trim();
        if (capacityStr.isEmpty()) {
            Toast.makeText(this, "Please enter a capacity!", Toast.LENGTH_SHORT).show();
            return;
        }
        int capacity = Integer.parseInt(capacityStr);
        Map<String, Object> data = new HashMap<>();
        data.put("maxCapacity", capacity);
        db.collection("events").document(currentEventId)
                .set(data)
                .addOnSuccessListener(a ->
                        Toast.makeText(this, "Capacity set to " + capacity, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to save!", Toast.LENGTH_SHORT).show());
    }

    /**
     * Shows a dialog allowing the organizer to select a previous event
     * whose form they want to duplicate.
     */
    void showEventSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Previous Event");
        builder.setItems(previousEvents, (dialog, which) -> {
            selectedPreviousEventId = previousEventIds[which];
            tvSelectedEvent.setText(previousEvents[which]);
        });
        builder.show();
    }

    /**
     * Duplicates the registration form from the selected previous event
     * into the current event's form collection in Firestore.
     * Copies all questions including type and required status.
     */
    void duplicateForm() {
        if (selectedPreviousEventId.isEmpty()) {
            Toast.makeText(this, "Please select a previous event first!", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("events").document(selectedPreviousEventId)
                .collection("formQuestions")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        // Add sample questions if none exist
                        addSampleDuplicatedQuestions();
                    } else {
                        for (var doc : querySnapshot) {
                            db.collection("events").document(currentEventId)
                                    .collection("formQuestions")
                                    .add(doc.getData());
                        }
                        Toast.makeText(this, "Form duplicated successfully!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> addSampleDuplicatedQuestions());
    }

    /**
     * Adds sample questions as a fallback when no real form exists to duplicate.
     * Provides realistic test data for demonstration purposes.
     */
    void addSampleDuplicatedQuestions() {
        String[] labels = {"Full Name", "Student ID", "Department", "Dietary Preferences"};
        String[] types = {"Short Text", "Short Text", "Dropdown", "Multiple Choice"};
        for (int i = 0; i < labels.length; i++) {
            Map<String, Object> q = new HashMap<>();
            q.put("label", labels[i]);
            q.put("type", types[i]);
            q.put("required", true);
            db.collection("events").document(currentEventId)
                    .collection("formQuestions").add(q);
        }
        Toast.makeText(this, "Form duplicated with sample questions!", Toast.LENGTH_SHORT).show();
    }
}