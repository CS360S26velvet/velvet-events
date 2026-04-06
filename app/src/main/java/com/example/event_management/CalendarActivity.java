package com.example.event_management;

import android.annotation.SuppressLint;
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
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalendarActivity extends AppCompatActivity {

    LinearLayout calendarGrid, selectedDateEvents, savedEventsList;
    TextView tvMonthYear, tvSelectedDate, tvNoEvents;
    Button btnPrevMonth, btnNextMonth;
    Button navDashboard, navBrowseEvents, navMyRegistrations, navNotifications, btnLogout;

    FirebaseFirestore db;
    String userId; // ← no longer hardcoded

    Calendar currentCalendar = Calendar.getInstance();
    int selectedDay = -1;

    Map<Integer, List<CalendarEvent>> eventsByDay = new HashMap<>();
    List<CalendarEvent> allEvents = new ArrayList<>();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calendar_activity);

        // ← Receive userId from previous activity
        userId = getIntent().getStringExtra("userId");

        calendarGrid       = findViewById(R.id.calendarGrid);
        selectedDateEvents = findViewById(R.id.selectedDateEvents);
        savedEventsList    = findViewById(R.id.upcomingEventsList);
        tvMonthYear        = findViewById(R.id.tvMonthYear);
        tvSelectedDate     = findViewById(R.id.tvSelectedDate);
        tvNoEvents         = findViewById(R.id.tvNoEvents);
        btnPrevMonth       = findViewById(R.id.btnPrevMonth);
        btnNextMonth       = findViewById(R.id.btnNextMonth);
        navDashboard       = findViewById(R.id.navDashboard);
        navBrowseEvents    = findViewById(R.id.navBrowseEvents);
        navMyRegistrations = findViewById(R.id.navMyRegistrations);
        navNotifications   = findViewById(R.id.navNotifications);
        btnLogout          = findViewById(R.id.btnLogout);

        db = FirebaseFirestore.getInstance();

        loadCalendarEventsFromFirebase();

        btnPrevMonth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentCalendar.add(Calendar.MONTH, -1);
                selectedDay = -1;
                updateCalendar();
            }
        });

        btnNextMonth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentCalendar.add(Calendar.MONTH, 1);
                selectedDay = -1;
                updateCalendar();
            }
        });

        navDashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CalendarActivity.this, AttendeeActivity.class);
                intent.putExtra("userId", userId); // ← pass forward
                startActivity(intent);
                finish();
            }
        });

        navBrowseEvents.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CalendarActivity.this, EventBrowsingActivity.class);
                intent.putExtra("userId", userId); // ← pass forward
                startActivity(intent);
            }
        });

        navMyRegistrations.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CalendarActivity.this, MyRegistrationsActivity.class);
                intent.putExtra("userId", userId); // ← pass forward
                startActivity(intent);
            }
        });

        navNotifications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CalendarActivity.this, NotificationsActivity.class);
                intent.putExtra("userId", userId); // ← pass forward
                startActivity(intent);
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(CalendarActivity.this, MainActivity.class));
                finish(); // ← clear from back stack on logout
            }
        });
    }

    private void loadCalendarEventsFromFirebase() {
        db.collection("users").document(userId).collection("calendarEvents").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allEvents.clear();
                    eventsByDay.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        CalendarEvent event = new CalendarEvent();
                        event.eventId  = doc.getId();
                        event.title    = doc.getString("title");
                        event.date     = doc.getString("date");
                        event.time     = doc.getString("time");
                        event.venue    = doc.getString("venue");
                        event.category = doc.getString("category");

                        allEvents.add(event);

                        int day = parseDayFromDate(event.date);
                        if (day != -1) {
                            if (!eventsByDay.containsKey(day)) {
                                eventsByDay.put(day, new ArrayList<>());
                            }
                            eventsByDay.get(day).add(event);
                        }
                    }
                    updateCalendar();
                    renderSavedEventsList();
                }).addOnFailureListener(e ->
                        tvSelectedDate.setText("Failed to load events that you added to the calendar"));
    }

    private int parseDayFromDate(String dateStr) {
        try {
            if (dateStr == null) return -1;
            String[] parts = dateStr.trim().split(" ");
            if (parts.length >= 2) {
                return Integer.parseInt(parts[1].replace(",", ""));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private void updateCalendar() {
        calendarGrid.removeAllViews();

        String[] months = {"January","February","March","April","May","June",
                "July","August","September","October","November","December"};
        int month = currentCalendar.get(Calendar.MONTH);
        int year  = currentCalendar.get(Calendar.YEAR);
        tvMonthYear.setText(months[month] + " " + year);

        Calendar temp = (Calendar) currentCalendar.clone();
        temp.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = temp.get(Calendar.DAY_OF_WEEK) - 1;
        int daysInMonth    = temp.getActualMaximum(Calendar.DAY_OF_MONTH);

        Calendar today = Calendar.getInstance();
        int todayDay   = today.get(Calendar.DAY_OF_MONTH);
        int todayMonth = today.get(Calendar.MONTH);
        int todayYear  = today.get(Calendar.YEAR);

        int dayCounter = 1;

        while (dayCounter <= daysInMonth) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            for (int col = 0; col < 7; col++) {
                TextView cell = new TextView(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, 44, 1f);
                params.setMargins(2, 2, 2, 2);
                cell.setLayoutParams(params);
                cell.setGravity(Gravity.CENTER);
                cell.setTextSize(12);
                cell.setTextColor(0xFF1A1A2E);

                if (dayCounter == 1 && col < firstDayOfWeek) {
                    cell.setText("");
                } else if (dayCounter <= daysInMonth) {
                    final int day = dayCounter;
                    cell.setText(String.valueOf(day));

                    boolean isToday  = (day == todayDay && month == todayMonth && year == todayYear);
                    boolean hasEvent = eventsByDay.containsKey(day);

                    if (isToday) {
                        cell.setBackgroundColor(0xFFDDD8F5);
                        cell.setTextColor(0xFF5B2D8E);
                        cell.setTypeface(null, Typeface.BOLD);
                    }
                    if (hasEvent) {
                        cell.setBackgroundColor(0xFF5B2D8E);
                        cell.setTextColor(0xFFFFFFFF);
                        cell.setTypeface(null, Typeface.BOLD);
                    }

                    cell.setOnClickListener(v -> {
                        selectedDay = day;
                        showEventsForDay(day, months[month], year);
                    });

                    dayCounter++;
                } else {
                    cell.setText("");
                }
                row.addView(cell);
            }
            calendarGrid.addView(row);
        }

        if (selectedDay != -1) {
            showEventsForDay(selectedDay, months[month], year);
        } else {
            tvSelectedDate.setText("Tap a date to see your saved events");
            selectedDateEvents.removeAllViews();
            tvNoEvents.setVisibility(View.GONE);
        }
    }

    private void showEventsForDay(int day, String monthName, int year) {
        selectedDateEvents.removeAllViews();
        tvSelectedDate.setText("Events on " + monthName + " " + day + ", " + year);

        List<CalendarEvent> events = eventsByDay.get(day);
        if (events == null || events.isEmpty()) {
            tvNoEvents.setVisibility(View.VISIBLE);
        } else {
            tvNoEvents.setVisibility(View.GONE);
            for (CalendarEvent event : events) {
                selectedDateEvents.addView(createEventRow(event));
            }
        }
    }

    private void renderSavedEventsList() {
        savedEventsList.removeAllViews();

        if (allEvents.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("You haven't added any events yet.\nBrowse events and tap 'Add to Calendar'!");
            empty.setTextColor(0xFFAAAAAA);
            empty.setTextSize(13);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, 16, 0, 16);
            savedEventsList.addView(empty);
        } else {
            for (CalendarEvent event : allEvents) {
                savedEventsList.addView(createEventRow(event));
            }
        }
    }

    private View createEventRow(CalendarEvent event) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 0, 0, 10);
        row.setLayoutParams(rowParams);
        row.setBackgroundColor(0xFFF8F6FF);
        row.setPadding(12, 12, 12, 12);

        // Colored left bar
        View colorBar = new View(this);
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(4,
                LinearLayout.LayoutParams.MATCH_PARENT);
        barParams.setMargins(0, 0, 12, 0);
        colorBar.setLayoutParams(barParams);
        colorBar.setBackgroundColor("Society Events".equals(event.category) ? 0xFFE91E8C : 0xFF00BCD4);
        row.addView(colorBar);

        // Event info
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvTitle = new TextView(this);
        tvTitle.setText(event.title);
        tvTitle.setTextColor(0xFF1A1A2E);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextSize(13);
        info.addView(tvTitle);

        TextView tvDetails = new TextView(this);
        tvDetails.setText("📅 " + event.date + "   🕐 " + event.time + "   📍 " + event.venue);
        tvDetails.setTextColor(0xFF888888);
        tvDetails.setTextSize(11);
        info.addView(tvDetails);

        row.addView(info);

        return row;
    }
}