package com.lums.eventhub.organizer.vendors;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.lums.eventhub.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * VendorDirectoryActivity.java
 *
 * CHANGES:
 *   1. Past usage history shows SOCIETY NAME (organizer name) — not event title
 *   2. Phone and email are REQUIRED when adding a vendor (enforced in AddVendorActivity)
 *   3. "Save to Favourites" button REMOVED from detail dialog
 *   4. Top stars rating REMOVED from detail dialog (usedByCount kept)
 *   5. After "Mark as Used", dialog DISMISSES and returns to vendor list
 *
 * Firestore collection: vendors/
 *   Fields: name, category, rating (double), usedByCount (long), about,
 *           phone, email, address, logoBase64 (optional),
 *           usageHistory (List<Map> with {eventName (= societyName), date})
 */
public class VendorDirectoryActivity extends AppCompatActivity {

    private EditText     etSearch;
    private RecyclerView recyclerView;
    private VendorAdapter adapter;
    private final List<Vendor> allVendors   = new ArrayList<>();
    private final List<Vendor> shownVendors = new ArrayList<>();
    private String currentFilter = "All";
    private String organizerUsername, societyName, currentEventTitle;
    private FirebaseFirestore db;

    // Track open dialog so we can dismiss it after marking used
    private AlertDialog currentDialog;

