package com.example.event_management;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class AttendeeActivity extends AppCompatActivity {
    private Button btnBrowseEvents;
    private Button navBrowseEvents;
    private Button btnMyRegistrations;
    private Button navMyRegistrations;
    private Button btnNotifications;
    private Button navNotifs;
    private Button btnCalendar;
    private Button navHome;
    private Button logout;
    private TextView tvRegisteredCount;
    private TextView tvNotifCount;
    private FirebaseFirestore db;
    private String userId = "3khY0RCTezX40llTbDbz"; //will change this later


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.attendee_activity);
        btnBrowseEvents = (Button)findViewById(R.id.btnBrowseEvents);
        navBrowseEvents = (Button)findViewById(R.id.navBrowseEvents);

        btnCalendar     = (Button)findViewById(R.id.btnCalendar);

        btnNotifications= (Button)findViewById(R.id.btnNotifications);
        navNotifs       = (Button)findViewById(R.id.navNotifications);

        btnMyRegistrations=(Button)findViewById(R.id.btnMyRegistrations);
        navMyRegistrations=(Button)findViewById(R.id.navMyRegistrations);

        navHome = (Button)findViewById(R.id.navDashboard);

        logout = (Button)findViewById(R.id.btnLogout);
        tvRegisteredCount = findViewById(R.id.tvRegisteredCount);
        tvNotifCount      = findViewById(R.id.tvNotifCount);
        db                = FirebaseFirestore.getInstance();

        btnBrowseEvents.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AttendeeActivity.this,EventBrowsingActivity.class));
            } //THIS WILL GO TO THE EVENT BROWSING SCREEN!!! I HAVE TO IMPLEMENT THIS
        });
        navBrowseEvents.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AttendeeActivity.this,EventBrowsingActivity.class));
            }//THIS WILL GO TO THE EVENT BROWSING SCREEN!!! I HAVE TO IMPLEMENT THIS
        });
        btnCalendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AttendeeActivity.this,CalendarActivity.class));
            }//THIS CALENDAR ACTIVITY WILL BE IMPLEMENTED BY THE ADMIN
        });
        btnNotifications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AttendeeActivity.this,NotificationsActivity.class));
            }//NOTIFS WHO WILL IMPLEMENT THIS?
        });
        navNotifs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AttendeeActivity.this,NotificationsActivity.class));
            }
        });
        btnMyRegistrations.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AttendeeActivity.this,MyRegistrationsActivity.class));
            }// ????????????????
        });
        navMyRegistrations.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AttendeeActivity.this,MyRegistrationsActivity.class));
            }
        });

        navHome.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                //DO NOTHING ALR ON HOMEPAGE
            }
        });

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AttendeeActivity.this, MainActivity.class));
            }//will go back to main activity which is the login interface
        });

        loadCounts();

        //HOW TO SHOW RECENT ACTIVITY??????????

    }

    /**
     * this function stores how many events the user has registered for and how many notifications the user has received
     */
    private void loadCounts() {
        // Count registrations
        db.collection("users")
                .document(userId)
                .collection("registrations")
                .get()
                .addOnSuccessListener(snapshots -> {
                    tvRegisteredCount.setText(String.valueOf(snapshots.size()));
                });

        // Count unread notifications
        db.collection("users")
                .document(userId)
                .collection("notifications")
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(snapshots -> {
                    tvNotifCount.setText(String.valueOf(snapshots.size()));
                });
    }
}
