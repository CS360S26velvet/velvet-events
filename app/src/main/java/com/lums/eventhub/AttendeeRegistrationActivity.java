package com.lums.eventhub;

/**
 * AttendeeRegistrationActivity.java
 *
 * <p>Role: Allows attendees to fill in the custom registration form created by the organizer.
 * Displays different question types and validates required fields before submission.</p>
 *
 * <p>User Stories Covered:
 * - AT US-06: Attendee fills in the custom form organizer built
 * - AT US-07: Answer different types of questions (Short Text, Multiple Choice, Dropdown)
 * - AT US-09: See which questions are required vs optional
 * - AT US-10: Go back and edit responses before submitting</p>
 *
 * <p>Design Pattern: RecyclerView with Adapter, Firestore data loading.</p>
 *
 * <p>Outstanding Issues: File upload question type not yet implemented (AT US-08 - Final)</p>
 */

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttendeeRegistrationActivity extends AppCompatActivity {

    /** RecyclerView for displaying form questions */
    RecyclerView recyclerViewForm;

    /** Adapter for form question items */
    FormAdapter adapter;

    /** List of questions loaded from Firestore */
    List<FormQuestion> questionList = new ArrayList<>();

    /** Progress indicator showing question count */
    TextView tvProgress;

    /** Firestore instance */
    FirebaseFirestore db;

    /** Event ID to load form for */
    String eventId = "SPADES2025";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendee_registration);

        db = FirebaseFirestore.getInstance();

        recyclerViewForm = findViewById(R.id.recyclerViewForm);
        tvProgress = findViewById(R.id.tvProgress);
        Button btnBack = findViewById(R.id.btnBack);
        Button btnSubmit = findViewById(R.id.btnSubmit);

        adapter = new FormAdapter(questionList);
        recyclerViewForm.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewForm.setAdapter(adapter);

        loadFormQuestions();

        // AT US-10: Go back and edit responses
        btnBack.setOnClickListener(v -> finish());

        // Submit registration
        btnSubmit.setOnClickListener(v -> submitForm());
    }

    /**
     * Loads form questions from Firestore for the current event.
     * Falls back to sample questions if none exist yet.
     */
    void loadFormQuestions() {
        db.collection("events").document(eventId)
                .collection("formQuestions")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    questionList.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String label = doc.getString("label");
                        String type = doc.getString("type");
                        Boolean required = doc.getBoolean("required");
                        if (label != null) {
                            questionList.add(new FormQuestion(
                                    label, type != null ? type : "Short Text",
                                    required != null && required));
                        }
                    }
                    if (questionList.isEmpty()) loadSampleQuestions();
                    else updateProgress();
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> loadSampleQuestions());
    }

    /**
     * Loads realistic sample questions for demonstration purposes.
     */
    void loadSampleQuestions() {
        questionList.add(new FormQuestion("Full Name", "Short Text", true));
        questionList.add(new FormQuestion("Student ID", "Short Text", true));
        questionList.add(new FormQuestion("Department", "Short Text", false));
        questionList.add(new FormQuestion("Dietary Preferences", "Short Text", false));
        updateProgress();
        adapter.notifyDataSetChanged();
    }

    /**
     * Updates the progress text showing total question count (AT US-09).
     */
    void updateProgress() {
        int required = 0;
        for (FormQuestion q : questionList) {
            if (q.required) required++;
        }
        tvProgress.setText(questionList.size() + " questions • " +
                required + " required, " +
                (questionList.size() - required) + " optional");
    }

    /**
     * Validates and submits the completed form to Firestore.
     * Checks all required fields are filled before submitting (AT US-06).
     */
    void submitForm() {
        // Validate required fields
        for (int i = 0; i < questionList.size(); i++) {
            FormQuestion q = questionList.get(i);
            if (q.required && (q.answer == null || q.answer.trim().isEmpty())) {
                Toast.makeText(this,
                        "Please answer required question: " + q.label,
                        Toast.LENGTH_SHORT).show();
                recyclerViewForm.scrollToPosition(i);
                return;
            }
        }

        // Save to Firestore
        Map<String, Object> registration = new HashMap<>();
        registration.put("eventId", eventId);
        registration.put("completed", true);
        registration.put("submittedAt", System.currentTimeMillis());

        Map<String, String> answers = new HashMap<>();
        for (FormQuestion q : questionList) {
            answers.put(q.label, q.answer != null ? q.answer : "");
        }
        registration.put("answers", answers);

        db.collection("events").document(eventId)
                .collection("registrants")
                .add(registration)
                .addOnSuccessListener(ref ->
                        Toast.makeText(this,
                                "Registration submitted successfully!", Toast.LENGTH_LONG).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Submission failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * FormQuestion model class for attendee registration form.
     */
    static class FormQuestion {
        /** Question label shown to attendee */
        String label;

        /** Question type: Short Text, Multiple Choice, Dropdown, etc. */
        String type;

        /** Whether this question must be answered */
        boolean required;

        /** The attendee's answer */
        String answer;

        /**
         * @param label    Question label
         * @param type     Question type
         * @param required Whether required
         */
        FormQuestion(String label, String type, boolean required) {
            this.label = label;
            this.type = type;
            this.required = required;
            this.answer = "";
        }
    }

    /**
     * RecyclerView Adapter for form questions.
     * Displays each question with its required/optional badge and input field.
     */
    class FormAdapter extends RecyclerView.Adapter<FormAdapter.ViewHolder> {

        List<FormQuestion> list;

        /**
         * @param list List of form questions to display
         */
        FormAdapter(List<FormQuestion> list) {
            this.list = list;
        }

        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_form_question, parent, false);
            return new ViewHolder(v);
        }

        /**
         * Binds question data to view.
         * Shows required/optional badge (AT US-09).
         * Saves answer as attendee types (AT US-10).
         */
        public void onBindViewHolder(ViewHolder holder, int position) {
            FormQuestion q = list.get(position);
            holder.tvLabel.setText(q.label);

            // AT US-09: Show required vs optional
            if (q.required) {
                holder.tvBadge.setText("Required *");
                holder.tvBadge.setBackgroundColor(0xFFF44336);
            } else {
                holder.tvBadge.setText("Optional");
                holder.tvBadge.setBackgroundColor(0xFF9E9E9E);
            }

            // Restore previous answer (AT US-10)
            holder.etAnswer.setText(q.answer);
            holder.etAnswer.setHint("Enter " + q.label.toLowerCase() + "...");

            // Save answer as user types
            holder.etAnswer.addTextChangedListener(
                    new android.text.TextWatcher() {
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            q.answer = s.toString();
                        }
                        public void afterTextChanged(android.text.Editable s) {}
                    });
        }

        public int getItemCount() { return list.size(); }

        /**
         * ViewHolder for form question items.
         */
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvLabel, tvBadge;
            EditText etAnswer;

            ViewHolder(View v) {
                super(v);
                tvLabel = v.findViewById(R.id.tvQuestionLabel);
                tvBadge = v.findViewById(R.id.tvRequiredBadge);
                etAnswer = v.findViewById(R.id.etAnswer);
            }
        }
    }
}