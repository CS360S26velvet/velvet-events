package com.lums.eventhub;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * VendorDirectoryActivity.java
 *
 * Organiser browses all vendors stored in Firestore vendors/ collection.
 * Features:
 *   - Grid of vendor cards (name, category badge, star rating, used-by count)
 *   - Filter tabs: All / Catering / AV & Tech / Printing / Decor / Transport
 *   - Search bar by vendor name
 *   - Tap card → detail bottom sheet (AlertDialog): about, phone, email, address,
 *     past usage history, "Mark as Used for <event>", "Save to Favourites"
 *   - FAB → AddVendorActivity
 *
 * Firestore collection: vendors/
 *   Fields: name, category, rating (double), usedByCount (long), about,
 *           phone, email, address, logoBase64 (optional),
 *           usageHistory (List<Map> with {eventName, date}),
 *           favouritedBy (List<String> of organizerUsernames)
 */
public class VendorDirectoryActivity extends AppCompatActivity {

    private EditText    etSearch;
    private RecyclerView recyclerView;
    private VendorAdapter adapter;
    private final List<Vendor> allVendors      = new ArrayList<>();
    private final List<Vendor> shownVendors    = new ArrayList<>();
    private String currentFilter = "All";
    private String organizerUsername, societyName, currentEventTitle;
    private FirebaseFirestore db;

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

