package com.lums.eventhub.admin.calendar;

import android.os.Bundle;
import android.widget.CalendarView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.lums.eventhub.R;

public class CalendarActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        CalendarView calendarView = findViewById(R.id.calendarView);
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            // In a real app, you would query Firestore for events on this specific date
            String date = dayOfMonth + "/" + (month + 1) + "/" + year;
            Toast.makeText(this, "Events for " + date, Toast.LENGTH_SHORT).show();
        });
    }
}