    // Filter buttons
    private Button btnAll, btnCatering, btnAV, btnPrinting, btnDecor, btnTransport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vendor_directory);

        db = FirebaseFirestore.getInstance();
        organizerUsername = getIntent().getStringExtra("organizerUsername");
        societyName       = getIntent().getStringExtra("societyName");
        if (organizerUsername == null) organizerUsername = "ORG0012";
        if (societyName == null)       societyName       = "My Society";

        loadCurrentEventTitle();
        bindViews();
        setupFilterButtons();
        setupSearch();
        setupRecyclerView();
        loadVendors();

        findViewById(R.id.fabAddVendor).setOnClickListener(v -> {
            Intent i = new Intent(this, AddVendorActivity.class);
            i.putExtra("organizerUsername", organizerUsername);
            startActivityForResult(i, 1001);
        });

        findViewById(R.id.btnVendorBack).setOnClickListener(v -> finish());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            loadVendors();
        }
    }

    private void bindViews() {
        etSearch      = findViewById(R.id.etVendorSearch);
        btnAll        = findViewById(R.id.btnFilterAll);
        btnCatering   = findViewById(R.id.btnFilterCatering);
        btnAV         = findViewById(R.id.btnFilterAV);
        btnPrinting   = findViewById(R.id.btnFilterPrinting);
        btnDecor      = findViewById(R.id.btnFilterDecor);
        btnTransport  = findViewById(R.id.btnFilterTransport);
    }

    private void setupFilterButtons() {
        btnAll.setOnClickListener(v       -> applyFilter("All"));
        btnCatering.setOnClickListener(v  -> applyFilter("Catering"));
        btnAV.setOnClickListener(v        -> applyFilter("AV & Tech"));
        btnPrinting.setOnClickListener(v  -> applyFilter("Printing"));
        btnDecor.setOnClickListener(v     -> applyFilter("Decor"));
        btnTransport.setOnClickListener(v -> applyFilter("Transport"));
        highlightFilter("All");
    }

    private void applyFilter(String filter) {
        currentFilter = filter;
        highlightFilter(filter);
        rebuildShown();
    }

    private void highlightFilter(String active) {
        Button[] btns   = {btnAll, btnCatering, btnAV, btnPrinting, btnDecor, btnTransport};
        String[] labels = {"All", "Catering", "AV & Tech", "Printing", "Decor", "Transport"};
        for (int i = 0; i < btns.length; i++) {
            boolean isActive = labels[i].equals(active);
            btns[i].setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    isActive ? 0xFF1565C0 : 0xFFE8EAF6));
            btns[i].setTextColor(isActive ? 0xFFFFFFFF : 0xFF1565C0);
        }
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(Editable s) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                rebuildShown();
            }
        });
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerVendors);
        adapter      = new VendorAdapter(shownVendors);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);
    }

    private void rebuildShown() {
        String query = etSearch.getText().toString().trim().toLowerCase();
        shownVendors.clear();
        for (Vendor v : allVendors) {
            if (!"All".equals(currentFilter) && !currentFilter.equals(v.category)) continue;
            if (!query.isEmpty() && !v.name.toLowerCase().contains(query)) continue;
            shownVendors.add(v);
        }
        adapter.notifyDataSetChanged();
        TextView tvCount = findViewById(R.id.tvVendorCount);
        if (tvCount != null) tvCount.setText(shownVendors.size() + " vendors found");
    }

    // ── Load vendors from Firestore ───────────────────────────────────────────

    private void loadVendors() {
        db.collection("vendors").get()
                .addOnSuccessListener(snap -> {
                    allVendors.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        allVendors.add(vendorFromDoc(doc));
                    }
                    rebuildShown();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load vendors", Toast.LENGTH_SHORT).show());
    }

    @SuppressWarnings("unchecked")
    private Vendor vendorFromDoc(QueryDocumentSnapshot doc) {
        Vendor v     = new Vendor();
        v.id         = doc.getId();
        v.name       = nvl(doc.getString("name"), "Unnamed Vendor");
        v.category   = nvl(doc.getString("category"), "Other");
        v.about      = nvl(doc.getString("about"), "");
        v.phone      = nvl(doc.getString("phone"), "");
        v.email      = nvl(doc.getString("email"), "");
        v.address    = nvl(doc.getString("address"), "");
        v.logoBase64 = nvl(doc.getString("logoBase64"), "");
        Double rating = doc.getDouble("rating");
        v.rating      = rating != null ? rating : 0.0;
        Long used     = doc.getLong("usedByCount");
        v.usedByCount = used != null ? used.intValue() : 0;
        Object hist = doc.get("usageHistory");
        if (hist instanceof List) {
            v.usageHistory = (List<Map<String, Object>>) hist;
        }
        return v;
    }

    private void loadCurrentEventTitle() {
        db.collection("proposals")
                .whereEqualTo("organizerUsername", organizerUsername)
                .whereEqualTo("status", "Approved")
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        currentEventTitle = snap.getDocuments().get(0).getString("title");
                    }
                    if (currentEventTitle == null) currentEventTitle = societyName + " Event";
                });
    }

    // ── Vendor detail dialog ──────────────────────────────────────────────────

    private void showVendorDetail(Vendor v) {
        View view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_vendor_detail, null);

        TextView     tvName    = view.findViewById(R.id.tvVDetailName);
        TextView     tvCat     = view.findViewById(R.id.tvVDetailCategory);
        TextView     tvUsedBy  = view.findViewById(R.id.tvVDetailUsedBy);
        TextView     tvAbout   = view.findViewById(R.id.tvVDetailAbout);
        TextView     tvPhone   = view.findViewById(R.id.tvVDetailPhone);
        TextView     tvEmail   = view.findViewById(R.id.tvVDetailEmail);
        TextView     tvAddress = view.findViewById(R.id.tvVDetailAddress);
        LinearLayout llHistory = view.findViewById(R.id.llVDetailHistory);
        ImageView    imgLogo   = view.findViewById(R.id.imgVDetailLogo);
        Button       btnMarkUsed = view.findViewById(R.id.btnMarkUsed);

        tvName.setText(v.name);
        tvCat.setText(v.category);
        tvUsedBy.setText("Used by " + v.usedByCount + " societies");
        tvAbout.setText(v.about.isEmpty() ? "No description available." : v.about);
        tvPhone.setText(v.phone.isEmpty() ? "—" : v.phone);
        tvEmail.setText(v.email.isEmpty() ? "—" : v.email);
        tvAddress.setText(v.address.isEmpty() ? "—" : v.address);

        // Logo
        if (!v.logoBase64.isEmpty()) {
            Bitmap bmp = base64ToBitmap(v.logoBase64);
            if (bmp != null) {
                imgLogo.setImageBitmap(bmp);
                imgLogo.setVisibility(View.VISIBLE);
            }
        }

        // Usage history — shows SOCIETY NAME in left column
        llHistory.removeAllViews();
        if (v.usageHistory != null && !v.usageHistory.isEmpty()) {
            for (Map<String, Object> entry : v.usageHistory) {
                View row = LayoutInflater.from(this)
                        .inflate(R.layout.item_usage_history_row, llHistory, false);
                // "eventName" field now stores the society name
                ((TextView) row.findViewById(R.id.tvHistoryEvent))
                        .setText(nvl((String) entry.get("eventName"), "—"));
                ((TextView) row.findViewById(R.id.tvHistoryDate))
                        .setText(nvl((String) entry.get("date"), ""));
                llHistory.addView(row);
            }
        } else {
            TextView noHist = new TextView(this);
            noHist.setText("No usage history yet.");
            noHist.setTextColor(0xFF999999);
            noHist.setTextSize(13f);
            llHistory.addView(noHist);
        }

        // Mark as Used — label uses society name
        btnMarkUsed.setText("Mark as Used for " + societyName);
        btnMarkUsed.setOnClickListener(btn -> markVendorUsed(v));

        currentDialog = new AlertDialog.Builder(this)
                .setView(view)
                .setNegativeButton("Close", null)
                .show();
    }

    /**
     * Stores the SOCIETY NAME in the usageHistory entry (not the event title).
     * After saving, dismisses the dialog so the organizer is back on the vendor list.
     */
    private void markVendorUsed(Vendor v) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("eventName", societyName);   // store society name
        entry.put("date", new java.text.SimpleDateFormat("MMM yyyy",
                java.util.Locale.getDefault()).format(new java.util.Date()));

        List<Map<String, Object>> history = v.usageHistory != null
                ? new ArrayList<>(v.usageHistory) : new ArrayList<>();
        history.add(0, entry);

        db.collection("vendors").document(v.id)
                .update("usageHistory", history,
                        "usedByCount", v.usedByCount + 1)
                .addOnSuccessListener(u -> {
                    Toast.makeText(this,
                            "Marked as used for " + societyName,
                            Toast.LENGTH_SHORT).show();
                    v.usedByCount++;
                    v.usageHistory = history;
                    adapter.notifyDataSetChanged();
                    // Dismiss dialog → returns to vendor list
                    if (currentDialog != null && currentDialog.isShowing()) {
                        currentDialog.dismiss();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Bitmap base64ToBitmap(String b64) {
        try {
            byte[] bytes = Base64.decode(b64, Base64.NO_WRAP);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) { return null; }
    }

    private String nvl(String s, String fallback) {
        return (s != null && !s.isEmpty()) ? s : fallback;
    }

    // ── Vendor model ──────────────────────────────────────────────────────────

    static class Vendor {
        String id, name, category, about, phone, email, address, logoBase64;
        double rating;
        int    usedByCount;
        List<Map<String, Object>> usageHistory;
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    class VendorAdapter extends RecyclerView.Adapter<VendorAdapter.VH> {

        private final List<Vendor> list;
        VendorAdapter(List<Vendor> list) { this.list = list; }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_vendor_card, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int position) {
            Vendor v = list.get(position);
            h.tvName.setText(v.name);
            h.tvCategory.setText(v.category);
            h.tvUsedBy.setText("Used by " + v.usedByCount + " societies");

            if (!v.logoBase64.isEmpty()) {
                Bitmap bmp = base64ToBitmap(v.logoBase64);
                if (bmp != null) {
                    h.imgLogo.setImageBitmap(bmp);
                    h.imgLogo.setVisibility(View.VISIBLE);
                }
            } else {
                h.imgLogo.setVisibility(View.GONE);
            }

            h.btnContact.setOnClickListener(btn -> showVendorDetail(v));
            h.itemView.setOnClickListener(view -> showVendorDetail(v));
        }

        @Override public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView  tvName, tvCategory, tvUsedBy;
            ImageView imgLogo;
            Button    btnContact;
            VH(View v) {
                super(v);
                tvName     = v.findViewById(R.id.tvVendorName);
                tvCategory = v.findViewById(R.id.tvVendorCategory);
                tvUsedBy   = v.findViewById(R.id.tvVendorUsedBy);
                imgLogo    = v.findViewById(R.id.imgVendorLogo);
                btnContact = v.findViewById(R.id.btnVendorContact);
            }
        }
    }
}