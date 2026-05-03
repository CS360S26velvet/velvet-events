package com.lums.eventhub.attendee.registration;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.lums.eventhub.attendee.feedback.FeedbackActivity;
import com.lums.eventhub.attendee.notifications.NotificationsActivity;
import com.lums.eventhub.R;
import com.lums.eventhub.attendee.dashboard.AttendeeActivity;
import com.lums.eventhub.attendee.events.EventBrowsingActivity;
import com.lums.eventhub.attendee.events.EventDetailsActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * MyRegistrationsActivity.java
 *
 * CHANGE: Uses addSnapshotListener (real-time) instead of .get() so that
 * payment status updates (Approved / Rejected) appear immediately without
 * the attendee needing to leave and re-enter the screen.
 */
public class MyRegistrationsActivity extends AppCompatActivity {

    LinearLayout registrationsList;
    TextView tvTotalCount, tvEmpty;
    Button navDashboard, navBrowseEvents, navMyRegistrations, navNotifications, btnLogout;

    FirebaseFirestore db;
    String userId;
    String username;

    // Real-time listener — must be removed onStop to avoid leaks
    private ListenerRegistration listenerReg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_registrations);

        userId   = getIntent().getStringExtra("userId");
        username = getIntent().getStringExtra("username");
        if (username == null) username = "";

        registrationsList  = findViewById(R.id.registrationsList);
        tvTotalCount       = findViewById(R.id.tvTotalCount);
        tvEmpty            = findViewById(R.id.tvEmpty);
        navDashboard       = findViewById(R.id.navDashboard);
        navBrowseEvents    = findViewById(R.id.navBrowseEvents);
        navMyRegistrations = findViewById(R.id.navMyRegistrations);
        navNotifications   = findViewById(R.id.navNotifications);
        btnLogout          = findViewById(R.id.btnLogout);

        db = FirebaseFirestore.getInstance();

        navDashboard.setOnClickListener(v -> {
            Intent intent = new Intent(MyRegistrationsActivity.this, AttendeeActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        navBrowseEvents.setOnClickListener(v -> {
            Intent intent = new Intent(MyRegistrationsActivity.this, EventBrowsingActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        navMyRegistrations.setOnClickListener(v -> { /* already here */ });

        navNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(MyRegistrationsActivity.this, NotificationsActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            startActivity(new Intent(MyRegistrationsActivity.this,
                    com.lums.eventhub.auth.LoginActivity.class));
            finish();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (listenerReg != null) {
            listenerReg.remove();
            listenerReg = null;
        }
    }

    /**
     * Real-time listener on users/{userId}/registrations.
     * Fires immediately on attach and again whenever any doc changes —
     * so Approved / Rejected updates from admin appear instantly.
     */
    private void startListening() {
        if (listenerReg != null) listenerReg.remove();

        listenerReg = db.collection("users").document(userId)
                .collection("registrations")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) {
                        tvEmpty.setText("Failed to load registrations");
                        return;
                    }

                    registrationsList.removeAllViews();
                    final int total = snapshots.size();

                    if (total == 0) {
                        tvTotalCount.setText("0");
                        tvEmpty.setVisibility(View.VISIBLE);
                        return;
                    }
                    tvEmpty.setVisibility(View.GONE);

                    final int[] count = {0};

                    for (QueryDocumentSnapshot doc : snapshots) {
                        String eventId           = doc.getString("eventId");
                        String title             = doc.getString("eventTitle");
                        String organizer         = doc.getString("organizer");
                        String date              = doc.getString("eventDate");
                        if (date == null || date.isEmpty()) date = doc.getString("startDate");
                        if (date == null || date.isEmpty()) date = doc.getString("date");
                        String venue             = doc.getString("venue");
                        String time              = doc.getString("time");
                        String fee               = doc.getString("fee");
                        String description       = doc.getString("description");
                        String regClosingDate    = doc.getString("RegClosingDate");
                        String category          = doc.getString("category");
                        int seatsBooked          = doc.getLong("seatsBooked") != null ? doc.getLong("seatsBooked").intValue() : 0;
                        int seatsTotal           = doc.getLong("seatsTotal")  != null ? doc.getLong("seatsTotal").intValue()  : 0;
                        String registrationDocId = doc.getString("registrationDocId");

                        final String fEventId     = eventId;
                        final String fTitle       = title;
                        final String fOrganizer   = organizer;
                        final String fDate        = date;
                        final String fVenue       = venue;
                        final String fTime        = time;
                        final String fFee         = fee;
                        final String fDesc        = description;
                        final String fRegClose    = regClosingDate;
                        final String fCategory    = category;
                        final int    fSeatsBooked = seatsBooked;
                        final int    fSeatsTotal  = seatsTotal;

                        if (registrationDocId != null && !registrationDocId.isEmpty()) {
                            // Read live paymentStatus from main registrations/ collection
                            db.collection("registrations").document(registrationDocId).get()
                                    .addOnSuccessListener(mainDoc -> {
                                        String paymentStatus   = "Pending";
                                        String rejectionReason = "";
                                        if (mainDoc.exists()) {
                                            String s = mainDoc.getString("paymentStatus");
                                            String r = mainDoc.getString("rejectionReason");
                                            if (s != null) paymentStatus   = s;
                                            if (r != null) rejectionReason = r;
                                        }
                                        final String ps = paymentStatus;
                                        final String rr = rejectionReason;

                                        if (fDate == null || fDate.isEmpty()) {
                                            db.collection("events").document(fEventId).get()
                                                    .addOnSuccessListener(evDoc -> {
                                                        String evDate = evDoc.getString("startDate");
                                                        if (evDate == null || evDate.isEmpty())
                                                            evDate = evDoc.getString("date");
                                                        registrationsList.addView(
                                                                buildCard(fEventId, fTitle, fOrganizer,
                                                                        evDate, fTime, fVenue, fFee, fDesc,
                                                                        fRegClose, fCategory,
                                                                        fSeatsBooked, fSeatsTotal, ps, rr));
                                                        count[0]++;
                                                        tvTotalCount.setText(String.valueOf(count[0]));
                                                    });
                                        } else {
                                            registrationsList.addView(
                                                    buildCard(fEventId, fTitle, fOrganizer,
                                                            fDate, fTime, fVenue, fFee, fDesc,
                                                            fRegClose, fCategory,
                                                            fSeatsBooked, fSeatsTotal, ps, rr));
                                            count[0]++;
                                            tvTotalCount.setText(String.valueOf(count[0]));
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        String s = doc.getString("paymentStatus");
                                        String r = doc.getString("rejectionReason");
                                        registrationsList.addView(
                                                buildCard(fEventId, fTitle, fOrganizer,
                                                        fDate, fTime, fVenue, fFee, fDesc,
                                                        fRegClose, fCategory,
                                                        fSeatsBooked, fSeatsTotal,
                                                        s != null ? s : "Pending",
                                                        r != null ? r : ""));
                                        count[0]++;
                                        tvTotalCount.setText(String.valueOf(count[0]));
                                    });
                        } else {
                            // No registrationDocId — use mirror copy
                            String paymentStatus   = doc.getString("paymentStatus");
                            String rejectionReason = doc.getString("rejectionReason");
                            if (paymentStatus   == null) paymentStatus   = "Pending";
                            if (rejectionReason == null) rejectionReason = "";
                            registrationsList.addView(
                                    buildCard(fEventId, fTitle, fOrganizer,
                                            fDate, fTime, fVenue, fFee, fDesc,
                                            fRegClose, fCategory,
                                            fSeatsBooked, fSeatsTotal,
                                            paymentStatus, rejectionReason));
                            count[0]++;
                            tvTotalCount.setText(String.valueOf(count[0]));
                        }
                    }
                });
    }

    private View buildCard(String eventId, String title, String organizer, String date,
                           String time, String venue, String fee, String description,
                           String regClosingDate, String category,
                           int seatsBooked, int seatsTotal,
                           String paymentStatus, String rejectionReason) {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(0xFFDDD8F5);
        card.setElevation(4f);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 16);
        card.setLayoutParams(cardParams);
        card.setPadding(16, 16, 16, 16);

        // Title
        TextView tvTitle = new TextView(this);
        tvTitle.setText(title != null ? title : "Event");
        tvTitle.setTextColor(0xFF1A1A2E);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextSize(16);
        card.addView(tvTitle);

        // Payment status badge
        TextView tvStatus = new TextView(this);
        int statusColor, statusBg;
        String statusLabel;
        switch (paymentStatus != null ? paymentStatus : "Pending") {
            case "Approved":
                statusLabel = "✅ Payment Approved";
                statusColor = 0xFF2E7D32; statusBg = 0xFFE8F5E9; break;
            case "Rejected":
                statusLabel = "❌ Payment Rejected";
                statusColor = 0xFFB71C1C; statusBg = 0xFFFFEBEE; break;
            default:
                statusLabel = "⏳ Awaiting Verification";
                statusColor = 0xFFE65100; statusBg = 0xFFFFF3E0; break;
        }
        tvStatus.setText(statusLabel);
        tvStatus.setTextColor(statusColor);
        tvStatus.setBackgroundColor(statusBg);
        tvStatus.setTextSize(12);
        tvStatus.setTypeface(null, Typeface.BOLD);
        tvStatus.setPadding(12, 6, 12, 6);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        statusParams.setMargins(0, 6, 0, 0);
        tvStatus.setLayoutParams(statusParams);
        card.addView(tvStatus);

        // Rejection reason
        if ("Rejected".equals(paymentStatus) && rejectionReason != null && !rejectionReason.isEmpty()) {
            TextView tvReason = new TextView(this);
            tvReason.setText("Reason: " + rejectionReason);
            tvReason.setTextColor(0xFFB71C1C);
            tvReason.setTextSize(12);
            LinearLayout.LayoutParams reasonParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            reasonParams.setMargins(0, 4, 0, 0);
            tvReason.setLayoutParams(reasonParams);
            card.addView(tvReason);
        }

        // Organizer
        TextView tvOrg = new TextView(this);
        tvOrg.setText(organizer != null ? organizer : "");
        tvOrg.setTextColor(0xFF888888);
        tvOrg.setTextSize(12);
        card.addView(tvOrg);

        // Date / time / venue
        TextView tvInfo = new TextView(this);
        tvInfo.setText("📅 " + (date != null ? date : "")
                + "   🕐 " + (time != null ? time : "")
                + "   📍 " + (venue != null ? venue : ""));
        tvInfo.setTextColor(0xFF444444);
        tvInfo.setTextSize(11);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        infoParams.setMargins(0, 8, 0, 12);
        tvInfo.setLayoutParams(infoParams);
        card.addView(tvInfo);

        // Button row
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams btnRowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnRowParams.setMargins(0, 4, 0, 0);
        btnRow.setLayoutParams(btnRowParams);

        Button btnView = new Button(this);
        btnView.setText("View Event");
        btnView.setTextColor(0xFFFFFFFF);
        btnView.setTextSize(13);
        btnView.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFF5B2D8E));
        LinearLayout.LayoutParams viewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        viewParams.setMargins(0, 0, 10, 0);
        btnView.setLayoutParams(viewParams);
        btnView.setOnClickListener(v -> {
            Intent intent = new Intent(this, EventDetailsActivity.class);
            intent.putExtra("userId",           userId);
            intent.putExtra("source",           "myRegistrations");
            intent.putExtra("username",         username);
            intent.putExtra("eventId",          eventId);
            intent.putExtra("eventTitle",       title);
            intent.putExtra("eventOrganizer",   organizer);
            intent.putExtra("eventDate",        date);
            intent.putExtra("eventVenue",       venue);
            intent.putExtra("Time",             time);
            intent.putExtra("fee",              fee);
            intent.putExtra("Description",      description);
            intent.putExtra("RegClosingDate",   regClosingDate);
            intent.putExtra("eventCategory",    category);
            intent.putExtra("eventSeatsBooked", seatsBooked);
            intent.putExtra("eventSeatsTotal",  seatsTotal);
            startActivity(intent);
        });
        btnRow.addView(btnView);

        // Rate Event button for past events
        if (isEventPast(date)) {
            Button btnFeedback = new Button(this);
            btnFeedback.setText("Rate Event");
            btnFeedback.setTextColor(0xFF5B2D8E);
            btnFeedback.setTextSize(13);
            btnFeedback.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFFEDE7F6));
            btnFeedback.setOnClickListener(v -> {
                Intent intent = new Intent(this, FeedbackActivity.class);
                intent.putExtra("userId",         userId);
                intent.putExtra("eventId",        eventId);
                intent.putExtra("eventTitle",     title);
                intent.putExtra("eventOrganizer", organizer);
                intent.putExtra("eventDate",      date);
                startActivity(intent);
            });
            btnRow.addView(btnFeedback);
        }

        card.addView(btnRow);
        return card;
    }

    private boolean isEventPast(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return false;
        String[] formats = {
                "MMM d, yyyy", "MMM dd, yyyy",
                "dd/MM/yyyy", "d/M/yyyy", "dd/MM/yy", "d/M/yy",
                "yyyy-MM-dd", "dd-MM-yyyy"
        };
        for (String fmt : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(fmt, Locale.getDefault());
                sdf.setLenient(false);
                if (fmt.contains("yy") && !fmt.contains("yyyy")) {
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    cal.set(java.util.Calendar.YEAR, 2000);
                    sdf.set2DigitYearStart(cal.getTime());
                }
                Date eventDate = sdf.parse(dateStr.trim());
                if (eventDate != null) return eventDate.before(new Date());
            } catch (Exception ignored) {}
        }
        return false;
    }
}