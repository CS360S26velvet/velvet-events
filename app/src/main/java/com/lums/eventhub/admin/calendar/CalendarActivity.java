package com.lums.eventhub.admin.calendar;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.lums.eventhub.R;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CalendarActivity.java
 *
 * Role: Admin screen that displays a monthly calendar view of all
 * approved events. Each approved proposal is pinned to its date on
 * the calendar grid. Admin can navigate between months and view
 * a full list of all approved events below the grid.
 *
 * Implements: Admin US-08, US-09
 */

public class CalendarActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private LinearLayout llCalendarGrid, llEventsList;
    private TextView tvMonthYear, tvEventsHeader;
    private Calendar displayedMonth;

    // Map of "day" -> list of event titles for quick lookup
    private Map<Integer, List<String>> eventsByDay = new HashMap<>();
    // All approved proposals for this month's list
    private List<Map<String, String>> allApprovedEvents = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        db = FirebaseFirestore.getInstance();

        llCalendarGrid = findViewById(R.id.llCalendarGrid);
        llEventsList   = findViewById(R.id.llEventsList);
        tvMonthYear    = findViewById(R.id.tvMonthYear);
        tvEventsHeader = findViewById(R.id.tvEventsHeader);

        displayedMonth = Calendar.getInstance();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnPrevMonth).setOnClickListener(v -> {
            displayedMonth.add(Calendar.MONTH, -1);
            loadEventsAndRender();
        });
        findViewById(R.id.btnNextMonth).setOnClickListener(v -> {
            displayedMonth.add(Calendar.MONTH, 1);
            loadEventsAndRender();
        });

        loadEventsAndRender();
    }

    private void loadEventsAndRender() {
        int year  = displayedMonth.get(Calendar.YEAR);
        int month = displayedMonth.get(Calendar.MONTH) + 1; // 1-based

        String[] monthNames = {"January","February","March","April","May","June",
                "July","August","September","October","November","December"};
        tvMonthYear.setText(monthNames[month - 1] + " " + year);
        tvEventsHeader.setText("All Events — " + monthNames[month - 1] + " " + year);

        eventsByDay.clear();
        allApprovedEvents.clear();

        db.collection("proposals").whereEqualTo("status", "approved").get()
                .addOnSuccessListener(query -> {
                    for (QueryDocumentSnapshot doc : query) {
                        String dateStr = doc.getString("eventDate"); // format: dd/MM/yyyy
                        if (dateStr == null) continue;

                        try {
                            String[] parts = dateStr.split("/");
                            if (parts.length < 3) continue;
                            int d = Integer.parseInt(parts[0]);
                            int m = Integer.parseInt(parts[1]);
                            int y = Integer.parseInt(parts[2]);

                            // Add to events list regardless of month (show all approved)
                            Map<String, String> event = new HashMap<>();
                            event.put("title",     doc.getString("title"));
                            event.put("organizer", doc.getString("organizerUsername"));
                            event.put("date",      dateStr);
                            event.put("venue",     doc.getString("venue"));
                            event.put("status",    "Approved");
                            allApprovedEvents.add(event);

                            // Pin to calendar only if same month/year
                            if (m == month && y == year) {
                                if (!eventsByDay.containsKey(d)) {
                                    eventsByDay.put(d, new ArrayList<>());
                                }
                                String title = doc.getString("title");
                                eventsByDay.get(d).add(title != null ? title : "Event");
                            }
                        } catch (Exception ignored) {}
                    }

                    renderCalendarGrid(year, month);
                    renderEventsList();
                });
    }

    private void renderCalendarGrid(int year, int month) {
        llCalendarGrid.removeAllViews();

        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, 1);
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1; // 0=Sun
        int daysInMonth    = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        Calendar today = Calendar.getInstance();
        int todayDay   = today.get(Calendar.DAY_OF_MONTH);
        int todayMonth = today.get(Calendar.MONTH) + 1;
        int todayYear  = today.get(Calendar.YEAR);

        int day = 1;
        boolean started = false;

        for (int week = 0; week < 6; week++) {
            if (day > daysInMonth) break;

            LinearLayout weekRow = new LinearLayout(this);
            weekRow.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.bottomMargin = 4;
            weekRow.setLayoutParams(rowParams);

            for (int col = 0; col < 7; col++) {
                FrameLayout cell = new FrameLayout(this);
                LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(
                        0, dpToPx(48), 1f);
                cell.setLayoutParams(cellParams);

                if ((week == 0 && col < firstDayOfWeek) || !started && col < firstDayOfWeek) {
                    // Empty cell
                    if (week == 0) {
                        weekRow.addView(cell);
                        continue;
                    }
                } else {
                    started = true;
                }

                if (!started || day > daysInMonth) {
                    weekRow.addView(cell);
                    continue;
                }

                final int currentDay = day;
                boolean isToday = (currentDay == todayDay && month == todayMonth && year == todayYear);
                boolean hasEvent = eventsByDay.containsKey(currentDay);

                // Day number
                TextView tvDay = new TextView(this);
                FrameLayout.LayoutParams tvParams = new FrameLayout.LayoutParams(
                        dpToPx(36), dpToPx(36));
                tvParams.gravity = Gravity.CENTER;
                tvDay.setLayoutParams(tvParams);
                tvDay.setText(String.valueOf(currentDay));
                tvDay.setGravity(Gravity.CENTER);
                tvDay.setTextSize(13f);

                if (isToday) {
                    tvDay.setBackgroundResource(R.drawable.bg_calendar_today);
                    tvDay.setTextColor(Color.WHITE);
                    tvDay.setTypeface(null, Typeface.BOLD);
                } else if (hasEvent) {
                    tvDay.setBackgroundResource(R.drawable.bg_calendar_event_day);
                    tvDay.setTextColor(Color.parseColor("#17A2B8"));
                    tvDay.setTypeface(null, Typeface.BOLD);
                } else {
                    tvDay.setTextColor(Color.parseColor("#2D1B2E"));
                }

                cell.addView(tvDay);

                // Event dot
                if (hasEvent && !isToday) {
                    View dot = new View(this);
                    FrameLayout.LayoutParams dotParams = new FrameLayout.LayoutParams(
                            dpToPx(6), dpToPx(6));
                    dotParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                    dotParams.bottomMargin = dpToPx(4);
                    dot.setLayoutParams(dotParams);
                    dot.setBackgroundResource(R.drawable.bg_event_dot);
                    cell.addView(dot);
                }

                // Click to show events on that day
                if (hasEvent) {
                    List<String> events = eventsByDay.get(currentDay);
                    cell.setOnClickListener(v -> {
                        StringBuilder sb = new StringBuilder();
                        for (String e : events) sb.append("• ").append(e).append("\n");
                        Toast.makeText(this, sb.toString().trim(), Toast.LENGTH_LONG).show();
                    });
                }

                weekRow.addView(cell);
                day++;
            }
            llCalendarGrid.addView(weekRow);
        }
    }

    private void renderEventsList() {
        llEventsList.removeAllViews();

        if (allApprovedEvents.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("No approved events");
            tv.setTextColor(Color.parseColor("#C4A8B0"));
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(0, dpToPx(24), 0, dpToPx(24));
            llEventsList.addView(tv);
            return;
        }

        for (int i = 0; i < allApprovedEvents.size(); i++) {
            Map<String, String> e = allApprovedEvents.get(i);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));
            row.setBackgroundColor(i % 2 == 0 ? Color.WHITE : Color.parseColor("#F5FFFE"));

            row.addView(makeEventCell(e.get("title"),     2f, true));
            String org = e.get("organizer") != null ? e.get("organizer") : "—";
            if (org.contains("_")) org = org.substring(org.indexOf("_") + 1).toUpperCase();
            row.addView(makeEventCell(org,                1.5f, false));
            row.addView(makeEventCell(e.get("date"),      1.5f, false));
            row.addView(makeEventCell(e.get("venue"),     1.5f, false));

            // Status badge
            TextView tvStatus = new TextView(this);
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tvStatus.setLayoutParams(sp);
            tvStatus.setText("✓ Approved");
            tvStatus.setTextSize(10f);
            tvStatus.setTextColor(Color.parseColor("#27AE60"));
            tvStatus.setTypeface(null, Typeface.BOLD);
            row.addView(tvStatus);

            llEventsList.addView(row);

            // Divider
            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(Color.parseColor("#E0F5F5"));
            llEventsList.addView(divider);
        }
    }

    private TextView makeEventCell(String text, float weight, boolean bold) {
        TextView tv = new TextView(this);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, weight));
        tv.setText(text != null ? text : "—");
        tv.setTextSize(12f);
        tv.setTextColor(Color.parseColor("#2D1B2E"));
        if (bold) tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(0, 0, dpToPx(6), 0);
        return tv;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}