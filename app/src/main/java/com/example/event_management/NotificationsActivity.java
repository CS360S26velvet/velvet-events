package com.example.event_management;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    LinearLayout notificationsList;
    TextView tvUnreadCount, tvEmpty;
    Button btnMarkAllRead, navDashboard, navBrowseEvents, navMyRegistrations, navNotifications, btnLogout;

    FirebaseFirestore db;

    String userId = "3khY0RCTezX40llTbDbz";

    List<Notification> allNotifications = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notifications_activity);

        notificationsList  = findViewById(R.id.notificationsList);
        tvUnreadCount      = findViewById(R.id.tvUnreadCount);
        tvEmpty            = findViewById(R.id.tvEmpty);
        navDashboard       = findViewById(R.id.navDashboard);
        navBrowseEvents    = findViewById(R.id.navBrowseEvents);
        navMyRegistrations = findViewById(R.id.navMyRegistrations);
        navNotifications   = findViewById(R.id.navNotifications);
        btnLogout          = findViewById(R.id.btnLogout);

        db = FirebaseFirestore.getInstance();

        loadNotifications();

        // Bottom nav
        navDashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(NotificationsActivity.this, AttendeeActivity.class));
            }
        });

        navBrowseEvents.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               startActivity(new Intent(NotificationsActivity.this, EventBrowsingActivity.class));

           }
       });
        navMyRegistrations.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                  startActivity(new Intent(NotificationsActivity.this, MyRegistrationsActivity.class));

              }
        });
        navNotifications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //alr here
            }
        });
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(NotificationsActivity.this, MainActivity.class));
            }
        });
    }

    /**
     * This fucntion loads data from firebase
     */
    private void loadNotifications() {
        db.collection("users")
                .document(userId)
                .collection("notifications")
                .addSnapshotListener((snapshots, error) -> {
                    // Stop if there was an error
                    if (error != null || snapshots == null) {
                        tvEmpty.setText("Failed to load notifications");
                        return;
                    }

                    // Reload everything fresh whenever Firebase changes
                    allNotifications.clear();

                    for (QueryDocumentSnapshot doc : snapshots) {
                        Notification notif = new Notification();
                        notif.notifId = doc.getId();
                        notif.title   = doc.getString("title");
                        notif.message = doc.getString("message");
                        notif.type    = doc.getString("type");
                        notif.eventId = doc.getString("eventId");
                        notif.sentAt  = doc.getString("sentAt");

                        // Safely read isRead whether stored as boolean or string
                        Object isReadVal = doc.get("isRead");
                        if (isReadVal instanceof Boolean) {
                            notif.isRead = (Boolean) isReadVal;
                        } else if (isReadVal instanceof String) {
                            notif.isRead = "true".equals(isReadVal);
                        } else {
                            notif.isRead = false;
                        }

                        allNotifications.add(notif);
                    }

                    displayNotifications();
                });
    }

    /**
     * displays the notifs
     */
    private void displayNotifications() {
        notificationsList.removeAllViews();

        if (allNotifications.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            tvUnreadCount.setText("0");
            return;
        }

        tvEmpty.setVisibility(View.GONE);

        // Count unread
        int unread = 0;
        for (Notification n : allNotifications) {
            if (!n.isRead) unread++;
        }
        tvUnreadCount.setText(String.valueOf(unread));

        for (Notification notif : allNotifications) {
            notificationsList.addView(buildNotifCard(notif));
        }
    }

    /**
     * this function makes a single card to display a notification
     * @param notif
     * @return
     */
    private View buildNotifCard(Notification notif) {
        // Outer card
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setElevation(2f);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 12);
        card.setLayoutParams(cardParams);
        card.setPadding(16, 16, 16, 16);

        // Top row: icon + title + unread dot
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams topParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        topParams.setMargins(0, 0, 0, 6);
        topRow.setLayoutParams(topParams);

        // Icon box
        TextView tvIcon = new TextView(this);
        tvIcon.setLayoutParams(new LinearLayout.LayoutParams(40, 40));
        tvIcon.setGravity(Gravity.CENTER);
        tvIcon.setTextSize(16);

        // Set icon and color based on type
        switch (notif.type != null ? notif.type : "") {
            case "confirmation":
            case "payment_received":
                tvIcon.setText("✅");
                tvIcon.setBackgroundColor(0xFF4CAF50); // green
                break;
            case "emergency":
            case "change":
                tvIcon.setText("⚠");
                tvIcon.setBackgroundColor(0xFFFF9800); // orange
                break;
            case "rejection":
            case "payment_rejected":
                tvIcon.setText("✕");
                tvIcon.setBackgroundColor(0xFFE53935); // red
                tvIcon.setTextColor(0xFFFFFFFF);
                break;
            case "reminder":
            default:
                tvIcon.setText("🕐");
                tvIcon.setBackgroundColor(0xFF29B6F6); // blue
                break;
        }

        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(40, 40);
        iconParams.setMargins(0, 0, 12, 0);
        tvIcon.setLayoutParams(iconParams);
        topRow.addView(tvIcon);

        // Title
        TextView tvTitle = new TextView(this);
        tvTitle.setText(notif.title != null ? notif.title : "Notification");
        tvTitle.setTextColor(0xFF1A1A2E);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextSize(14);
        tvTitle.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        topRow.addView(tvTitle);


        card.addView(topRow);

        // Message
        TextView tvMessage = new TextView(this);
        tvMessage.setText(notif.message != null ? notif.message : "");
        tvMessage.setTextColor(0xFF444444);
        tvMessage.setTextSize(12);
        LinearLayout.LayoutParams msgParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        msgParams.setMargins(52, 0, 0, 6); // indent to align with title
        tvMessage.setLayoutParams(msgParams);
        card.addView(tvMessage);

        // Time sent
        TextView tvTime = new TextView(this);
        tvTime.setText(notif.sentAt != null ? notif.sentAt : "");
        tvTime.setTextColor(0xFF888888);
        tvTime.setTextSize(11);
        LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        timeParams.setMargins(52, 0, 0, 10);
        tvTime.setLayoutParams(timeParams);
        card.addView(tvTime);

        // Bottom buttons row
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);
        btnRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        card.addView(btnRow);

        return card;
    }

}