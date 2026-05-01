package com.lums.eventhub;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FormBuilderActivity.java  (UPDATED v4)
 *
 * KEY CHANGES:
 *
 * 1. FeeBlock (Registration) — always required=true, locked.
 *    Card shows: "Registration Fee: X", "Bank Info: Y", "Attach Proof [required]"
 *
 * 2. AccomBlock (Accommodation) — always required=true, locked.
 *    Card shows:
 *      MCQ: "Do you want accommodation? (Fee = X)"  →  Yes / No
 *      [Conditional, shown only if Yes selected on attendee side:]
 *        "Accommodation Fee: X"
 *        "Bank Account Information: Y"
 *        "Attach Accommodation Proof [required]"
 *    Saved to Firestore with type="AccomBlock" so EventDetailsActivity
 *    knows to render it conditionally.
 *
 * 3. Save = draft only (formReleased = false).
 *    Release button = sets formReleased = true.
 */
public class FormBuilderActivity extends AppCompatActivity {

    private RecyclerView       recyclerViewQuestions;
    private QuestionAdapter    adapter;
    private final List<FormQuestion> questions = new ArrayList<>();
    private FirebaseFirestore  db;

    private String  eventId, eventName;
    private boolean hasRegFee, hasAccommodation;
    private String  regFee = "", regBankInfo = "";
    private String  accommodationFee = "", accommodationBankInfo = "";

