package com.lums.eventhub;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
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
 * CHANGES (on top of previous version):
 *   - cardBudgetDoc now opens an IMAGE PICKER (instead of showing a toast)
 *   - Budget image is stored as budgetImageBase64 in Firestore
 *   - Budget image is loaded back when editing an existing proposal
 *   - Budget image preview shown inside the card after selection
 */
public class ProposalFormActivity extends AppCompatActivity {

    private String organizerUsername;
    private String societyName;

    // Event banner image
    private static final int IMAGE_PICK_RC        = 600;
    // Budget document image
    private static final int BUDGET_IMAGE_PICK_RC = 601;

    private String    eventImageBase64  = "";
    private String    budgetImageBase64 = "";
    private ImageView imgEventPreview;
    private Button    btnPickEventImage;
    private ImageView imgBudgetDocPreview;
    private TextView  tvBudgetDocLabel;

    private EditText etTitle, etDescription, etAboutEvent, etSocietyName;
    private EditText etStartDate, etEndDate, etVenue;
    private RadioGroup   rgEventType;
    private EditText     etParticipants;
    private LinearLayout llGuestRows;
    private EditText     etBudget;
    private LinearLayout llSessionRows;
    private CheckBox     cbAccommodation;
    private LinearLayout llAccommodationFields;
    private EditText     etLodgingCount, etCheckIn, etCheckOut, etSpecialRequirements;

