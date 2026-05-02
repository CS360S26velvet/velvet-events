package com.lums.eventhub;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * EventDetailsActivity.java  (UPDATED v5)
 *
 * CHANGES:
 * 1. FeeBlock always required — cannot submit without attaching reg proof.
 * 2. AccomBlock rendered as MCQ "Do you want accommodation? (Fee=X) Yes/No"
 *    If Yes → reveals: Accommodation Fee, Bank Info, Attach Proof (required)
 *    If No  → proof not required for accommodation
 * 3. Two separate proof URLs stored in registration doc:
 *      paymentProofUrl       — registration fee proof
 *      accommodationProofUrl — accommodation fee proof (only if Yes selected)
 * 4. Capacity counts ONLY Approved registrations (Pending/Rejected don't count).
 */
public class EventDetailsActivity extends AppCompatActivity {

    // ── Views ──────────────────────────────────────────────────────────────────
    private Button      btnRegister, btnAddToCalendar, btnBackBottom;
    private TextView    tvHeroCategory, tvHeroTitle;
    private TextView    tvStartDate, tvEndDate, tvVenue;
    private TextView    tvOrganizer, tvRegFee, tvAccomFee;
    private TextView    tvSeats, tvSeatsPercent, tvAboutEvent, tvRegCloses;
    private ProgressBar progressSeats;
    private LinearLayout llScheduleContainer;
    private ImageView   imgHero;

    // ── Data ───────────────────────────────────────────────────────────────────
    private String eventId, eventTitle, eventOrganizer, eventCategory;
    private String userId, username, source;
    private FirebaseFirestore db;

    private String startDate = "", endDate = "", venue = "";
    private String regFee = "", accomFee = "";
    private String aboutEvent = "";
    private int    seatsTotal = 0, seatsBooked = 0;

    // ── Form state ─────────────────────────────────────────────────────────────
    private static final int FILE_PICK_RC = 300;
    private int pendingFormFieldIndex = -1;

    // Parallel lists — one slot per question
    private final List<String>  formFileNames   = new ArrayList<>();
    private final List<Uri>     formFileUris    = new ArrayList<>();
    private final List<Button>  formFileButtons = new ArrayList<>();
    // Track which field index is the accom proof (for conditional required check)
    private int accomProofFieldIndex = -1;
    // Track the radio group for the accom Yes/No question
    private RadioGroup accomRadioGroup = null;

    // Base64-encoded image data keyed by field index
    private final Map<Integer, String> formFileBase64 = new HashMap<>();
    private List<Map<String, Object>> currentQuestions = new ArrayList<>();

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.event_detail);

        db      = FirebaseFirestore.getInstance();

        Intent i      = getIntent();
        userId        = i.getStringExtra("userId");
        username      = i.getStringExtra("username");
        source        = i.getStringExtra("source");
        eventId       = i.getStringExtra("eventId");
        eventTitle    = i.getStringExtra("eventTitle");
        eventOrganizer= i.getStringExtra("eventOrganizer");
        eventCategory = i.getStringExtra("eventCategory");

        if (username       == null) username       = "";
        if (eventTitle     == null) eventTitle     = "";
        if (eventOrganizer == null) eventOrganizer = "";
        if (eventCategory  == null) eventCategory  = "";

        bindViews();
        loadEventFromFirestore();
        checkIfAlreadyRegistered();

        btnBackBottom.setOnClickListener(v -> navigateBack());
        btnAddToCalendar.setOnClickListener(v -> addToCalendar());
        btnRegister.setOnClickListener(v -> openRegistrationForm());
    }

    // ── Bind views ─────────────────────────────────────────────────────────────

    private void bindViews() {
        btnBackBottom    = findViewById(R.id.btnBackBottom);
        btnRegister      = findViewById(R.id.btnRegister);
        btnAddToCalendar = findViewById(R.id.btnAddToCalendar);
        tvHeroCategory   = findViewById(R.id.tvHeroCategory);
        tvHeroTitle      = findViewById(R.id.tvHeroTitle);
        imgHero          = findViewById(R.id.imgHero);
        tvStartDate      = findViewById(R.id.tvDate);
        tvEndDate        = findViewById(R.id.tvEndDate);
        tvVenue          = findViewById(R.id.tvVenue);
        tvOrganizer      = findViewById(R.id.tvOrganizer);
        tvRegFee         = findViewById(R.id.tvFee);
        tvAccomFee       = findViewById(R.id.tvAccomFee);
        tvSeats          = findViewById(R.id.tvSeats);
        tvSeatsPercent   = findViewById(R.id.tvSeatsPercent);
        progressSeats    = findViewById(R.id.progressSeats);
        tvAboutEvent     = findViewById(R.id.tvDescription);
        tvRegCloses      = findViewById(R.id.tvRegCloses);
        llScheduleContainer = findViewById(R.id.llScheduleContainer);

        tvHeroCategory.setText(eventCategory);
        tvHeroTitle.setText(eventTitle);
        tvOrganizer.setText(eventOrganizer);
        btnRegister.setText("Register for this Event");
        tvHeroCategory.setBackgroundColor(
                "Society Events".equals(eventCategory) ? 0xFFE91E8C : 0xFF7B2FBE);
    }

    // ── Load event ─────────────────────────────────────────────────────────────

    private void loadEventFromFirestore() {
        db.collection("proposals").document(eventId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) populateFromDoc(doc);
                    else db.collection("events").document(eventId).get()
                            .addOnSuccessListener(this::populateFromDoc);
                })
                .addOnFailureListener(e ->
                        db.collection("events").document(eventId).get()
                                .addOnSuccessListener(this::populateFromDoc));
    }

    private void populateFromDoc(DocumentSnapshot doc) {
        if (!doc.exists()) return;
        startDate  = nvl(doc.getString("startDate"), nvl(doc.getString("date"), "—"));
        endDate    = nvl(doc.getString("endDate"), "");
        venue      = nvl(doc.getString("venue"), "—");
        regFee     = nvl(doc.getString("regFee"), "");
        accomFee   = nvl(doc.getString("accommodationFee"), "");
        aboutEvent = nvl(doc.getString("aboutEvent"), nvl(doc.getString("description"), ""));
        Long total = doc.getLong("expectedParticipants");
        seatsTotal = (total != null) ? total.intValue() : 0;

        tvStartDate.setText(startDate);
        if (tvEndDate != null) tvEndDate.setText(endDate.isEmpty() ? "—" : endDate);
        tvVenue.setText(venue);
        tvAboutEvent.setText(aboutEvent);
        tvRegFee.setText(regFee.isEmpty() ? "Free" : regFee);

        View accomRow = findViewById(R.id.rowAccomFee);
        if (tvAccomFee != null && accomRow != null) {
            if (accomFee.isEmpty()) { accomRow.setVisibility(View.GONE); }
            else { tvAccomFee.setText(accomFee); accomRow.setVisibility(View.VISIBLE); }
        }
        tvRegCloses.setText("Registration closes " + (endDate.isEmpty() ? startDate : endDate));

        // Show event banner image in hero
        String imageBase64 = doc.getString("eventImageBase64");
        if (imageBase64 != null && !imageBase64.isEmpty() && imgHero != null) {
            try {
                byte[] bytes = android.util.Base64.decode(imageBase64, android.util.Base64.NO_WRAP);
                android.graphics.Bitmap bmp = android.graphics.BitmapFactory
                        .decodeByteArray(bytes, 0, bytes.length);
                if (bmp != null) imgHero.setImageBitmap(bmp);
            } catch (Exception ignored) {}
        }

        // Read registrationDeadline — set by organiser in RegistrationFeeSetupActivity
        String dl = doc.getString("registrationDeadline");
        android.util.Log.d("EventDetails", "populateFromDoc: registrationDeadline=" + dl
                + " endDate=" + endDate + " docId=" + doc.getId());
        if (dl != null && !dl.isEmpty()) {
            tvRegCloses.setText("Registration closes " + dl);
            checkDeadline(dl);
        }

        loadApprovedCount();
        loadSchedule(doc);
    }

    // ── Capacity — ONLY Approved ───────────────────────────────────────────────

    private void loadApprovedCount() {
        db.collection("registrations")
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("paymentStatus", "Approved")
                .get()
                .addOnSuccessListener(snap -> { seatsBooked = snap.size(); updateCapacityUI(); })
                .addOnFailureListener(e -> updateCapacityUI());
    }

    private void updateCapacityUI() {
        int available = Math.max(0, seatsTotal - seatsBooked);
        tvSeats.setText(seatsBooked + " / " + seatsTotal + " registered");
        int pct = seatsTotal > 0 ? (seatsBooked * 100) / seatsTotal : 0;
        tvSeatsPercent.setText(pct + "% full");
        progressSeats.setProgress(pct);
        if (available <= 0 && seatsTotal > 0) {
            tvSeats.setTextColor(0xFFE53935);
            btnRegister.setEnabled(false);
            btnRegister.setText("Fully Booked");
            btnRegister.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFFAAAAAA));
        } else {
            tvSeats.setTextColor(0xFF1A1A2E);
            tvSeatsPercent.setTextColor(0xFF4CAF50);
        }
    }

    // ── Schedule ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void loadSchedule(DocumentSnapshot doc) {
        if (llScheduleContainer == null) return;
        llScheduleContainer.removeAllViews();
        Object raw = doc.get("sessions");
        if (!(raw instanceof List)) return;
        List<Map<String, Object>> sessions = (List<Map<String, Object>>) raw;
        for (int i = 0; i < sessions.size(); i++) {
            Map<String, Object> s = sessions.get(i);
            String name  = nvl((String) s.get("name"), "Session");
            String sv    = nvl((String) s.get("venue"), "");
            String st    = nvl((String) s.get("startTime"), "");
            String et    = nvl((String) s.get("endTime"), "");
            String tr    = st.isEmpty() ? "" : st + (et.isEmpty() ? "" : " – " + et);
            View row = LayoutInflater.from(this)
                    .inflate(R.layout.item_schedule_row, llScheduleContainer, false);
            ((TextView) row.findViewById(R.id.tvScheduleTime)).setText(tr);
            ((TextView) row.findViewById(R.id.tvScheduleName)).setText(name);
            ((TextView) row.findViewById(R.id.tvScheduleVenue)).setText(sv);
            llScheduleContainer.addView(row);
            if (i < sessions.size() - 1) {
                View div = new View(this);
                div.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 1));
                div.setBackgroundColor(0xFFF0F0F0);
                llScheduleContainer.addView(div);
            }
        }
    }

    // ── Open registration form ─────────────────────────────────────────────────

    private void openRegistrationForm() {
        db.collection("proposals").document(eventId)
                .collection("formQuestions").orderBy("order").get()
                .addOnSuccessListener(snap -> {
                    List<Map<String, Object>> qs = new ArrayList<>();
                    for (QueryDocumentSnapshot d : snap) {
                        Map<String, Object> q = new HashMap<>();
                        q.put("label",     d.getString("label"));
                        q.put("type",      d.getString("type"));
                        q.put("required",  d.getBoolean("required"));
                        q.put("locked",    d.getBoolean("locked"));
                        q.put("feeAmount", d.getString("feeAmount"));
                        q.put("bankInfo",  d.getString("bankInfo"));
                        q.put("blockType", d.getString("blockType"));
                        Object opts = d.get("options");
                        q.put("options", opts != null ? opts : new ArrayList<>());
                        qs.add(q);
                    }
                    if (qs.isEmpty()) showSimpleRegistrationDialog();
                    else showFormDialog(qs);
                })
                .addOnFailureListener(e -> showSimpleRegistrationDialog());
    }

    private void showSimpleRegistrationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Register for " + eventTitle)
                .setMessage("Confirm your registration.")
                .setPositiveButton("Confirm", (d, w) -> submitRegistration(new HashMap<>(), null, null))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Build form dialog ──────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void showFormDialog(List<Map<String, Object>> questions) {
        currentQuestions = questions;
        formFileNames.clear();
        formFileUris.clear();
        formFileButtons.clear();
        accomProofFieldIndex = -1;
        accomRadioGroup      = null;
        formFileBase64.clear();
        for (int i = 0; i < questions.size(); i++) {
            formFileNames.add("");
            formFileUris.add(null);
            formFileButtons.add(null);
        }

        View formView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_registration_form, null);
        LinearLayout container = formView.findViewById(R.id.llFormContainer);
        ((TextView) formView.findViewById(R.id.tvFormTitle))
                .setText(eventTitle + " — Registration Form");

        List<Object> fieldViews = new ArrayList<>();

        for (int i = 0; i < questions.size(); i++) {
            Map<String, Object> q = questions.get(i);
            String  type      = nvl((String) q.get("type"), "Short Text");
            String  label     = nvl((String) q.get("label"), "");
            boolean required  = Boolean.TRUE.equals(q.get("required"))
                    || "FeeBlock".equals(type) || "AccomBlock".equals(type);
            String  feeAmount = nvl((String) q.get("feeAmount"), "");
            String  bankInfo  = nvl((String) q.get("bankInfo"), "");

            if ("FeeBlock".equals(type)) {
                // ── Registration fee block — always required ────────────────
                View card = LayoutInflater.from(this)
                        .inflate(R.layout.item_form_info_card, container, false);
                ((TextView) card.findViewById(R.id.tvCardTitle)).setText("Registration Payment");
                ((TextView) card.findViewById(R.id.tvCardFee)).setText(
                        feeAmount.isEmpty() ? "" : "Fee: " + feeAmount);
                ((TextView) card.findViewById(R.id.tvCardBank)).setText(
                        bankInfo.isEmpty() ? "" : "Bank Info: " + bankInfo);

                Button btnFile = card.findViewById(R.id.btnCardFileUpload);
                btnFile.setText("📎 Attach Registration Proof  (required)");
                final int idx = i;
                btnFile.setOnClickListener(v -> { pendingFormFieldIndex = idx; openFilePicker(); });
                formFileButtons.set(i, btnFile);
                container.addView(card);
                fieldViews.add(btnFile);

            } else if ("AccomBlock".equals(type)) {
                // ── Accommodation block — MCQ + conditional reveal ──────────
                container.addView(makeLabelView(
                        "Do you want accommodation? (Fee = " + feeAmount + ")", true));

                RadioGroup rg = new RadioGroup(this);
                rg.setOrientation(RadioGroup.VERTICAL);
                RadioButton rbYes = new RadioButton(this);
                rbYes.setText("Yes");
                RadioButton rbNo  = new RadioButton(this);
                rbNo.setText("No");
                rg.addView(rbYes);
                rg.addView(rbNo);
                container.addView(rg);
                accomRadioGroup = rg;
                fieldViews.add(rg);

                // Conditional section — hidden until Yes selected
                LinearLayout llConditional = new LinearLayout(this);
                llConditional.setOrientation(LinearLayout.VERTICAL);
                llConditional.setVisibility(View.GONE);
                LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                clp.topMargin = 8;
                llConditional.setLayoutParams(clp);

                // Fee info
                TextView tvFeeInfo = new TextView(this);
                tvFeeInfo.setText("Accommodation Fee: " + feeAmount);
                tvFeeInfo.setTextSize(13f);
                tvFeeInfo.setTextColor(0xFF1565C0);
                tvFeeInfo.setTypeface(tvFeeInfo.getTypeface(), android.graphics.Typeface.BOLD);
                tvFeeInfo.setPadding(0, 8, 0, 4);
                llConditional.addView(tvFeeInfo);

                // Bank info
                if (!bankInfo.isEmpty()) {
                    TextView tvBank = new TextView(this);
                    tvBank.setText("Bank Info: " + bankInfo);
                    tvBank.setTextSize(13f);
                    tvBank.setTextColor(0xFF555555);
                    tvBank.setPadding(0, 4, 0, 8);
                    llConditional.addView(tvBank);
                }

                // Proof upload button
                Button btnAccomFile = new Button(this);
                final int idx = i;
                btnAccomFile.setText("📎 Attach Accommodation Proof  (required)");
                btnAccomFile.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFFE3F2FD));
                btnAccomFile.setTextColor(0xFF1565C0);
                LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                blp.bottomMargin = 8;
                btnAccomFile.setLayoutParams(blp);
                btnAccomFile.setOnClickListener(v -> { pendingFormFieldIndex = idx; openFilePicker(); });
                formFileButtons.set(i, btnAccomFile);
                accomProofFieldIndex = i;
                llConditional.addView(btnAccomFile);

                container.addView(llConditional);

                // Show/hide conditional on Yes/No selection
                rg.setOnCheckedChangeListener((group, checkedId) -> {
                    boolean isYes = (group.getCheckedRadioButtonId() == rbYes.getId());
                    llConditional.setVisibility(isYes ? View.VISIBLE : View.GONE);
                });

            } else if ("File Upload".equals(type)) {
                container.addView(makeLabelView(label, required));
                Button btnPick = new Button(this);
                btnPick.setText(required ? "📎 Tap to upload  (required)" : "📎 Tap to upload");
                btnPick.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFFE3F2FD));
                btnPick.setTextColor(0xFF1565C0);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.bottomMargin = 8;
                btnPick.setLayoutParams(lp);
                final int idx = i;
                btnPick.setOnClickListener(v -> { pendingFormFieldIndex = idx; openFilePicker(); });
                formFileButtons.set(i, btnPick);
                container.addView(btnPick);
                fieldViews.add(btnPick);

            } else if ("Multiple Choice".equals(type) || "Dropdown".equals(type)) {
                container.addView(makeLabelView(label, required));
                List<String> opts = (List<String>) q.get("options");
                RadioGroup rg = new RadioGroup(this);
                rg.setOrientation(RadioGroup.VERTICAL);
                if (opts != null) for (String opt : opts) {
                    RadioButton rb = new RadioButton(this); rb.setText(opt); rg.addView(rb);
                }
                container.addView(rg);
                fieldViews.add(rg);

            } else if ("Paragraph".equals(type)) {
                container.addView(makeLabelView(label, required));
                EditText et = makeEditText(true);
                container.addView(et);
                fieldViews.add(et);

            } else if ("Info".equals(type)) {
                TextView tv = new TextView(this);
                tv.setText(label);
                tv.setTextSize(13f);
                tv.setTextColor(0xFF555555);
                tv.setPadding(0, 8, 0, 8);
                container.addView(tv);
                fieldViews.add(null);

            } else {
                container.addView(makeLabelView(label, required));
                EditText et = makeEditText(false);
                container.addView(et);
                fieldViews.add(et);
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(formView)
                .setPositiveButton("Submit Registration", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnSubmit = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnSubmit.setOnClickListener(v -> {

                // ── Validation ──────────────────────────────────────────────
                for (int i = 0; i < questions.size(); i++) {
                    Map<String, Object> q = questions.get(i);
                    String  type    = nvl((String) q.get("type"), "Short Text");
                    String  label   = nvl((String) q.get("label"), "Q" + (i + 1));
                    boolean req     = Boolean.TRUE.equals(q.get("required"))
                            || "FeeBlock".equals(type) || "AccomBlock".equals(type);

                    if ("FeeBlock".equals(type)) {
                        // Always required — must have reg proof attached
                        if (formFileUris.get(i) == null) {
                            Toast.makeText(this,
                                    "Please attach Registration Payment Proof before submitting.",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                    } else if ("AccomBlock".equals(type)) {
                        // MCQ must be answered
                        if (accomRadioGroup == null
                                || accomRadioGroup.getCheckedRadioButtonId() == -1) {
                            Toast.makeText(this,
                                    "Please answer: Do you want accommodation?",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // If Yes selected, proof is required
                        boolean accomYes = isAccomYesSelected();
                        if (accomYes && accomProofFieldIndex >= 0
                                && formFileUris.get(accomProofFieldIndex) == null) {
                            Toast.makeText(this,
                                    "Please attach Accommodation Payment Proof.",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                    } else if ("File Upload".equals(type) && req) {
                        if (formFileUris.get(i) == null) {
                            Toast.makeText(this,
                                    "Please attach file for: " + label,
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                    } else if (req && i < fieldViews.size()) {
                        Object view = fieldViews.get(i);
                        if (view instanceof EditText) {
                            if (((EditText) view).getText().toString().trim().isEmpty()) {
                                ((EditText) view).setError("Required");
                                ((EditText) view).requestFocus();
                                return;
                            }
                        } else if (view instanceof RadioGroup) {
                            if (((RadioGroup) view).getCheckedRadioButtonId() == -1) {
                                Toast.makeText(this,
                                        "Please select an option for: " + label,
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                    }
                }

                // ── Collect answers ─────────────────────────────────────────
                Map<String, Object> answers = new HashMap<>();
                // Record accommodation choice
                if (accomRadioGroup != null) {
                    answers.put("wantsAccommodation", isAccomYesSelected() ? "Yes" : "No");
                }

                for (int i = 0; i < questions.size(); i++) {
                    String label = nvl((String) questions.get(i).get("label"), "Q" + (i + 1));
                    if (i >= fieldViews.size()) continue;
                    Object view = fieldViews.get(i);
                    if (view instanceof EditText) {
                        answers.put(label, ((EditText) view).getText().toString().trim());
                    } else if (view instanceof RadioGroup && view != accomRadioGroup) {
                        int cid = ((RadioGroup) view).getCheckedRadioButtonId();
                        if (cid != -1) {
                            answers.put(label,
                                    ((RadioButton) ((RadioGroup) view).findViewById(cid))
                                            .getText().toString());
                        }
                    } else if (view instanceof Button) {
                        answers.put(label, formFileNames.get(i));
                    }
                }

                dialog.dismiss();
                uploadFilesAndSubmit(answers);
            });
        });

        dialog.show();
    }

    private boolean isAccomYesSelected() {
        if (accomRadioGroup == null) return false;
        int cid = accomRadioGroup.getCheckedRadioButtonId();
        if (cid == -1) return false;
        RadioButton rb = accomRadioGroup.findViewById(cid);
        return rb != null && "Yes".equals(rb.getText().toString());
    }

    // ── Encode images and submit (no Firebase Storage — Base64 in Firestore) ────

    private void uploadFilesAndSubmit(Map<String, Object> answers) {
        boolean accomYes = isAccomYesSelected();

        // Find reg proof Base64
        String regProofBase64   = null;
        String accomProofBase64 = null;

        for (int i = 0; i < currentQuestions.size(); i++) {
            String type = nvl((String) currentQuestions.get(i).get("type"), "");
            String b64  = formFileBase64.get(i);
            if (b64 == null) continue;

            if ("FeeBlock".equals(type)) {
                regProofBase64 = b64;
            } else if ("AccomBlock".equals(type) && accomYes) {
                accomProofBase64 = b64;
            } else if (i == accomProofFieldIndex && accomYes) {
                accomProofBase64 = b64;
            }
        }

        submitRegistration(answers, regProofBase64, accomProofBase64);
    }

    // ── Submit to Firestore ────────────────────────────────────────────────────

    private void submitRegistration(Map<String, Object> answers,
                                    String paymentProofBase64,
                                    String accommodationProofBase64) {
        String attendeeName = username;
        if (attendeeName.contains("_")) {
            attendeeName = attendeeName.substring(attendeeName.indexOf("_") + 1);
        }

        Map<String, Object> reg = new HashMap<>();
        reg.put("eventId",              eventId);
        reg.put("eventTitle",           eventTitle);
        reg.put("eventDate",            startDate);   // saved so MyRegistrations can check if past
        reg.put("studentName",          attendeeName);
        reg.put("studentId",            username);
        reg.put("userId",               userId);
        reg.put("amount",               regFee.isEmpty() ? "Free" : regFee);
        reg.put("accommodationAmount",  accomFee.isEmpty() ? "" : accomFee);
        reg.put("paymentStatus", regFee.isEmpty() ? "Approved" : "Pending");
        reg.put("submittedAt",          System.currentTimeMillis());
        reg.put("answers",              answers);
        reg.put("rejectionReason",      "");
        // Base64 image data — stored directly in Firestore, rendered as ImageView on org side
        reg.put("paymentProofBase64",       paymentProofBase64       != null ? paymentProofBase64       : "");
        reg.put("accommodationProofBase64", accommodationProofBase64  != null ? accommodationProofBase64  : "");
        // Keep URL fields empty (not using Firebase Storage)
        reg.put("paymentProofUrl",       "");
        reg.put("accommodationProofUrl", "");

        db.collection("registrations").add(reg)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this,
                            "Registration submitted! Awaiting payment verification.",
                            Toast.LENGTH_LONG).show();
                    setAlreadyRegisteredState();
                    db.collection("users").document(userId)
                            .collection("registrations").document(eventId).set(reg);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Submission failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    // ── File picker ────────────────────────────────────────────────────────────

    private void openFilePicker() {
        // Images only — Base64 encoded and stored in Firestore (no Firebase Storage needed)
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select Proof Image (JPG/PNG)"), FILE_PICK_RC);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_PICK_RC && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            Uri uri = data.getData();
            String name = uri.getLastPathSegment();
            if (name == null) name = "image";

            // Encode image to Base64 immediately — compress to keep Firestore doc small
            String base64 = encodeImageToBase64(uri);

            if (pendingFormFieldIndex >= 0 && pendingFormFieldIndex < formFileNames.size()) {
                formFileNames.set(pendingFormFieldIndex, name);
                formFileUris.set(pendingFormFieldIndex, uri);
                // Store Base64 in the parallel list (reuse uri slot conceptually)
                if (base64 != null) {
                    // Store in a dedicated base64 map keyed by field index
                    formFileBase64.put(pendingFormFieldIndex, base64);
                }
                Button btn = formFileButtons.get(pendingFormFieldIndex);
                if (btn != null) {
                    btn.setText("✅ Image selected: " + name);
                    btn.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0xFFE8F5E9));
                    btn.setTextColor(0xFF2E7D32);
                }
            }
        }
        pendingFormFieldIndex = -1;
    }

    /**
     * Reads an image URI, scales it down to max 800px wide, compresses to JPEG 70%,
     * and returns a Base64 string. Keeps size well under Firestore's 1MB doc limit.
     */
    private String encodeImageToBase64(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) return null;

            // Decode full bitmap
            Bitmap original = BitmapFactory.decodeStream(is);
            is.close();
            if (original == null) return null;

            // Scale down to max 800px on longest side
            int maxPx = 800;
            int w = original.getWidth(), h = original.getHeight();
            if (w > maxPx || h > maxPx) {
                float scale = Math.min((float) maxPx / w, (float) maxPx / h);
                original = Bitmap.createScaledBitmap(original,
                        Math.round(w * scale), Math.round(h * scale), true);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            original.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] bytes = baos.toByteArray();
            return Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (Exception e) {
            Toast.makeText(this, "Could not read image: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    // ── Calendar ───────────────────────────────────────────────────────────────

    private void addToCalendar() {
        // Store date as ISO "yyyy-MM-dd" so AttendeeCalendarActivity can parse it reliably
        String isoDate = toIsoDate(startDate);

        Map<String, Object> cal = new HashMap<>();
        cal.put("title",    eventTitle);
        cal.put("venue",    venue);
        cal.put("date",     isoDate);
        cal.put("category", eventCategory);
        db.collection("users").document(userId)
                .collection("calendarEvents").document(eventId).set(cal)
                .addOnSuccessListener(v ->
                        Toast.makeText(this, "Added to Calendar!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to add to calendar", Toast.LENGTH_SHORT).show());
    }

    /** Normalises any date string to ISO "yyyy-MM-dd". Falls back to original if unparseable. */
    private String toIsoDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return "";
        dateStr = dateStr.trim();
        SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        iso.setLenient(false);
        try { iso.parse(dateStr); return dateStr; } catch (ParseException ignored) {}
        try {
            SimpleDateFormat dmy = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
            dmy.setLenient(false);
            return iso.format(dmy.parse(dateStr));
        } catch (ParseException ignored) {}
        for (String fmt : new String[]{"MMM d, yyyy", "MMM dd, yyyy", "MMM d yyyy", "MMM dd yyyy"}) {
            try {
                SimpleDateFormat f = new SimpleDateFormat(fmt, Locale.ENGLISH);
                f.setLenient(false);
                Date d = f.parse(dateStr);
                return iso.format(d);
            } catch (ParseException ignored) {}
        }
        return dateStr;
    }

    // ── Already registered ─────────────────────────────────────────────────────

    private void checkIfAlreadyRegistered() {
        db.collection("users").document(userId)
                .collection("registrations").document(eventId).get()
                .addOnSuccessListener(doc -> { if (doc.exists()) setAlreadyRegisteredState(); });
    }

    private void setAlreadyRegisteredState() {
        btnRegister.setEnabled(false);
        btnRegister.setText("Already Registered ✓");
        btnRegister.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFF4CAF50));
    }

    // ── Back ───────────────────────────────────────────────────────────────────

    private void navigateBack() {
        if ("myRegistrations".equals(source)) {
            Intent i = new Intent(this, MyRegistrationsActivity.class);
            i.putExtra("userId", userId); startActivity(i);
        } else {
            Intent i = new Intent(this, EventBrowsingActivity.class);
            i.putExtra("userId", userId); i.putExtra("username", username); startActivity(i);
        }
        finish();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private TextView makeLabelView(String label, boolean required) {
        TextView tv = new TextView(this);
        tv.setText(required ? label + " *" : label);
        tv.setTextSize(13f);
        tv.setTextColor(0xFF333333);
        tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 12;
        tv.setLayoutParams(lp);
        return tv;
    }

    private EditText makeEditText(boolean multiline) {
        EditText et = new EditText(this);
        et.setBackground(getDrawable(R.drawable.bg_input_field));
        et.setPadding(24, 20, 24, 20);
        if (multiline) {
            et.setMinLines(3);
            et.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                    | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            et.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 4; lp.bottomMargin = 4;
        et.setLayoutParams(lp);
        return et;
    }

    private String nvl(String s, String fallback) {
        return (s != null && !s.isEmpty()) ? s : fallback;
    }
    private String nvl(String s) { return s != null ? s : ""; }

    /** Parses deadline and locks the register button if it has passed */
    private void checkDeadline(String deadlineStr) {
        if (deadlineStr == null || deadlineStr.isEmpty()) return;
        String[] formats = {
                "dd/MM/yyyy", "d/M/yyyy", "dd/MM/yy", "d/M/yy",
                "MMM d, yyyy", "MMM dd, yyyy", "yyyy-MM-dd"
        };
        for (String fmt : formats) {
            try {
                java.text.SimpleDateFormat sdf =
                        new java.text.SimpleDateFormat(fmt, java.util.Locale.getDefault());
                sdf.setLenient(false);
                if (fmt.contains("yy") && !fmt.contains("yyyy")) {
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    cal.set(java.util.Calendar.YEAR, 2000);
                    sdf.set2DigitYearStart(cal.getTime());
                }
                java.util.Date deadline = sdf.parse(deadlineStr.trim());
                if (deadline != null && new java.util.Date().after(deadline)) {
                    // Deadline passed — lock the register button
                    if (btnRegister != null) {
                        btnRegister.setEnabled(false);
                        btnRegister.setText("Registration Closed");
                        btnRegister.setBackgroundTintList(
                                android.content.res.ColorStateList.valueOf(0xFF9E9E9E));
                    }
                    if (tvRegCloses != null) {
                        tvRegCloses.setText("Registration closed on " + deadlineStr);
                        tvRegCloses.setTextColor(0xFFE53935);
                    }
                }
                return; // parsed successfully
            } catch (Exception ignored) {}
        }
    }
}