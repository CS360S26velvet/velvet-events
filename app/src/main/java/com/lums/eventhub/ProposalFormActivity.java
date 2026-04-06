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

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ProposalFormActivity.java
 *
 * 5-section proposal form. All data persisted to Firestore proposals/ collection.
 *
 * Launched from OrganizerDashboardActivity in two modes:
 *   (a) New event  — no intent extra "proposalId"
 *   (b) Edit event — intent extra "proposalId" set; loads existing data from Firestore
 *
 * On "Save Draft":
 *   - Writes status="Draft" to Firestore
 *   - Stays on form (does NOT finish) — organizer can keep editing
 *   - proposalId is captured after first save so subsequent saves update the same doc
 *
 * On "Submit to CCA":
 *   - Validates required fields, writes status="Submitted", then finishes
 *   - Dashboard refreshes via onResume and shows updated status
 *
 * Canonical field: organizerUsername (== organizerId in older code)
 * Received from OrganizerDashboardActivity via intent extra "organizerUsername".
 *
 * Status values (shared with admin ProposalDetailActivity):
 *   "Draft"    — saved, not visible to admin
 *   "Submitted"— visible to admin for review
 *
 * User Stories: Org US-02, US-03, US-04, US-05, US-08, US-16
 */
public class ProposalFormActivity extends AppCompatActivity {

    // Received from OrganizerDashboardActivity via intent — NOT hardcoded
    private String organizerUsername;
    private String societyName;

    private EditText   etTitle, etDescription, etSocietyName, etDate, etVenue;
    private RadioGroup rgEventType;
    private EditText     etParticipants;
    private LinearLayout llGuestRows;
    private EditText etBudget;
    private LinearLayout llSessionRows;
    private CheckBox     cbAccommodation;
    private LinearLayout llAccommodationFields;
    private EditText     etLodgingCount, etCheckIn, etCheckOut, etSpecialRequirements;

