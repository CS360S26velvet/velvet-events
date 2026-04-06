package com.lums.eventhub;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ProposalFormActivity.java
 *
 * <p>5-section proposal form. All data — including guests and sessions — is
 * fully persisted to Firestore. Edit mode reloads everything including dynamic
 * guest and session rows.</p>
 *
 * <p>Save as Draft: no validation, saves/updates proposals/{id} with status="Draft".
 * Submit to CCA: validates Section 1, saves with status="Submitted".</p>
 *
 * <p>Data written to: proposals/{id}<br>
 * Guests stored as: guests (List of Maps)<br>
 * Sessions stored as: sessions (List of Maps)</p>
 *
 * <p>User Stories: Org US-02, US-03, US-04, US-05, US-08, US-16</p>
 */
public class ProposalFormActivity extends AppCompatActivity {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    private static final String ORGANIZER_ID = "ORG0012";
    private static final String SOCIETY_NAME = "SPADES Society";

    // -------------------------------------------------------------------------
    // Section 1
    // -------------------------------------------------------------------------
    private EditText   etTitle, etDescription, etSocietyName, etDate, etVenue;
    private RadioGroup rgEventType;

    // -------------------------------------------------------------------------
    // Section 2
    // -------------------------------------------------------------------------
    private EditText     etParticipants;
    private LinearLayout llGuestRows;

    // -------------------------------------------------------------------------
    // Section 3
    // -------------------------------------------------------------------------
    private EditText etBudget;

    // -------------------------------------------------------------------------
    // Section 4
    // -------------------------------------------------------------------------
    private LinearLayout llSessionRows;

    // -------------------------------------------------------------------------
    // Section 5
    // -------------------------------------------------------------------------
    private CheckBox     cbAccommodation;
    private LinearLayout llAccommodationFields;
    private EditText     etLodgingCount, etCheckIn, etCheckOut, etSpecialRequirements;