    private FirebaseFirestore db;
    private String            proposalId;
    private TextView          tvHeaderTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proposal_form);

        db = FirebaseFirestore.getInstance();

        organizerUsername = getIntent().getStringExtra("organizerUsername");
        societyName       = getIntent().getStringExtra("societyName");
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
        etAboutEvent          = findViewById(R.id.etAboutEvent);
        rgEventType           = findViewById(R.id.rgEventType);
        etSocietyName         = findViewById(R.id.etSocietyName);
        imgEventPreview       = findViewById(R.id.imgEventPreview);
        btnPickEventImage     = findViewById(R.id.btnPickEventImage);
        etStartDate           = findViewById(R.id.etStartDate);
        etEndDate             = findViewById(R.id.etEndDate);
        etVenue               = findViewById(R.id.etVenue);
        etParticipants        = findViewById(R.id.etParticipants);
        llGuestRows           = findViewById(R.id.llGuestRows);
        etBudget              = findViewById(R.id.etBudget);
        imgBudgetDocPreview   = findViewById(R.id.imgBudgetDocPreview);
        tvBudgetDocLabel      = findViewById(R.id.tvBudgetDocLabel);
        llSessionRows         = findViewById(R.id.llSessionRows);
        cbAccommodation       = findViewById(R.id.cbAccommodation);
        llAccommodationFields = findViewById(R.id.llAccommodationFields);
        etLodgingCount        = findViewById(R.id.etLodgingCount);
        etCheckIn             = findViewById(R.id.etCheckIn);
        etCheckOut            = findViewById(R.id.etCheckOut);
        etSpecialRequirements = findViewById(R.id.etSpecialRequirements);

        // Event banner image picker
        btnPickEventImage.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(
                    android.content.Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
            startActivityForResult(
                    android.content.Intent.createChooser(intent, "Select Event Banner"),
                    IMAGE_PICK_RC);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;

        if (requestCode == IMAGE_PICK_RC) {
            // Event banner
            Bitmap bmp = decodeBitmap(data.getData(), 1024);
            if (bmp != null) {
                eventImageBase64 = encodeBitmap(bmp, 80);
                imgEventPreview.setImageBitmap(bmp);
                imgEventPreview.setVisibility(View.VISIBLE);
                btnPickEventImage.setText("✅ Image selected");
            } else {
                Toast.makeText(this, "Could not load image", Toast.LENGTH_SHORT).show();
            }

        } else if (requestCode == BUDGET_IMAGE_PICK_RC) {
            // Budget document image
            Bitmap bmp = decodeBitmap(data.getData(), 1024);
            if (bmp != null) {
                budgetImageBase64 = encodeBitmap(bmp, 80);
                if (imgBudgetDocPreview != null) {
                    imgBudgetDocPreview.setImageBitmap(bmp);
                    imgBudgetDocPreview.setVisibility(View.VISIBLE);
                }
                if (tvBudgetDocLabel != null) {
                    tvBudgetDocLabel.setText("✅ Budget document attached");
                    tvBudgetDocLabel.setTextColor(0xFF2E7D32);
                }
            } else {
                Toast.makeText(this, "Could not load image", Toast.LENGTH_SHORT).show();
            }
        }
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
                Toast.makeText(this, "At least one session is required.",
                        Toast.LENGTH_SHORT).show();
            } else {
                llSessionRows.removeView(row);
            }
        });
        llSessionRows.addView(row);
    }

    /**
     * Budget card now opens an image picker.
     * Supporting docs card still shows coming-soon message.
     */
    private void wireDocumentCards() {
        findViewById(R.id.cardSupportingDocs).setOnClickListener(v ->
                Toast.makeText(this,
                        "File upload will be available in final version.",
                        Toast.LENGTH_SHORT).show());

        // Budget document — open image picker
        findViewById(R.id.cardBudgetDoc).setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(
                    android.content.Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
            startActivityForResult(
                    android.content.Intent.createChooser(intent, "Select Budget Document"),
                    BUDGET_IMAGE_PICK_RC);
        });
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
        String aboutEvent  = etAboutEvent.getText().toString().trim();
        String startDate   = etStartDate.getText().toString().trim();
        String endDate     = etEndDate.getText().toString().trim();
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
        data.put("aboutEvent",            aboutEvent);
        data.put("eventType",             eventType);
        data.put("societyName",           societyName);
        data.put("startDate",             startDate);
        data.put("endDate",               endDate);
        data.put("date",                  startDate);  // backward-compat
        data.put("venue",                 venue);
        data.put("expectedParticipants",  participants);
        data.put("estimatedBudget",       budget);
        data.put("requiresAccommodation", requiresAccommodation);
        data.put("accommodationCount",    lodgingCount);
        data.put("checkInDate",           checkInDate);
        data.put("checkOutDate",          checkOutDate);
        data.put("specialRequirements",   specialReqs);
        data.put("organizerUsername",     organizerUsername);
        data.put("guests",                collectGuests());
        data.put("sessions",              collectSessions());

        if (!eventImageBase64.isEmpty())  data.put("eventImageBase64",  eventImageBase64);
        if (!budgetImageBase64.isEmpty()) data.put("budgetImageBase64", budgetImageBase64);

        if (submit) {
            data.put("status",      "Submitted");
            data.put("submittedAt", System.currentTimeMillis());
        } else {
            data.put("status",    "Draft");
            data.put("updatedAt", System.currentTimeMillis());
        }

        if (proposalId != null) {
            db.collection("proposals").document(proposalId)
                    .set(data)
                    .addOnSuccessListener(v -> {
                        Toast.makeText(this,
                                submit ? "Submitted to CCA!" : "Draft saved!",
                                Toast.LENGTH_SHORT).show();
                        if (submit) finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());
        } else {
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
        if (etStartDate.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Please fill in: Start Date", Toast.LENGTH_SHORT).show();
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
                    setText(etAboutEvent,  doc.getString("aboutEvent"));
                    // startDate with fallback to legacy "date" field
                    String sd = doc.getString("startDate");
                    if (sd == null) sd = doc.getString("date");
                    setText(etStartDate, sd);
                    setText(etEndDate,   doc.getString("endDate"));
                    setText(etVenue,     doc.getString("venue"));
                    setText(etSocietyName, doc.getString("societyName"));

                    // Event banner
                    String existingImg = doc.getString("eventImageBase64");
                    if (existingImg != null && !existingImg.isEmpty()) {
                        eventImageBase64 = existingImg;
                        Bitmap bmp = bitmapFromBase64(existingImg);
                        if (bmp != null) {
                            imgEventPreview.setImageBitmap(bmp);
                            imgEventPreview.setVisibility(View.VISIBLE);
                            btnPickEventImage.setText("✅ Image selected");
                        }
                    }

                    // Budget document image
                    String existingBudget = doc.getString("budgetImageBase64");
                    if (existingBudget != null && !existingBudget.isEmpty()) {
                        budgetImageBase64 = existingBudget;
                        Bitmap bmp = bitmapFromBase64(existingBudget);
                        if (bmp != null && imgBudgetDocPreview != null) {
                            imgBudgetDocPreview.setImageBitmap(bmp);
                            imgBudgetDocPreview.setVisibility(View.VISIBLE);
                        }
                        if (tvBudgetDocLabel != null) {
                            tvBudgetDocLabel.setText("✅ Budget document attached");
                            tvBudgetDocLabel.setTextColor(0xFF2E7D32);
                        }
                    }

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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Bitmap decodeBitmap(android.net.Uri uri, int maxPx) {
        try {
            java.io.InputStream is = getContentResolver().openInputStream(uri);
            Bitmap bmp = BitmapFactory.decodeStream(is);
            if (is != null) is.close();
            if (bmp == null) return null;
            int w = bmp.getWidth(), h = bmp.getHeight();
            if (w > maxPx || h > maxPx) {
                float s = Math.min((float) maxPx / w, (float) maxPx / h);
                bmp = Bitmap.createScaledBitmap(bmp,
                        Math.round(w * s), Math.round(h * s), true);
            }
            return bmp;
        } catch (Exception e) { return null; }
    }

    private String encodeBitmap(Bitmap bmp, int quality) {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
    }

    private Bitmap bitmapFromBase64(String b64) {
        try {
            byte[] bytes = Base64.decode(b64, Base64.NO_WRAP);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) { return null; }
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