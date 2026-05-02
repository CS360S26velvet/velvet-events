package com.lums.eventhub;

/**
 * RegistrationFeeSetupActivity.java
 *
 * Role: First step before building the registration form for an event.
 * Asks the organiser two questions:
 *   1. "Do you want a registration fee?"   → Yes / No
 *   2. "Do you want accommodation?"        → Yes / No
 *
 * If Yes to either, shows the corresponding fee input field.
 * On Save → launches FormBuilderActivity, passing fee/accommodation data as extras.
 * If both No → launches FormBuilderActivity directly (no fee/accommodation defaults).
 *
 * Extras received (from AttendeeRegistrationActivity):
 *   "eventId"   — Firestore document ID of the event
 *   "eventName" — display name of the event
 *
 * Extras sent to FormBuilderActivity:
 *   "eventId"            — forwarded
 *   "eventName"          — forwarded
 *   "hasRegFee"          — boolean
 *   "regFee"             — String (e.g. "PKR 500")
 *   "regBankInfo"        — String (organiser's bank account info, entered at runtime)
 *   "hasAccommodation"   — boolean
 *   "accommodationFee"   — String
 *   "accommodationBankInfo" — String
 */

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegistrationFeeSetupActivity extends AppCompatActivity {

    // -------------------------------------------------------------------------
    // Extras keys
    // -------------------------------------------------------------------------
    public static final String EXTRA_HAS_REG_FEE           = "hasRegFee";
    public static final String EXTRA_REG_FEE               = "regFee";
    public static final String EXTRA_REG_BANK_INFO         = "regBankInfo";
    public static final String EXTRA_HAS_ACCOMMODATION     = "hasAccommodation";
    public static final String EXTRA_ACCOMMODATION_FEE     = "accommodationFee";
    public static final String EXTRA_ACCOMMODATION_BANK    = "accommodationBankInfo";
    public static final String EXTRA_REG_DEADLINE          = "registrationDeadline";

    // -------------------------------------------------------------------------
    // Views
    // -------------------------------------------------------------------------
    private RadioGroup  rgRegFee, rgAccommodation;
    private RadioButton rbRegFeeYes, rbRegFeeNo, rbAccomYes, rbAccomNo;
    private LinearLayout layoutRegFeeFields, layoutAccommodationFields;
    private EditText    etRegFee, etRegBankInfo, etAccommodationFee, etAccommodationBankInfo, etRegDeadline;
    private Button      btnSaveSetup;

    private String eventId, eventName;
    private FirebaseFirestore db;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration_fee_setup);

        db        = FirebaseFirestore.getInstance();
        eventId   = getIntent().getStringExtra("eventId");
        eventName = getIntent().getStringExtra("eventName");

        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "No event selected.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Update header title
        TextView tvTitle = findViewById(R.id.tvSetupTitle);
        if (tvTitle != null) tvTitle.setText("Form Setup — " + eventName);

        bindViews();
        wireRadioGroups();
        wireSave();
        loadExistingSetup();
    }

    // -------------------------------------------------------------------------
    // Bind views
    // -------------------------------------------------------------------------

    private void bindViews() {
        rgRegFee              = findViewById(R.id.rgRegFee);
        rgAccommodation       = findViewById(R.id.rgAccommodation);
        rbRegFeeYes           = findViewById(R.id.rbRegFeeYes);
        rbRegFeeNo            = findViewById(R.id.rbRegFeeNo);
        rbAccomYes            = findViewById(R.id.rbAccomYes);
        rbAccomNo             = findViewById(R.id.rbAccomNo);
        layoutRegFeeFields    = findViewById(R.id.layoutRegFeeFields);
        layoutAccommodationFields = findViewById(R.id.layoutAccommodationFields);
        etRegFee              = findViewById(R.id.etRegFee);
        etRegBankInfo         = findViewById(R.id.etRegBankInfo);
        etAccommodationFee    = findViewById(R.id.etAccommodationFee);
        etAccommodationBankInfo = findViewById(R.id.etAccommodationBankInfo);
        etRegDeadline         = findViewById(R.id.etRegDeadline);
        btnSaveSetup          = findViewById(R.id.btnSaveSetup);
    }

    // -------------------------------------------------------------------------
    // Wire radio groups
    // -------------------------------------------------------------------------

    private void wireRadioGroups() {
        rgRegFee.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbRegFeeYes) {
                layoutRegFeeFields.setVisibility(View.VISIBLE);
            } else {
                layoutRegFeeFields.setVisibility(View.GONE);
            }
        });

        rgAccommodation.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbAccomYes) {
                layoutAccommodationFields.setVisibility(View.VISIBLE);
            } else {
                layoutAccommodationFields.setVisibility(View.GONE);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Load existing setup from Firestore (if organiser already saved this)
    // -------------------------------------------------------------------------

    private void loadExistingSetup() {
        db.collection("proposals").document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    Boolean hasRegFee = doc.getBoolean("hasRegFee");
                    Boolean hasAccom  = doc.getBoolean("hasAccommodation");
                    String  regFee    = doc.getString("regFee");
                    String  regBank   = doc.getString("regBankInfo");
                    String  accomFee  = doc.getString("accommodationFee");
                    String  accomBank = doc.getString("accommodationBankInfo");

                    if (Boolean.TRUE.equals(hasRegFee)) {
                        rbRegFeeYes.setChecked(true);
                        layoutRegFeeFields.setVisibility(View.VISIBLE);
                        if (regFee  != null) etRegFee.setText(regFee);
                        if (regBank != null) etRegBankInfo.setText(regBank);
                    } else if (hasRegFee != null) {
                        rbRegFeeNo.setChecked(true);
                    }

                    if (Boolean.TRUE.equals(hasAccom)) {
                        rbAccomYes.setChecked(true);
                        layoutAccommodationFields.setVisibility(View.VISIBLE);
                        if (accomFee  != null) etAccommodationFee.setText(accomFee);
                        if (accomBank != null) etAccommodationBankInfo.setText(accomBank);
                    } else if (hasAccom != null) {
                        rbAccomNo.setChecked(true);
                    }

                    // Pre-fill deadline directly from proposals/ doc (same doc we just loaded)
                    String deadline = doc.getString("registrationDeadline");
                    if (deadline != null && !deadline.isEmpty() && etRegDeadline != null) {
                        etRegDeadline.setText(deadline);
                    }
                });
    }

    // -------------------------------------------------------------------------
    // Wire Save button
    // -------------------------------------------------------------------------

    private void wireSave() {
        btnSaveSetup.setOnClickListener(v -> {
            boolean hasRegFee      = rbRegFeeYes.isChecked();
            boolean hasAccommodation = rbAccomYes.isChecked();

            // Validate: if yes to reg fee, fee amount must be provided
            if (hasRegFee) {
                String fee = etRegFee.getText().toString().trim();
                if (fee.isEmpty()) {
                    etRegFee.setError("Please enter the registration fee amount.");
                    etRegFee.requestFocus();
                    return;
                }
            }

            if (hasAccommodation) {
                String fee = etAccommodationFee.getText().toString().trim();
                if (fee.isEmpty()) {
                    etAccommodationFee.setError("Please enter the accommodation fee amount.");
                    etAccommodationFee.requestFocus();
                    return;
                }
            }

            String deadline = etRegDeadline.getText().toString().trim();
            String regFee   = hasRegFee        ? etRegFee.getText().toString().trim()               : "";
            String regBank  = hasRegFee        ? etRegBankInfo.getText().toString().trim()          : "";
            String accomFee = hasAccommodation ? etAccommodationFee.getText().toString().trim()     : "";
            String accomBank= hasAccommodation ? etAccommodationBankInfo.getText().toString().trim(): "";

            // Always save everything — fees, accommodation AND deadline — in one write
            Map<String, Object> data = new HashMap<>();
            data.put("hasRegFee",             hasRegFee);
            data.put("regFee",                regFee);
            data.put("regBankInfo",           regBank);
            data.put("hasAccommodation",      hasAccommodation);
            data.put("accommodationFee",      accomFee);
            data.put("accommodationBankInfo", accomBank);
            data.put("registrationDeadline",  deadline);

            db.collection("proposals").document(eventId)
                    .set(data, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(unused -> {
                        // Also write deadline to events/ for attendee side
                        Map<String, Object> evData = new HashMap<>();
                        evData.put("registrationDeadline", deadline);
                        db.collection("events").document(eventId)
                                .set(evData, com.google.firebase.firestore.SetOptions.merge());
                        launchFormBuilder(hasRegFee, regFee, regBank,
                                hasAccommodation, accomFee, accomBank);
                    })
                    .addOnFailureListener(e ->
                            launchFormBuilder(hasRegFee, regFee, regBank,
                                    hasAccommodation, accomFee, accomBank));
        });
    }

    // -------------------------------------------------------------------------
    // Launch FormBuilderActivity
    // -------------------------------------------------------------------------

    private void launchFormBuilder(
            boolean hasRegFee,   String regFee,    String regBank,
            boolean hasAccom,    String accomFee,  String accomBank) {

        Intent intent = new Intent(this, FormBuilderActivity.class);
        intent.putExtra("eventId",              eventId);
        intent.putExtra("eventName",            eventName);
        intent.putExtra(EXTRA_HAS_REG_FEE,      hasRegFee);
        intent.putExtra(EXTRA_REG_FEE,          regFee);
        intent.putExtra(EXTRA_REG_BANK_INFO,    regBank);
        intent.putExtra(EXTRA_HAS_ACCOMMODATION, hasAccom);
        intent.putExtra(EXTRA_ACCOMMODATION_FEE, accomFee);
        intent.putExtra(EXTRA_ACCOMMODATION_BANK, accomBank);
        intent.putExtra(EXTRA_REG_DEADLINE,
                etRegDeadline.getText().toString().trim());
        startActivity(intent);
    }
}