    // -------------------------------------------------------------------------
    // Misc
    // -------------------------------------------------------------------------
    private FirebaseFirestore db;
    private String            proposalId;   // null = create, non-null = edit
    private TextView          tvHeaderTitle;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proposal_form);

        db = FirebaseFirestore.getInstance();
        bindViews();
        wireAccommodationToggle();
        wireGuestButton();
        wireSessionButton();
        wireBottomBar();
        wireDocumentCards();

        // Default: one empty session
        addSessionRow(null);

        proposalId = getIntent().getStringExtra("proposalId");
        if (proposalId != null) {
            tvHeaderTitle.setText("Edit Proposal");
            loadProposalForEdit(proposalId);
        }
    }

    // -------------------------------------------------------------------------
    // Bind views
    // -------------------------------------------------------------------------

    private void bindViews() {
        tvHeaderTitle         = findViewById(R.id.tvProposalHeaderTitle);
        etTitle               = findViewById(R.id.etTitle);
        etDescription         = findViewById(R.id.etDescription);
        rgEventType           = findViewById(R.id.rgEventType);
        etSocietyName         = findViewById(R.id.etSocietyName);
        etDate                = findViewById(R.id.etDate);
        etVenue               = findViewById(R.id.etVenue);
        etParticipants        = findViewById(R.id.etParticipants);
        llGuestRows           = findViewById(R.id.llGuestRows);
        etBudget              = findViewById(R.id.etBudget);
        llSessionRows         = findViewById(R.id.llSessionRows);
        cbAccommodation       = findViewById(R.id.cbAccommodation);
        llAccommodationFields = findViewById(R.id.llAccommodationFields);
        etLodgingCount        = findViewById(R.id.etLodgingCount);
        etCheckIn             = findViewById(R.id.etCheckIn);
        etCheckOut            = findViewById(R.id.etCheckOut);
        etSpecialRequirements = findViewById(R.id.etSpecialRequirements);
    }

    // -------------------------------------------------------------------------
    // Accommodation toggle
    // -------------------------------------------------------------------------

    private void wireAccommodationToggle() {
        cbAccommodation.setOnCheckedChangeListener((b, checked) ->
                llAccommodationFields.setVisibility(checked ? View.VISIBLE : View.GONE));
    }

    // -------------------------------------------------------------------------
    // Guest rows
    // -------------------------------------------------------------------------

    private void wireGuestButton() {
        Button btnAddGuest = findViewById(R.id.btnAddGuest);
        btnAddGuest.setOnClickListener(v -> addGuestRow(null));
    }

    /**
     * Inflates a guest row, optionally pre-filled from a saved Map.
     *
     * @param data Map with keys "name", "title", "organization", or null for empty row.
     */
    private void addGuestRow(Map<String, Object> data) {
        View row = LayoutInflater.from(this)
                .inflate(R.layout.item_guest, llGuestRows, false);

        EditText etName = row.findViewById(R.id.etGuestName);
        EditText etGuestTitle = row.findViewById(R.id.etGuestTitle);
        EditText etOrg  = row.findViewById(R.id.etGuestOrg);
        Button   btnRem = row.findViewById(R.id.btnRemoveGuest);

        if (data != null) {
            setText(etName,       (String) data.get("name"));
            setText(etGuestTitle, (String) data.get("title"));
            setText(etOrg,        (String) data.get("organization"));
        }

        btnRem.setOnClickListener(v -> llGuestRows.removeView(row));
        llGuestRows.addView(row);
    }

    // -------------------------------------------------------------------------
    // Session rows
    // -------------------------------------------------------------------------

    private void wireSessionButton() {
        Button btnAddSession = findViewById(R.id.btnAddSession);
        btnAddSession.setOnClickListener(v -> addSessionRow(null));
    }

    /**
     * Inflates a session row, optionally pre-filled from a saved Map.
     *
     * @param data Map with keys "name", "venue", "startTime", "endTime", or null for empty.
     */
    private void addSessionRow(Map<String, Object> data) {
        View row = LayoutInflater.from(this)
                .inflate(R.layout.item_session, llSessionRows, false);

        EditText etSessionName  = row.findViewById(R.id.etSessionName);
        EditText etSessionVenue = row.findViewById(R.id.etSessionVenue);
        EditText etStartTime    = row.findViewById(R.id.etStartTime);
        EditText etEndTime      = row.findViewById(R.id.etEndTime);
        Button   btnRem         = row.findViewById(R.id.btnRemoveSession);

        if (data != null) {
            setText(etSessionName,  (String) data.get("name"));
            setText(etSessionVenue, (String) data.get("venue"));
            setText(etStartTime,    (String) data.get("startTime"));
            setText(etEndTime,      (String) data.get("endTime"));
        }

        btnRem.setOnClickListener(v -> {
            if (llSessionRows.getChildCount() <= 1) {
                Toast.makeText(this, "At least one session is required.",
                        Toast.LENGTH_SHORT).show();
            } else {
                llSessionRows.removeView(row);
            }
        });

        llSessionRows.addView(row);
    }

    // -------------------------------------------------------------------------
    // Document upload placeholders
    // -------------------------------------------------------------------------

    private void wireDocumentCards() {
        String msg = "File upload will be available in final version.";
        findViewById(R.id.cardSupportingDocs).setOnClickListener(v ->
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
        findViewById(R.id.cardBudgetDoc).setOnClickListener(v ->
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
    }

    // -------------------------------------------------------------------------
    // Bottom bar
    // -------------------------------------------------------------------------

    private void wireBottomBar() {
        Button btnSaveDraft = findViewById(R.id.btnSaveDraft);
        Button btnSubmitCCA = findViewById(R.id.btnSubmitCCA);
        btnSaveDraft.setOnClickListener(v -> saveProposal(false));
        btnSubmitCCA.setOnClickListener(v -> saveProposal(true));
    }

    // -------------------------------------------------------------------------
    // Collect guests and sessions from UI
    // -------------------------------------------------------------------------

    /**
     * Reads all guest rows from the UI and returns them as a list of Maps.
     * Each Map has keys: "name", "title", "organization".
     */
    private List<Map<String, Object>> collectGuests() {
        List<Map<String, Object>> guests = new ArrayList<>();
        for (int i = 0; i < llGuestRows.getChildCount(); i++) {
            View row = llGuestRows.getChildAt(i);
            String name  = getText(row, R.id.etGuestName);
            String title = getText(row, R.id.etGuestTitle);
            String org   = getText(row, R.id.etGuestOrg);
            if (!name.isEmpty() || !title.isEmpty() || !org.isEmpty()) {
                Map<String, Object> g = new HashMap<>();
                g.put("name",         name);
                g.put("title",        title);
                g.put("organization", org);
                guests.add(g);
            }
        }
        return guests;
    }

    /**
     * Reads all session rows from the UI and returns them as a list of Maps.
     * Each Map has keys: "name", "venue", "startTime", "endTime".
     */
    private List<Map<String, Object>> collectSessions() {
        List<Map<String, Object>> sessions = new ArrayList<>();
        for (int i = 0; i < llSessionRows.getChildCount(); i++) {
            View row = llSessionRows.getChildAt(i);
            String name       = getText(row, R.id.etSessionName);
            String venue      = getText(row, R.id.etSessionVenue);
            String startTime  = getText(row, R.id.etStartTime);
            String endTime    = getText(row, R.id.etEndTime);
            Map<String, Object> s = new HashMap<>();
            s.put("name",      name);
            s.put("venue",     venue);
            s.put("startTime", startTime);
            s.put("endTime",   endTime);
            sessions.add(s);
        }
        return sessions;
    }

    // -------------------------------------------------------------------------
    // Save / Submit
    // -------------------------------------------------------------------------

    /**
     * Builds the full data map and writes to proposals/{id}.
     * If submit=true, validates Section 1 first and sets status="Submitted".
     * If submit=false, no validation, status="Draft".
     *
     * @param submit True = Submit to CCA, False = Save as Draft.
     */
    private void saveProposal(boolean submit) {
        if (submit && !validateSection1()) return;

        String title       = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String date        = etDate.getText().toString().trim();
        String venue       = etVenue.getText().toString().trim();

        String eventType = "";
        int checkedId = rgEventType.getCheckedRadioButtonId();
        if (checkedId != -1) {
            eventType = ((RadioButton) findViewById(checkedId)).getText().toString();
        }

        long participants = parseLong(etParticipants.getText().toString().trim());
        long budget       = parseLong(etBudget.getText().toString().trim());

        boolean requiresAccommodation = cbAccommodation.isChecked();
        long    lodgingCount          = parseLong(etLodgingCount.getText().toString().trim());
        String  checkInDate           = etCheckIn.getText().toString().trim();
        String  checkOutDate          = etCheckOut.getText().toString().trim();
        String  specialReqs           = etSpecialRequirements.getText().toString().trim();

        Map<String, Object> data = new HashMap<>();
        data.put("title",                 title);
        data.put("description",           description);
        data.put("eventType",             eventType);
        data.put("societyName",           SOCIETY_NAME);
        data.put("date",                  date);
        data.put("venue",                 venue);
        data.put("expectedParticipants",  participants);
        data.put("estimatedBudget",       budget);
        data.put("requiresAccommodation", requiresAccommodation);
        data.put("accommodationCount",    lodgingCount);
        data.put("checkInDate",           checkInDate);
        data.put("checkOutDate",          checkOutDate);
        data.put("specialRequirements",   specialReqs);
        data.put("organizerId",           ORGANIZER_ID);
        data.put("guests",                collectGuests());
        data.put("sessions",              collectSessions());

        if (submit) {
            data.put("status",      "Submitted");
            data.put("submittedAt", System.currentTimeMillis());
        } else {
            data.put("status",   "Draft");
            data.put("createdAt", System.currentTimeMillis());
        }

        if (proposalId != null) {
            // Update existing document
            db.collection("proposals").document(proposalId)
                    .set(data)
                    .addOnSuccessListener(v -> {
                        Toast.makeText(this,
                                submit ? "Submitted to CCA!" : "Draft saved!",
                                Toast.LENGTH_SHORT).show();
                        if (submit) finish(); // go back to dashboard after submit
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());
        } else {
            // Create new document
            db.collection("proposals")
                    .add(data)
                    .addOnSuccessListener(ref -> {
                        proposalId = ref.getId();
                        Toast.makeText(this,
                                submit ? "Submitted to CCA!" : "Draft saved!",
                                Toast.LENGTH_SHORT).show();
                        if (submit) finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());
        }
    }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    /** Validates required fields in Section 1 only. */
    private boolean validateSection1() {
        if (etTitle.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Please fill in: Event Title", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (etDescription.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Please fill in: Description", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (rgEventType.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select: Event Type", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (etDate.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Please fill in: Date", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (etVenue.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Please fill in: Venue", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    // -------------------------------------------------------------------------
    // Edit mode — load from Firestore
    // -------------------------------------------------------------------------

    /**
     * Loads an existing proposal from Firestore and populates all fields
     * including dynamic guest rows and session rows.
     *
     * @param id Firestore document ID of the proposal.
     */
    private void loadProposalForEdit(String id) {
        db.collection("proposals").document(id)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    // Section 1
                    setText(etTitle,       doc.getString("title"));
                    setText(etDescription, doc.getString("description"));
                    setText(etDate,        doc.getString("date"));
                    setText(etVenue,       doc.getString("venue"));
                    setText(etSocietyName, doc.getString("societyName"));

                    String eventType = doc.getString("eventType");
                    if ("Society Event".equals(eventType)) {
                        rgEventType.check(R.id.rbSocietyEvent);
                    } else if ("School-Led Workshop".equals(eventType)) {
                        rgEventType.check(R.id.rbSchoolWorkshop);
                    }

                    // Section 2
                    setText(etParticipants, longToString(doc.getLong("expectedParticipants")));
                    loadGuestRows(doc);

                    // Section 3
                    setText(etBudget, longToString(doc.getLong("estimatedBudget")));

                    // Section 4
                    loadSessionRows(doc);

                    // Section 5
                    Boolean reqAcc = doc.getBoolean("requiresAccommodation");
                    if (Boolean.TRUE.equals(reqAcc)) {
                        cbAccommodation.setChecked(true);
                        llAccommodationFields.setVisibility(View.VISIBLE);
                    }
                    setText(etLodgingCount,        longToString(doc.getLong("accommodationCount")));
                    setText(etCheckIn,             doc.getString("checkInDate"));
                    setText(etCheckOut,            doc.getString("checkOutDate"));
                    setText(etSpecialRequirements, doc.getString("specialRequirements"));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not load proposal.", Toast.LENGTH_SHORT).show());
    }

    /**
     * Rebuilds guest rows from the saved "guests" array in a Firestore document.
     *
     * @param doc The Firestore document snapshot.
     */
    @SuppressWarnings("unchecked")
    private void loadGuestRows(DocumentSnapshot doc) {
        List<Map<String, Object>> guests =
                (List<Map<String, Object>>) doc.get("guests");
        if (guests == null || guests.isEmpty()) return;
        llGuestRows.removeAllViews();
        for (Map<String, Object> g : guests) {
            addGuestRow(g);
        }
    }

    /**
     * Rebuilds session rows from the saved "sessions" array in a Firestore document.
     * Clears the default empty row first.
     *
     * @param doc The Firestore document snapshot.
     */
    @SuppressWarnings("unchecked")
    private void loadSessionRows(DocumentSnapshot doc) {
        List<Map<String, Object>> sessions =
                (List<Map<String, Object>>) doc.get("sessions");
        if (sessions == null || sessions.isEmpty()) return;
        llSessionRows.removeAllViews();
        for (Map<String, Object> s : sessions) {
            addSessionRow(s);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void setText(EditText et, String value) {
        if (et != null && value != null) et.setText(value);
    }

    private String getText(View parent, int id) {
        EditText et = parent.findViewById(id);
        return et != null ? et.getText().toString().trim() : "";
    }

    private String longToString(Long v) {
        return (v == null || v == 0) ? "" : String.valueOf(v);
    }

    private long parseLong(String s) {
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return 0L; }
    }
}