        // Get current active event title for "Mark as Used" button
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
            // Vendor was added — reload list
            loadVendors();
        }
    }

    private void bindViews() {
        etSearch        = findViewById(R.id.etVendorSearch);
        btnAll          = findViewById(R.id.btnFilterAll);
        btnCatering     = findViewById(R.id.btnFilterCatering);
        btnAV           = findViewById(R.id.btnFilterAV);
        btnPrinting     = findViewById(R.id.btnFilterPrinting);
        btnDecor        = findViewById(R.id.btnFilterDecor);
        btnTransport    = findViewById(R.id.btnFilterTransport);
    }

    private void setupFilterButtons() {
        btnAll.setOnClickListener(v      -> applyFilter("All"));
        btnCatering.setOnClickListener(v -> applyFilter("Catering"));
        btnAV.setOnClickListener(v       -> applyFilter("AV & Tech"));
        btnPrinting.setOnClickListener(v -> applyFilter("Printing"));
        btnDecor.setOnClickListener(v    -> applyFilter("Decor"));
        btnTransport.setOnClickListener(v-> applyFilter("Transport"));
        highlightFilter("All");
    }

    private void applyFilter(String filter) {
        currentFilter = filter;
        highlightFilter(filter);
        rebuildShown();
    }

    private void highlightFilter(String active) {
        Button[] btns = {btnAll, btnCatering, btnAV, btnPrinting, btnDecor, btnTransport};
        String[] labels = {"All","Catering","AV & Tech","Printing","Decor","Transport"};
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
                        Vendor v = vendorFromDoc(doc);
                        allVendors.add(v);
                    }
                    rebuildShown();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load vendors", Toast.LENGTH_SHORT).show());
    }

    @SuppressWarnings("unchecked")
    private Vendor vendorFromDoc(QueryDocumentSnapshot doc) {
        Vendor v = new Vendor();
        v.id           = doc.getId();
        v.name         = nvl(doc.getString("name"), "Unnamed Vendor");
        v.category     = nvl(doc.getString("category"), "Other");
        v.about        = nvl(doc.getString("about"), "");
        v.phone        = nvl(doc.getString("phone"), "");
        v.email        = nvl(doc.getString("email"), "");
        v.address      = nvl(doc.getString("address"), "");
        v.logoBase64   = nvl(doc.getString("logoBase64"), "");
        Double rating  = doc.getDouble("rating");
        v.rating       = rating != null ? rating : 0.0;
        Long used      = doc.getLong("usedByCount");
        v.usedByCount  = used != null ? used.intValue() : 0;
        Object hist    = doc.get("usageHistory");
        if (hist instanceof List) {
            v.usageHistory = (List<Map<String, Object>>) hist;
        }
        Object favs = doc.get("favouritedBy");
        if (favs instanceof List) {
            v.favouritedBy = (List<String>) favs;
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

        TextView  tvName        = view.findViewById(R.id.tvVDetailName);
        TextView  tvCategory    = view.findViewById(R.id.tvVDetailCategory);
        TextView  tvRating      = view.findViewById(R.id.tvVDetailRating);
        TextView  tvUsedBy      = view.findViewById(R.id.tvVDetailUsedBy);
        TextView  tvAbout       = view.findViewById(R.id.tvVDetailAbout);
        TextView  tvPhone       = view.findViewById(R.id.tvVDetailPhone);
        TextView  tvEmail       = view.findViewById(R.id.tvVDetailEmail);
        TextView  tvAddress     = view.findViewById(R.id.tvVDetailAddress);
        LinearLayout llHistory  = view.findViewById(R.id.llVDetailHistory);
        ImageView imgLogo       = view.findViewById(R.id.imgVDetailLogo);
        Button    btnMarkUsed   = view.findViewById(R.id.btnMarkUsed);
        Button    btnFavourite  = view.findViewById(R.id.btnSaveFavourite);

        tvName.setText(v.name);
        tvCategory.setText(v.category);
        tvRating.setText(starsFor(v.rating) + "  " + v.rating);
        tvUsedBy.setText("Used by " + v.usedByCount + " societies");
        tvAbout.setText(v.about.isEmpty() ? "No description available." : v.about);
        tvPhone.setText(v.phone.isEmpty() ? "—" : v.phone);
        tvEmail.setText(v.email.isEmpty() ? "—" : v.email);
        tvAddress.setText(v.address.isEmpty() ? "—" : v.address);

        // Logo
        if (!v.logoBase64.isEmpty()) {
            Bitmap bmp = base64ToBitmap(v.logoBase64);
            if (bmp != null) { imgLogo.setImageBitmap(bmp); imgLogo.setVisibility(View.VISIBLE); }
        }

        // Usage history
        llHistory.removeAllViews();
        if (v.usageHistory != null && !v.usageHistory.isEmpty()) {
            for (Map<String, Object> entry : v.usageHistory) {
                View row = LayoutInflater.from(this)
                        .inflate(R.layout.item_usage_history_row, llHistory, false);
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

        // Mark as Used button
        String eventLabel = currentEventTitle != null ? currentEventTitle : societyName + " Event";
        btnMarkUsed.setText("Mark as Used for " + eventLabel);
        btnMarkUsed.setOnClickListener(btn -> markVendorUsed(v, eventLabel));

        // Favourite button
        boolean alreadyFav = v.favouritedBy != null && v.favouritedBy.contains(organizerUsername);
        btnFavourite.setText(alreadyFav ? "★ Saved to Favourites" : "☆ Save to Favourites");
        btnFavourite.setOnClickListener(btn -> toggleFavourite(v, btnFavourite));

        new AlertDialog.Builder(this)
                .setView(view)
                .setNegativeButton("Close", null)
                .show();
    }

    private void markVendorUsed(Vendor v, String eventTitle) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("eventName", eventTitle);
        entry.put("date", new java.text.SimpleDateFormat("MMM yyyy",
                java.util.Locale.getDefault()).format(new java.util.Date()));

        List<Map<String, Object>> history = v.usageHistory != null
                ? new ArrayList<>(v.usageHistory) : new ArrayList<>();
        history.add(0, entry);

        db.collection("vendors").document(v.id)
                .update("usageHistory", history,
                        "usedByCount", v.usedByCount + 1)
                .addOnSuccessListener(u -> {
                    Toast.makeText(this, "Marked as used for " + eventTitle,
                            Toast.LENGTH_SHORT).show();
                    v.usedByCount++;
                    v.usageHistory = history;
                    adapter.notifyDataSetChanged();
                });
    }

    private void toggleFavourite(Vendor v, Button btn) {
        List<String> favs = v.favouritedBy != null
                ? new ArrayList<>(v.favouritedBy) : new ArrayList<>();
        boolean isFav = favs.contains(organizerUsername);
        if (isFav) {
            favs.remove(organizerUsername);
            btn.setText("☆ Save to Favourites");
        } else {
            favs.add(organizerUsername);
            btn.setText("★ Saved to Favourites");
        }
        v.favouritedBy = favs;
        db.collection("vendors").document(v.id).update("favouritedBy", favs);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String starsFor(double rating) {
        int full  = (int) rating;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) sb.append(i < full ? "★" : "☆");
        return sb.toString();
    }

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
        List<String> favouritedBy;
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
            h.tvRating.setText(starsFor(v.rating) + "  " + v.rating);
            h.tvUsedBy.setText("Used by " + v.usedByCount + " societies");

            if (!v.logoBase64.isEmpty()) {
                Bitmap bmp = base64ToBitmap(v.logoBase64);
                if (bmp != null) { h.imgLogo.setImageBitmap(bmp); h.imgLogo.setVisibility(View.VISIBLE); }
            } else {
                h.imgLogo.setVisibility(View.GONE);
            }

            h.btnContact.setOnClickListener(btn -> showVendorDetail(v));
            h.itemView.setOnClickListener(view -> showVendorDetail(v));
        }

        @Override public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView  tvName, tvCategory, tvRating, tvUsedBy;
            ImageView imgLogo;
            Button    btnContact;
            VH(View v) {
                super(v);
                tvName     = v.findViewById(R.id.tvVendorName);
                tvCategory = v.findViewById(R.id.tvVendorCategory);
                tvRating   = v.findViewById(R.id.tvVendorRating);
                tvUsedBy   = v.findViewById(R.id.tvVendorUsedBy);
                imgLogo    = v.findViewById(R.id.imgVendorLogo);
                btnContact = v.findViewById(R.id.btnVendorContact);
            }
        }
    }
}