    private FirebaseFirestore db;
    private String            proposalId;   // null = new proposal, non-null = editing existing
    private TextView          tvHeaderTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proposal_form);

        db = FirebaseFirestore.getInstance();

        // Get organizer identity from intent (set by OrganizerDashboardActivity)
        organizerUsername = getIntent().getStringExtra("organizerUsername");
        societyName       = getIntent().getStringExtra("societyName");

        // Fallbacks for direct launch / dev
        if (organizerUsername == null) organizerUsername = "ORG0012";
        if (societyName == null)       societyName       = "SPADES Society";

        bindViews();
        wireAccommodationToggle();
        wireGuestButton();
        wireSessionButton();
        wireBottomBar();
        wireDocumentCards();

        addSessionRow(null);

        proposalId = getIntent().getStringExtra("proposalId");
        if (proposalId != null) {
            tvHeaderTitle.setText("Edit Proposal");
            loadProposalForEdit(proposalId);
        }
    }

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

    private void wireAccommodationToggle() {
        cbAccommodation.setOnCheckedChangeListener((b, checked) ->
                llAccommodationFields.setVisibility(checked ? View.VISIBLE : View.GONE));
    }

    private void wireGuestButton() {
        Button btnAddGuest = findViewById(R.id.btnAddGuest);
        btnAddGuest.setOnClickListener(v -> addGuestRow(null));
    }

    private void addGuestRow(Map<String, Object> data) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_guest, llGuestRows, false);
        EditText etName       = row.findViewById(R.id.etGuestName);
        EditText etGuestTitle = row.findViewById(R.id.etGuestTitle);
        EditText etOrg        = row.findViewById(R.id.etGuestOrg);
        Button   btnRem       = row.findViewById(R.id.btnRemoveGuest);
        if (data != null) {
            setText(etName,       (String) data.get("name"));
            setText(etGuestTitle, (String) data.get("title"));
            setText(etOrg,        (String) data.get("organization"));
        }
        btnRem.setOnClickListener(v -> llGuestRows.removeView(row));
        llGuestRows.addView(row);
    }

    private void wireSessionButton() {
        Button btnAddSession = findViewById(R.id.btnAddSession);
        btnAddSession.setOnClickListener(v -> addSessionRow(null));
    }

    private void addSessionRow(Map<String, Object> data) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_session, llSessionRows, false);
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
                Toast.makeText(this, "At least one session is required.", Toast.LENGTH_SHORT).show();
            } else {
                llSessionRows.removeView(row);
            }
        });
        llSessionRows.addView(row);
    }

    private void wireDocumentCards() {
        String msg = "File upload will be available in final version.";
        findViewById(R.id.cardSupportingDocs).setOnClickListener(v ->
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
        findViewById(R.id.cardBudgetDoc).setOnClickListener(v ->
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
    }

    private void wireBottomBar() {
        Button btnSaveDraft = findViewById(R.id.btnSaveDraft);
        Button btnSubmitCCA = findViewById(R.id.btnSubmitCCA);
        btnSaveDraft.setOnClickListener(v -> saveProposal(false));
        btnSubmitCCA.setOnClickListener(v -> saveProposal(true));
    }

    private List<Map<String, Object>> collectGuests() {
        List<Map<String, Object>> guests = new ArrayList<>();
        for (int i = 0; i < llGuestRows.getChildCount(); i++) {
            View row   = llGuestRows.getChildAt(i);
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

    private List<Map<String, Object>> collectSessions() {
        List<Map<String, Object>> sessions = new ArrayList<>();
        for (int i = 0; i < llSessionRows.getChildCount(); i++) {
            View row = llSessionRows.getChildAt(i);
            Map<String, Object> s = new HashMap<>();
            s.put("name",      getText(row, R.id.etSessionName));
            s.put("venue",     getText(row, R.id.etSessionVenue));
            s.put("startTime", getText(row, R.id.etStartTime));
            s.put("endTime",   getText(row, R.id.etEndTime));
            sessions.add(s);
        }
        return sessions;
    }

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

        long    participants          = parseLong(etParticipants.getText().toString().trim());
        long    budget                = parseLong(etBudget.getText().toString().trim());
        boolean requiresAccommodation = cbAccommodation.isChecked();
        long    lodgingCount          = parseLong(etLodgingCount.getText().toString().trim());
        String  checkInDate           = etCheckIn.getText().toString().trim();
        String  checkOutDate          = etCheckOut.getText().toString().trim();
        String  specialReqs           = etSpecialRequirements.getText().toString().trim();

        Map<String, Object> data = new HashMap<>();
        data.put("title",                 title);
        data.put("description",           description);
        data.put("eventType",             eventType);
        data.put("societyName",           societyName);
        data.put("date",                  date);
        data.put("venue",                 venue);
        data.put("expectedParticipants",  participants);
        data.put("estimatedBudget",       budget);
        data.put("requiresAccommodation", requiresAccommodation);
        data.put("accommodationCount",    lodgingCount);
        data.put("checkInDate",           checkInDate);
        data.put("checkOutDate",          checkOutDate);
        data.put("specialRequirements",   specialReqs);
        // CANONICAL FIELD: organizerUsername (received from OrganizerDashboardActivity)
        data.put("organizerUsername",     organizerUsername);
        data.put("guests",                collectGuests());
        data.put("sessions",              collectSessions());

        if (submit) {
            data.put("status",      "Submitted");
            data.put("submittedAt", System.currentTimeMillis());
        } else {
            data.put("status",    "Draft");
            data.put("updatedAt", System.currentTimeMillis());
        }

        if (proposalId != null) {
            // Update existing proposal document
            db.collection("proposals").document(proposalId)
                    .set(data)
                    .addOnSuccessListener(v -> {
                        Toast.makeText(this,
                                submit ? "Submitted to CCA!" : "Draft saved!",
                                Toast.LENGTH_SHORT).show();
                        // On submit: go back to dashboard (which refreshes in onResume)
                        // On draft save: STAY on form so organizer can keep editing
                        if (submit) finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());
        } else {
            // New proposal — create document, capture the new ID
            db.collection("proposals")
                    .add(data)
                    .addOnSuccessListener(ref -> {
                        proposalId = ref.getId(); // capture so next save updates same doc
                        Toast.makeText(this,
                                submit ? "Submitted to CCA!" : "Draft saved!",
                                Toast.LENGTH_SHORT).show();
                        // On submit: go back to dashboard
                        // On draft save: STAY on form — proposalId now set for future saves
                        if (submit) finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());
        }
    }

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

    @SuppressWarnings("unchecked")
    private void loadProposalForEdit(String id) {
        db.collection("proposals").document(id)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
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

                    setText(etParticipants, longToString(doc.getLong("expectedParticipants")));

                    List<Map<String, Object>> guests =
                            (List<Map<String, Object>>) doc.get("guests");
                    if (guests != null && !guests.isEmpty()) {
                        llGuestRows.removeAllViews();
                        for (Map<String, Object> g : guests) addGuestRow(g);
                    }

                    setText(etBudget, longToString(doc.getLong("estimatedBudget")));

                    List<Map<String, Object>> sessions =
                            (List<Map<String, Object>>) doc.get("sessions");
                    if (sessions != null && !sessions.isEmpty()) {
                        llSessionRows.removeAllViews();
                        for (Map<String, Object> s : sessions) addSessionRow(s);
                    }

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
