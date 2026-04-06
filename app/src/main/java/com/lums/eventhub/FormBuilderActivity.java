package com.lums.eventhub;

/**
 * FormBuilderActivity.java
 *
 * Role: Organizer builds / edits the registration form for a specific event.
 * This IS the registration form — building it = defining what attendees will fill.
 *
 * Flow:
 *   - Receives eventId + eventName from AttendeeRegistrationActivity via Intent extras
 *   - Loads existing saved questions from events/{eventId}/formQuestions (if any)
 *   - If no saved questions: blank builder (create from scratch)
 *   - Organizer adds/edits/reorders/duplicates/deletes questions
 *   - Preview: shows an AlertDialog simulating how attendees see the form
 *   - Save: writes all questions to Firestore as events/{eventId}/formQuestions
 *           and sets events/{eventId}.formActive = true so attendees can see it
 *
 * Wires to existing IDs in activity_form_builder.xml:
 *   btnPreview, btnSaveForm,
 *   btnAddShortText, btnAddParagraph, btnAddMultiChoice,
 *   btnAddDropdown, btnAddDate, btnAddFileUpload,
 *   recyclerViewQuestions
 *
 * Note: organizerId == societyId — same concept, using organizerId everywhere.
 *
 * User Stories: Org US-18, US-19, US-22
 */

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FormBuilderActivity extends AppCompatActivity {

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private RecyclerView       recyclerViewQuestions;
    private QuestionAdapter    adapter;
    private List<FormQuestion> questions = new ArrayList<>();
    private FirebaseFirestore  db;

    private String eventId;
    private String eventName;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form_builder);

        db        = FirebaseFirestore.getInstance();
        eventId   = getIntent().getStringExtra("eventId");
        eventName = getIntent().getStringExtra("eventName");

        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "No event selected.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Update header title to show which event this form is for
        TextView headerTitle = findViewById(R.id.tvFormBuilderTitle);
        if (headerTitle != null) {
            headerTitle.setText("Form Builder — " + eventName);
        }

        setupRecyclerView();
        wireTypeButtons();
        wirePreview();
        wireSave();
        loadExistingQuestions();
    }

    // -------------------------------------------------------------------------
    // RecyclerView setup
    // -------------------------------------------------------------------------

    private void setupRecyclerView() {
        recyclerViewQuestions = findViewById(R.id.recyclerViewQuestions);
        adapter               = new QuestionAdapter(questions);
        recyclerViewQuestions.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewQuestions.setAdapter(adapter);
    }

    // -------------------------------------------------------------------------
    // Wire type buttons
    // -------------------------------------------------------------------------

    private void wireTypeButtons() {
        findViewById(R.id.btnAddShortText).setOnClickListener(v ->
                addQuestion(new FormQuestion("Short Text")));

        findViewById(R.id.btnAddParagraph).setOnClickListener(v ->
                addQuestion(new FormQuestion("Paragraph")));

        findViewById(R.id.btnAddMultiChoice).setOnClickListener(v -> {
            FormQuestion q = new FormQuestion("Multiple Choice");
            q.options.add("");
            addQuestion(q);
        });

        findViewById(R.id.btnAddDropdown).setOnClickListener(v -> {
            FormQuestion q = new FormQuestion("Dropdown");
            q.options.add("");
            addQuestion(q);
        });

        findViewById(R.id.btnAddDate).setOnClickListener(v ->
                addQuestion(new FormQuestion("Date")));

        findViewById(R.id.btnAddFileUpload).setOnClickListener(v ->
                addQuestion(new FormQuestion("File Upload")));
    }

    /** Adds a question, notifies adapter, scrolls to bottom. */
    private void addQuestion(FormQuestion q) {
        questions.add(q);
        int pos = questions.size() - 1;
        adapter.notifyItemInserted(pos);
        recyclerViewQuestions.scrollToPosition(pos);
    }

    // -------------------------------------------------------------------------
    // Load from Firestore
    // -------------------------------------------------------------------------

    /**
     * Loads existing formQuestions subcollection from Firestore.
     * Stored under proposals/{eventId}/formQuestions — proposals is the
     * collection used by OrganizerDashboard, not events/.
     * If empty, leaves blank builder ready for fresh creation.
     * Questions are ordered by their saved "order" field.
     */
    private void loadExistingQuestions() {
        db.collection("proposals").document(eventId)
                .collection("formQuestions")
                .orderBy("order")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    questions.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        FormQuestion q = new FormQuestion(
                                doc.getString("type") != null ? doc.getString("type") : "Short Text");
                        q.label    = doc.getString("label")    != null ? doc.getString("label")    : "";
                        q.required = Boolean.TRUE.equals(doc.getBoolean("required"));
                        q.docId    = doc.getId();

                        // Load options for choice-type questions
                        Object rawOptions = doc.get("options");
                        if (rawOptions instanceof List) {
                            //noinspection unchecked
                            List<Object> opts = (List<Object>) rawOptions;
                            q.options.clear();
                            for (Object o : opts) {
                                q.options.add(o != null ? o.toString() : "");
                            }
                        }
                        questions.add(q);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    // Firestore unreachable or no sub-collection yet — blank builder
                    Toast.makeText(this,
                            "Could not load saved form. Starting fresh.", Toast.LENGTH_SHORT).show();
                });
    }

    // -------------------------------------------------------------------------
    // Preview
    // -------------------------------------------------------------------------

    /**
     * Shows an AlertDialog preview of how the form looks to attendees.
     * Displays each question label, type, and whether it's required.
     */
    private void wirePreview() {
        findViewById(R.id.btnPreview).setOnClickListener(v -> {
            if (questions.isEmpty()) {
                Toast.makeText(this, "Add at least one question to preview.", Toast.LENGTH_SHORT).show();
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("── ").append(eventName).append(" Registration ──\n\n");
            for (int i = 0; i < questions.size(); i++) {
                FormQuestion q = questions.get(i);
                sb.append("Q").append(i + 1).append(". ")
                        .append(q.label.isEmpty() ? "(No label)" : q.label)
                        .append(q.required ? "  *required" : "  (optional)")
                        .append("\n  Type: ").append(q.type);
                if (!q.options.isEmpty()) {
                    sb.append("\n  Options: ");
                    for (int j = 0; j < q.options.size(); j++) {
                        sb.append("\n    ").append(j + 1).append(". ")
                                .append(q.options.get(j).isEmpty() ? "(empty)" : q.options.get(j));
                    }
                }
                sb.append("\n\n");
            }

            new AlertDialog.Builder(this)
                    .setTitle("Form Preview")
                    .setMessage(sb.toString())
                    .setPositiveButton("Close", null)
                    .show();
        });
    }

    // -------------------------------------------------------------------------
    // Save to Firestore
    // -------------------------------------------------------------------------

    /**
     * Writes all questions to proposals/{eventId}/formQuestions using a batch write.
     * proposals/ is the collection used by OrganizerDashboard (not events/).
     * Deletes old docs first, then writes fresh ones in order.
     * Also sets proposals/{eventId}.formActive = true.
     */
    private void wireSave() {
        findViewById(R.id.btnSaveForm).setOnClickListener(v -> saveForm());
    }

    private void saveForm() {
        if (questions.isEmpty()) {
            Toast.makeText(this, "Add at least one question before saving.", Toast.LENGTH_SHORT).show();
            return;
        }

        // First delete existing formQuestions docs, then write fresh
        db.collection("proposals").document(eventId)
                .collection("formQuestions")
                .get()
                .addOnSuccessListener(existing -> {
                    WriteBatch batch = db.batch();

                    // Delete all existing question docs
                    for (QueryDocumentSnapshot doc : existing) {
                        batch.delete(doc.getReference());
                    }

                    // Write each question as a new doc with an order field
                    for (int i = 0; i < questions.size(); i++) {
                        FormQuestion q = questions.get(i);
                        Map<String, Object> data = new HashMap<>();
                        data.put("label",    q.label);
                        data.put("type",     q.type);
                        data.put("required", q.required);
                        data.put("options",  q.options);
                        data.put("order",    i);
                        batch.set(
                                db.collection("proposals").document(eventId)
                                        .collection("formQuestions").document(),
                                data
                        );
                    }

                    // Mark form as active on the proposal document
                    // (proposals/ is the collection used by OrganizerDashboard, not events/)
                    batch.update(
                            db.collection("proposals").document(eventId),
                            "formActive", true
                    );

                    batch.commit()
                            .addOnSuccessListener(unused ->
                                    Toast.makeText(this,
                                            "Form saved! Attendees can now see it.",
                                            Toast.LENGTH_LONG).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,
                                            "Save failed: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error reading existing form: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    // -------------------------------------------------------------------------
    // FormQuestion model
    // -------------------------------------------------------------------------

    /**
     * Model for a single form question in the builder.
     * type     — "Short Text" | "Paragraph" | "Multiple Choice" |
     *            "Checkboxes" | "Dropdown" | "Date" | "File Upload"
     * label    — the question text
     * required — whether attendee must answer
     * options  — list of option strings (for choice-type questions)
     * docId    — Firestore document ID (if loaded from Firestore; null for new)
     */
    static class FormQuestion {
        String       type;
        String       label    = "";
        boolean      required = false;
        List<String> options  = new ArrayList<>();
        String       docId    = null;   // null = not yet saved

        FormQuestion(String type) {
            this.type = type;
        }

        /** Deep copy for Duplicate. */
        FormQuestion copy() {
            FormQuestion c = new FormQuestion(this.type);
            c.label    = this.label;
            c.required = this.required;
            c.options  = new ArrayList<>(this.options);
            // docId intentionally null — copy is a new unsaved question
            return c;
        }

        /** Returns true if this type uses an options list. */
        boolean hasOptions() {
            return type.equals("Multiple Choice")
                    || type.equals("Checkboxes")
                    || type.equals("Dropdown");
        }
    }

    // -------------------------------------------------------------------------
    // RecyclerView Adapter
    // -------------------------------------------------------------------------

    class QuestionAdapter extends RecyclerView.Adapter<QuestionAdapter.ViewHolder> {

        private final List<FormQuestion> list;

        QuestionAdapter(List<FormQuestion> list) { this.list = list; }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_question_editor, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            FormQuestion q = list.get(position);

            // Q number
            holder.tvQuestionNumber.setText("Q" + (position + 1));

            // Label — set text without triggering watcher, then attach watcher
            holder.etQuestionLabel.removeTextChangedListener(holder.labelWatcher);
            holder.etQuestionLabel.setText(q.label);
            holder.labelWatcher = new SimpleTextWatcher(s -> q.label = s);
            holder.etQuestionLabel.addTextChangedListener(holder.labelWatcher);

            // Type label
            holder.tvQuestionType.setText(q.type);

            // Required switch
            holder.swRequired.setOnCheckedChangeListener(null);
            holder.swRequired.setChecked(q.required);
            holder.swRequired.setOnCheckedChangeListener((btn, checked) -> q.required = checked);

            // Options section
            if (q.hasOptions()) {
                holder.llOptions.setVisibility(View.VISIBLE);
                rebuildOptionRows(holder, q);
                holder.btnAddOption.setOnClickListener(v -> {
                    q.options.add("");
                    rebuildOptionRows(holder, q);
                });
            } else {
                holder.llOptions.setVisibility(View.GONE);
            }

            // Duplicate
            holder.btnDuplicate.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos == RecyclerView.NO_ID) return;
                FormQuestion copy = list.get(pos).copy();
                list.add(pos + 1, copy);
                notifyItemInserted(pos + 1);
                notifyItemRangeChanged(pos + 1, list.size() - pos - 1);
                recyclerViewQuestions.scrollToPosition(pos + 1);
            });

            // Delete
            holder.btnDelete.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos == RecyclerView.NO_ID) return;
                list.remove(pos);
                notifyItemRemoved(pos);
                notifyItemRangeChanged(pos, list.size() - pos);
            });
        }

        /**
         * Rebuilds the option rows inside llOptions for choice-type questions.
         * Removes all child views except btnAddOption, then re-adds option rows.
         */
        private void rebuildOptionRows(ViewHolder holder, FormQuestion q) {
            LinearLayout ll = holder.llOptions;

            // Remove all views except btnAddOption (last child)
            int count = ll.getChildCount();
            if (count > 1) {
                ll.removeViews(0, count - 1);
            }

            // Re-add one row per option, inserted before btnAddOption
            for (int i = 0; i < q.options.size(); i++) {
                final int index = i;
                View row = LayoutInflater.from(ll.getContext())
                        .inflate(R.layout.item_option_row, ll, false);

                EditText etOption  = row.findViewById(R.id.etOptionText);
                Button   btnRemove = row.findViewById(R.id.btnRemoveOption);

                etOption.setText(q.options.get(i));
                etOption.addTextChangedListener(new SimpleTextWatcher(s -> {
                    if (index < q.options.size()) q.options.set(index, s);
                }));

                btnRemove.setOnClickListener(v -> {
                    if (index < q.options.size()) {
                        q.options.remove(index);
                        rebuildOptionRows(holder, q);
                    }
                });

                ll.addView(row, ll.getChildCount() - 1); // insert before btnAddOption
            }
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView     tvQuestionNumber, tvQuestionType;
            EditText     etQuestionLabel;
            Switch       swRequired;
            LinearLayout llOptions;
            Button       btnAddOption, btnDuplicate, btnDelete;
            TextWatcher  labelWatcher;

            ViewHolder(View v) {
                super(v);
                tvQuestionNumber = v.findViewById(R.id.tvQuestionNumber);
                etQuestionLabel  = v.findViewById(R.id.etQuestionLabel);
                tvQuestionType   = v.findViewById(R.id.tvQuestionType);
                swRequired       = v.findViewById(R.id.swRequired);
                llOptions        = v.findViewById(R.id.llOptions);
                btnAddOption     = v.findViewById(R.id.btnAddOption);
                btnDuplicate     = v.findViewById(R.id.btnDuplicate);
                btnDelete        = v.findViewById(R.id.btnDelete);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helper: simple TextWatcher
    // -------------------------------------------------------------------------

    interface TextCallback { void onText(String s); }

    static class SimpleTextWatcher implements TextWatcher {
        private final TextCallback cb;
        SimpleTextWatcher(TextCallback cb) { this.cb = cb; }
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            cb.onText(s.toString());
        }
        public void afterTextChanged(Editable s) {}
    }
}