    // File pick for organiser-side preview attachment
    private int    pendingFilePickPosition = -1;
    private static final int FILE_PICK_BASE = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form_builder);

        db        = FirebaseFirestore.getInstance();
        eventId   = getIntent().getStringExtra("eventId");
        eventName = getIntent().getStringExtra("eventName");

        hasRegFee         = getIntent().getBooleanExtra(RegistrationFeeSetupActivity.EXTRA_HAS_REG_FEE,       false);
        regFee            = nvl(getIntent().getStringExtra(RegistrationFeeSetupActivity.EXTRA_REG_FEE));
        regBankInfo       = nvl(getIntent().getStringExtra(RegistrationFeeSetupActivity.EXTRA_REG_BANK_INFO));
        hasAccommodation  = getIntent().getBooleanExtra(RegistrationFeeSetupActivity.EXTRA_HAS_ACCOMMODATION, false);
        accommodationFee  = nvl(getIntent().getStringExtra(RegistrationFeeSetupActivity.EXTRA_ACCOMMODATION_FEE));
        accommodationBankInfo = nvl(getIntent().getStringExtra(RegistrationFeeSetupActivity.EXTRA_ACCOMMODATION_BANK));

        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "No event selected.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView headerTitle = findViewById(R.id.tvFormBuilderTitle);
        if (headerTitle != null) headerTitle.setText("Form Builder — " + eventName);

        setupRecyclerView();
        wireTypeButtons();
        wirePreview();
        wireSave();
        wireRelease();
        loadExistingQuestions();
    }

    private String nvl(String s) { return s != null ? s : ""; }

    // ── RecyclerView ──────────────────────────────────────────────────────────

    private void setupRecyclerView() {
        recyclerViewQuestions = findViewById(R.id.recyclerViewQuestions);
        adapter               = new QuestionAdapter(questions);
        recyclerViewQuestions.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewQuestions.setAdapter(adapter);
    }

    // ── Type buttons ──────────────────────────────────────────────────────────

    private void wireTypeButtons() {
        findViewById(R.id.btnAddShortText).setOnClickListener(v ->
                addQuestion(new FormQuestion("Short Text")));
        findViewById(R.id.btnAddParagraph).setOnClickListener(v ->
                addQuestion(new FormQuestion("Paragraph")));
        findViewById(R.id.btnAddMultiChoice).setOnClickListener(v -> {
            FormQuestion q = new FormQuestion("Multiple Choice");
            q.options.add(""); addQuestion(q);
        });
        findViewById(R.id.btnAddDropdown).setOnClickListener(v -> {
            FormQuestion q = new FormQuestion("Dropdown");
            q.options.add(""); addQuestion(q);
        });
        findViewById(R.id.btnAddDate).setOnClickListener(v ->
                addQuestion(new FormQuestion("Date")));
        findViewById(R.id.btnAddFileUpload).setOnClickListener(v ->
                addQuestion(new FormQuestion("File Upload")));
    }

    private void addQuestion(FormQuestion q) {
        questions.add(q);
        int pos = questions.size() - 1;
        adapter.notifyItemInserted(pos);
        recyclerViewQuestions.scrollToPosition(pos);
    }

    // ── Default locked blocks ─────────────────────────────────────────────────

    /**
     * Builds the default locked fee/accommodation blocks.
     *
     * FeeBlock: Registration payment card
     *   — always required
     *   — contains: fee amount, bank info, proof upload
     *
     * AccomBlock: Accommodation card
     *   — always required (the MCQ part)
     *   — contains: MCQ Yes/No, and if Yes: fee, bank info, proof upload
     *   — EventDetailsActivity renders the conditional reveal on attendee side
     */
    private List<FormQuestion> buildDefaultBlocks() {
        List<FormQuestion> defaults = new ArrayList<>();

        if (hasRegFee) {
            FormQuestion block = new FormQuestion("FeeBlock");
            block.label     = "Registration Payment";
            block.locked    = true;
            block.required  = true;   // always required
            block.feeAmount = regFee;
            block.bankInfo  = regBankInfo;
            block.blockType = "reg";
            defaults.add(block);
        }

        if (hasAccommodation) {
            FormQuestion block = new FormQuestion("AccomBlock");
            block.label     = "Accommodation";
            block.locked    = true;
            block.required  = true;   // MCQ is always required
            block.feeAmount = accommodationFee;
            block.bankInfo  = accommodationBankInfo;
            block.blockType = "accom";
            defaults.add(block);
        }

        return defaults;
    }

    // ── Load from Firestore ───────────────────────────────────────────────────

    private void loadExistingQuestions() {
        db.collection("proposals").document(eventId)
                .collection("formQuestions")
                .orderBy("order")
                .get()
                .addOnSuccessListener(snap -> {
                    questions.clear();
                    questions.addAll(buildDefaultBlocks());

                    for (QueryDocumentSnapshot doc : snap) {
                        if (Boolean.TRUE.equals(doc.getBoolean("locked"))) continue;
                        FormQuestion q = new FormQuestion(
                                nvl(doc.getString("type")).isEmpty() ? "Short Text" : doc.getString("type"));
                        q.label    = nvl(doc.getString("label"));
                        q.required = Boolean.TRUE.equals(doc.getBoolean("required"));
                        q.docId    = doc.getId();
                        Object raw = doc.get("options");
                        if (raw instanceof List) {
                            //noinspection unchecked
                            for (Object o : (List<Object>) raw)
                                q.options.add(o != null ? o.toString() : "");
                        }
                        questions.add(q);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    questions.clear();
                    questions.addAll(buildDefaultBlocks());
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Could not load saved form.", Toast.LENGTH_SHORT).show();
                });
    }

    // ── Preview ───────────────────────────────────────────────────────────────

    private void wirePreview() {
        findViewById(R.id.btnPreview).setOnClickListener(v -> {
            if (questions.isEmpty()) {
                Toast.makeText(this, "Add at least one question to preview.", Toast.LENGTH_SHORT).show();
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("── ").append(eventName).append(" Registration ──\n\n");
            for (FormQuestion q : questions) {
                if (q.isBlock()) {
                    sb.append("── ").append(q.label).append(" [built-in, required] ──\n");
                    if ("accom".equals(q.blockType)) {
                        sb.append("  MCQ: Do you want accommodation? (Fee = ").append(q.feeAmount).append(")\n");
                        sb.append("  If Yes:\n");
                        sb.append("    Accommodation Fee: ").append(q.feeAmount).append("\n");
                        sb.append("    Bank Info: ").append(q.bankInfo.isEmpty() ? "(not entered)" : q.bankInfo).append("\n");
                        sb.append("    Attach Accommodation Proof [required]\n\n");
                    } else {
                        sb.append("  Registration Fee: ").append(q.feeAmount).append("\n");
                        sb.append("  Bank Info: ").append(q.bankInfo.isEmpty() ? "(not entered)" : q.bankInfo).append("\n");
                        sb.append("  Attach Proof [required]\n\n");
                    }
                } else {
                    sb.append("• ").append(q.label.isEmpty() ? "(no label)" : q.label)
                            .append(q.required ? "  *required" : "")
                            .append("\n  Type: ").append(q.type).append("\n\n");
                }
            }
            new AlertDialog.Builder(this)
                    .setTitle("Form Preview")
                    .setMessage(sb.toString())
                    .setPositiveButton("Close", null)
                    .show();
        });
    }

    // ── Save draft ────────────────────────────────────────────────────────────

    private void wireSave() {
        findViewById(R.id.btnSaveForm).setOnClickListener(v -> saveForm(false));
    }

    // ── Release ───────────────────────────────────────────────────────────────

    private void wireRelease() {
        Button btnRelease = findViewById(R.id.btnReleaseForm);
        if (btnRelease == null) return;
        btnRelease.setOnClickListener(v -> {
            if (questions.isEmpty()) {
                Toast.makeText(this, "Add at least one question before releasing.", Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle("Release to Attendees?")
                    .setMessage("This will make the form visible to attendees.")
                    .setPositiveButton("Release", (d, w) -> saveForm(true))
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    // ── Save to Firestore ─────────────────────────────────────────────────────

    private void saveForm(boolean release) {
        db.collection("proposals").document(eventId)
                .collection("formQuestions")
                .get()
                .addOnSuccessListener(existing -> {
                    WriteBatch batch = db.batch();
                    for (QueryDocumentSnapshot doc : existing) batch.delete(doc.getReference());

                    for (int i = 0; i < questions.size(); i++) {
                        FormQuestion q = questions.get(i);
                        Map<String, Object> data = new HashMap<>();
                        data.put("label",     q.label);
                        data.put("type",      q.type);
                        data.put("required",  q.required);
                        data.put("options",   q.options);
                        data.put("order",     i);
                        data.put("locked",    q.locked);
                        data.put("feeAmount", q.feeAmount);
                        data.put("bankInfo",  q.bankInfo);
                        data.put("blockType", q.blockType);
                        batch.set(
                                db.collection("proposals").document(eventId)
                                        .collection("formQuestions").document(),
                                data);
                    }

                    Map<String, Object> update = new HashMap<>();
                    update.put("formActive",   true);
                    update.put("formReleased", release);
                    batch.update(db.collection("proposals").document(eventId), update);

                    batch.commit()
                            .addOnSuccessListener(unused ->
                                    Toast.makeText(this,
                                            release ? "Form released to attendees!" : "Form saved as draft.",
                                            Toast.LENGTH_LONG).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Save failed: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ── File picker (organiser side) ──────────────────────────────────────────

    public void openFilePicker(int position) {
        pendingFilePickPosition = position;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES,
                new String[]{"image/jpeg","image/png","application/pdf",
                        "application/msword",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"});
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select File"),
                FILE_PICK_BASE + (position % 200));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            String name = data.getData().getLastPathSegment();
            if (pendingFilePickPosition >= 0 && pendingFilePickPosition < questions.size()) {
                questions.get(pendingFilePickPosition).pickedFileName =
                        name != null ? name : data.getData().toString();
                adapter.notifyItemChanged(pendingFilePickPosition);
            }
        }
        pendingFilePickPosition = -1;
    }

    // ── FormQuestion model ────────────────────────────────────────────────────

    static class FormQuestion {
        String       type;
        String       label       = "";
        boolean      required    = false;
        List<String> options     = new ArrayList<>();
        String       docId       = null;
        boolean      locked      = false;
        String       feeAmount   = "";
        String       bankInfo    = "";
        String       blockType   = "";   // "reg" | "accom" | ""
        String       pickedFileName = "";

        FormQuestion(String type) { this.type = type; }

        boolean isBlock() { return "FeeBlock".equals(type) || "AccomBlock".equals(type); }

        boolean hasOptions() {
            return type.equals("Multiple Choice") || type.equals("Checkboxes") || type.equals("Dropdown");
        }

        FormQuestion copy() {
            FormQuestion c = new FormQuestion(type);
            c.label = label; c.required = required;
            c.options = new ArrayList<>(options);
            return c;
        }
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    class QuestionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int VT_BLOCK    = 0;
        private static final int VT_QUESTION = 1;
        private final List<FormQuestion> list;

        QuestionAdapter(List<FormQuestion> list) { this.list = list; }

        @Override
        public int getItemViewType(int pos) {
            return list.get(pos).isBlock() ? VT_BLOCK : VT_QUESTION;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inf = LayoutInflater.from(parent.getContext());
            if (viewType == VT_BLOCK) {
                return new BlockVH(inf.inflate(R.layout.item_fee_block, parent, false));
            }
            return new QuestionVH(inf.inflate(R.layout.item_question_editor, parent, false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            FormQuestion q = list.get(position);
            if (holder instanceof BlockVH) bindBlock((BlockVH) holder, q, position);
            else bindQuestion((QuestionVH) holder, q, position);
        }

        // ── Block card ──────────────────────────────────────────────────────

        private void bindBlock(BlockVH h, FormQuestion q, int position) {
            if ("reg".equals(q.blockType)) {
                h.tvBlockTitle.setText("Registration Payment  [built-in, required]");
                h.tvFeeLabel.setText("Registration Fee:");
                // Hide accommodation MCQ row
                h.llAccomConditional.setVisibility(View.GONE);
            } else {
                // AccomBlock — show MCQ info + conditional fields preview
                h.tvBlockTitle.setText("Accommodation  [built-in, required]");
                h.tvFeeLabel.setText("Accommodation Fee:");
                h.llAccomConditional.setVisibility(View.VISIBLE);
                h.tvAccomQuestion.setText(
                        "MCQ: \"Do you want accommodation? (Fee = " + q.feeAmount + ")\"\n"
                                + "  ○ Yes  ○ No\n"
                                + "If Yes → fee, bank info, and proof upload shown to attendee.");
            }

            h.tvFeeAmount.setText(q.feeAmount.isEmpty() ? "(not set)" : q.feeAmount);
            h.tvBankInfo.setText(q.bankInfo.isEmpty() ? "(not entered)" : q.bankInfo);

            String picked = q.pickedFileName;
            h.btnPickFile.setText((picked != null && !picked.isEmpty())
                    ? "📎 " + picked : "Attach Proof — Tap to Upload (required)");
            h.btnPickFile.setOnClickListener(v -> openFilePicker(position));
        }

        // ── Regular question ────────────────────────────────────────────────

        private void bindQuestion(QuestionVH h, FormQuestion q, int position) {
            h.tvQuestionNumber.setText("Q" + (position + 1));

            h.etQuestionLabel.removeTextChangedListener(h.labelWatcher);
            h.etQuestionLabel.setText(q.label);
            h.labelWatcher = new SimpleTextWatcher(s -> q.label = s);
            h.etQuestionLabel.addTextChangedListener(h.labelWatcher);

            h.tvQuestionType.setText(q.type);

            h.swRequired.setOnCheckedChangeListener(null);
            h.swRequired.setChecked(q.required);
            h.swRequired.setOnCheckedChangeListener((btn, checked) -> q.required = checked);

            if ("File Upload".equals(q.type)) {
                h.btnFilePickInline.setVisibility(View.VISIBLE);
                String picked = q.pickedFileName;
                h.btnFilePickInline.setText((picked != null && !picked.isEmpty())
                        ? "📎 " + picked : "Tap to select file");
                h.btnFilePickInline.setOnClickListener(v -> openFilePicker(position));
            } else {
                h.btnFilePickInline.setVisibility(View.GONE);
            }

            if (q.hasOptions()) {
                h.llOptions.setVisibility(View.VISIBLE);
                rebuildOptionRows(h, q);
                h.btnAddOption.setOnClickListener(v -> { q.options.add(""); rebuildOptionRows(h, q); });
            } else {
                h.llOptions.setVisibility(View.GONE);
            }

            h.btnDuplicate.setOnClickListener(v -> {
                int pos = h.getAdapterPosition();
                if (pos < 0) return;
                list.add(pos + 1, list.get(pos).copy());
                notifyItemInserted(pos + 1);
                notifyItemRangeChanged(pos + 1, list.size() - pos - 1);
                recyclerViewQuestions.scrollToPosition(pos + 1);
            });

            h.btnDelete.setOnClickListener(v -> {
                int pos = h.getAdapterPosition();
                if (pos < 0) return;
                list.remove(pos);
                notifyItemRemoved(pos);
                notifyItemRangeChanged(pos, list.size() - pos);
            });
        }

        private void rebuildOptionRows(QuestionVH h, FormQuestion q) {
            LinearLayout ll = h.llOptions;
            int count = ll.getChildCount();
            if (count > 1) ll.removeViews(0, count - 1);
            for (int i = 0; i < q.options.size(); i++) {
                final int idx = i;
                View row = LayoutInflater.from(ll.getContext())
                        .inflate(R.layout.item_option_row, ll, false);
                EditText et  = row.findViewById(R.id.etOptionText);
                Button   rem = row.findViewById(R.id.btnRemoveOption);
                et.setText(q.options.get(i));
                et.addTextChangedListener(new SimpleTextWatcher(s -> {
                    if (idx < q.options.size()) q.options.set(idx, s);
                }));
                rem.setOnClickListener(v -> {
                    if (idx < q.options.size()) { q.options.remove(idx); rebuildOptionRows(h, q); }
                });
                ll.addView(row, ll.getChildCount() - 1);
            }
        }

        @Override public int getItemCount() { return list.size(); }

        // ── ViewHolders ─────────────────────────────────────────────────────

        class BlockVH extends RecyclerView.ViewHolder {
            TextView     tvBlockTitle, tvAccomQuestion, tvFeeLabel, tvFeeAmount, tvBankInfo;
            LinearLayout llAccomConditional;
            Button       btnPickFile;
            BlockVH(View v) {
                super(v);
                tvBlockTitle        = v.findViewById(R.id.tvBlockTitle);
                tvAccomQuestion     = v.findViewById(R.id.tvAccomQuestion);
                tvFeeLabel          = v.findViewById(R.id.tvFeeLabel);
                tvFeeAmount         = v.findViewById(R.id.tvFeeAmount);
                tvBankInfo          = v.findViewById(R.id.tvBankInfo);
                llAccomConditional  = v.findViewById(R.id.llAccomConditional);
                btnPickFile         = v.findViewById(R.id.btnPickFile);
            }
        }

        class QuestionVH extends RecyclerView.ViewHolder {
            TextView     tvQuestionNumber, tvQuestionType;
            EditText     etQuestionLabel;
            Switch       swRequired;
            LinearLayout llOptions;
            Button       btnAddOption, btnDuplicate, btnDelete, btnFilePickInline;
            TextWatcher  labelWatcher;
            QuestionVH(View v) {
                super(v);
                tvQuestionNumber  = v.findViewById(R.id.tvQuestionNumber);
                etQuestionLabel   = v.findViewById(R.id.etQuestionLabel);
                tvQuestionType    = v.findViewById(R.id.tvQuestionType);
                swRequired        = v.findViewById(R.id.swRequired);
                llOptions         = v.findViewById(R.id.llOptions);
                btnAddOption      = v.findViewById(R.id.btnAddOption);
                btnDuplicate      = v.findViewById(R.id.btnDuplicate);
                btnDelete         = v.findViewById(R.id.btnDelete);
                btnFilePickInline = v.findViewById(R.id.btnFilePickInline);
            }
        }
    }

    interface TextCallback { void onText(String s); }

    static class SimpleTextWatcher implements TextWatcher {
        private final TextCallback cb;
        SimpleTextWatcher(TextCallback cb) { this.cb = cb; }
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        public void onTextChanged(CharSequence s, int start, int before, int count) { cb.onText(s.toString()); }
        public void afterTextChanged(Editable s) {}
    }
}